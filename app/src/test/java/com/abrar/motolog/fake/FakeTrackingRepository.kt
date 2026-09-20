package com.abrar.motolog.fake

import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.domain.model.RideStats
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.domain.repository.TrackingSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeTrackingRepository : TrackingRepository {

    private val _sessionState = MutableStateFlow<TrackingSessionState>(TrackingSessionState.Idle)
    override val sessionState: StateFlow<TrackingSessionState> = _sessionState.asStateFlow()

    var startCalled = false
    var pauseCalled = false
    var resumeCalled = false
    var stopCalled = false
    var resetCalled = false
    var activeRideToReturn: RideEntity? = null
    var recoveredRideCalled = false
    var discardedRideCalled = false

    override fun startTracking() {
        startCalled = true
        _sessionState.value = TrackingSessionState.WaitingForGps()
    }

    override fun pauseTracking() {
        pauseCalled = true
        val current = _sessionState.value
        if (current is TrackingSessionState.Tracking) {
            _sessionState.value = current.copy(isPaused = true)
        }
    }

    override fun resumeTracking() {
        resumeCalled = true
        val current = _sessionState.value
        if (current is TrackingSessionState.Tracking) {
            _sessionState.value = current.copy(isPaused = false)
        }
    }

    override fun stopTracking() {
        stopCalled = true
        val stats = (_sessionState.value as? TrackingSessionState.Tracking)?.stats ?: RideStats()
        _sessionState.value = TrackingSessionState.Stopped(stats)
    }

    override fun resetToIdle() {
        resetCalled = true
        _sessionState.value = TrackingSessionState.Idle
    }

    override suspend fun checkActiveRideOnStartup(): RideEntity? {
        return activeRideToReturn
    }

    override suspend fun recoverRide(activeRide: RideEntity): RideStats {
        recoveredRideCalled = true
        val stats = RideStats(totalDistanceMeters = activeRide.distanceMeters)
        _sessionState.value = TrackingSessionState.Stopped(stats)
        return stats
    }

    override suspend fun discardRide(activeRide: RideEntity) {
        discardedRideCalled = true
        _sessionState.value = TrackingSessionState.Idle
    }

    override fun updateState(state: TrackingSessionState) {
        _sessionState.value = state
    }
}
