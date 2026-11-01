package com.abrar.motolog.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.repository.NavigationSessionRepository
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.shared.domain.engine.RouteProgressEngine
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteProgress
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

/**
 * ViewModel managing active route navigation, real-time GPS progress matching,
 * turn-by-turn guidance, and camera perspective modes.
 *
 * Keeps navigation logic cleanly decoupled from LiveViewModel's ride telemetry.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val navigationSessionRepository: NavigationSessionRepository,
    private val trackingRepository: TrackingRepository,
    private val settingsRepository: SettingsRepository,
    private val routeProgressEngine: RouteProgressEngine = RouteProgressEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NavigationUiState>(NavigationUiState.Idle)
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var lastCheckpointTimestampMs: Long = 0L
    private var lastCheckpointDistanceMeters: Double = 0.0

    val useMetricUnits: StateFlow<Boolean> = settingsRepository.useMetricUnits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = true
        )

    init {
        // 1. Observe active persistent navigation session
        viewModelScope.launch {
            navigationSessionRepository.observeActiveSessionWithRoute().collect { sessionWithRoute ->
                if (sessionWithRoute == null) {
                    _uiState.value = NavigationUiState.Idle
                    routeProgressEngine.reset()
                } else {
                    val (session, route) = sessionWithRoute
                    if (session.endedAtTimestamp != null || route == null) {
                        _uiState.value = NavigationUiState.Idle
                        routeProgressEngine.reset()
                    } else {
                        setupActiveRoute(session, route)
                    }
                }
            }
        }

        // 2. Observe location fixes from TrackingRepository
        viewModelScope.launch {
            trackingRepository.sessionState.collect { sessionState ->
                val current = _uiState.value
                if (current is NavigationUiState.Navigating) {
                    when (sessionState) {
                        is TrackingSessionState.Tracking -> {
                            val lat = sessionState.latitude
                            val lon = sessionState.longitude
                            val speedKmh = sessionState.stats.currentSpeedKmh
                            val accuracy = sessionState.accuracyMeters
                            val isGpsLost = sessionState.isGpsLost

                            if (lat != null && lon != null && !isGpsLost) {
                                val fix = LocationPoint(
                                    latitude = lat,
                                    longitude = lon,
                                    speedMps = (speedKmh / 3.6).toFloat(),
                                    accuracyMeters = accuracy,
                                    timestamp = System.currentTimeMillis()
                                )
                                processLocationUpdate(fix, current, sessionState.stats.currentSpeedKmh)
                            } else if (isGpsLost && !current.isGpsLost) {
                                _uiState.update { state ->
                                    if (state is NavigationUiState.Navigating) {
                                        state.copy(isGpsLost = true)
                                    } else state
                                }
                            }
                        }
                        is TrackingSessionState.WaitingForGps -> {
                            _uiState.update { state ->
                                if (state is NavigationUiState.Navigating) {
                                    state.copy(isGpsLost = true)
                                } else state
                            }
                        }
                        else -> {
                            // In Idle/Stopped, keep navigation session intact
                        }
                    }
                }
            }
        }
    }

    private fun setupActiveRoute(
        session: com.abrar.motolog.shared.domain.model.NavigationSession,
        route: PlannedRoute
    ) {
        if (routeProgressEngine.currentRoute?.id != route.id) {
            routeProgressEngine.setRoute(route)
        }

        val initialProgress = routeProgressEngine.lastProgress ?: RouteProgress(
            nearestPointIndex = 0,
            distanceAlongRouteMeters = 0.0,
            remainingDistanceMeters = route.totalDistanceMeters,
            progressFraction = 0f,
            crossTrackDistanceMeters = 0.0,
            status = NavigationStatus.ON_ROUTE,
            currentInstructionIndex = if (route.instructions.isNotEmpty()) 0 else null,
            timestamp = System.currentTimeMillis()
        )

        val destinationPoint = route.geometry.lastOrNull()
        val originPoint = route.geometry.firstOrNull()

        val destName = route.instructions.lastOrNull()?.streetName
            ?: route.instructions.lastOrNull()?.instructionText
            ?: "Destination"

        val currInstr = initialProgress.currentInstructionIndex?.let { route.instructions.getOrNull(it) }
        val nextInstr = initialProgress.currentInstructionIndex?.let { route.instructions.getOrNull(it + 1) }
        val distToManeuver = initialProgress.currentInstructionIndex?.let {
            routeProgressEngine.getDistanceToInstruction(it)
        } ?: 0.0

        val currentFollowMode = (_uiState.value as? NavigationUiState.Navigating)?.cameraFollowMode
            ?: CameraFollowMode.FOLLOW_HEAD_UP

        _uiState.value = NavigationUiState.Navigating(
            session = session,
            route = route,
            progress = initialProgress,
            destinationName = destName,
            destinationLatitude = destinationPoint?.latitude ?: 0.0,
            destinationLongitude = destinationPoint?.longitude ?: 0.0,
            originLatitude = originPoint?.latitude,
            originLongitude = originPoint?.longitude,
            currentInstruction = currInstr,
            nextInstruction = nextInstr,
            distanceToNextManeuverMeters = distToManeuver,
            remainingDistanceMeters = initialProgress.remainingDistanceMeters,
            remainingDurationSeconds = route.expectedDurationSeconds,
            etaTimestampEpochMs = System.currentTimeMillis() + (route.expectedDurationSeconds * 1000L),
            status = initialProgress.status,
            isArrived = initialProgress.status == NavigationStatus.ARRIVED,
            cameraFollowMode = currentFollowMode,
            isOffRouteWarning = initialProgress.status == NavigationStatus.OFF_ROUTE_WARNING,
            isRerouting = initialProgress.status == NavigationStatus.OFF_ROUTE_REROUTE_NEEDED,
            isGpsLost = false
        )
    }

    private fun processLocationUpdate(
        fix: LocationPoint,
        current: NavigationUiState.Navigating,
        currentSpeedKmh: Double
    ) {
        val updatedProgress = routeProgressEngine.onLocationUpdate(fix)

        val currIdx = updatedProgress.currentInstructionIndex
        val currentInstruction = currIdx?.let { current.route.instructions.getOrNull(it) }
        val nextInstruction = currIdx?.let { current.route.instructions.getOrNull(it + 1) }
        val distToManeuver = currIdx?.let { routeProgressEngine.getDistanceToInstruction(it) } ?: 0.0

        // Remaining duration & ETA calculation
        val remainingDist = updatedProgress.remainingDistanceMeters
        val totalDist = max(1.0, current.route.totalDistanceMeters)
        val initialDuration = current.route.expectedDurationSeconds

        val remainingDurationSeconds: Long = if (currentSpeedKmh >= 15.0) {
            val speedMps = currentSpeedKmh / 3.6
            (remainingDist / speedMps).toLong().coerceAtLeast(0L)
        } else {
            // Proportionally interpolate provider's expected duration
            val fractionRemaining = (remainingDist / totalDist).coerceIn(0.0, 1.0)
            (initialDuration * fractionRemaining).toLong().coerceAtLeast(0L)
        }

        val etaEpochMs = System.currentTimeMillis() + (remainingDurationSeconds * 1000L)
        val isArrived = updatedProgress.status == NavigationStatus.ARRIVED

        _uiState.value = current.copy(
            progress = updatedProgress,
            currentInstruction = currentInstruction,
            nextInstruction = nextInstruction,
            distanceToNextManeuverMeters = distToManeuver,
            remainingDistanceMeters = remainingDist,
            remainingDurationSeconds = remainingDurationSeconds,
            etaTimestampEpochMs = etaEpochMs,
            status = updatedProgress.status,
            isArrived = isArrived,
            isOffRouteWarning = updatedProgress.status == NavigationStatus.OFF_ROUTE_WARNING,
            isRerouting = updatedProgress.status == NavigationStatus.OFF_ROUTE_REROUTE_NEEDED,
            isGpsLost = false
        )

        // Throttled checkpointing to Room database
        val nowMs = System.currentTimeMillis()
        val timeDelta = nowMs - lastCheckpointTimestampMs
        val distanceDelta = kotlin.math.abs(updatedProgress.distanceAlongRouteMeters - lastCheckpointDistanceMeters)
        val statusChanged = updatedProgress.status != current.progress.status

        if (timeDelta >= 5_000L || distanceDelta >= 25.0 || statusChanged || isArrived) {
            lastCheckpointTimestampMs = nowMs
            lastCheckpointDistanceMeters = updatedProgress.distanceAlongRouteMeters
            viewModelScope.launch {
                navigationSessionRepository.checkpointProgress(
                    sessionId = current.session.sessionId,
                    progress = updatedProgress
                )
                if (isArrived && current.session.status != NavigationStatus.ARRIVED) {
                    navigationSessionRepository.finishSession(
                        sessionId = current.session.sessionId,
                        finalStatus = NavigationStatus.ARRIVED
                    )
                }
            }
        }
    }

    /**
     * Stop and finish active navigation. Does NOT stop ride recording.
     */
    fun endNavigation() {
        val current = _uiState.value
        if (current is NavigationUiState.Navigating) {
            viewModelScope.launch {
                navigationSessionRepository.finishSession(
                    sessionId = current.session.sessionId,
                    finalStatus = if (current.isArrived) NavigationStatus.ARRIVED else NavigationStatus.ON_ROUTE
                )
                routeProgressEngine.reset()
                _uiState.value = NavigationUiState.Idle
            }
        }
    }

    /**
     * Set explicit camera follow mode (Head-Up, North-Up, or Free).
     */
    fun setCameraFollowMode(mode: CameraFollowMode) {
        _uiState.update { state ->
            if (state is NavigationUiState.Navigating) {
                state.copy(cameraFollowMode = mode)
            } else state
        }
    }

    /**
     * Toggle camera orientation between Head-Up (3D bearing) and North-Up.
     */
    fun toggleCameraFollowMode() {
        _uiState.update { state ->
            if (state is NavigationUiState.Navigating) {
                val nextMode = when (state.cameraFollowMode) {
                    CameraFollowMode.FOLLOW_HEAD_UP -> CameraFollowMode.FOLLOW_NORTH_UP
                    CameraFollowMode.FOLLOW_NORTH_UP -> CameraFollowMode.FOLLOW_HEAD_UP
                    CameraFollowMode.FREE -> CameraFollowMode.FOLLOW_HEAD_UP
                }
                state.copy(cameraFollowMode = nextMode)
            } else state
        }
    }

    /**
     * Re-center camera onto current rider position with auto-follow engaged.
     */
    fun recenterCamera() {
        setCameraFollowMode(CameraFollowMode.FOLLOW_HEAD_UP)
    }

    /**
     * Called when the rider gestures or drags the map manually.
     */
    fun onUserMapInteraction() {
        _uiState.update { state ->
            if (state is NavigationUiState.Navigating && state.cameraFollowMode != CameraFollowMode.FREE) {
                state.copy(cameraFollowMode = CameraFollowMode.FREE)
            } else state
        }
    }

    /**
     * Dismiss arrival dialog while preserving navigation state.
     */
    fun dismissArrival() {
        _uiState.update { state ->
            if (state is NavigationUiState.Navigating) {
                state.copy(isArrived = false)
            } else state
        }
    }
}
