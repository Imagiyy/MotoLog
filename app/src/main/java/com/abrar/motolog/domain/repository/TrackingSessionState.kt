package com.abrar.motolog.domain.repository

import com.abrar.motolog.domain.model.RideStats

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

    /** Tracking is actively recording or manually paused. */
    data class Tracking(
        val rideId: Long,
        val isPaused: Boolean = false,
        val stats: RideStats = RideStats(),
        val accuracyMeters: Float = 0f
    ) : TrackingSessionState

    /** Tracking session finished via Stop. */
    data class Stopped(
        val stats: RideStats = RideStats()
    ) : TrackingSessionState
}
