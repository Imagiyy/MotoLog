package com.abrar.motolog.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.domain.engine.RideCalculator
import com.abrar.motolog.domain.model.GpsPoint
import com.abrar.motolog.domain.model.RideStats
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.domain.repository.TrackingSessionState
import com.abrar.motolog.service.TrackingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton implementation of [TrackingRepository] bridging UI and TrackingService.
 */
@Singleton
class TrackingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rideDao: RideDao,
    private val ridePointDao: RidePointDao,
    private val maintenanceNotificationManager: javax.inject.Provider<com.abrar.motolog.service.MaintenanceNotificationManager>
) : TrackingRepository {

    private val _sessionState = MutableStateFlow<TrackingSessionState>(TrackingSessionState.Idle)
    override val sessionState: StateFlow<TrackingSessionState> = _sessionState.asStateFlow()

    override fun startTracking() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun pauseTracking() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    override fun resumeTracking() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_RESUME
        }
        context.startService(intent)
    }

    override fun stopTracking() {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun resetToIdle() {
        _sessionState.value = TrackingSessionState.Idle
    }

    override suspend fun checkActiveRideOnStartup(): RideEntity? = withContext(Dispatchers.IO) {
        rideDao.findActiveRide()
    }

    override suspend fun recoverRide(activeRide: RideEntity): RideStats = withContext(Dispatchers.IO) {
        val rawPoints = ridePointDao.getPointsForRideOnce(activeRide.id)
        val gpsPoints = rawPoints.map { p ->
            GpsPoint(
                timestampEpochMs = p.timestamp,
                latitude = p.latitude,
                longitude = p.longitude,
                speedMps = p.speedMs.toFloat(),
                accuracyMeters = p.accuracyMeters,
                isPaused = p.isPaused,
                isGap = p.isGap
            )
        }

        // Replay through pure RideCalculator engine
        val recoveredStats = RideCalculator.processAll(gpsPoints)
        val endTime = rawPoints.lastOrNull()?.timestamp ?: System.currentTimeMillis()

        val rideName = if (activeRide.name.isBlank()) {
            com.abrar.motolog.domain.util.RideNameGenerator.defaultNameForTimestamp(activeRide.startTime)
        } else {
            activeRide.name
        }

        val recoveredRide = activeRide.copy(
            name = rideName,
            endTime = endTime,
            distanceMeters = recoveredStats.totalDistanceMeters,
            elapsedTimeMs = recoveredStats.elapsedTimeMs,
            movingTimeMs = recoveredStats.movingTimeMs,
            avgMovingSpeedMs = recoveredStats.avgMovingSpeedKmh / 3.6,
            overallAvgSpeedMs = recoveredStats.avgOverallSpeedKmh / 3.6,
            maxSpeedMs = recoveredStats.maxSpeedKmh / 3.6,
            status = RideStatus.RECOVERED
        )
        rideDao.update(recoveredRide)

        _sessionState.value = TrackingSessionState.Stopped(recoveredStats)
        try {
            maintenanceNotificationManager.get().checkAndNotify(activeRide.bikeId)
        } catch (e: Exception) {
            // Non-critical
        }
        recoveredStats
    }

    override suspend fun discardRide(activeRide: RideEntity) = withContext(Dispatchers.IO) {
        ridePointDao.deletePointsForRide(activeRide.id)
        rideDao.delete(activeRide)
        _sessionState.value = TrackingSessionState.Idle
    }

    override fun updateState(state: TrackingSessionState) {
        _sessionState.value = state
    }
}
