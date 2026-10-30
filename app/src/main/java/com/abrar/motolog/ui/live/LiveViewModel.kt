package com.abrar.motolog.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.domain.repository.GarageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

import com.abrar.motolog.shared.domain.map.MapStyleProvider
import com.abrar.motolog.ui.theme.ThemeMode

/**
 * ViewModel for the Live ride tracking screen.
 *
 * Does NOT hold the tracking session or GPS math itself; observes shared state
 * from [TrackingRepository] and forwards user intent to the foreground service.
 */
@HiltViewModel
class LiveViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val settingsRepository: SettingsRepository,
    private val garageRepository: GarageRepository? = null,
    private val ridePointDao: RidePointDao? = null,
    val mapStyleProvider: MapStyleProvider? = null
) : ViewModel() {

    constructor(
        trackingRepository: TrackingRepository,
        settingsRepository: SettingsRepository
    ) : this(trackingRepository, settingsRepository, null, null, null)

    private val _uiState = MutableStateFlow<LiveUiState>(LiveUiState.Idle)
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var activeRidePointsJob: Job? = null
    private val _routePoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    private var currentActiveBikeName: String? = null
    private var currentTrackingRideId: Long? = null

    val isDisclaimerAccepted: StateFlow<Boolean> = settingsRepository.disclaimerAccepted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false
        )

    val keepScreenOn: StateFlow<Boolean> = settingsRepository.keepScreenOn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = true
        )

    val batteryGuidanceSeen: StateFlow<Boolean> = settingsRepository.batteryGuidanceSeen
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false
        )

    val useMetricUnits: StateFlow<Boolean> = settingsRepository.useMetricUnits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = true
        )

    val defaultMapTheme: StateFlow<com.abrar.motolog.shared.domain.model.MapThemePreference> = settingsRepository.defaultMapTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = com.abrar.motolog.shared.domain.model.MapThemePreference.LIGHT
        )

    val dashboardTextColor: StateFlow<Long> = settingsRepository.dashboardTextColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SettingsRepository.DEFAULT_DASHBOARD_TEXT_COLOR
        )

    val dashboardBgColor: StateFlow<Long> = settingsRepository.dashboardBgColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SettingsRepository.DEFAULT_DASHBOARD_BG_COLOR
        )

    val dashboardAccentColor: StateFlow<Long> = settingsRepository.dashboardAccentColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SettingsRepository.DEFAULT_DASHBOARD_ACCENT_COLOR
        )

    val dashboardComponentColor: StateFlow<Long> = settingsRepository.dashboardComponentColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SettingsRepository.DEFAULT_DASHBOARD_COMPONENT_COLOR
        )

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ThemeMode.RACING_TFT
        )

    init {
        // Observe current active bike name from Garage
        viewModelScope.launch {
            garageRepository?.currentBikeId?.collect { bikeId ->
                currentActiveBikeName = if (bikeId != null) {
                    garageRepository.getBikeById(bikeId)?.name
                } else {
                    null
                }
                val current = _uiState.value
                if (current is LiveUiState.Tracking) {
                    _uiState.value = current.copy(bikeName = currentActiveBikeName)
                }
            }
        }

        // Observe repository session state from foreground service
        viewModelScope.launch {
            trackingRepository.sessionState.collect { sessionState ->
                // Do not override an active recovery prompt with Idle
                if (_uiState.value is LiveUiState.RecoveryPrompt && sessionState is TrackingSessionState.Idle) {
                    return@collect
                }
                _uiState.value = when (sessionState) {
                    is TrackingSessionState.Idle -> {
                        stopObservingRidePoints()
                        LiveUiState.Idle
                    }
                    is TrackingSessionState.WaitingForGps -> {
                        stopObservingRidePoints()
                        LiveUiState.WaitingForGps(
                            currentAccuracyMeters = sessionState.accuracyMeters
                        )
                    }
                    is TrackingSessionState.Tracking -> {
                        startObservingRidePointsIfNeeded(sessionState.rideId)
                        val coords = _routePoints.value.toMutableList()
                        val lat = sessionState.latitude
                        val lon = sessionState.longitude
                        if (lat != null && lon != null) {
                            val newPoint = Pair(lat, lon)
                            if (coords.isEmpty() || coords.last() != newPoint) {
                                coords.add(newPoint)
                                if (coords.size > 2000) {
                                    // Use simplified Douglas-Peucker to preserve route shape
                                    _routePoints.value = simplifyRoute(coords, 0.00005)
                                } else {
                                    _routePoints.value = coords
                                }
                            }
                        }
                        LiveUiState.Tracking(
                            speedKmh = sessionState.stats.currentSpeedKmh,
                            accuracyMeters = sessionState.accuracyMeters,
                            isPaused = sessionState.isPaused,
                            pauseState = sessionState.pauseState,
                            isGpsLost = sessionState.isGpsLost,
                            stats = sessionState.stats,
                            isSpeedAlert = sessionState.isSpeedAlert,
                            latitude = sessionState.latitude,
                            longitude = sessionState.longitude,
                            routeCoordinates = _routePoints.value,
                            bikeName = currentActiveBikeName,
                            altitudeMeters = sessionState.altitudeMeters,
                            bearingDegrees = sessionState.bearingDegrees
                        )
                    }
                    is TrackingSessionState.Stopped -> {
                        stopObservingRidePoints()
                        LiveUiState.Stopped(
                            stats = sessionState.stats
                        )
                    }
                }
            }
        }

        // Check for unfinished ride on startup (crash/force-kill recovery)
        checkActiveRideRecovery()
    }

    private fun startObservingRidePointsIfNeeded(rideId: Long) {
        if (currentTrackingRideId == rideId) return
        currentTrackingRideId = rideId
        activeRidePointsJob?.cancel()
        activeRidePointsJob = viewModelScope.launch(Dispatchers.IO) {
            val entities = ridePointDao?.getPointsForRideOnce(rideId) ?: emptyList()
            val initial = entities.map { Pair(it.latitude, it.longitude) }
            _routePoints.value = initial
        }
    }

    private fun stopObservingRidePoints() {
        activeRidePointsJob?.cancel()
        activeRidePointsJob = null
        currentTrackingRideId = null
        _routePoints.value = emptyList()
    }

    private fun checkActiveRideRecovery() {
        viewModelScope.launch {
            if (trackingRepository.sessionState.value is TrackingSessionState.Idle) {
                val activeRide = trackingRepository.checkActiveRideOnStartup()
                if (activeRide != null) {
                    _uiState.value = LiveUiState.RecoveryPrompt(activeRide)
                }
            }
        }
    }

    fun startTracking() {
        trackingRepository.startTracking()
    }

    fun pauseTracking() {
        trackingRepository.pauseTracking()
    }

    fun resumeTracking() {
        trackingRepository.resumeTracking()
    }

    fun stopTracking() {
        trackingRepository.stopTracking()
    }

    fun resetToIdle() {
        trackingRepository.resetToIdle()
        _uiState.value = LiveUiState.Idle
    }

    fun recoverRide(activeRide: RideEntity) {
        viewModelScope.launch {
            val stats = trackingRepository.recoverRide(activeRide)
            _uiState.value = LiveUiState.Stopped(stats)
        }
    }

    fun discardRide(activeRide: RideEntity) {
        viewModelScope.launch {
            trackingRepository.discardRide(activeRide)
            _uiState.value = LiveUiState.Idle
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch {
            settingsRepository.setDisclaimerAccepted(true)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(enabled)
        }
    }

    fun setBatteryGuidanceSeen(seen: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBatteryGuidanceSeen(seen)
        }
    }

    /**
     * Simplified iterative Douglas-Peucker for live route display thinning.
     * Operates in degree space (sufficient for display-only visual accuracy).
     */
    private fun simplifyRoute(
        points: List<Pair<Double, Double>>,
        epsilon: Double
    ): List<Pair<Double, Double>> {
        if (points.size <= 2) return points
        val keep = BooleanArray(points.size) { false }
        keep[0] = true
        keep[points.lastIndex] = true

        // Iterative stack-based RDP
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to points.lastIndex)

        while (stack.isNotEmpty()) {
            val (startIdx, endIdx) = stack.removeLast()
            if (endIdx - startIdx < 2) continue

            var maxDist = 0.0
            var maxIdx = startIdx
            val s = points[startIdx]
            val e = points[endIdx]
            val dx = e.first - s.first
            val dy = e.second - s.second
            val lineLenSq = dx * dx + dy * dy

            for (i in (startIdx + 1) until endIdx) {
                val p = points[i]
                val dist = if (lineLenSq == 0.0) {
                    val px = p.first - s.first
                    val py = p.second - s.second
                    kotlin.math.sqrt(px * px + py * py)
                } else {
                    val t = ((p.first - s.first) * dx + (p.second - s.second) * dy) / lineLenSq
                    val tc = t.coerceIn(0.0, 1.0)
                    val projX = s.first + tc * dx
                    val projY = s.second + tc * dy
                    val px = p.first - projX
                    val py = p.second - projY
                    kotlin.math.sqrt(px * px + py * py)
                }
                if (dist > maxDist) {
                    maxDist = dist
                    maxIdx = i
                }
            }

            if (maxDist > epsilon) {
                keep[maxIdx] = true
                stack.addLast(startIdx to maxIdx)
                stack.addLast(maxIdx to endIdx)
            }
        }

        return points.filterIndexed { index, _ -> keep[index] }
    }
}
