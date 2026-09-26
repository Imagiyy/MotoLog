package com.abrar.motolog.ui.live

import androidx.compose.runtime.Immutable
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats

/**
 * UI State for the Live tracking screen.
 */
@Immutable
sealed interface LiveUiState {
    /**
     * Initial state before tracking has started.
     */
    @Immutable
    data object Idle : LiveUiState

    /**
     * Start was pressed and location updates are active, but waiting for
     * a usable GPS fix (accuracy <= 25 m per TrackingConstants.MIN_GPS_ACCURACY_METERS).
     */
    @Immutable
    data class WaitingForGps(
        val currentAccuracyMeters: Float? = null
    ) : LiveUiState

    /**
     * Active tracking with a usable GPS fix.
     * Displays live speed in km/h and accumulated ride statistics.
     */
    @Immutable
    data class Tracking(
        val speedKmh: Double,
        val accuracyMeters: Float,
        val isPaused: Boolean = false,
        val pauseState: PauseState = if (isPaused) PauseState.MANUALLY_PAUSED else PauseState.RECORDING,
        val isGpsLost: Boolean = false,
        val stats: RideStats = RideStats(),
        val isSpeedAlert: Boolean = false,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val routeCoordinates: List<Pair<Double, Double>> = emptyList(),
        val bikeName: String? = null
    ) : LiveUiState {

        constructor(
            speedKmh: Double,
            accuracyMeters: Float,
            pauseState: PauseState,
            isGpsLost: Boolean = false,
            stats: RideStats = RideStats(),
            isSpeedAlert: Boolean = false,
            latitude: Double? = null,
            longitude: Double? = null,
            routeCoordinates: List<Pair<Double, Double>> = emptyList(),
            bikeName: String? = null
        ) : this(
            speedKmh = speedKmh,
            accuracyMeters = accuracyMeters,
            isPaused = pauseState.isPaused,
            pauseState = pauseState,
            isGpsLost = isGpsLost,
            stats = stats,
            isSpeedAlert = isSpeedAlert,
            latitude = latitude,
            longitude = longitude,
            routeCoordinates = routeCoordinates,
            bikeName = bikeName
        )
    }

    /**
     * Unfinished ride from crash/force-kill detected on app startup.
     */
    @Immutable
    data class RecoveryPrompt(
        val activeRide: com.abrar.motolog.data.local.entity.RideEntity
    ) : LiveUiState

    /**
     * Tracking stopped after confirming 2-second Hold-to-Stop.
     * Location updates are completely ceased.
     */
    @Immutable
    data class Stopped(
        val stats: RideStats = RideStats()
    ) : LiveUiState
}
