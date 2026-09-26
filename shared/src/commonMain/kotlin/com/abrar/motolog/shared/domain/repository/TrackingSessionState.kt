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
        val isPaused: Boolean = false,
        val pauseState: PauseState = if (isPaused) PauseState.MANUALLY_PAUSED else PauseState.RECORDING,
        val isGpsLost: Boolean = false,
        val stats: RideStats = RideStats(),
        val accuracyMeters: Float = 0f,
        val isSpeedAlert: Boolean = false,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) : TrackingSessionState {

        constructor(
            rideId: Long,
            pauseState: PauseState,
            isGpsLost: Boolean = false,
            stats: RideStats = RideStats(),
            accuracyMeters: Float = 0f,
            isSpeedAlert: Boolean = false,
            latitude: Double? = null,
            longitude: Double? = null
        ) : this(
            rideId = rideId,
            isPaused = pauseState.isPaused,
            pauseState = pauseState,
            isGpsLost = isGpsLost,
            stats = stats,
            accuracyMeters = accuracyMeters,
            isSpeedAlert = isSpeedAlert,
            latitude = latitude,
            longitude = longitude
        )
    }

    /** Tracking session finished via Stop. */
    data class Stopped(
        val stats: RideStats = RideStats()
    ) : TrackingSessionState
}
