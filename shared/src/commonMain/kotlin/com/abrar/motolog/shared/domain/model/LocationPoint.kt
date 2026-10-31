package com.abrar.motolog.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Pure Kotlin representation of a GPS location fix.
 *
 * Free of any Android framework dependencies (e.g., android.location.Location)
 * to maintain clean domain boundary isolation and allow full multiplatform reusability.
 *
 * [timestamp] is wall-clock UTC epoch millis (for display, storage, export).
 * [elapsedTimestampMs] is monotonic elapsed-realtime millis (for calculation engine Δt).
 * When elapsedTimestampMs is null, the engine falls back to [timestamp].
 */
@Serializable
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float? = null,
    val accuracyMeters: Float = 0f,
    val timestamp: Long = 0L,
    val altitudeMeters: Double? = null,
    val elapsedTimestampMs: Long? = null,
    val bearingDegrees: Float? = null
)
