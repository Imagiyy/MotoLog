package com.abrar.motolog.domain.model

/**
 * Immutable snapshot of calculated ride metrics.
 */
data class RideStats(
    val totalDistanceMeters: Double = 0.0,
    val elapsedTimeMs: Long = 0L,
    val movingTimeMs: Long = 0L,
    val stoppedTimeMs: Long = 0L,
    val currentSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val avgMovingSpeedKmh: Double = 0.0,
    val avgOverallSpeedKmh: Double = 0.0,
    val gapCount: Int = 0,
    val acceptedPointCount: Int = 0,
    val rejectedPointCount: Int = 0
)
