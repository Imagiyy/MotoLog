package com.abrar.motolog.data.routing

import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.shared.domain.engine.PlaceRankingEngine
import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.DestinationSourceType
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlaceResult
import com.abrar.motolog.shared.domain.model.RoutingError
import com.abrar.motolog.shared.domain.model.RoutingMode
import com.abrar.motolog.shared.domain.repository.GeocodingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GraphHopper and OpenStreetMap implementation of [GeocodingRepository].
 *
 * Supports both GraphHopper Cloud and Keyless (self-hosted / Photon fallback) routing modes
 * with Nearby-First multi-factor ranking and coordinate bounds validation.
 */
@Singleton
class GraphHopperGeocodingRepository @Inject constructor(
    private val httpTransport: HttpTransport,
    private val settingsRepository: SettingsRepository
) : GeocodingRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun searchPlaces(
        query: String,
        nearLocation: LocationPoint?,
        radiusMeters: Int?
    ): Result<List<PlaceResult>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            val mode = settingsRepository.routingMode.first()
            val nearbyEnabled = settingsRepository.nearbySearchEnabled.first()
            val initialRadiusKm = settingsRepository.initialSearchRadiusKm.first()
            val globalFallback = settingsRepository.globalSearchFallback.first()
            val effectiveRadiusMeters = (radiusMeters ?: (initialRadiusKm * 1000)).toDouble()

            val rawPlaces = when (mode) {
                RoutingMode.GRAPH_HOPPER_CLOUD -> {
                    searchCloud(trimmedQuery, nearLocation, nearbyEnabled)
                }
                RoutingMode.KEYLESS_GRAPH_HOPPER -> {
                    searchKeyless(trimmedQuery, nearLocation, nearbyEnabled)
                }
            }

            // Apply deterministic Nearby-First ranking engine
            val ranked = if (nearbyEnabled) {
                PlaceRankingEngine.rankPlaces(
                    places = rawPlaces,
                    query = trimmedQuery,
                    userLocation = nearLocation,
                    searchRadiusMeters = effectiveRadiusMeters,
                    allowGlobalFallback = globalFallback
                )
            } else {
                PlaceRankingEngine.rankPlaces(
                    places = rawPlaces,
                    query = trimmedQuery,
                    userLocation = null, // Disable distance bias
                    searchRadiusMeters = effectiveRadiusMeters,
                    allowGlobalFallback = true
                )
            }

            Result.success(ranked)
        } catch (e: SocketTimeoutException) {
            Result.failure(RoutingError.Timeout("Geocoding request timed out"))
        } catch (e: CancellationException) {
            Result.failure(RoutingError.RequestCancelled())
        } catch (e: RoutingError) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(RoutingError.NetworkError("Network error during geocoding: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(RoutingError.ProviderError(500, e.message ?: "Unknown geocoding error"))
        }
    }

    override suspend fun searchDestinations(
        query: String,
        nearLocation: LocationPoint?
    ): Result<List<Destination>> {
        return searchPlaces(query, nearLocation).map { places ->
            places.map { place -> place.toDestination() }
        }
    }

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<Destination> {
        if (latitude.isNaN() || longitude.isNaN() || latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            return Result.failure(RoutingError.InvalidCoordinates())
        }

        return try {
            val mode = settingsRepository.routingMode.first()
            val destination = when (mode) {
                RoutingMode.GRAPH_HOPPER_CLOUD -> reverseGeocodeCloud(latitude, longitude)
                RoutingMode.KEYLESS_GRAPH_HOPPER -> reverseGeocodeKeyless(latitude, longitude)
            }
            Result.success(destination)
        } catch (e: SocketTimeoutException) {
            Result.failure(RoutingError.Timeout("Reverse geocode request timed out"))
        } catch (e: CancellationException) {
            Result.failure(RoutingError.RequestCancelled())
        } catch (e: RoutingError) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(RoutingError.NetworkError("Network error during reverse geocoding: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(RoutingError.ProviderError(500, e.message ?: "Unknown reverse geocoding error"))
        }
    }

    // ============================================================
    // Cloud Geocoding (GraphHopper Hosted API)
    // ============================================================

    private suspend fun searchCloud(
        query: String,
        nearLocation: LocationPoint?,
        nearbyEnabled: Boolean
    ): List<PlaceResult> {
        val providerUrl = settingsRepository.routingProviderUrl.first()
        val apiKey = settingsRepository.routingApiKey.first()

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val queryParams = mutableListOf(
            "q=$encodedQuery",
            "locale=en",
            "limit=20"
        )

        if (nearbyEnabled && nearLocation != null && !nearLocation.latitude.isNaN() && !nearLocation.longitude.isNaN()) {
            queryParams.add("point=${nearLocation.latitude},${nearLocation.longitude}")
        }

        if (apiKey.isNotBlank()) {
            queryParams.add("key=${URLEncoder.encode(apiKey, "UTF-8")}")
        }

        val fullUrl = "${providerUrl.removeSuffix("/")}/geocode?${queryParams.joinToString("&")}"
        val response = httpTransport.get(fullUrl)

        when (response.statusCode) {
            in 200..299 -> {
                val dto = json.decodeFromString<GraphHopperGeocodeResponseDto>(response.body)
                return dto.hits.mapNotNull { hit ->
                    val pt = hit.point ?: return@mapNotNull null
                    PlaceResult(
                        id = hit.osmId,
                        name = hit.name?.takeIf { it.isNotBlank() } ?: "Unnamed Location",
                        formattedAddress = formatHitAddress(hit),
                        latitude = pt.lat,
                        longitude = pt.lng,
                        provider = "GraphHopper Cloud"
                    )
                }
            }
            401, 403 -> throw RoutingError.ApiKeyMissing("Invalid or missing GraphHopper Cloud API key.")
            429 -> throw RoutingError.RateLimitExceeded()
            else -> throw RoutingError.ProviderError(response.statusCode, "Geocoding failed with HTTP ${response.statusCode}")
        }
    }

    private suspend fun reverseGeocodeCloud(latitude: Double, longitude: Double): Destination {
        val providerUrl = settingsRepository.routingProviderUrl.first()
        val apiKey = settingsRepository.routingApiKey.first()

        val queryParams = mutableListOf(
            "reverse=true",
            "point=$latitude,$longitude",
            "locale=en"
        )

        if (apiKey.isNotBlank()) {
            queryParams.add("key=${URLEncoder.encode(apiKey, "UTF-8")}")
        }

        val fullUrl = "${providerUrl.removeSuffix("/")}/geocode?${queryParams.joinToString("&")}"
        val response = httpTransport.get(fullUrl)

        return if (response.statusCode in 200..299) {
            val dto = json.decodeFromString<GraphHopperGeocodeResponseDto>(response.body)
            val firstHit = dto.hits.firstOrNull()
            Destination(
                id = UUID.randomUUID().toString(),
                latitude = latitude,
                longitude = longitude,
                displayName = firstHit?.let { formatHitDisplayName(it) } ?: "Location: %.5f, %.5f".format(latitude, longitude),
                providerPlaceId = firstHit?.osmId,
                sourceType = DestinationSourceType.MAP_PIN,
                createdAtTimestamp = System.currentTimeMillis()
            )
        } else {
            // Graceful fallback on non-critical reverse geocode error
            Destination(
                id = UUID.randomUUID().toString(),
                latitude = latitude,
                longitude = longitude,
                displayName = "Dropped Pin (%.4f, %.4f)".format(latitude, longitude),
                sourceType = DestinationSourceType.MAP_PIN,
                createdAtTimestamp = System.currentTimeMillis()
            )
        }
    }

    // ============================================================
    // Keyless Geocoding (Self-Hosted GraphHopper / Photon OSM Fallback)
    // ============================================================

    private suspend fun searchKeyless(
        query: String,
        nearLocation: LocationPoint?,
        nearbyEnabled: Boolean
    ): List<PlaceResult> {
        val selfHostedUrl = settingsRepository.routingSelfHostedUrl.first()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        // 1. First attempt self-hosted /geocode if available (NO API KEY SENT)
        try {
            val queryParams = mutableListOf(
                "q=$encodedQuery",
                "locale=en",
                "limit=20"
            )
            if (nearbyEnabled && nearLocation != null && !nearLocation.latitude.isNaN() && !nearLocation.longitude.isNaN()) {
                queryParams.add("point=${nearLocation.latitude},${nearLocation.longitude}")
            }
            val fullUrl = "${selfHostedUrl.removeSuffix("/")}/geocode?${queryParams.joinToString("&")}"
            val response = httpTransport.get(fullUrl)

            if (response.statusCode in 200..299) {
                val dto = json.decodeFromString<GraphHopperGeocodeResponseDto>(response.body)
                if (dto.hits.isNotEmpty()) {
                    return dto.hits.mapNotNull { hit ->
                        val pt = hit.point ?: return@mapNotNull null
                        PlaceResult(
                            id = hit.osmId,
                            name = hit.name?.takeIf { it.isNotBlank() } ?: "Unnamed Location",
                            formattedAddress = formatHitAddress(hit),
                            latitude = pt.lat,
                            longitude = pt.lng,
                            provider = "Keyless GraphHopper"
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Self-hosted server may only run routing engine; fall back to free OSM Photon geocoder
        }

        // 2. Fall back to OpenStreetMap Photon public geocoder (no key required)
        val photonParams = mutableListOf("q=$encodedQuery", "limit=20")
        if (nearbyEnabled && nearLocation != null && !nearLocation.latitude.isNaN() && !nearLocation.longitude.isNaN()) {
            photonParams.add("lat=${nearLocation.latitude}")
            photonParams.add("lon=${nearLocation.longitude}")
        }
        val photonUrl = "https://photon.komoot.io/api/?${photonParams.joinToString("&")}"
        val photonResponse = httpTransport.get(photonUrl)

        if (photonResponse.statusCode in 200..299) {
            val photonDto = json.decodeFromString<PhotonGeocodeResponseDto>(photonResponse.body)
            return photonDto.features.mapNotNull { feat ->
                val coords = feat.geometry?.coordinates ?: return@mapNotNull null
                if (coords.size < 2) return@mapNotNull null
                val lon = coords[0]
                val lat = coords[1]
                val props = feat.properties
                val name = props?.name ?: props?.street ?: "Unnamed Location"
                PlaceResult(
                    id = props?.osmId?.toString(),
                    name = name,
                    formattedAddress = formatPhotonAddress(props),
                    latitude = lat,
                    longitude = lon,
                    provider = "OpenStreetMap"
                )
            }
        } else {
            throw RoutingError.ProviderError(photonResponse.statusCode, "Keyless geocoding service unavailable (HTTP ${photonResponse.statusCode})")
        }
    }

    private suspend fun reverseGeocodeKeyless(latitude: Double, longitude: Double): Destination {
        val selfHostedUrl = settingsRepository.routingSelfHostedUrl.first()

        // 1. Try self-hosted /geocode?reverse=true
        try {
            val fullUrl = "${selfHostedUrl.removeSuffix("/")}/geocode?reverse=true&point=$latitude,$longitude&locale=en"
            val response = httpTransport.get(fullUrl)
            if (response.statusCode in 200..299) {
                val dto = json.decodeFromString<GraphHopperGeocodeResponseDto>(response.body)
                val firstHit = dto.hits.firstOrNull()
                if (firstHit != null) {
                    return Destination(
                        id = UUID.randomUUID().toString(),
                        latitude = latitude,
                        longitude = longitude,
                        displayName = formatHitDisplayName(firstHit),
                        providerPlaceId = firstHit.osmId,
                        sourceType = DestinationSourceType.MAP_PIN,
                        createdAtTimestamp = System.currentTimeMillis()
                    )
                }
            }
        } catch (_: Exception) {
            // Fall back to OSM Photon reverse
        }

        // 2. Try OSM Photon reverse geocode
        try {
            val photonUrl = "https://photon.komoot.io/reverse?lat=$latitude&lon=$longitude"
            val response = httpTransport.get(photonUrl)
            if (response.statusCode in 200..299) {
                val photonDto = json.decodeFromString<PhotonGeocodeResponseDto>(response.body)
                val feat = photonDto.features.firstOrNull()
                if (feat?.properties != null) {
                    val displayName = listOfNotNull(
                        feat.properties.name,
                        feat.properties.street,
                        feat.properties.city,
                        feat.properties.country
                    ).filter { it.isNotBlank() }.distinct().joinToString(", ")

                    return Destination(
                        id = UUID.randomUUID().toString(),
                        latitude = latitude,
                        longitude = longitude,
                        displayName = displayName.ifEmpty { "Location: %.5f, %.5f".format(latitude, longitude) },
                        providerPlaceId = feat.properties.osmId?.toString(),
                        sourceType = DestinationSourceType.MAP_PIN,
                        createdAtTimestamp = System.currentTimeMillis()
                    )
                }
            }
        } catch (_: Exception) {
            // Coordinate fallback
        }

        return Destination(
            id = UUID.randomUUID().toString(),
            latitude = latitude,
            longitude = longitude,
            displayName = "Dropped Pin (%.4f, %.4f)".format(latitude, longitude),
            sourceType = DestinationSourceType.MAP_PIN,
            createdAtTimestamp = System.currentTimeMillis()
        )
    }

    // ============================================================
    // Address Formatting Helpers
    // ============================================================

    private fun formatHitAddress(hit: GraphHopperHitDto): String {
        val parts = mutableListOf<String>()
        val streetWithNum = listOfNotNull(
            hit.housenumber?.takeIf { it.isNotBlank() },
            hit.street?.takeIf { it.isNotBlank() }
        ).joinToString(" ").trim()

        if (streetWithNum.isNotEmpty()) parts.add(streetWithNum)
        hit.city?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
        hit.state?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
        hit.country?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }

        return parts.joinToString(", ")
    }

    private fun formatHitDisplayName(hit: GraphHopperHitDto): String {
        val name = hit.name?.takeIf { it.isNotBlank() }
        val address = formatHitAddress(hit)
        return when {
            name != null && address.isNotEmpty() && !address.contains(name) -> "$name, $address"
            name != null -> name
            address.isNotEmpty() -> address
            else -> "Unknown Location"
        }
    }

    private fun formatPhotonAddress(props: PhotonPropertiesDto?): String {
        if (props == null) return ""
        val parts = mutableListOf<String>()
        val streetWithNum = listOfNotNull(
            props.housenumber?.takeIf { it.isNotBlank() },
            props.street?.takeIf { it.isNotBlank() }
        ).joinToString(" ").trim()

        if (streetWithNum.isNotEmpty()) parts.add(streetWithNum)
        props.city?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
        props.state?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
        props.country?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }

        return parts.joinToString(", ")
    }
}
