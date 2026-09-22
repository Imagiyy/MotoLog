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
import com.abrar.motolog.data.sensor.BarometerSource
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.engine.ElevationCalculator
import com.abrar.motolog.domain.engine.PointFilterResult
import com.abrar.motolog.domain.engine.RideCalculator
import com.abrar.motolog.domain.location.LocationSource
import com.abrar.motolog.domain.model.GpsPoint
import com.abrar.motolog.domain.model.PauseState
import com.abrar.motolog.domain.model.RideStats
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.domain.repository.TrackingSessionState
import com.abrar.motolog.domain.time.Clock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.abrar.motolog.domain.engine.SpeedAlertEngine
import com.abrar.motolog.domain.model.SpeedAlertStyle
import com.abrar.motolog.domain.model.TrackingMode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Foreground Service owning the motorcycle ride tracking session.
 *
 * Runs with foregroundServiceType="location" independently of any UI.
 * Collects GPS updates, feeds RideCalculator, coordinates auto-pause and manual pause,
 * monitors GPS signal loss, buffers and batch-writes points to Room, periodically checkpoints
 * the active ride, and maintains an ongoing notification with live glanceable stats and controls.
 */
@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var locationSource: LocationSource
    @Inject lateinit var rideNotificationManager: RideNotificationManager
    @Inject lateinit var trackingRepository: TrackingRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var rideDao: RideDao
    @Inject lateinit var ridePointDao: RidePointDao
    @Inject lateinit var clock: Clock
    @Inject lateinit var maintenanceNotificationManager: MaintenanceNotificationManager
    @Inject lateinit var barometerSource: BarometerSource

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val rideCalculator = RideCalculator()
    private var activeRideId: Long = 0L
    private val isTracking = AtomicBoolean(false)
    private val isGpsLost = AtomicBoolean(false)
    private val isHardwareGpsAvailable = AtomicBoolean(true)

    private var locationJob: Job? = null
    private var availabilityJob: Job? = null
    private var signalLossMonitorJob: Job? = null
    private var settingsJob: Job? = null
    private var batchWriterJob: Job? = null
    private var checkpointJob: Job? = null
    private var barometerJob: Job? = null

    /** Latest barometer altitude reading. Null if barometer not available. */
    private val latestBarometerAltitude = AtomicReference<Double?>(null)
    /** Whether barometer is available and providing data for this ride. */
    private var usingBarometer = false

    // Non-dropping queue for batching points to Room
    private val pointChannel = Channel<RidePointEntity>(Channel.UNLIMITED)

    private var lastLocationPointTime = 0L
    private var lastNotificationUpdateTime = 0L
    private var lastObservedAccuracy = Float.MAX_VALUE
    private var lastKnownLatitude: Double? = null
    private var lastKnownLongitude: Double? = null

    private val speedAlertEngine = SpeedAlertEngine()
    private var isMetricUnits = true
    private var speedAlertEnabled = false
    private var speedAlertThresholdKmh = TrackingConstants.SPEED_ALERT_DEFAULT_THRESHOLD_KMH
    private var speedAlertStyle = SpeedAlertStyle.ALL
    private var activeTrackingMode = TrackingMode.HIGH_ACCURACY
    private var toneGenerator: ToneGenerator? = null

    companion object {
        const val ACTION_START = "com.abrar.motolog.action.START"
        const val ACTION_PAUSE = "com.abrar.motolog.action.PAUSE"
        const val ACTION_RESUME = "com.abrar.motolog.action.RESUME"
        const val ACTION_STOP = "com.abrar.motolog.action.STOP"
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
        isGpsLost.set(false)
        lastLocationPointTime = 0L
        speedAlertEngine.reset()

        // Observe settings
        settingsJob?.cancel()
        settingsJob = serviceScope.launch {
            launch {
                settingsRepository.autoPauseEnabled.collect { enabled ->
                    rideCalculator.autoPauseStateMachine.autoPauseEnabled = enabled
                }
            }
            launch {
                settingsRepository.useMetricUnits.collect { metric ->
                    isMetricUnits = metric
                }
            }
            launch {
                settingsRepository.speedAlertEnabled.collect { enabled ->
                    speedAlertEnabled = enabled
                }
            }
            launch {
                settingsRepository.speedAlertThresholdKmh.collect { threshold ->
                    speedAlertThresholdKmh = threshold
                }
            }
            launch {
                settingsRepository.speedAlertStyle.collect { style ->
                    speedAlertStyle = style
                }
            }
        }

        // 1. Immediately promote to Foreground to satisfy Android 14 requirements
        promoteToForeground(
            RideStats(),
            pauseState = PauseState.RECORDING,
            isGpsLost = false,
            accuracyMeters = Float.MAX_VALUE,
            isWaitingGps = true
        )

        trackingRepository.updateState(TrackingSessionState.WaitingForGps())

        serviceScope.launch {
            // 2. Read selected tracking mode before starting collection
            activeTrackingMode = settingsRepository.trackingMode.firstOrNull() ?: TrackingMode.HIGH_ACCURACY
            val currentBikeId = settingsRepository.currentBikeId.firstOrNull()
            val initialRide = RideEntity(
                bikeId = currentBikeId,
                startTime = System.currentTimeMillis(),
                status = RideStatus.ACTIVE
            )
            activeRideId = rideDao.insert(initialRide)
            rideCalculator.reset()
            latestBarometerAltitude.set(null)

            // 3. Start barometer if available
            usingBarometer = false
            if (barometerSource.isAvailable()) {
                startBarometerCollection()
            }

            // 4. Start background point batch writer
            startBatchWriter()

            // 5. Start periodic checkpoint runner
            startCheckpointRunner()

            // 6. Start location collection & signal monitoring
            startLocationCollection()
            startSignalLossMonitor()
        }
    }

    /**
     * Start collecting location updates from LocationSource.
     */
    private fun startLocationCollection() {
        locationJob?.cancel()
        locationJob = serviceScope.launch {
            locationSource.getLocationUpdates(activeTrackingMode)
                .catch {
                    // Location stream failure: safely stop and preserve session data
                    handleStop()
                }
                .collect { locationPoint ->
                    lastLocationPointTime = clock.currentTimeMillis()
                    lastObservedAccuracy = locationPoint.accuracyMeters

                    // If we previously flagged GPS lost, clear it upon new fix arrival
                    val wasGpsLost = isGpsLost.getAndSet(false)

                    // Check accuracy fix
                    if (locationPoint.accuracyMeters > TrackingConstants.MIN_GPS_ACCURACY_METERS) {
                        if (rideCalculator.stats.acceptedPointCount == 0) {
                            trackingRepository.updateState(
                                TrackingSessionState.WaitingForGps(locationPoint.accuracyMeters)
                            )
                            updateNotificationThrottled(
                                rideCalculator.stats,
                                pauseState = rideCalculator.autoPauseStateMachine.pauseState,
                                isGpsLost = false,
                                accuracyMeters = locationPoint.accuracyMeters,
                                isWaitingGps = true
                            )
                        }
                        // Ignore points with accuracy worse than 25m while tracking per rule 7
                        return@collect
                    }

                    lastKnownLatitude = locationPoint.latitude
                    lastKnownLongitude = locationPoint.longitude

                    val gpsPoint = GpsPoint(
                        timestampEpochMs = locationPoint.timestamp,
                        latitude = locationPoint.latitude,
                        longitude = locationPoint.longitude,
                        speedMps = locationPoint.speedMps,
                        accuracyMeters = locationPoint.accuracyMeters
                    )

                    val filterResult = rideCalculator.process(gpsPoint)

                    val (isGap, pauseState, speedKmh) = if (filterResult is PointFilterResult.Accepted) {
                        Triple(filterResult.isGap, filterResult.pauseState, filterResult.speedKmh)
                    } else {
                        Triple(false, rideCalculator.autoPauseStateMachine.pauseState, 0.0)
                    }

                    // Evaluate speed alert
                    val alertFired = speedAlertEngine.evaluate(
                        currentSpeedKmh = speedKmh,
                        thresholdKmh = speedAlertThresholdKmh,
                        isEnabled = speedAlertEnabled,
                        currentTimeMs = locationPoint.timestamp
                    )
                    if (alertFired) {
                        triggerSpeedAlertFeedback(speedAlertStyle)
                    }

                    // Buffer point for database write
                    val speedMs = if (locationPoint.speedMps != null) {
                        locationPoint.speedMps.toDouble()
                    } else {
                        speedKmh / 3.6
                    }

                    // Determine best altitude: barometer > GPS
                    val altitude = latestBarometerAltitude.get()
                        ?: locationPoint.altitudeMeters
                        ?: 0.0

                    val pointEntity = RidePointEntity(
                        rideId = activeRideId,
                        timestamp = locationPoint.timestamp,
                        latitude = locationPoint.latitude,
                        longitude = locationPoint.longitude,
                        speedMs = speedMs,
                        accuracyMeters = locationPoint.accuracyMeters,
                        altitudeMeters = altitude,
                        isPaused = pauseState.isPaused,
                        isGap = isGap
                    )
                    pointChannel.trySend(pointEntity)

                    val currentStats = rideCalculator.stats
                    val isSpeedExceeded = speedAlertEnabled && speedKmh >= speedAlertThresholdKmh

                    trackingRepository.updateState(
                        TrackingSessionState.Tracking(
                            rideId = activeRideId,
                            pauseState = pauseState,
                            isGpsLost = false,
                            stats = currentStats,
                            accuracyMeters = locationPoint.accuracyMeters,
                            isSpeedAlert = isSpeedExceeded,
                            latitude = locationPoint.latitude,
                            longitude = locationPoint.longitude
                        )
                    )

                    if (wasGpsLost) {
                        // Immediately inform user of recovered fix
                        updateNotificationImmediate(
                            currentStats,
                            pauseState = pauseState,
                            isGpsLost = false,
                            accuracyMeters = locationPoint.accuracyMeters,
                            isWaitingGps = false
                        )
                    } else {
                        updateNotificationThrottled(
                            currentStats,
                            pauseState = pauseState,
                            isGpsLost = false,
                            accuracyMeters = locationPoint.accuracyMeters,
                            isWaitingGps = false
                        )
                    }
                }
        }

        // Availability callback monitor
        availabilityJob?.cancel()
        availabilityJob = serviceScope.launch {
            locationSource.getLocationAvailability()
                .catch { /* ignore */ }
                .collect { available ->
                    isHardwareGpsAvailable.set(available)
                }
        }
    }

    /**
     * Start collecting barometer altitude readings.
     * The latest reading is stored atomically for the location callback to pick up.
     */
    private fun startBarometerCollection() {
        barometerJob?.cancel()
        barometerJob = serviceScope.launch {
            barometerSource.getAltitudeUpdates()
                .catch { /* Barometer failure is non-critical; fall back to GPS altitude */ }
                    .collect { altitude ->
                        usingBarometer = true
                    latestBarometerAltitude.set(altitude)
                }
        }
    }

    /**
     * Periodic monitor declaring GPS signal lost if no update arrives within GPS_SIGNAL_LOST_TIMEOUT_SECONDS.
     */
    private fun startSignalLossMonitor() {
        signalLossMonitorJob?.cancel()
        signalLossMonitorJob = serviceScope.launch {
            while (isActive && isTracking.get()) {
                delay(1_000L)
                val now = clock.currentTimeMillis()
                val lastPoint = lastLocationPointTime
                val timeoutSec = if (activeTrackingMode == TrackingMode.BATTERY_SAVER) {
                    TrackingConstants.BATTERY_SAVER_SIGNAL_LOST_TIMEOUT_SECONDS
                } else {
                    TrackingConstants.GPS_SIGNAL_LOST_TIMEOUT_SECONDS
                }
                val isTimedOut = lastPoint > 0L && (now - lastPoint >= timeoutSec * 1000L)
                val isUnavailable = !isHardwareGpsAvailable.get()

                if ((isTimedOut || isUnavailable) && !isGpsLost.get() && rideCalculator.stats.acceptedPointCount > 0) {
                    isGpsLost.set(true)
                    val stats = rideCalculator.stats
                    val pauseState = rideCalculator.autoPauseStateMachine.pauseState

                    trackingRepository.updateState(
                        TrackingSessionState.Tracking(
                            rideId = activeRideId,
                            pauseState = pauseState,
                            isGpsLost = true,
                            stats = stats,
                            accuracyMeters = lastObservedAccuracy,
                            latitude = lastKnownLatitude,
                            longitude = lastKnownLongitude
                        )
                    )

                    updateNotificationImmediate(
                        stats = stats,
                        pauseState = pauseState,
                        isGpsLost = true,
                        accuracyMeters = lastObservedAccuracy,
                        isWaitingGps = false
                    )
                }
            }
        }
    }

    /**
     * Pause active ride manually.
     */
    private fun handlePause() {
        if (!isTracking.get()) return
        val pauseState = rideCalculator.manualPause()

        val stats = rideCalculator.stats
        trackingRepository.updateState(
            TrackingSessionState.Tracking(
                rideId = activeRideId,
                pauseState = pauseState,
                isGpsLost = isGpsLost.get(),
                stats = stats,
                accuracyMeters = lastObservedAccuracy,
                latitude = lastKnownLatitude,
                longitude = lastKnownLongitude
            )
        )
        updateNotificationImmediate(
            stats,
            pauseState = pauseState,
            isGpsLost = isGpsLost.get(),
            accuracyMeters = lastObservedAccuracy,
            isWaitingGps = false
        )
    }

    /**
     * Resume paused ride manually.
     */
    private fun handleResume() {
        if (!isTracking.get()) return
        val pauseState = rideCalculator.manualResume(clock.currentTimeMillis())

        val stats = rideCalculator.stats
        trackingRepository.updateState(
            TrackingSessionState.Tracking(
                rideId = activeRideId,
                pauseState = pauseState,
                isGpsLost = isGpsLost.get(),
                stats = stats,
                accuracyMeters = lastObservedAccuracy,
                latitude = lastKnownLatitude,
                longitude = lastKnownLongitude
            )
        )
        updateNotificationImmediate(
            stats,
            pauseState = pauseState,
            isGpsLost = isGpsLost.get(),
            accuracyMeters = lastObservedAccuracy,
            isWaitingGps = false
        )
    }

    /**
     * Stop ride, flush database writes, checkpoint final stats, dismiss notification, and terminate service.
     */
    private fun handleStop() {
        if (!isTracking.get()) return
        isTracking.set(false)

        serviceScope.launch {
            // 1. Cease GPS location updates, barometer & monitors
            locationJob?.cancel()
            availabilityJob?.cancel()
            signalLossMonitorJob?.cancel()
            settingsJob?.cancel()
            barometerJob?.cancel()
            locationSource.stopLocationUpdates()
            barometerSource.stopListening()
            lastKnownLatitude = null
            lastKnownLongitude = null

            // 2. Stop periodic tasks
            checkpointJob?.cancel()

            // 3. Flush remaining points in buffer
            flushPoints()
            batchWriterJob?.cancel()

            // 4. Checkpoint final stats & mark COMPLETED
            val finalStats = rideCalculator.stats
            val now = System.currentTimeMillis()
            val existingRide = rideDao.getRideById(activeRideId)
            val startTime = existingRide?.startTime ?: now
            val rideName = if (existingRide?.name.isNullOrBlank()) {
                com.abrar.motolog.domain.util.RideNameGenerator.defaultNameForTimestamp(startTime)
            } else {
                existingRide.name
            }
            val completedRide = RideEntity(
                id = activeRideId,
                bikeId = existingRide?.bikeId,
                name = rideName,
                startTime = startTime,
                endTime = now,
                distanceMeters = finalStats.totalDistanceMeters,
                elapsedTimeMs = finalStats.elapsedTimeMs,
                movingTimeMs = finalStats.movingTimeMs,
                avgMovingSpeedMs = finalStats.avgMovingSpeedKmh / 3.6,
                overallAvgSpeedMs = finalStats.avgOverallSpeedKmh / 3.6,
                maxSpeedMs = finalStats.maxSpeedKmh / 3.6,
                status = RideStatus.COMPLETED
            )

            // 4b. Compute elevation gain/loss from stored points
            val elevationResult = computeElevation()
            val finalRide = completedRide.copy(
                elevationGainMeters = elevationResult?.gainMeters ?: 0.0,
                elevationLossMeters = elevationResult?.lossMeters ?: 0.0,
                elevationSource = when {
                    usingBarometer && elevationResult != null -> "barometer"
                    elevationResult != null -> "gps"
                    else -> ""
                }
            )
            rideDao.update(finalRide)

            // 5. Update shared state
            trackingRepository.updateState(TrackingSessionState.Stopped(finalStats))

            // 6. Check maintenance reminders for this bike
            try {
                maintenanceNotificationManager.checkAndNotify(completedRide.bikeId)
            } catch (e: Exception) {
                // Non-critical, ignore
            }

            // 7. Tear down foreground service and dismiss notification
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
        pauseState: PauseState,
        isGpsLost: Boolean,
        accuracyMeters: Float,
        isWaitingGps: Boolean
    ) {
        val notification = rideNotificationManager.buildNotification(
            stats = stats,
            pauseState = pauseState,
            isGpsLost = isGpsLost,
            accuracyMeters = accuracyMeters,
            isWaitingGps = isWaitingGps,
            isMetric = isMetricUnits
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
        pauseState: PauseState,
        isGpsLost: Boolean,
        accuracyMeters: Float,
        isWaitingGps: Boolean
    ) {
        val now = clock.currentTimeMillis()
        if (now - lastNotificationUpdateTime >= TrackingConstants.NOTIFICATION_UPDATE_INTERVAL_MS) {
            lastNotificationUpdateTime = now
            updateNotificationImmediate(stats, pauseState, isGpsLost, accuracyMeters, isWaitingGps)
        }
    }

    private fun updateNotificationImmediate(
        stats: RideStats,
        pauseState: PauseState,
        isGpsLost: Boolean,
        accuracyMeters: Float,
        isWaitingGps: Boolean
    ) {
        val notification = rideNotificationManager.buildNotification(
            stats = stats,
            pauseState = pauseState,
            isGpsLost = isGpsLost,
            accuracyMeters = accuracyMeters,
            isWaitingGps = isWaitingGps,
            isMetric = isMetricUnits
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(TrackingConstants.NOTIFICATION_ID, notification)
    }

    /**
     * Triggers non-blocking haptic and/or subtle audio alert when speed threshold is crossed.
     */
    private fun triggerSpeedAlertFeedback(style: SpeedAlertStyle) {
        if (style == SpeedAlertStyle.VISUAL_AND_HAPTIC || style == SpeedAlertStyle.ALL) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(250)
                }
            } catch (_: Exception) {}
        }

        if (style == SpeedAlertStyle.ALL) {
            try {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
                }
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
            } catch (_: Exception) {}
        }
    }

    /**
     * Compute elevation gain and loss from stored points for the active ride.
     */
    private suspend fun computeElevation(): ElevationCalculator.ElevationResult? = withContext(Dispatchers.IO) {
        val points = ridePointDao.getPointsForRideOnce(activeRideId)
        if (points.size < 2) return@withContext null

        val rawElevations = points.map { it.altitudeMeters }
        // If all altitudes are 0.0 (no elevation recorded), return null
        if (rawElevations.all { it == 0.0 }) return@withContext null

        val threshold = if (usingBarometer) {
            TrackingConstants.ELEVATION_MIN_CHANGE_BAROMETER_METERS
        } else {
            TrackingConstants.ELEVATION_MIN_CHANGE_GPS_METERS
        }

        ElevationCalculator.calculate(
            rawElevations = rawElevations,
            minimumChangeThreshold = threshold,
            smoothingWindow = TrackingConstants.ELEVATION_SMOOTHING_WINDOW
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        isTracking.set(false)
        locationJob?.cancel()
        availabilityJob?.cancel()
        signalLossMonitorJob?.cancel()
        settingsJob?.cancel()
        barometerJob?.cancel()
        barometerSource.stopListening()
        locationSource.stopLocationUpdates()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
        serviceScope.cancel()
    }
}

