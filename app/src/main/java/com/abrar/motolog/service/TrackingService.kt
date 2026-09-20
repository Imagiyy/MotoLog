package com.abrar.motolog.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.engine.PointFilterResult
import com.abrar.motolog.domain.engine.RideCalculator
import com.abrar.motolog.domain.location.LocationSource
import com.abrar.motolog.domain.model.GpsPoint
import com.abrar.motolog.domain.model.RideStats
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.domain.repository.TrackingSessionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Foreground Service owning the motorcycle ride tracking session.
 *
 * Runs with foregroundServiceType="location" independently of any UI.
 * Collects GPS updates, feeds RideCalculator, buffers and batch-writes
 * points to Room, periodically checkpoints the active ride, and maintains
 * an ongoing notification with live glanceable stats and controls.
 */
@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var locationSource: LocationSource
    @Inject lateinit var rideNotificationManager: RideNotificationManager
    @Inject lateinit var trackingRepository: TrackingRepository
    @Inject lateinit var rideDao: RideDao
    @Inject lateinit var ridePointDao: RidePointDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val rideCalculator = RideCalculator()
    private var activeRideId: Long = 0L
    private val isPaused = AtomicBoolean(false)
    private val isTracking = AtomicBoolean(false)

    private var locationJob: Job? = null
    private var batchWriterJob: Job? = null
    private var checkpointJob: Job? = null

    // Non-dropping queue for batching points to Room
    private val pointChannel = Channel<RidePointEntity>(Channel.UNLIMITED)

    private var lastNotificationUpdateTime = 0L
    private var lastObservedAccuracy = Float.MAX_VALUE

    companion object {
        const val ACTION_START = "com.abrar.motolog.action.START"
        const val ACTION_PAUSE = "com.abrar.motolog.action.PAUSE"
        const val ACTION_RESUME = "com.abrar.motolog.action.RESUME"
        const val ACTION_STOP = "com.abrar.motolog.action.STOP"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> handleStart()
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> handleStop()
        }

        return START_NOT_STICKY
    }

    /**
     * Start ride tracking session.
     */
    private fun handleStart() {
        if (isTracking.get()) {
            // Already tracking; ignore duplicate start
            return
        }
        isTracking.set(true)
        isPaused.set(false)

        // 1. Immediately promote to Foreground to satisfy Android 14 requirements
        promoteToForeground(
            RideStats(),
            isPaused = false,
            accuracyMeters = Float.MAX_VALUE,
            isWaitingGps = true
        )

        trackingRepository.updateState(TrackingSessionState.WaitingForGps())

        serviceScope.launch {
            // 2. Create Ride row in Room
            val initialRide = RideEntity(
                startTime = System.currentTimeMillis(),
                status = RideStatus.ACTIVE
            )
            activeRideId = rideDao.insert(initialRide)
            rideCalculator.reset()

            // 3. Start background point batch writer
            startBatchWriter()

            // 4. Start periodic checkpoint runner
            startCheckpointRunner()

            // 5. Start location collection
            startLocationCollection()
        }
    }

    /**
     * Start collecting location updates from LocationSource.
     */
    private fun startLocationCollection() {
        locationJob?.cancel()
        locationJob = serviceScope.launch {
            locationSource.getLocationUpdates()
                .catch {
                    // Location stream failure: safely stop and preserve session data
                    handleStop()
                }
                .collect { locationPoint ->
                    lastObservedAccuracy = locationPoint.accuracyMeters

                    // Check accuracy fix
                    if (locationPoint.accuracyMeters > TrackingConstants.MIN_GPS_ACCURACY_METERS) {
                        if (rideCalculator.stats.acceptedPointCount == 0) {
                            trackingRepository.updateState(
                                TrackingSessionState.WaitingForGps(locationPoint.accuracyMeters)
                            )
                            updateNotificationThrottled(
                                rideCalculator.stats,
                                isPaused = isPaused.get(),
                                accuracyMeters = locationPoint.accuracyMeters,
                                isWaitingGps = true
                            )
                        }
                        return@collect
                    }

                    val gpsPoint = GpsPoint(
                        timestampEpochMs = locationPoint.timestamp,
                        latitude = locationPoint.latitude,
                        longitude = locationPoint.longitude,
                        speedMps = locationPoint.speedMps,
                        accuracyMeters = locationPoint.accuracyMeters
                    )

                    val pausedNow = isPaused.get()
                    val filterResult = if (!pausedNow) {
                        rideCalculator.process(gpsPoint)
                    } else {
                        null
                    }

                    // Buffer point for database write
                    val isGap = filterResult is PointFilterResult.Accepted && filterResult.isGap
                    val speedMs = if (locationPoint.speedMps != null) {
                        locationPoint.speedMps.toDouble()
                    } else {
                        (filterResult as? PointFilterResult.Accepted)?.speedKmh?.div(3.6) ?: 0.0
                    }

                    val pointEntity = RidePointEntity(
                        rideId = activeRideId,
                        timestamp = locationPoint.timestamp,
                        latitude = locationPoint.latitude,
                        longitude = locationPoint.longitude,
                        speedMs = speedMs,
                        accuracyMeters = locationPoint.accuracyMeters,
                        isPaused = pausedNow,
                        isGap = isGap
                    )
                    pointChannel.trySend(pointEntity)

                    val currentStats = rideCalculator.stats
                    trackingRepository.updateState(
                        TrackingSessionState.Tracking(
                            rideId = activeRideId,
                            isPaused = pausedNow,
                            stats = currentStats,
                            accuracyMeters = locationPoint.accuracyMeters
                        )
                    )

                    updateNotificationThrottled(
                        currentStats,
                        isPaused = pausedNow,
                        accuracyMeters = locationPoint.accuracyMeters,
                        isWaitingGps = false
                    )
                }
        }
    }

    /**
     * Pause active ride.
     */
    private fun handlePause() {
        if (!isTracking.get() || isPaused.get()) return
        isPaused.set(true)

        val stats = rideCalculator.stats
        trackingRepository.updateState(
            TrackingSessionState.Tracking(
                rideId = activeRideId,
                isPaused = true,
                stats = stats,
                accuracyMeters = lastObservedAccuracy
            )
        )
        updateNotificationImmediate(stats, isPaused = true, accuracyMeters = lastObservedAccuracy, isWaitingGps = false)
    }

    /**
     * Resume paused ride.
     */
    private fun handleResume() {
        if (!isTracking.get() || !isPaused.get()) return
        isPaused.set(false)

        val stats = rideCalculator.stats
        trackingRepository.updateState(
            TrackingSessionState.Tracking(
                rideId = activeRideId,
                isPaused = false,
                stats = stats,
                accuracyMeters = lastObservedAccuracy
            )
        )
        updateNotificationImmediate(stats, isPaused = false, accuracyMeters = lastObservedAccuracy, isWaitingGps = false)
    }

    /**
     * Stop ride, flush database writes, checkpoint final stats, dismiss notification, and terminate service.
     */
    private fun handleStop() {
        if (!isTracking.get()) return
        isTracking.set(false)

        serviceScope.launch {
            // 1. Cease GPS location updates
            locationJob?.cancel()
            locationSource.stopLocationUpdates()

            // 2. Stop periodic tasks
            checkpointJob?.cancel()

            // 3. Flush remaining points in buffer
            flushPoints()
            batchWriterJob?.cancel()

            // 4. Checkpoint final stats & mark COMPLETED
            val finalStats = rideCalculator.stats
            val now = System.currentTimeMillis()
            val completedRide = RideEntity(
                id = activeRideId,
                startTime = (rideDao.getRideById(activeRideId)?.startTime) ?: now,
                endTime = now,
                distanceMeters = finalStats.totalDistanceMeters,
                elapsedTimeMs = finalStats.elapsedTimeMs,
                movingTimeMs = finalStats.movingTimeMs,
                avgMovingSpeedMs = finalStats.avgMovingSpeedKmh / 3.6,
                overallAvgSpeedMs = finalStats.avgOverallSpeedKmh / 3.6,
                maxSpeedMs = finalStats.maxSpeedKmh / 3.6,
                status = RideStatus.COMPLETED
            )
            rideDao.update(completedRide)

            // 5. Update shared state
            trackingRepository.updateState(TrackingSessionState.Stopped(finalStats))

            // 6. Tear down foreground service and dismiss notification
            withContext(Dispatchers.Main) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    /**
     * Dedicated background batch writer writing points to Room every DB_BATCH_WRITE_INTERVAL_MS.
     */
    private fun startBatchWriter() {
        batchWriterJob?.cancel()
        batchWriterJob = serviceScope.launch(Dispatchers.IO) {
            val batch = mutableListOf<RidePointEntity>()
            while (isActive) {
                delay(TrackingConstants.DB_BATCH_WRITE_INTERVAL_MS)
                while (true) {
                    val point = pointChannel.tryReceive().getOrNull() ?: break
                    batch.add(point)
                }
                if (batch.isNotEmpty()) {
                    ridePointDao.insertAll(batch)
                    batch.clear()
                }
            }
        }
    }

    /**
     * Flush all buffered points to Room immediately.
     */
    private suspend fun flushPoints() = withContext(Dispatchers.IO) {
        val remaining = mutableListOf<RidePointEntity>()
        while (true) {
            val point = pointChannel.tryReceive().getOrNull() ?: break
            remaining.add(point)
        }
        if (remaining.isNotEmpty()) {
            ridePointDao.insertAll(remaining)
        }
    }

    /**
     * Periodic checkpoint updating Ride summary fields in Room every CHECKPOINT_INTERVAL_MS.
     */
    private fun startCheckpointRunner() {
        checkpointJob?.cancel()
        checkpointJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(TrackingConstants.CHECKPOINT_INTERVAL_MS)
                if (!isTracking.get()) break

                val stats = rideCalculator.stats
                val existing = rideDao.getRideById(activeRideId)
                if (existing != null && existing.status == RideStatus.ACTIVE) {
                    val checkpoint = existing.copy(
                        distanceMeters = stats.totalDistanceMeters,
                        elapsedTimeMs = stats.elapsedTimeMs,
                        movingTimeMs = stats.movingTimeMs,
                        avgMovingSpeedMs = stats.avgMovingSpeedKmh / 3.6,
                        overallAvgSpeedMs = stats.avgOverallSpeedKmh / 3.6,
                        maxSpeedMs = stats.maxSpeedKmh / 3.6
                    )
                    rideDao.update(checkpoint)
                }
            }
        }
    }

    /**
     * Promote service to Foreground with location type.
     */
    private fun promoteToForeground(
        stats: RideStats,
        isPaused: Boolean,
        accuracyMeters: Float,
        isWaitingGps: Boolean
    ) {
        val notification = rideNotificationManager.buildNotification(
            stats, isPaused, accuracyMeters, isWaitingGps
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                TrackingConstants.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(TrackingConstants.NOTIFICATION_ID, notification)
        }
    }

    /**
     * Update notification with rate-limiting to prevent notification churn.
     */
    private fun updateNotificationThrottled(
        stats: RideStats,
        isPaused: Boolean,
        accuracyMeters: Float,
        isWaitingGps: Boolean
    ) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateTime >= TrackingConstants.NOTIFICATION_UPDATE_INTERVAL_MS) {
            lastNotificationUpdateTime = now
            updateNotificationImmediate(stats, isPaused, accuracyMeters, isWaitingGps)
        }
    }

    private fun updateNotificationImmediate(
        stats: RideStats,
        isPaused: Boolean,
        accuracyMeters: Float,
        isWaitingGps: Boolean
    ) {
        val notification = rideNotificationManager.buildNotification(
            stats, isPaused, accuracyMeters, isWaitingGps
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(TrackingConstants.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isTracking.set(false)
        locationJob?.cancel()
        locationSource.stopLocationUpdates()
        serviceScope.cancel()
    }
}
