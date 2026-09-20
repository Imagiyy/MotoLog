package com.abrar.motolog.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.location.LocationSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val locationSource: LocationSource,
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

    private var trackingJob: Job? = null

    /**
     * Start location tracking.
     * Transitions immediately to WaitingForGps until accuracy <= 25m fix arrives.
     */
    fun startTracking() {
        if (_uiState.value is LiveUiState.Tracking || _uiState.value is LiveUiState.WaitingForGps) {
            return
        }

        _uiState.value = LiveUiState.WaitingForGps()

        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            locationSource.getLocationUpdates()
                .catch {
                    // Handle failure or cancellation by stopping updates
                    stopTracking()
                }
                .collect { point ->
                    if (point.accuracyMeters > TrackingConstants.MIN_GPS_ACCURACY_METERS) {
                        _uiState.value = LiveUiState.WaitingForGps(currentAccuracyMeters = point.accuracyMeters)
                    } else {
                        val rawSpeedKmh = (point.speedMps ?: 0f) * 3.6
                        val speedKmh = if (rawSpeedKmh < TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH) {
                            0.0
                        } else {
                            rawSpeedKmh
                        }
                        _uiState.value = LiveUiState.Tracking(
                            speedKmh = speedKmh,
                            accuracyMeters = point.accuracyMeters
                        )
                    }
                }
        }
    }

    /**
     * Stop location tracking.
     * Cancels collection and explicitly releases GPS hardware updates.
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        locationSource.stopLocationUpdates()
        _uiState.value = LiveUiState.Stopped
    }

    /**
     * Reset from Stopped back to Idle state.
     */
    fun resetToIdle() {
        stopTracking()
        _uiState.value = LiveUiState.Idle
    }

    /**
     * Acknowledge the one-time rider safety disclaimer.
     */
    fun acceptDisclaimer() {
        viewModelScope.launch {
            settingsRepository.setDisclaimerAccepted(true)
        }
    }

    /**
     * Toggle the keep-screen-on setting.
     */
    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
