package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.PauseState

/**
 * Filter decision returned by the calculation engine for each processed GPS point.
 */
sealed interface PointFilterResult {

    /**
     * Point passed all filtering and validity checks.
     *
     * @property distanceIncrementMeters Distance added by this point (0.0 if stationary jitter or across a gap).
     * @property speedKmh Filtered speed in km/h (0.0 if below stationary threshold).
     * @property isGap True if the time since the last accepted point exceeded the gap threshold.
     * @property pauseState Current pause state at the time of processing this point.
     */
    data class Accepted(
        val distanceIncrementMeters: Double,
        val speedKmh: Double,
        val isGap: Boolean,
        val pauseState: PauseState = PauseState.RECORDING
    ) : PointFilterResult

    /**
     * Point was rejected by the filtering engine.
     *
     * @property reason Specific reason for rejection.
     */
    data class Rejected(
        val reason: RejectionReason
    ) : PointFilterResult
}

enum class RejectionReason {
    POOR_ACCURACY,
    NON_MONOTONIC_TIMESTAMP,
    SPIKE_SPEED,
    SPIKE_ACCELERATION
}
