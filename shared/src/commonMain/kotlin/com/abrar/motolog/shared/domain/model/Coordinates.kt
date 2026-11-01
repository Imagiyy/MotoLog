package com.abrar.motolog.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Lightweight geographical coordinate representing latitude and longitude.
 */
@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    /**
     * Check if latitude and longitude are finite and within physical bounds.
     */
    fun isValid(): Boolean =
        !latitude.isNaN() && !longitude.isNaN() &&
        !latitude.isInfinite() && !longitude.isInfinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

fun Coordinates.toLocationPoint(timestamp: Long = 0L): LocationPoint =
    LocationPoint(latitude = latitude, longitude = longitude, timestamp = timestamp)

fun LocationPoint.toCoordinates(): Coordinates =
    Coordinates(latitude = latitude, longitude = longitude)
