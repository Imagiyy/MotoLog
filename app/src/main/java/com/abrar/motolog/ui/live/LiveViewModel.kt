package com.abrar.motolog.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.domain.repository.TrackingSessionState
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
import kotlinx.coroutines.Job

import com.abrar.motolog.domain.map.MapStyleProvider
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

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ThemeMode.DARK
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
                        if (sessionState.latitude != null && sessionState.longitude != null) {
                            val newPoint = Pair(sessionState.latitude, sessionState.longitude)
                            if (coords.isEmpty() || coords.last() != newPoint) {
                                coords.add(newPoint)
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
                            routeCoordinates = coords,
                            bikeName = currentActiveBikeName
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

        // Observe route points updates from DB
        viewModelScope.launch {
            _routePoints.collect { points ->
                val current = _uiState.value
                if (current is LiveUiState.Tracking) {
                    val coords = points.toMutableList()
                    if (current.latitude != null && current.longitude != null) {
                        val newPoint = Pair(current.latitude, current.longitude)
                        if (coords.isEmpty() || coords.last() != newPoint) {
                            coords.add(newPoint)
                        }
                    }
                    _uiState.value = current.copy(routeCoordinates = coords)
                }
            }
        }

        // Check for unfinished ride on startup (crash/force-kill recovery)
        checkActiveRideRecovery()
    }

    private fun startObservingRidePointsIfNeeded(rideId: Long) {
        if (currentTrackingRideId == rideId && activeRidePointsJob != null) return
        currentTrackingRideId = rideId
        activeRidePointsJob?.cancel()
        activeRidePointsJob = viewModelScope.launch {
            ridePointDao?.getPointsForRide(rideId)?.collect { entities ->
                _routePoints.value = entities.map { Pair(it.latitude, it.longitude) }
            }
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
}
