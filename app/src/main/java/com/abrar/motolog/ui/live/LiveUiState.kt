package com.abrar.motolog.ui.live

import com.abrar.motolog.domain.model.PauseState
import com.abrar.motolog.domain.model.RideStats

/**
 * UI State for the Live tracking screen.
 */
sealed interface LiveUiState {
    /**
     * Initial state before tracking has started.
     */
    data object Idle : LiveUiState

    /**
     * Start was pressed and location updates are active, but waiting for
     * a usable GPS fix (accuracy <= 25 m per TrackingConstants.MIN_GPS_ACCURACY_METERS).
     */
    data class WaitingForGps(
        val currentAccuracyMeters: Float? = null
    ) : LiveUiState

    /**
     * Active tracking with a usable GPS fix.
     * Displays live speed in km/h and accumulated ride statistics.
     */
    data class Tracking(
        val speedKmh: Double,
        val accuracyMeters: Float,
        val isPaused: Boolean = false,
        val pauseState: PauseState = if (isPaused) PauseState.MANUALLY_PAUSED else PauseState.RECORDING,
        val isGpsLost: Boolean = false,
        val stats: RideStats = RideStats()
    ) : LiveUiState {

        constructor(
            speedKmh: Double,
            accuracyMeters: Float,
            pauseState: PauseState,
            isGpsLost: Boolean = false,
            stats: RideStats = RideStats()
        ) : this(
            speedKmh = speedKmh,
            accuracyMeters = accuracyMeters,
            isPaused = pauseState.isPaused,
            pauseState = pauseState,
            isGpsLost = isGpsLost,
            stats = stats
        )
    }

    /**
     * Unfinished ride from crash/force-kill detected on app startup.
     */
    data class RecoveryPrompt(
        val activeRide: com.abrar.motolog.data.local.entity.RideEntity
    ) : LiveUiState

    /**
     * Tracking stopped after confirming 2-second Hold-to-Stop.
     * Location updates are completely ceased.
     */
    data class Stopped(
        val stats: RideStats = RideStats()
    ) : LiveUiState
}
