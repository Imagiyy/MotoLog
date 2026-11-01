package com.abrar.motolog.ui.navigation

import com.abrar.motolog.shared.domain.model.NavigationSession
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteInstruction
import com.abrar.motolog.shared.domain.model.RouteProgress

/**
 * Camera follow and orientation mode for the live navigation map.
 */
enum class CameraFollowMode {
    /** Map rotates dynamically with the motorcycle's bearing with 3D perspective pitch. */
    FOLLOW_HEAD_UP,

    /** Map is oriented North-up, centered dynamically on current rider position. */
    FOLLOW_NORTH_UP,

    /** Free pan/zoom by user (auto-centering paused until 'Recenter' is tapped). */
    FREE
}

/**
 * UI state for real-time destination route tracking and navigation.
 */
sealed interface NavigationUiState {

    /** No navigation session is currently active. */
    data object Idle : NavigationUiState

    /** Active route navigation with live telemetry, turn-by-turn guidance, and progress tracking. */
    data class Navigating(
        val session: NavigationSession,
        val route: PlannedRoute,
        val progress: RouteProgress,
        val destinationName: String,
        val destinationLatitude: Double,
        val destinationLongitude: Double,
        val originLatitude: Double? = null,
        val originLongitude: Double? = null,
        val currentInstruction: RouteInstruction? = null,
        val nextInstruction: RouteInstruction? = null,
        val distanceToNextManeuverMeters: Double = 0.0,
        val remainingDistanceMeters: Double = 0.0,
        val remainingDurationSeconds: Long = 0L,
        val etaTimestampEpochMs: Long = 0L,
        val status: NavigationStatus = NavigationStatus.ON_ROUTE,
        val isArrived: Boolean = false,
        val cameraFollowMode: CameraFollowMode = CameraFollowMode.FOLLOW_HEAD_UP,
        val isOffRouteWarning: Boolean = false,
        val isRerouting: Boolean = false,
        val isGpsLost: Boolean = false,
        val rerouteErrorMessage: String? = null
    ) : NavigationUiState
}
