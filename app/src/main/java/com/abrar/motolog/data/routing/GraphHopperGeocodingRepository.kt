package com.abrar.motolog.data.routing

import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.DestinationSourceType
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.RoutingError
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
 * GraphHopper implementation of [GeocodingRepository].
 *
 * Supports free-text forward geocoding and reverse geocoding for user-placed map pins.
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

    override suspend fun searchDestinations(
        query: String,
        nearLocation: LocationPoint?
    ): Result<List<Destination>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            val providerUrl = settingsRepository.routingProviderUrl.first()
            val apiKey = settingsRepository.routingApiKey.first()

            val encodedQuery = URLEncoder.encode(trimmedQuery, "UTF-8")
            val queryParams = mutableListOf(
                "q=$encodedQuery",
                "locale=en",
                "limit=10"
            )

            if (nearLocation != null && !nearLocation.latitude.isNaN() && !nearLocation.longitude.isNaN()) {
                queryParams.add("point=${nearLocation.latitude},${nearLocation.longitude}")
            }

            if (apiKey.isNotBlank()) {
                queryParams.add("key=${URLEncoder.encode(apiKey, "UTF-8")}")
            }

            val fullUrl = "${providerUrl.removeSuffix("/")}/geocode?${queryParams.joinToString("&")}"
            val response = httpTransport.get(fullUrl)

            when (response.statusCode) {
                in 200..299 -> {
                    val dto = try {
                        json.decodeFromString<GraphHopperGeocodeResponseDto>(response.body)
                    } catch (e: Exception) {
                        return Result.failure(
                            RoutingError.ProviderError(response.statusCode, "Failed to parse geocode response: ${e.message}")
                        )
                    }

                    val destinations = dto.hits.mapNotNull { hit ->
                        val pt = hit.point ?: return@mapNotNull null
                        Destination(
                            id = UUID.randomUUID().toString(),
                            latitude = pt.lat,
                            longitude = pt.lng,
                            displayName = formatHitDisplayName(hit),
                            providerPlaceId = hit.osmId,
                            sourceType = DestinationSourceType.SEARCH,
                            createdAtTimestamp = System.currentTimeMillis()
                        )
                    }

                    Result.success(destinations)
                }

                401, 403 -> {
                    Result.failure(
                        RoutingError.ApiKeyMissing(
                            "Invalid or missing GraphHopper API key."
                        )
                    )
                }

                429 -> {
                    Result.failure(RoutingError.RateLimitExceeded())
                }

                else -> {
                    Result.failure(RoutingError.ProviderError(response.statusCode, "Geocoding failed with HTTP ${response.statusCode}"))
                }
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(RoutingError.Timeout("Geocoding request timed out"))
        } catch (e: CancellationException) {
            Result.failure(RoutingError.RequestCancelled())
        } catch (e: IOException) {
            Result.failure(RoutingError.NetworkError("Network error during geocoding: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(RoutingError.ProviderError(500, e.message ?: "Unknown geocoding error"))
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

            when (response.statusCode) {
                in 200..299 -> {
                    val dto = try {
                        json.decodeFromString<GraphHopperGeocodeResponseDto>(response.body)
                    } catch (e: Exception) {
                        return Result.failure(
                            RoutingError.ProviderError(response.statusCode, "Failed to parse reverse geocode response: ${e.message}")
                        )
                    }

                    val firstHit = dto.hits.firstOrNull()
                    val displayName = if (firstHit != null) {
                        formatHitDisplayName(firstHit)
                    } else {
                        "Location: %.5f, %.5f".format(latitude, longitude)
                    }

                    Result.success(
                        Destination(
                            id = UUID.randomUUID().toString(),
                            latitude = latitude,
                            longitude = longitude,
                            displayName = displayName,
                            providerPlaceId = firstHit?.osmId,
                            sourceType = DestinationSourceType.MAP_PIN,
                            createdAtTimestamp = System.currentTimeMillis()
                        )
                    )
                }

                401, 403 -> {
                    Result.failure(
                        RoutingError.ProviderError(
                            response.statusCode,
                            "Invalid or missing GraphHopper API key."
                        )
                    )
                }

                429 -> {
                    Result.failure(RoutingError.RateLimitExceeded())
                }

                else -> {
                    // Graceful fallback on non-critical reverse-geocode failures: return coordinate label
                    Result.success(
                        Destination(
                            id = UUID.randomUUID().toString(),
                            latitude = latitude,
                            longitude = longitude,
                            displayName = "Dropped Pin (%.4f, %.4f)".format(latitude, longitude),
                            sourceType = DestinationSourceType.MAP_PIN,
                            createdAtTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(RoutingError.Timeout("Reverse geocode request timed out"))
        } catch (e: CancellationException) {
            Result.failure(RoutingError.RequestCancelled())
        } catch (e: IOException) {
            Result.failure(RoutingError.NetworkError("Network error during reverse geocoding: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(RoutingError.ProviderError(500, e.message ?: "Unknown reverse geocoding error"))
        }
    }

    private fun formatHitDisplayName(hit: GraphHopperHitDto): String {
        val parts = mutableListOf<String>()

        hit.name?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

        val streetWithNumber = listOfNotNull(
            hit.housenumber?.takeIf { it.isNotBlank() },
            hit.street?.takeIf { it.isNotBlank() }
        ).joinToString(" ").trim()

        if (streetWithNumber.isNotEmpty() && !parts.contains(streetWithNumber)) {
            parts.add(streetWithNumber)
        }

        hit.city?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
        hit.state?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
        hit.country?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            "Unknown Location"
        }
    }
}
