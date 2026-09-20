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

/**
 * ViewModel for the Live ride tracking screen.
 *
 * Does NOT hold the tracking session or GPS math itself; observes shared state
 * from [TrackingRepository] and forwards user intent to the foreground service.
 */
@HiltViewModel
class LiveViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiveUiState>(LiveUiState.Idle)
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

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

    init {
        // Observe repository session state from foreground service
        viewModelScope.launch {
            trackingRepository.sessionState.collect { sessionState ->
                // Do not override an active recovery prompt with Idle
                if (_uiState.value is LiveUiState.RecoveryPrompt && sessionState is TrackingSessionState.Idle) {
                    return@collect
                }
                _uiState.value = when (sessionState) {
                    is TrackingSessionState.Idle -> LiveUiState.Idle
                    is TrackingSessionState.WaitingForGps -> LiveUiState.WaitingForGps(
                        currentAccuracyMeters = sessionState.accuracyMeters
                    )
                    is TrackingSessionState.Tracking -> LiveUiState.Tracking(
                        speedKmh = sessionState.stats.currentSpeedKmh,
                        accuracyMeters = sessionState.accuracyMeters,
                        isPaused = sessionState.isPaused,
                        stats = sessionState.stats
                    )
                    is TrackingSessionState.Stopped -> LiveUiState.Stopped(
                        stats = sessionState.stats
                    )
                }
            }
        }

        // Check for unfinished ride on startup (crash/force-kill recovery)
        checkActiveRideRecovery()
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
