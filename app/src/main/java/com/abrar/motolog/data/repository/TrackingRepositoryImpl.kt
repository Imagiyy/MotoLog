package com.abrar.motolog.data.repository

import android.content.Context
import android.content.Intent
import androidx.room.withTransaction
import androidx.core.content.ContextCompat
import com.abrar.motolog.data.local.MotoLogDatabase
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.shared.domain.engine.RideCalculator
import com.abrar.motolog.shared.domain.model.GpsPoint
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
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
    private val database: MotoLogDatabase,
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
        val activeRide = rideDao.findActiveRide() ?: return@withContext null
        val pointCount = ridePointDao.getPointCount(activeRide.id)
        if (pointCount == 0) {
            database.withTransaction {
                ridePointDao.deletePointsForRide(activeRide.id)
                rideDao.delete(activeRide)
            }
            return@withContext null
        }
        activeRide
    }

    override suspend fun recoverRide(activeRide: RideEntity): RideStats = withContext(Dispatchers.IO) {
        // Replay in pages so recovery does not materialize an entire long ride in memory.
        val calculator = RideCalculator()
        var lastPointTimestamp = System.currentTimeMillis()
        var offset = 0
        val pageSize = 1_000
        while (true) {
            val page = ridePointDao.getPointsPagedForRide(activeRide.id, pageSize, offset)
            if (page.isEmpty()) break
            page.forEach { point ->
                calculator.process(
                    GpsPoint(
                        timestampEpochMs = point.timestamp,
                        latitude = point.latitude,
                        longitude = point.longitude,
                        speedMps = point.speedMs.toFloat(),
                        accuracyMeters = point.accuracyMeters,
                        isPaused = point.isPaused,
                        isGap = point.isGap
                    )
                )
                lastPointTimestamp = point.timestamp
            }
            offset += page.size
            if (page.size < pageSize) break
        }

        // Replay through pure RideCalculator engine
        val recoveredStats = calculator.stats
        val endTime = lastPointTimestamp

        val rideName = if (activeRide.name.isBlank()) {
            com.abrar.motolog.shared.domain.util.RideNameGenerator.defaultNameForTimestamp(activeRide.startTime)
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
        database.withTransaction {
            rideDao.update(recoveredRide)
        }

        _sessionState.value = TrackingSessionState.Stopped(recoveredStats)
        try {
            maintenanceNotificationManager.get().checkAndNotify(activeRide.bikeId)
        } catch (e: Exception) {
            // Non-critical
        }
        recoveredStats
    }

    override suspend fun discardRide(activeRide: RideEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
            ridePointDao.deletePointsForRide(activeRide.id)
            rideDao.delete(activeRide)
        }
        _sessionState.value = TrackingSessionState.Idle
    }

    override fun updateState(state: TrackingSessionState) {
        _sessionState.value = state
    }
}
