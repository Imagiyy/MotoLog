package com.abrar.motolog.shared.domain.model

/**
 * Pure Kotlin representation of a GPS location fix.
 *
 * Free of any Android framework dependencies (e.g., android.location.Location)
 * to maintain clean domain boundary isolation and allow full multiplatform reusability.
 */
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float?,
    val accuracyMeters: Float,
    val timestamp: Long,
    val altitudeMeters: Double? = null
)
