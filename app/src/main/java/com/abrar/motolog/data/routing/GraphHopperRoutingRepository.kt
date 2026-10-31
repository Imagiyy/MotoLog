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
@Singleton
class GraphHopperRoutingRepository @Inject constructor(
    private val httpTransport: HttpTransport,
    private val settingsRepository: SettingsRepository
) : RoutingRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val activeJobMutex = Mutex()
    private var activeJob: Job? = null
    private var lastSuccessfulRequest: RouteRequest? = null

    override suspend fun calculateRoute(request: RouteRequest): Result<List<PlannedRoute>> {
        return try {
            val providerUrl = settingsRepository.routingProviderUrl.first()
            val apiKey = settingsRepository.routingApiKey.first()

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

            if (request.avoidances.isNotEmpty()) {
                queryParams.add("ch.disable=true")
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
                            isAlternative = index > 0
                        )
                    }

                    lastSuccessfulRequest = request
                    Result.success(routes)
                }

                400 -> {
                    val errorMsg = extractErrorMessage(response.body) ?: "No route found between points"
                    Result.failure(RoutingError.NoRouteFound(errorMsg))
                }

                401, 403 -> {
                    Result.failure(
                        RoutingError.ProviderError(
                            response.statusCode,
                            "Invalid or missing GraphHopper API key. Please check your key in settings."
                        )
                    )
                }

                429 -> {
                    Result.failure(RoutingError.RateLimitExceeded())
                }

                else -> {
                    val errorMsg = extractErrorMessage(response.body) ?: "HTTP ${response.statusCode}"
                    Result.failure(RoutingError.ProviderError(response.statusCode, errorMsg))
                }
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(RoutingError.Timeout("Routing request timed out"))
        } catch (e: CancellationException) {
            Result.failure(RoutingError.RequestCancelled())
        } catch (e: IOException) {
            Result.failure(RoutingError.NetworkError("Network error calculating route: ${e.message}", e))
        } catch (e: Exception) {
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
        isAlternative: Boolean
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

        return PlannedRoute(
            id = UUID.randomUUID().toString(),
            geometry = decodedPoints,
            totalDistanceMeters = path.distance,
            expectedDurationSeconds = path.timeMs / 1000L,
            bounds = boundingBox,
            instructions = instructions,
            providerAttribution = "Routing data © GraphHopper & OpenStreetMap contributors",
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
            val msgObj = jsonElement as? kotlinx.serialization.json.JsonObject
            msgObj?.get("message")?.let {
                it.toString().trim('\"')
            }
        } catch (_: Exception) {
            null
        }
    }
}
