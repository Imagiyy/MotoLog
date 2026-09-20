package com.abrar.motolog.ui.live

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
     * Displays live speed in km/h (smoothed / stationary-filtered).
     */
    data class Tracking(
        val speedKmh: Double,
        val accuracyMeters: Float
    ) : LiveUiState

    /**
     * Tracking stopped after confirming 2-second Hold-to-Stop.
     * Location updates are completely ceased.
     */
    data object Stopped : LiveUiState
}
