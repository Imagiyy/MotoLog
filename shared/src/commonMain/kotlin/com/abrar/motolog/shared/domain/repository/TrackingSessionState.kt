package com.abrar.motolog.shared.domain.repository

import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats

/**
 * Represents the live state of the motorcycle ride tracking session.
 * Shared between TrackingService, TrackingRepository, and LiveViewModel.
 */
sealed interface TrackingSessionState {

    /** Tracking is not active. Ready to start a new ride. */
    data object Idle : TrackingSessionState

    /** Tracking initiated; waiting for usable GPS fix (accuracy <= 25m). */
    data class WaitingForGps(
        val accuracyMeters: Float = Float.MAX_VALUE
    ) : TrackingSessionState

    /** Tracking is actively recording or paused. */
    data class Tracking(
        val rideId: Long,
        val pauseState: PauseState = PauseState.RECORDING,
        val isPaused: Boolean = pauseState.isPaused,
        val isGpsLost: Boolean = false,
        val stats: RideStats = RideStats(),
        val accuracyMeters: Float = 0f,
        val isSpeedAlert: Boolean = false,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val altitudeMeters: Double? = null,
        val bearingDegrees: Float? = null
    ) : TrackingSessionState {

        constructor(
            rideId: Long,
            isPaused: Boolean,
            stats: RideStats = RideStats(),
            accuracyMeters: Float = 0f
        ) : this(
            rideId = rideId,
            pauseState = if (isPaused) PauseState.MANUALLY_PAUSED else PauseState.RECORDING,
            isPaused = isPaused,
            stats = stats,
            accuracyMeters = accuracyMeters
        )
    }

    /** Tracking session finished via Stop. */
    data class Stopped(
        val stats: RideStats = RideStats()
    ) : TrackingSessionState
}
