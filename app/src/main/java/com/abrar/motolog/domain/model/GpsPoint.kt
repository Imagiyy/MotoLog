package com.abrar.motolog.domain.model

/**
 * Pure Kotlin representation of an incoming GPS fix used by the calculation engine.
 *
 * Free of any Android dependencies to allow JVM unit testing and GPX replay.
 */
data class GpsPoint(
    val timestampEpochMs: Long,
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float? = null,
    val accuracyMeters: Float = Float.MAX_VALUE,
    val speedAccuracyMps: Float? = null,
    val altitudeMeters: Double? = null,
    val isPaused: Boolean = false,
    val isGap: Boolean = false
)
