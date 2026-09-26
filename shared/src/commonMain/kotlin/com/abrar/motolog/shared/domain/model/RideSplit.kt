package com.abrar.motolog.shared.domain.model

/**
 * Represents a single distance split (e.g., 1-kilometre or 1-mile segment)
 * during a ride session.
 *
 * Free of Android framework dependencies to allow multiplatform usage.
 */
data class RideSplit(
    /** 1-based sequential split number (e.g., 1 for 1st km, 2 for 2nd km) */
    val splitNumber: Int,

    /** Distance of this split in meters (e.g., 1000.0 for full km, or < 1000.0 for final partial split) */
    val distanceMeters: Double,

    /** Moving duration spent actively riding within this split in milliseconds (excludes paused & gap time) */
    val durationMs: Long,

    /** Average moving speed achieved within this split in km/h */
    val avgSpeedKmh: Double,

    /** True if this is an incomplete split at the end of the ride */
    val isPartial: Boolean = false
)
