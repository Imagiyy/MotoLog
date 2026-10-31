package com.abrar.motolog.shared.domain.model

import com.abrar.motolog.shared.domain.TrackingConstants

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
) {
    /**
     * Checks whether this ride has all zero or negligible values (no real riding recorded).
     */
    fun isZeroRide(minDistanceMeters: Double = TrackingConstants.MIN_SAVEABLE_RIDE_DISTANCE_METERS): Boolean {
        return acceptedPointCount == 0 ||
            (totalDistanceMeters < minDistanceMeters && movingTimeMs <= 0L && maxSpeedKmh <= TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH)
    }
}
