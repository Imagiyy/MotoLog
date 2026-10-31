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
import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.engine.ElevationCalculator
import com.abrar.motolog.shared.domain.engine.PointFilterResult
import com.abrar.motolog.shared.domain.engine.RideCalculator
import com.abrar.motolog.shared.domain.location.LocationSource
import com.abrar.motolog.shared.domain.model.GpsPoint
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import com.abrar.motolog.shared.domain.time.Clock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.abrar.motolog.shared.domain.engine.SpeedAlertEngine
import com.abrar.motolog.shared.domain.model.SpeedAlertStyle
import com.abrar.motolog.shared.domain.model.TrackingMode
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
    @Inject lateinit var voiceAnnouncer: HelmetVoiceAnnouncer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Explicit service lifecycle states to prevent start/stop races.
     * Transitions: IDLE -> STARTING -> TRACKING -> STOPPING -> IDLE
     */
    private enum class ServiceState { IDLE, STARTING, TRACKING, STOPPING }

    /** Mutex serializing all command handling to prevent concurrent state mutations. */
    private val commandMutex = Mutex()

    /** Mutex confining all RideCalculator access to prevent concurrent state corruption. */
    private val calculatorMutex = Mutex()

    private val rideCalculator = RideCalculator()
    @Volatile private var activeRideId: Long = 0L
    @Volatile private var serviceState: ServiceState = ServiceState.IDLE
    private val isGpsLost = AtomicBoolean(false)
    private val isHardwareGpsAvailable = AtomicBoolean(true)

    private var locationJob: Job? = null
    private var availabilityJob: Job? = null
    private var signalLossMonitorJob: Job? = null
    private var settingsJob: Job? = null
    private var batchWriterJob: Job? = null
    private var checkpointJob: Job? = null
    private var barometerJob: Job? = null
    private var startupJob: Job? = null

    /** Latest barometer altitude reading. Null if barometer not available. */
    private val latestBarometerAltitude = AtomicReference<Double?>(null)
    /** Whether barometer is available and providing data for this ride. */
    private var usingBarometer = false

    // Bounded queue for batching points to Room. Backpressure suspends location processing
    // rather than growing the heap unboundedly if Room writes stall.
    private var pointChannel = Channel<RidePointEntity>(capacity = 5_000)

    private var lastLocationCallbackTime = 0L
    /** Tracks when we last accepted a valid GPS fix (passes accuracy filter). Used for GPS-loss detection. */
    private var lastValidFixTime = 0L
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

    private val currentSamplingRateMs = java.util.concurrent.atomic.AtomicLong(TrackingConstants.LOCATION_UPDATE_INTERVAL_MS)
    private var stationarySinceEpochMs = 0L

    private fun checkAdaptiveSampling(speedKmh: Double, pauseState: PauseState, isGpsLost: Boolean, nowEpochMs: Long) {
        if (activeTrackingMode == TrackingMode.BATTERY_SAVER) return

        val targetIntervalMs = when {
            isGpsLost -> TrackingConstants.ADAPTIVE_GPS_LOST_INTERVAL_MS
            pauseState.isPaused -> TrackingConstants.ADAPTIVE_PAUSED_INTERVAL_MS
            speedKmh < TrackingConstants.AUTO_PAUSE_SPEED_THRESHOLD_KMH -> {
                if (stationarySinceEpochMs == 0L) {
                    stationarySinceEpochMs = nowEpochMs
                    TrackingConstants.LOCATION_UPDATE_INTERVAL_MS
                } else if (nowEpochMs - stationarySinceEpochMs >= 10_000L) {
                    TrackingConstants.ADAPTIVE_STATIONARY_INTERVAL_MS
                } else {
                    TrackingConstants.LOCATION_UPDATE_INTERVAL_MS
                }
            }
            else -> {
                stationarySinceEpochMs = 0L
                TrackingConstants.LOCATION_UPDATE_INTERVAL_MS
            }
        }

        if (currentSamplingRateMs.getAndSet(targetIntervalMs) != targetIntervalMs) {
            val fastestMs = if (targetIntervalMs == TrackingConstants.LOCATION_UPDATE_INTERVAL_MS) {
                TrackingConstants.LOCATION_FASTEST_INTERVAL_MS
            } else {
                (targetIntervalMs - 1_000L).coerceAtLeast(1_000L)
            }
            locationSource.updateSamplingRate(targetIntervalMs, fastestMs)
        }
    }

    companion object {
        const val ACTION_START = "com.abrar.motolog.action.START"
        const val ACTION_PAUSE = "com.abrar.motolog.action.PAUSE"
        const val ACTION_RESUME = "com.abrar.motolog.action.RESUME"
        const val ACTION_STOP = "com.abrar.motolog.action.STOP"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        // Serialize all commands through the mutex to prevent races
        serviceScope.launch {
            commandMutex.withLock {
                when (action) {
                    ACTION_START -> handleStart()
                    ACTION_PAUSE -> handlePause()
                    ACTION_RESUME -> handleResume()
                    ACTION_STOP -> handleStop()
                }
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Start ride tracking session.
     */
    /**
     * Start ride tracking session.
     * Called inside commandMutex so no concurrent start/stop/pause/resume can interleave.
     */
    private suspend fun handleStart() {
        if (serviceState != ServiceState.IDLE) {
            // Already tracking or in transition; ignore duplicate start
            return
        }
        serviceState = ServiceState.STARTING
        isGpsLost.set(false)
        lastLocationCallbackTime = 0L
        lastValidFixTime = 0L
        speedAlertEngine.reset()

        // Observe settings
        settingsJob?.cancel()
        settingsJob = serviceScope.launch {
            launch {
                settingsRepository.autoPauseEnabled.collect { enabled ->
                    calculatorMutex.withLock {
                        rideCalculator.autoPauseStateMachine.autoPauseEnabled = enabled
                    }
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
            launch {
                settingsRepository.voiceAnnouncementsEnabled.collect { enabled ->
                    voiceAnnouncer.setEnabled(enabled)
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

        // 2. Complete initialization synchronously within the mutex
        // so activeRideId is guaranteed to be valid before any other command.
        activeTrackingMode = settingsRepository.trackingMode.firstOrNull() ?: TrackingMode.HIGH_ACCURACY
        val currentBikeId = settingsRepository.currentBikeId.firstOrNull()
        val initialRide = RideEntity(
            bikeId = currentBikeId,
            startTime = System.currentTimeMillis(),
            status = RideStatus.ACTIVE
        )
        activeRideId = rideDao.insert(initialRide)
        calculatorMutex.withLock { rideCalculator.reset() }
        latestBarometerAltitude.set(null)

        // 3. Start barometer if available
        usingBarometer = false
        if (barometerSource.isAvailable()) {
            startBarometerCollection()
        }

        // 4. Start background point batch writer
        pointChannel = Channel(capacity = 5_000)
        startBatchWriter()

        // 5. Start periodic checkpoint runner
        startCheckpointRunner()

        // 6. Start location collection & signal monitoring
        startLocationCollection()
        startSignalLossMonitor()

        // Mark fully ready — only now can pause/resume/stop proceed
        serviceState = ServiceState.TRACKING
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
                    lastLocationCallbackTime = clock.currentTimeMillis()
                    lastObservedAccuracy = locationPoint.accuracyMeters

                    // If we previously flagged GPS lost, clear it upon new fix arrival
                    val wasGpsLost = isGpsLost.getAndSet(false)

                    // Check accuracy fix — do NOT update lastValidFixTime
                    // for poor-accuracy callbacks, only for fixes that pass filtering.
                    if (locationPoint.accuracyMeters > TrackingConstants.MIN_GPS_ACCURACY_METERS) {
                        val statsSnapshot = calculatorMutex.withLock { rideCalculator.stats }
                        if (statsSnapshot.acceptedPointCount == 0) {
                            val pauseSnapshot = calculatorMutex.withLock { rideCalculator.autoPauseStateMachine.pauseState }
                            trackingRepository.updateState(
                                TrackingSessionState.WaitingForGps(locationPoint.accuracyMeters)
                            )
                            updateNotificationThrottled(
                                statsSnapshot,
                                pauseState = pauseSnapshot,
                                isGpsLost = false,
                                accuracyMeters = locationPoint.accuracyMeters,
                                isWaitingGps = true
                            )
                        }
                        // Ignore points with accuracy worse than 25m while tracking per rule 7
                        return@collect
                    }

                    // Fix #16: Validate coordinates and finiteness
                    if (locationPoint.latitude.isNaN() || locationPoint.latitude.isInfinite() ||
                        locationPoint.latitude < -90.0 || locationPoint.latitude > 90.0 ||
                        locationPoint.longitude.isNaN() || locationPoint.longitude.isInfinite() ||
                        locationPoint.longitude < -180.0 || locationPoint.longitude > 180.0
                    ) {
                        return@collect
                    }

                    // Good-accuracy fix: update valid fix time for GPS-loss detection
                    lastValidFixTime = clock.currentTimeMillis()

                    lastKnownLatitude = locationPoint.latitude
                    lastKnownLongitude = locationPoint.longitude

                    // Fix #14: Use consistent UTC epoch timestamp for both engine and persistence.
                    // This guarantees that real-time tracking calculations match recovery replay.
                    val pointTimestamp = locationPoint.timestamp

                    val gpsPoint = GpsPoint(
                        timestampEpochMs = pointTimestamp,
                        latitude = locationPoint.latitude,
                        longitude = locationPoint.longitude,
                        speedMps = locationPoint.speedMps,
                        accuracyMeters = locationPoint.accuracyMeters
                    )

                    val filterResult = calculatorMutex.withLock { rideCalculator.process(gpsPoint) }

                    // Fix #25: Do not persist rejected GPS samples — they would corrupt
                    // maps, exports, and recovery replay.
                    if (filterResult is PointFilterResult.Rejected) {
                        return@collect
                    }

                    val (isGap, pauseState, speedKmh) = if (filterResult is PointFilterResult.Accepted) {
                        Triple(filterResult.isGap, filterResult.pauseState, filterResult.speedKmh)
                    } else {
                        val ps = calculatorMutex.withLock { rideCalculator.autoPauseStateMachine.pauseState }
                        Triple(false, ps, 0.0)
                    }

                    // Evaluate speed alert
                    val alertFired = speedAlertEngine.evaluate(
                        currentSpeedKmh = speedKmh,
                        thresholdKmh = speedAlertThresholdKmh,
                        isEnabled = speedAlertEnabled,
                        currentTimeMs = pointTimestamp
                    )
                    if (alertFired) {
                        triggerSpeedAlertFeedback(speedAlertStyle)
                    }

                    // Buffer point for database write
                    val pointSpeedMps = locationPoint.speedMps
                    val speedMs = if (pointSpeedMps != null) {
                        pointSpeedMps.toDouble()
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
                    // Use suspending send: if the bounded channel is full,
                    // this back-pressures the location collector instead of growing the heap.
                    pointChannel.send(pointEntity)

                    val currentStats = calculatorMutex.withLock { rideCalculator.stats }
                    if (currentStats.acceptedPointCount == 1) {
                        voiceAnnouncer.announceRideStarted()
                    }
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
                            longitude = locationPoint.longitude,
                            altitudeMeters = altitude,
                            bearingDegrees = locationPoint.bearingDegrees
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

                    checkAdaptiveSampling(speedKmh, pauseState, false, pointTimestamp)
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
            while (isActive && serviceState == ServiceState.TRACKING) {
                delay(1_000L)
                val now = clock.currentTimeMillis()
                // Fix #7: Use lastValidFixTime (updated only after accuracy check passes)
                // instead of lastLocationCallbackTime (updated on every callback).
                // This prevents poor-quality callbacks from resetting the GPS-loss timer.
                val lastValid = lastValidFixTime
                val timeoutSec = if (activeTrackingMode == TrackingMode.BATTERY_SAVER) {
                    TrackingConstants.BATTERY_SAVER_SIGNAL_LOST_TIMEOUT_SECONDS
                } else {
                    TrackingConstants.GPS_SIGNAL_LOST_TIMEOUT_SECONDS
                }
                val isTimedOut = lastValid > 0L && (now - lastValid >= timeoutSec * 1000L)
                val isUnavailable = !isHardwareGpsAvailable.get()

                val (stats, pauseState) = calculatorMutex.withLock {
                    rideCalculator.stats to rideCalculator.autoPauseStateMachine.pauseState
                }
                if ((isTimedOut || isUnavailable) && !isGpsLost.get() && stats.acceptedPointCount > 0) {
                    isGpsLost.set(true)

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

                    checkAdaptiveSampling(0.0, pauseState, true, now)
                }
            }
        }
    }

    /**
     * Pause active ride manually.
     * Called inside commandMutex.
     */
    private suspend fun handlePause() {
        if (serviceState != ServiceState.TRACKING) return
        val (pauseState, stats) = calculatorMutex.withLock {
            rideCalculator.manualPause() to rideCalculator.stats
        }
        voiceAnnouncer.announcePaused()

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
        checkAdaptiveSampling(0.0, pauseState, isGpsLost.get(), clock.currentTimeMillis())
    }

    /**
     * Resume paused ride manually.
     * Called inside commandMutex.
     */
    private suspend fun handleResume() {
        if (serviceState != ServiceState.TRACKING) return
        val (pauseState, stats) = calculatorMutex.withLock {
            rideCalculator.manualResume(clock.currentTimeMillis()) to rideCalculator.stats
        }
        voiceAnnouncer.announceResumed()

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
        checkAdaptiveSampling(10.0, pauseState, isGpsLost.get(), clock.currentTimeMillis())
    }

    /**
     * Stop ride, flush database writes, checkpoint final stats, dismiss notification, and terminate service.
     * Called inside commandMutex so no concurrent start/pause/resume can interleave.
     */
    private suspend fun handleStop() {
        if (serviceState != ServiceState.TRACKING && serviceState != ServiceState.STARTING) return
        serviceState = ServiceState.STOPPING

        // Validate activeRideId was assigned (protects against race where stop arrives during STARTING)
        if (activeRideId <= 0L) {
            Log.w("TrackingService", "handleStop called with invalid activeRideId=$activeRideId, aborting")
            serviceState = ServiceState.IDLE
            trackingRepository.updateState(TrackingSessionState.Idle)
            withContext(Dispatchers.Main) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return
        }

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

        // 3. Close the channel to signal the writer, then flush remaining points
        pointChannel.close()
        // Wait for the batch writer to drain remaining items with a timeout safety net
        withTimeoutOrNull(3_000L) {
            batchWriterJob?.join()
        } ?: run {
            Log.w("TrackingService", "Batch writer drain timed out, cancelling job")
            batchWriterJob?.cancelAndJoin()
        }

        // 4. Checkpoint final stats & mark COMPLETED
        val finalStats = calculatorMutex.withLock { rideCalculator.stats }
        val now = System.currentTimeMillis()
        val existingRide = rideDao.getRideById(activeRideId)
        val startTime = existingRide?.startTime ?: now
        val rideName = if (existingRide?.name.isNullOrBlank()) {
            com.abrar.motolog.shared.domain.util.RideNameGenerator.defaultNameForTimestamp(startTime)
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

        voiceAnnouncer.announceRideCompleted(
            distanceKm = finalStats.totalDistanceMeters / 1000.0,
            avgSpeedKmh = finalStats.avgMovingSpeedKmh,
            isMetric = isMetricUnits
        )

        // 7. Release audio resources and tear down foreground service
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}

        activeRideId = 0L
        serviceState = ServiceState.IDLE

        withContext(Dispatchers.Main) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * Dedicated background batch writer writing points to Room every DB_BATCH_WRITE_INTERVAL_MS.
     */
    /**
     * Dedicated batch writer consuming from the bounded point channel.
     * Retries failed Room writes with exponential backoff to avoid data loss.
     * Drains remaining items when the channel is closed (during stop).
     */
    private fun startBatchWriter() {
        batchWriterJob?.cancel()
        batchWriterJob = serviceScope.launch(Dispatchers.IO) {
            val batch = mutableListOf<RidePointEntity>()
            try {
                while (isActive) {
                    // Drain all available items
                    while (true) {
                        val point = pointChannel.tryReceive().getOrNull() ?: break
                        batch.add(point)
                    }
                    if (batch.isNotEmpty()) {
                        writeBatchWithRetry(batch)
                        batch.clear()
                    }
                    if (pointChannel.isClosedForReceive) {
                        break
                    }
                    delay(TrackingConstants.DB_BATCH_WRITE_INTERVAL_MS)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Normal cancellation — fall through to drain
            }
            // Drain any remaining items after channel close or cancellation
            while (true) {
                val point = pointChannel.tryReceive().getOrNull() ?: break
                batch.add(point)
            }
            if (batch.isNotEmpty()) {
                writeBatchWithRetry(batch)
                batch.clear()
            }
        }
    }

    /**
     * Writes a batch of points to Room with chunking and retry on failure.
     * Retries up to 3 times with exponential backoff before logging the failure.
     */
    private suspend fun writeBatchWithRetry(batch: List<RidePointEntity>) {
        val chunks = batch.chunked(100)
        for (chunk in chunks) {
            writeChunkWithRetry(chunk)
        }
    }

    private suspend fun writeChunkWithRetry(chunk: List<RidePointEntity>) {
        var attempts = 0
        val maxAttempts = 3
        var delayMs = 500L
        val startTime = android.os.SystemClock.elapsedRealtime()
        while (attempts < maxAttempts) {
            try {
                ridePointDao.insertAll(chunk)
                val durationMs = android.os.SystemClock.elapsedRealtime() - startTime
                if (durationMs > 250) {
                    Log.w("TrackingService", "Slow batch write: ${chunk.size} points took ${durationMs}ms")
                }
                return
            } catch (e: Exception) {
                attempts++
                Log.e("TrackingService", "Batch write failed (attempt $attempts/$maxAttempts): ${e.message}")
                if (attempts < maxAttempts) {
                    delay(delayMs)
                    delayMs *= 2
                }
            }
        }
        Log.e("TrackingService", "Batch write permanently failed after $maxAttempts attempts, ${chunk.size} points lost")
    }

    // flushPoints() is no longer needed — the batch writer drains remaining items
    // when the channel is closed in handleStop().

    /**
     * Periodic checkpoint updating Ride summary fields in Room every CHECKPOINT_INTERVAL_MS.
     */
    private fun startCheckpointRunner() {
        checkpointJob?.cancel()
        checkpointJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(TrackingConstants.CHECKPOINT_INTERVAL_MS)
                if (serviceState != ServiceState.TRACKING) break

                val currentSession = trackingRepository.sessionState.value as? TrackingSessionState.Tracking ?: continue
                val stats = currentSession.stats
                val rideId = activeRideId
                if (rideId <= 0L) continue
                val existing = rideDao.getRideById(rideId)
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
            voiceAnnouncer.announceSpeedWarning()
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

    /**
     * Fix #5: onDestroy now attempts to flush pending points and checkpoint
     * the ride as recoverable before cancelling everything.
     * Note: Android may kill the process without calling onDestroy;
     * frequent checkpoints and bounded loss windows are the primary defense.
     */
    override fun onDestroy() {
        super.onDestroy()

        // Stop location callbacks immediately to prevent new points
        locationJob?.cancel()
        availabilityJob?.cancel()
        signalLossMonitorJob?.cancel()
        settingsJob?.cancel()
        barometerJob?.cancel()
        barometerSource.stopListening()
        locationSource.stopLocationUpdates()

        // If we're still tracking, attempt to flush and mark ride as recoverable
        if (serviceState == ServiceState.TRACKING && activeRideId > 0L) {
            try {
                // Close channel so batch writer drains remaining items
                pointChannel.close()
                // We can't reliably suspend here, but the batch writer's
                // drain loop in its finally block will handle remaining items
                // if the scope hasn't been cancelled yet.

                // Mark ride as ACTIVE (which is already the status) so recovery
                // can detect it. The checkpoint runner has been keeping it updated.
                Log.w("TrackingService", "onDestroy with active ride $activeRideId; ride left as ACTIVE for recovery")
            } catch (e: Exception) {
                Log.e("TrackingService", "Error during onDestroy flush: ${e.message}")
            }
        }

        serviceState = ServiceState.IDLE
        voiceAnnouncer.shutdown()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
        serviceScope.cancel()
    }
}

