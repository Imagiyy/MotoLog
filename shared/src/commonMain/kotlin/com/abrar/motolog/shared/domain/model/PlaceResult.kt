package com.abrar.motolog.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Normalized, provider-independent place result from geocoding search.
 *
 * @property id Optional provider identifier (e.g. OSM ID or Place ID).
 * @property name Primary name of the location or point of interest.
 * @property formattedAddress Full locality / address line.
 * @property latitude Validated latitude in degrees [-90.0, 90.0].
 * @property longitude Validated longitude in degrees [-180.0, 180.0].
 * @property distanceFromUserMeters Straight-line distance from user's current coordinate if available.
 * @property provider Originating geocoder provider name (e.g., "GraphHopper", "Photon", "Nominatim").
 * @property isFartherMatch True if this place was returned via global fallback outside initial search radius.
 */
@Serializable
data class PlaceResult(
    val id: String? = null,
    val name: String,
    val formattedAddress: String = "",
    val latitude: Double,
    val longitude: Double,
    val distanceFromUserMeters: Double? = null,
    val provider: String = "GraphHopper",
    val isFartherMatch: Boolean = false
) {
    /**
     * Verify latitude and longitude are finite and within physical bounds.
     */
    fun hasValidCoordinates(): Boolean {
        return !latitude.isNaN() &&
                !longitude.isNaN() &&
                !latitude.isInfinite() &&
                !longitude.isInfinite() &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0
    }

    /**
     * Convert this search result into a domain [Destination] entity for route planning.
     */
    fun toDestination(sourceType: DestinationSourceType = DestinationSourceType.SEARCH): Destination {
        val fullDisplay = if (formattedAddress.isNotBlank() && !formattedAddress.equals(name, ignoreCase = true)) {
            "$name, $formattedAddress"
        } else {
            name
        }
        return Destination(
            id = id ?: "dest_${latitude}_${longitude}",
            latitude = latitude,
            longitude = longitude,
            displayName = fullDisplay,
            providerPlaceId = id,
            sourceType = sourceType,
            createdAtTimestamp = 0L,
            isFartherMatch = isFartherMatch
        )
    }
}
