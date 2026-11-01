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
import com.abrar.motolog.shared.domain.model.RouteProfile
import com.abrar.motolog.shared.domain.model.RouteRequest
import com.abrar.motolog.shared.domain.model.RoutingError
import com.abrar.motolog.shared.domain.repository.RoutingRepository
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import com.abrar.motolog.shared.domain.time.Clock
import com.abrar.motolog.shared.domain.time.DefaultClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

import com.abrar.motolog.shared.domain.engine.NavigationVoiceEngine
import com.abrar.motolog.shared.domain.voice.VoiceAnnouncer
import com.abrar.motolog.shared.domain.diagnostics.NavigationDiagnostics
import com.abrar.motolog.shared.domain.diagnostics.NavigationDiagnosticsSnapshot

/**
 * ViewModel managing active route navigation, real-time GPS progress matching,
 * turn-by-turn guidance, off-route detection, rerouting, camera perspective modes,
 * and motorcycle acoustic voice guidance with Bluetooth helmet audio focus.
 *
 * Keeps navigation logic cleanly decoupled from LiveViewModel's ride telemetry.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val navigationSessionRepository: NavigationSessionRepository,
    private val trackingRepository: TrackingRepository,
    private val settingsRepository: SettingsRepository,
    private val routingRepository: RoutingRepository,
    private val routeProgressEngine: RouteProgressEngine = RouteProgressEngine(),
    private val clock: Clock = DefaultClock,
    private val voiceEngine: NavigationVoiceEngine = NavigationVoiceEngine(),
    private val voiceAnnouncer: VoiceAnnouncer? = null,
    private val diagnostics: NavigationDiagnostics = NavigationDiagnostics()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NavigationUiState>(NavigationUiState.Idle)
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var lastCheckpointTimestampMs: Long = 0L
    private var lastCheckpointDistanceMeters: Double = 0.0

    private var latestFix: LocationPoint? = null
    private var rerouteJob: Job? = null
    private var lastRerouteFailureTimestampMs: Long = 0L

    val useMetricUnits: StateFlow<Boolean> = settingsRepository.useMetricUnits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = true
        )

    val autoRerouteEnabled: StateFlow<Boolean> = settingsRepository.routingAutoRerouteEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
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
                                val now = clock.currentTimeMillis()
                                val fix = LocationPoint(
                                    latitude = lat,
                                    longitude = lon,
                                    speedMps = (speedKmh / 3.6).toFloat(),
                                    accuracyMeters = accuracy,
                                    timestamp = now,
                                    elapsedTimestampMs = now
                                )
                                processLocationUpdate(fix, current, sessionState.stats.currentSpeedKmh)
                            } else if (isGpsLost && !current.isGpsLost) {
                                diagnostics.recordGpsSignalLoss()
                                _uiState.update { state ->
                                    if (state is NavigationUiState.Navigating) {
                                        state.copy(isGpsLost = true)
                                    } else state
                                }
                            }
                        }
                        is TrackingSessionState.WaitingForGps -> {
                            if (!current.isGpsLost) {
                                diagnostics.recordGpsSignalLoss()
                            }
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

        // 3. Observe voice announcements preference
        viewModelScope.launch {
            settingsRepository.voiceAnnouncementsEnabled.collect { enabled ->
                voiceAnnouncer?.setGloballyEnabled(enabled)
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
            isGpsLost = false,
            isMuted = voiceAnnouncer?.isMuted() ?: false
        )
    }

    private fun processLocationUpdate(
        fix: LocationPoint,
        current: NavigationUiState.Navigating,
        currentSpeedKmh: Double
    ) {
        latestFix = fix
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

        val nowMs = clock.currentTimeMillis()
        val etaEpochMs = nowMs + (remainingDurationSeconds * 1000L)
        val isArrived = updatedProgress.status == NavigationStatus.ARRIVED
        val isOffRoute = updatedProgress.status == NavigationStatus.OFF_ROUTE_WARNING
        val isRerouteNeeded = updatedProgress.status == NavigationStatus.OFF_ROUTE_REROUTE_NEEDED

        if (isOffRoute && !current.isOffRouteWarning) {
            diagnostics.recordOffRouteWarning()
        }
        if (isArrived && !current.isArrived) {
            diagnostics.recordArrival()
        }

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
            isOffRouteWarning = isOffRoute,
            isRerouting = isRerouteNeeded || current.isRerouting,
            isGpsLost = false
        )

        // Trigger automatic reroute if needed, auto-reroute is enabled, and backoff has elapsed
        if (isRerouteNeeded && autoRerouteEnabled.value) {
            val backoffElapsed = lastRerouteFailureTimestampMs == 0L || (nowMs - lastRerouteFailureTimestampMs >= 15_000L)
            if (rerouteJob?.isActive != true && backoffElapsed) {
                triggerReroute(current, fix)
            }
        }

        // Evaluate acoustic voice guidance
        val announcement = voiceEngine.evaluateAnnouncement(
            instruction = currentInstruction,
            instructionIndex = currIdx,
            distanceToManeuverMeters = distToManeuver,
            currentSpeedKmh = currentSpeedKmh,
            isMetric = useMetricUnits.value,
            status = updatedProgress.status,
            nowMs = nowMs
        )
        if (announcement != null) {
            diagnostics.recordVoiceAnnouncement()
            voiceAnnouncer?.speak(announcement.text, isUrgent = announcement.isUrgent)
        }

        // Throttled checkpointing to Room database
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
     * Dispatch a recalculation / reroute request from the rider's current position to the destination.
     * Retains the current route line and state until recalculation succeeds.
     */
    fun triggerReroute(current: NavigationUiState.Navigating, originFix: LocationPoint) {
        diagnostics.recordRerouteTriggered()
        rerouteJob?.cancel()
        val nowMs = clock.currentTimeMillis()
        routeProgressEngine.markRerouteTriggered(nowMs)

        _uiState.update { state ->
            if (state is NavigationUiState.Navigating) {
                state.copy(isRerouting = true, rerouteErrorMessage = null)
            } else state
        }

        rerouteJob = viewModelScope.launch {
            val destination = LocationPoint(
                latitude = current.destinationLatitude,
                longitude = current.destinationLongitude
            )
            val request = RouteRequest(
                origin = originFix,
                destination = destination,
                travelProfile = RouteProfile.MOTORCYCLE,
                alternatives = false,
                requestTimestamp = nowMs
            )

            val result = routingRepository.calculateRoute(request)
            result.fold(
                onSuccess = { routes ->
                    val newRoute = routes.firstOrNull()
                    if (newRoute != null) {
                        applyRerouteSuccess(current, newRoute)
                    } else {
                        applyRerouteFailure("No route returned for recalculation. Retaining current route.")
                    }
                },
                onFailure = { error ->
                    val message = when (error) {
                        is RoutingError.Timeout -> "Reroute timed out. Retaining current route."
                        is RoutingError.RateLimitExceeded -> "Routing rate limit reached. Retaining current route."
                        is RoutingError.NoRouteFound -> "No alternative route found. Retaining current route."
                        else -> "Recalculation failed (${error.message ?: "network error"}). Retaining current route."
                    }
                    applyRerouteFailure(message)
                }
            )
        }
    }

    private suspend fun applyRerouteSuccess(
        current: NavigationUiState.Navigating,
        newRoute: PlannedRoute
    ) {
        diagnostics.recordRerouteSuccess()
        navigationSessionRepository.incrementRerouteCount(current.session.sessionId)
        val updatedSession = current.session.copy(
            rerouteCount = current.session.rerouteCount + 1,
            plannedRouteId = newRoute.id
        )
        navigationSessionRepository.saveActiveSession(updatedSession, newRoute)

        routeProgressEngine.setRoute(newRoute, isReroute = true)

        val nowMs = clock.currentTimeMillis()
        val newProgress = routeProgressEngine.lastProgress ?: RouteProgress(
            nearestPointIndex = 0,
            distanceAlongRouteMeters = 0.0,
            remainingDistanceMeters = newRoute.totalDistanceMeters,
            progressFraction = 0f,
            crossTrackDistanceMeters = 0.0,
            status = NavigationStatus.ON_ROUTE,
            timestamp = nowMs
        )

        val currInstr = newRoute.instructions.firstOrNull()
        val nextInstr = newRoute.instructions.getOrNull(1)
        val distToManeuver = if (newRoute.instructions.isNotEmpty()) {
            routeProgressEngine.getDistanceToInstruction(0)
        } else 0.0

        _uiState.update { state ->
            if (state is NavigationUiState.Navigating) {
                state.copy(
                    session = updatedSession,
                    route = newRoute,
                    progress = newProgress,
                    currentInstruction = currInstr,
                    nextInstruction = nextInstr,
                    distanceToNextManeuverMeters = distToManeuver,
                    remainingDistanceMeters = newRoute.totalDistanceMeters,
                    remainingDurationSeconds = newRoute.expectedDurationSeconds,
                    etaTimestampEpochMs = nowMs + (newRoute.expectedDurationSeconds * 1000L),
                    status = NavigationStatus.ON_ROUTE,
                    isOffRouteWarning = false,
                    isRerouting = false,
                    rerouteErrorMessage = null
                )
            } else state
        }

        val nextStreet = newRoute.instructions.firstOrNull()?.streetName
        val announcement = voiceEngine.onRerouteCompleted(nextStreet)
        voiceAnnouncer?.speak(announcement.text, isUrgent = announcement.isUrgent)
    }

    private fun applyRerouteFailure(message: String) {
        diagnostics.recordRerouteFailure()
        lastRerouteFailureTimestampMs = clock.currentTimeMillis()
        _uiState.update { state ->
            if (state is NavigationUiState.Navigating) {
                state.copy(
                    isRerouting = false,
                    rerouteErrorMessage = message
                )
            } else state
        }
    }

    /**
     * Request a manual reroute from the UI using the latest known rider coordinate.
     * Bypasses the network failure backoff timer.
     */
    fun requestManualReroute() {
        val current = _uiState.value as? NavigationUiState.Navigating ?: return
        val fix = latestFix ?: LocationPoint(
            latitude = current.originLatitude ?: current.route.geometry.firstOrNull()?.latitude ?: 0.0,
            longitude = current.originLongitude ?: current.route.geometry.firstOrNull()?.longitude ?: 0.0
        )
        lastRerouteFailureTimestampMs = 0L
        triggerReroute(current, fix)
    }

    /**
     * Dismiss the reroute error notification.
     */
    fun dismissRerouteError() {
        _uiState.update { state ->
            if (state is NavigationUiState.Navigating) {
                state.copy(rerouteErrorMessage = null)
            } else state
        }
    }

    /**
     * Stop and finish active navigation. Does NOT stop ride recording.
     * Cancels any pending or in-flight reroute network requests.
     */
    fun endNavigation() {
        voiceAnnouncer?.stop()
        voiceEngine.reset()
        rerouteJob?.cancel()
        routingRepository.cancelActiveRequests()
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

    override fun onCleared() {
        super.onCleared()
        voiceAnnouncer?.stop()
        rerouteJob?.cancel()
        routingRepository.cancelActiveRequests()
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

    /**
     * Toggle voice guidance mute for the active navigation session.
     */
    fun toggleMute() {
        val current = _uiState.value
        if (current is NavigationUiState.Navigating) {
            val nextMuted = !current.isMuted
            voiceAnnouncer?.setMuted(nextMuted)
            _uiState.value = current.copy(isMuted = nextMuted)
        }
    }

    /**
     * Export anonymized diagnostics counters as formatted JSON.
     * Contains zero coordinates, PII, or tokens.
     */
    fun getDiagnosticsJson(): String = diagnostics.exportDiagnosticsJson(clock.currentTimeMillis())

    /**
     * Get snapshot of current diagnostics counters.
     */
    fun getDiagnosticsSnapshot(): NavigationDiagnosticsSnapshot =
        diagnostics.getSnapshot(clock.currentTimeMillis())
}
