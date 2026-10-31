package com.abrar.motolog.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a geographical destination or waypoint selected for route planning.
 *
 * @property id Unique identifier for persistence or referencing.
 * @property latitude Target latitude.
 * @property longitude Target longitude.
 * @property displayName Human-readable label (e.g., place name, street, or city).
 * @property providerPlaceId Optional place identifier from geocoding provider (e.g. GraphHopper OSM ID).
 * @property sourceType Method used to specify the destination.
 * @property createdAtTimestamp UTC timestamp (ms) when this destination was created or selected.
 */
@Serializable
data class Destination(
    val id: String = "",
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val providerPlaceId: String? = null,
    val sourceType: DestinationSourceType = DestinationSourceType.SEARCH,
    val createdAtTimestamp: Long = 0L
)

@Serializable
enum class DestinationSourceType {
    SEARCH,
    MAP_PIN,
    SAVED,
    RECENT
}
