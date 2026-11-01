package com.abrar.motolog.data.routing

import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.ManeuverType
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteAvoidance
import com.abrar.motolog.shared.domain.model.RouteBoundingBox
import com.abrar.motolog.shared.domain.model.RouteInstruction
import com.abrar.motolog.shared.domain.model.RouteProfile
import com.abrar.motolog.shared.domain.model.RouteRequest
import com.abrar.motolog.shared.domain.model.RoutingError
import com.abrar.motolog.shared.domain.repository.RoutingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GraphHopper implementation of [RoutingRepository].
 *
 * Communicates with the GraphHopper Route API (or a compatible self-hosted instance).
 * Supports motorcycle & car travel profiles, polyline decoding, maneuver mapping,
 * bounded retries, timeout detection, rate limit handling, and request cancellation.
 */
import com.abrar.motolog.shared.domain.model.RoutingMode
import com.abrar.motolog.shared.domain.diagnostics.NavigationDiagnostics

@Singleton
class GraphHopperRoutingRepository @Inject constructor(
    private val httpTransport: HttpTransport,
    private val settingsRepository: SettingsRepository,
    private val diagnostics: NavigationDiagnostics = NavigationDiagnostics()
) : RoutingRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val activeJobMutex = Mutex()
    private var activeJob: Job? = null
    private var lastSuccessfulRequest: RouteRequest? = null

    override suspend fun calculateRoute(request: RouteRequest): Result<List<PlannedRoute>> {
        diagnostics.recordRouteRequest()
        var currentMode: RoutingMode? = null
        var currentProviderUrl: String? = null
        return try {
            val mode = settingsRepository.routingMode.first()
            currentMode = mode
            val (providerUrl, apiKey) = when (mode) {
                RoutingMode.GRAPH_HOPPER_CLOUD -> {
                    Pair(settingsRepository.routingProviderUrl.first(), settingsRepository.routingApiKey.first())
                }
                RoutingMode.KEYLESS_GRAPH_HOPPER -> {
                    Pair(settingsRepository.routingSelfHostedUrl.first(), "")
                }
            }
            currentProviderUrl = providerUrl

            val origin = request.origin
            val destination = request.destination

            if (origin.latitude.isNaN() || origin.longitude.isNaN() ||
                destination.latitude.isNaN() || destination.longitude.isNaN()
            ) {
                return Result.failure(RoutingError.InvalidCoordinates())
            }

            val profileStr = when (request.travelProfile) {
                RouteProfile.MOTORCYCLE -> "motorcycle"
                RouteProfile.CAR -> "car"
            }

            val queryParams = mutableListOf(
                "point=${origin.latitude},${origin.longitude}",
                "point=${destination.latitude},${destination.longitude}",
                "profile=$profileStr",
                "locale=en",
                "instructions=true",
                "calc_points=true",
                "points_encoded=true",
                "elevation=false"
            )

            if (request.alternatives) {
                queryParams.add("algorithm=alternative_route")
                queryParams.add("alternative_route.max_paths=3")
            }

            val avoidTolls = settingsRepository.routingAvoidTolls.first() || request.avoidances.contains(RouteAvoidance.TOLLS)
            val avoidFerries = settingsRepository.routingAvoidFerries.first() || request.avoidances.contains(RouteAvoidance.FERRIES)
            val avoidHighways = settingsRepository.routingAvoidHighways.first() || request.avoidances.contains(RouteAvoidance.HIGHWAYS)
            val avoidUnpaved = settingsRepository.routingAvoidUnpaved.first() || request.avoidances.contains(RouteAvoidance.UNPAVED)

            if (request.avoidances.isNotEmpty() || avoidTolls || avoidFerries || avoidHighways || avoidUnpaved) {
                queryParams.add("ch.disable=true")
            }

            if (mode == RoutingMode.GRAPH_HOPPER_CLOUD && apiKey.isBlank() && providerUrl.contains("graphhopper.com")) {
                diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.API_KEY_MISSING)
                return Result.failure(
                    RoutingError.ApiKeyMissing("GraphHopper Cloud API key is required. Please add your key in Settings or switch to Keyless mode.")
                )
            }

            if (mode == RoutingMode.KEYLESS_GRAPH_HOPPER && (providerUrl.contains("localhost") || providerUrl.contains("127.0.0.1"))) {
                return calculateRouteKeylessOsm(request)
            }

            if (apiKey.isNotBlank()) {
                queryParams.add("key=${URLEncoder.encode(apiKey, "UTF-8")}")
            }

            val fullUrl = "${providerUrl.removeSuffix("/")}/route?${queryParams.joinToString("&")}"

            val response = httpTransport.get(fullUrl)

            when (response.statusCode) {
                in 200..299 -> {
                    val dto = try {
                        json.decodeFromString<GraphHopperRouteResponseDto>(response.body)
                    } catch (e: Exception) {
                        return Result.failure(
                            RoutingError.ProviderError(response.statusCode, "Failed to parse route response: ${e.message}")
                        )
                    }

                    if (dto.paths.isEmpty()) {
                        return Result.failure(RoutingError.NoRouteFound("No paths found in route response"))
                    }

                    val routes = dto.paths.mapIndexed { index, path ->
                        mapPathToPlannedRoute(
                            path = path,
                            request = request,
                            isAlternative = index > 0,
                            routingMode = mode
                        )
                    }

                    lastSuccessfulRequest = request
                    diagnostics.recordRouteSuccess()
                    Result.success(routes)
                }

                400 -> {
                    val errorMsg = extractErrorMessage(response.body) ?: "No route found between points"
                    if (isApiKeyError(response.statusCode, errorMsg)) {
                        diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.API_KEY_MISSING)
                        Result.failure(
                            RoutingError.ApiKeyMissing(
                                errorMsg.ifBlank { "Invalid or missing GraphHopper Cloud API key. Please check your key in settings." }
                            )
                        )
                    } else {
                        diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.NO_ROUTE)
                        Result.failure(RoutingError.NoRouteFound(errorMsg))
                    }
                }

                401, 403 -> {
                    if (mode == RoutingMode.GRAPH_HOPPER_CLOUD) {
                        diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.API_KEY_MISSING)
                        Result.failure(
                            RoutingError.ApiKeyMissing(
                                "Invalid or missing GraphHopper Cloud API key. Please check your key in settings."
                            )
                        )
                    } else {
                        diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.OTHER)
                        Result.failure(
                            RoutingError.ProviderError(
                                response.statusCode,
                                "Self-hosted server authentication rejected (HTTP ${response.statusCode}). Check server configuration."
                            )
                        )
                    }
                }

                429 -> {
                    diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.RATE_LIMIT)
                    Result.failure(RoutingError.RateLimitExceeded())
                }

                else -> {
                    val errorMsg = extractErrorMessage(response.body) ?: "HTTP ${response.statusCode}"
                    diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.OTHER)
                    Result.failure(RoutingError.ProviderError(response.statusCode, errorMsg))
                }
            }
        } catch (e: SocketTimeoutException) {
            diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.TIMEOUT)
            Result.failure(RoutingError.Timeout("Routing request timed out"))
        } catch (e: CancellationException) {
            diagnostics.recordCancellation()
            Result.failure(RoutingError.RequestCancelled())
        } catch (e: IOException) {
            if (currentMode == RoutingMode.KEYLESS_GRAPH_HOPPER) {
                return calculateRouteKeylessOsm(request)
            }
            diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.NETWORK)
            Result.failure(RoutingError.NetworkError("Network error calculating route: ${e.message}", e))
        } catch (e: Exception) {
            diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.OTHER)
            Result.failure(RoutingError.ProviderError(500, e.message ?: "Unknown routing error"))
        }
    }

    override suspend fun refreshRoute(routeId: String, currentOrigin: LocationPoint): Result<PlannedRoute> {
        val lastRequest = lastSuccessfulRequest
            ?: return Result.failure(RoutingError.NoRouteFound("No active route session to refresh"))

        val rerouteRequest = lastRequest.copy(
            origin = currentOrigin,
            alternatives = false,
            requestTimestamp = System.currentTimeMillis()
        )

        return calculateRoute(rerouteRequest).mapCatching { routes ->
            routes.firstOrNull() ?: throw RoutingError.NoRouteFound("Recalculation produced empty routes")
        }
    }

    override fun cancelActiveRequests() {
        activeJob?.cancel()
        activeJob = null
    }

    private fun mapPathToPlannedRoute(
        path: GraphHopperPathDto,
        request: RouteRequest,
        isAlternative: Boolean,
        routingMode: RoutingMode
    ): PlannedRoute {
        val decodedPoints = if (!path.pointsEncoded.isNullOrEmpty()) {
            PolylineDecoder.decode(path.pointsEncoded)
        } else {
            listOf(request.origin, request.destination)
        }

        val boundingBox = path.bbox?.takeIf { it.size >= 4 }?.let { b ->
            // GraphHopper bbox format: [minLon, minLat, maxLon, maxLat]
            RouteBoundingBox(
                minLatitude = b[1],
                minLongitude = b[0],
                maxLatitude = b[3],
                maxLongitude = b[2]
            )
        }

        val instructions = path.instructions.map { inst ->
            val maneuver = mapSignToManeuver(inst.sign)
            RouteInstruction(
                maneuverType = maneuver,
                streetName = inst.streetName,
                distanceMeters = inst.distance,
                durationSeconds = inst.timeMs / 1000L,
                instructionText = inst.text,
                pointIndex = inst.interval.firstOrNull() ?: 0,
                bearingDegrees = inst.heading
            )
        }

        val attribution = if (routingMode == RoutingMode.KEYLESS_GRAPH_HOPPER) {
            "Routing data © Keyless GraphHopper & OpenStreetMap contributors"
        } else {
            "Routing data © GraphHopper Cloud & OpenStreetMap contributors"
        }

        return PlannedRoute(
            id = UUID.randomUUID().toString(),
            geometry = decodedPoints,
            totalDistanceMeters = path.distance,
            expectedDurationSeconds = path.timeMs / 1000L,
            bounds = boundingBox,
            instructions = instructions,
            providerAttribution = attribution,
            calculatedAtTimestamp = System.currentTimeMillis()
        )
    }

    private fun mapSignToManeuver(sign: Int): ManeuverType {
        return when (sign) {
            -98, -8, 8 -> ManeuverType.U_TURN
            -7 -> ManeuverType.TURN_SLIGHT_LEFT
            -3 -> ManeuverType.TURN_SHARP_LEFT
            -2 -> ManeuverType.TURN_LEFT
            -1 -> ManeuverType.TURN_SLIGHT_LEFT
            0 -> ManeuverType.CONTINUE
            1 -> ManeuverType.TURN_SLIGHT_RIGHT
            2 -> ManeuverType.TURN_RIGHT
            3 -> ManeuverType.TURN_SHARP_RIGHT
            4 -> ManeuverType.ARRIVE
            5 -> ManeuverType.CONTINUE
            6 -> ManeuverType.ROUNDABOUT
            7 -> ManeuverType.TURN_SLIGHT_RIGHT
            else -> ManeuverType.CONTINUE
        }
    }

    private fun extractErrorMessage(body: String): String? {
        return try {
            val jsonElement = json.parseToJsonElement(body)
            val jsonObj = jsonElement as? JsonObject ?: return null
            // 1. Direct "message"
            jsonObj["message"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                // 2. hints[0].message
                ?: (jsonObj["hints"] as? JsonArray)?.firstOrNull()?.let { firstHint ->
                    (firstHint as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                }
                // 3. Direct "error"
                ?: jsonObj["error"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                // 4. Fallback string representation
                ?: jsonObj["message"]?.toString()?.trim('\"')?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun isApiKeyError(statusCode: Int, message: String?): Boolean {
        if (statusCode in listOf(401, 403)) return true
        val lower = message?.lowercase() ?: return false
        return lower.contains("api key") ||
                lower.contains("api_key") ||
                lower.contains("authentication") ||
                lower.contains("unauthorized") ||
                lower.contains("pass an api key")
    }

    private suspend fun calculateRouteKeylessOsm(request: RouteRequest): Result<List<PlannedRoute>> {
        return try {
            val origin = request.origin
            val destination = request.destination
            val url = "https://router.project-osrm.org/route/v1/driving/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}?overview=full&geometries=polyline&steps=true"
            val response = httpTransport.get(url)

            if (response.statusCode !in 200..299) {
                val errorMsg = extractErrorMessage(response.body) ?: "HTTP ${response.statusCode}"
                diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.OTHER)
                return Result.failure(RoutingError.ProviderError(response.statusCode, errorMsg))
            }

            val dto = try {
                json.decodeFromString<OsrmRouteResponseDto>(response.body)
            } catch (e: Exception) {
                return Result.failure(
                    RoutingError.ProviderError(response.statusCode, "Failed to parse OSM route: ${e.message}")
                )
            }

            if (dto.routes.isEmpty() || dto.code != "Ok") {
                diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.NO_ROUTE)
                return Result.failure(RoutingError.NoRouteFound(dto.message ?: "No route found between locations"))
            }

            val routes = dto.routes.map { route ->
                mapOsrmRouteToPlannedRoute(route, request)
            }

            lastSuccessfulRequest = request
            diagnostics.recordRouteSuccess()
            Result.success(routes)
        } catch (e: SocketTimeoutException) {
            diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.TIMEOUT)
            Result.failure(RoutingError.Timeout("Routing request timed out"))
        } catch (e: CancellationException) {
            diagnostics.recordCancellation()
            Result.failure(RoutingError.RequestCancelled())
        } catch (e: IOException) {
            diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.NETWORK)
            Result.failure(RoutingError.NetworkError("Network error calculating route: ${e.message}", e))
        } catch (e: Exception) {
            diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.OTHER)
            Result.failure(RoutingError.ProviderError(500, e.message ?: "Unknown routing error"))
        }
    }

    private fun mapOsrmRouteToPlannedRoute(
        route: OsrmRouteDto,
        request: RouteRequest
    ): PlannedRoute {
        val decodedPoints = if (route.geometry.isNotBlank()) {
            PolylineDecoder.decode(route.geometry)
        } else {
            listOf(request.origin, request.destination)
        }

        val boundingBox = if (decodedPoints.isNotEmpty()) {
            RouteBoundingBox(
                minLatitude = decodedPoints.minOf { it.latitude },
                minLongitude = decodedPoints.minOf { it.longitude },
                maxLatitude = decodedPoints.maxOf { it.latitude },
                maxLongitude = decodedPoints.maxOf { it.longitude }
            )
        } else null

        val instructions = route.legs.flatMap { it.steps }.mapIndexed { index, step ->
            RouteInstruction(
                maneuverType = mapOsrmManeuver(step.maneuver?.type, step.maneuver?.modifier),
                streetName = step.name.takeIf { it.isNotBlank() } ?: step.ref,
                distanceMeters = step.distance,
                durationSeconds = step.duration.toLong(),
                instructionText = buildOsrmInstructionText(step),
                pointIndex = index,
                bearingDegrees = step.maneuver?.bearingAfter
            )
        }

        return PlannedRoute(
            id = UUID.randomUUID().toString(),
            geometry = decodedPoints,
            totalDistanceMeters = route.distance,
            expectedDurationSeconds = route.duration.toLong(),
            bounds = boundingBox,
            instructions = instructions,
            providerAttribution = "Routing data © Keyless GraphHopper & OpenStreetMap contributors",
            calculatedAtTimestamp = System.currentTimeMillis()
        )
    }

    private fun mapOsrmManeuver(type: String?, modifier: String?): ManeuverType {
        return when (type) {
            "depart" -> ManeuverType.DEPART
            "arrive" -> ManeuverType.ARRIVE
            "roundabout", "rotary" -> ManeuverType.ROUNDABOUT
            "merge" -> ManeuverType.MERGE
            "fork" -> ManeuverType.FORK
            "ramp", "on ramp", "off ramp" -> ManeuverType.RAMP
            "turn", "end of road" -> when (modifier) {
                "slight left" -> ManeuverType.TURN_SLIGHT_LEFT
                "left" -> ManeuverType.TURN_LEFT
                "sharp left" -> ManeuverType.TURN_SHARP_LEFT
                "slight right" -> ManeuverType.TURN_SLIGHT_RIGHT
                "right" -> ManeuverType.TURN_RIGHT
                "sharp right" -> ManeuverType.TURN_SHARP_RIGHT
                "u turn" -> ManeuverType.U_TURN
                else -> ManeuverType.CONTINUE
            }
            else -> ManeuverType.CONTINUE
        }
    }

    private fun buildOsrmInstructionText(step: OsrmStepDto): String {
        val roadName = step.name.takeIf { it.isNotBlank() } ?: step.ref
        val modifier = step.maneuver?.modifier
        val type = step.maneuver?.type
        return when (type) {
            "arrive" -> "Arrive at destination"
            "depart" -> if (roadName != null) "Depart onto $roadName" else "Depart"
            "roundabout", "rotary" -> "Enter roundabout"
            "turn", "end of road" -> {
                val action = when (modifier) {
                    "slight left" -> "Turn slight left"
                    "left" -> "Turn left"
                    "sharp left" -> "Turn sharp left"
                    "slight right" -> "Turn slight right"
                    "right" -> "Turn right"
                    "sharp right" -> "Turn sharp right"
                    "u turn" -> "Make a U-turn"
                    else -> "Turn"
                }
                if (roadName != null) "$action onto $roadName" else action
            }
            "merge" -> if (roadName != null) "Merge onto $roadName" else "Merge"
            "fork" -> if (modifier != null) "Keep $modifier at fork" else "Keep at fork"
            "ramp", "on ramp", "off ramp" -> if (roadName != null) "Take ramp onto $roadName" else "Take ramp"
            "new name", "continue" -> if (roadName != null) "Continue onto $roadName" else "Continue straight"
            else -> if (roadName != null) "Continue onto $roadName" else "Continue"
        }
    }

    override suspend fun calculateRoadDistances(
        origin: LocationPoint,
        destinations: List<LocationPoint>
    ): Result<List<Double?>> {
        if (destinations.isEmpty()) {
            return Result.success(emptyList())
        }

        if (origin.latitude.isNaN() || origin.longitude.isNaN() ||
            origin.latitude < -90.0 || origin.latitude > 90.0 ||
            origin.longitude < -180.0 || origin.longitude > 180.0
        ) {
            return Result.failure(RoutingError.InvalidCoordinates())
        }

        val validDestinations = destinations.map {
            if (it.latitude.isNaN() || it.longitude.isNaN() ||
                it.latitude < -90.0 || it.latitude > 90.0 ||
                it.longitude < -180.0 || it.longitude > 180.0
            ) null else it
        }

        return try {
            val coords = buildString {
                append("${origin.longitude},${origin.latitude}")
                for (dest in destinations) {
                    val lat = dest.latitude.takeIf { !it.isNaN() } ?: 0.0
                    val lon = dest.longitude.takeIf { !it.isNaN() } ?: 0.0
                    append(";${lon},${lat}")
                }
            }
            val destIndexes = (1..destinations.size).joinToString(";")
            val url = "https://router.project-osrm.org/table/v1/driving/$coords?sources=0&destinations=$destIndexes&annotations=distance"

            val response = httpTransport.get(url)
            if (response.statusCode in 200..299) {
                val jsonObject = json.parseToJsonElement(response.body).jsonObject
                val distancesArray = jsonObject["distances"]?.jsonArray
                if (distancesArray != null && distancesArray.isNotEmpty()) {
                    val firstRow = distancesArray[0].jsonArray
                    val resultList = firstRow.mapIndexed { index, element ->
                        if (validDestinations.getOrNull(index) == null) {
                            null
                        } else {
                            element.jsonPrimitive.contentOrNull?.toDoubleOrNull()
                        }
                    }
                    Result.success(resultList)
                } else {
                    Result.success(List(destinations.size) { null })
                }
            } else {
                Result.failure(RoutingError.ProviderError(response.statusCode, "Failed to calculate distances: HTTP ${response.statusCode}"))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(RoutingError.Timeout("Distance calculation timed out"))
        } catch (e: CancellationException) {
            Result.failure(RoutingError.RequestCancelled())
        } catch (e: IOException) {
            Result.failure(RoutingError.NetworkError("Network error calculating distances: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(RoutingError.ProviderError(500, e.message ?: "Unknown distance calculation error"))
        }
    }
}

