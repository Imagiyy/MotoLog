package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.model.GpsPoint
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin calculation engine for motorcycle ride tracking.
 *
 * Implements strict GPS point filtering, Haversine distance computation,
 * stationary jitter elimination, signal gap handling, robust speed smoothing,
 * and auto-pause state machine integration.
 *
 * Completely free of Android framework dependencies, making it directly multiplatform.
 */
class RideCalculator(
    private val minAccuracyMeters: Double = TrackingConstants.MIN_GPS_ACCURACY_METERS,
    private val stationarySpeedThresholdKmh: Double = TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH,
    private val lowSpeedDistanceThresholdKmh: Double = TrackingConstants.LOW_SPEED_DISTANCE_THRESHOLD_KMH,
    private val maxPlausibleSpeedKmh: Double = TrackingConstants.MAX_PLAUSIBLE_SPEED_KMH,
    private val maxAccelerationMs2: Double = TrackingConstants.MAX_ACCELERATION_MS2,
    private val gapThresholdMs: Long = TrackingConstants.GAP_THRESHOLD_MS,
    private val startConfirmationDistanceMeters: Double = TrackingConstants.START_CONFIRMATION_DISTANCE_METERS,
    val autoPauseStateMachine: AutoPauseStateMachine = AutoPauseStateMachine(autoPauseEnabled = false)
) {

    private var firstPointTimestampMs: Long? = null
    private var originPoint: GpsPoint? = null
    private var isConfirmed: Boolean = startConfirmationDistanceMeters <= 0.0
    private var pendingDistanceMeters: Double = 0.0
    private var pendingMovingTimeMs: Long = 0L

    private var lastAcceptedPoint: GpsPoint? = null
    private var lastAcceptedSpeedKmh: Double = 0.0
    private var previousPotentialMaxSpeedKmh: Double = 0.0

    private var totalDistanceMeters: Double = 0.0
    private var movingTimeMs: Long = 0L
    private var currentSpeedKmh: Double = 0.0
    private var maxSpeedKmh: Double = 0.0
    private var gapCount: Int = 0
    private var acceptedPointCount: Int = 0
    private var rejectedPointCount: Int = 0
    private var latestTimestampMs: Long = 0L

    /**
     * Current immutable statistics snapshot.
     */
    val stats: RideStats
        get() {
            val elapsed = if (isConfirmed && firstPointTimestampMs != null) {
                max(0L, latestTimestampMs - firstPointTimestampMs!!)
            } else 0L

            val stopped = max(0L, elapsed - movingTimeMs)
            val movingHours = movingTimeMs / 3_600_000.0
            val elapsedHours = elapsed / 3_600_000.0

            val avgMoving = when {
                movingTimeMs < 5_000L -> {
                    // For the first 5 seconds of movement, raw (distance / time) is volatile
                    // due to single-meter GPS discretization noise.
                    // Smooth by returning current speed (if moving) or 0.0.
                    if (currentSpeedKmh >= stationarySpeedThresholdKmh) currentSpeedKmh else 0.0
                }
                movingHours > 0.0 -> (totalDistanceMeters / 1000.0) / movingHours
                else -> 0.0
            }

            val avgOverall = when {
                elapsed < 5_000L -> {
                    if (currentSpeedKmh >= stationarySpeedThresholdKmh) currentSpeedKmh else 0.0
                }
                elapsedHours > 0.0 -> (totalDistanceMeters / 1000.0) / elapsedHours
                else -> 0.0
            }

            return RideStats(
                totalDistanceMeters = totalDistanceMeters,
                elapsedTimeMs = elapsed,
                movingTimeMs = movingTimeMs,
                stoppedTimeMs = stopped,
                currentSpeedKmh = currentSpeedKmh,
                maxSpeedKmh = maxSpeedKmh,
                avgMovingSpeedKmh = avgMoving,
                avgOverallSpeedKmh = avgOverall,
                gapCount = gapCount,
                acceptedPointCount = acceptedPointCount,
                rejectedPointCount = rejectedPointCount
            )
        }

    /**
     * Process a single GPS point in real time.
     */
    fun process(point: GpsPoint): PointFilterResult {
        // 1. Accuracy Check
        if (point.accuracyMeters > minAccuracyMeters) {
            rejectedPointCount++
            return PointFilterResult.Rejected(RejectionReason.POOR_ACCURACY)
        }

        val last = lastAcceptedPoint
        if (last == null) {
            // First accepted point
            firstPointTimestampMs = point.timestampEpochMs
            latestTimestampMs = point.timestampEpochMs
            lastAcceptedPoint = point
            originPoint = point
            acceptedPointCount++

            val speed = determineSpeed(point, 0.0, 0.0)
            lastAcceptedSpeedKmh = speed
            val pauseState = if (point.isPaused) {
                autoPauseStateMachine.manualPause()
            } else {
                autoPauseStateMachine.onSpeedUpdate(speed, point.timestampEpochMs)
            }
            if (isConfirmed) {
                currentSpeedKmh = if (pauseState.isPaused) 0.0 else speed
                updateMaxSpeed(speed, point.speedAccuracyMps)
            }
            return PointFilterResult.Accepted(
                distanceIncrementMeters = 0.0,
                speedKmh = if (isConfirmed && !pauseState.isPaused) speed else 0.0,
                isGap = false,
                pauseState = pauseState
            )
        }

        // 2. Monotonic Timestamp Check
        val deltaMs = point.timestampEpochMs - last.timestampEpochMs
        if (deltaMs <= 0L) {
            rejectedPointCount++
            return PointFilterResult.Rejected(RejectionReason.NON_MONOTONIC_TIMESTAMP)
        }

        val deltaSec = deltaMs / 1000.0
        val segmentDistance = haversineDistanceMeters(
            last.latitude, last.longitude,
            point.latitude, point.longitude
        )

        // 3. Signal Gap Check
        val isGap = deltaMs > gapThresholdMs || point.isGap
        if (isGap) {
            gapCount++
        }

        // 4. Speed Spike Check (Implied distance / time) - skip across gaps
        if (!isGap) {
            val impliedSpeedKmh = (segmentDistance / deltaSec) * 3.6
            if (impliedSpeedKmh > maxPlausibleSpeedKmh) {
                rejectedPointCount++
                return PointFilterResult.Rejected(RejectionReason.SPIKE_SPEED)
            }
        }

        // 5. Current Speed Calculation
        val calculatedSpeedKmh = if (isGap) {
            determineSpeed(point, 0.0, 0.0)
        } else {
            determineSpeed(point, segmentDistance, deltaSec)
        }

        // 6. Acceleration Spike Check - skip across gaps
        if (!isGap) {
            val lastSpeedMps = lastAcceptedSpeedKmh / 3.6
            val currentSpeedMps = calculatedSpeedKmh / 3.6
            val impliedAccelMs2 = abs(currentSpeedMps - lastSpeedMps) / deltaSec
            if (impliedAccelMs2 > maxAccelerationMs2) {
                rejectedPointCount++
                return PointFilterResult.Rejected(RejectionReason.SPIKE_ACCELERATION)
            }
        }

        // 7. Auto-Pause and Manual Pause State Machine Evaluation
        val pauseState = if (point.isPaused) {
            autoPauseStateMachine.manualPause()
        } else {
            if (autoPauseStateMachine.pauseState == PauseState.MANUALLY_PAUSED) {
                autoPauseStateMachine.manualResume(point.timestampEpochMs)
            }
            autoPauseStateMachine.onSpeedUpdate(calculatedSpeedKmh, point.timestampEpochMs)
        }

        // 8. Distance & Stationary Noise Filtering
        val distanceToAdd = when {
            pauseState.isPaused -> 0.0
            isGap -> 0.0 // Do not invent distance across gaps per AGENTS.md
            calculatedSpeedKmh < stationarySpeedThresholdKmh -> {
                // When stationary (speed < 1.5 km/h), zero distance added to prevent drift inflation
                0.0
            }
            calculatedSpeedKmh < lowSpeedDistanceThresholdKmh && segmentDistance < point.accuracyMeters -> {
                // Segment shorter than accuracy radius while speed under 3 km/h -> reject jitter
                0.0
            }
            else -> segmentDistance
        }

        // 9. Time Accumulation
        val movingDeltaMs = if (!pauseState.isPaused && !isGap && calculatedSpeedKmh >= stationarySpeedThresholdKmh && distanceToAdd > 0.0) {
            deltaMs
        } else {
            0L
        }

        latestTimestampMs = point.timestampEpochMs
        lastAcceptedPoint = point
        lastAcceptedSpeedKmh = calculatedSpeedKmh
        acceptedPointCount++

        val displaySpeedKmh = if (pauseState.isPaused) 0.0 else calculatedSpeedKmh

        if (!isConfirmed) {
            pendingDistanceMeters += distanceToAdd
            pendingMovingTimeMs += movingDeltaMs

            val origin = originPoint ?: point
            val displacement = haversineDistanceMeters(
                origin.latitude, origin.longitude,
                point.latitude, point.longitude
            )

            if (displacement >= startConfirmationDistanceMeters || pendingDistanceMeters >= startConfirmationDistanceMeters) {
                // 100m threshold reached: confirm ride and credit all initial distance and moving time
                isConfirmed = true
                totalDistanceMeters += pendingDistanceMeters
                movingTimeMs += pendingMovingTimeMs
                currentSpeedKmh = displaySpeedKmh
                if (!pauseState.isPaused) {
                    updateMaxSpeed(calculatedSpeedKmh, point.speedAccuracyMps)
                }

                return PointFilterResult.Accepted(
                    distanceIncrementMeters = pendingDistanceMeters,
                    speedKmh = displaySpeedKmh,
                    isGap = isGap,
                    pauseState = pauseState
                )
            } else {
                // Before reaching 100m: keep display at zero to eliminate mounting/driveway drift
                currentSpeedKmh = 0.0
                return PointFilterResult.Accepted(
                    distanceIncrementMeters = 0.0,
                    speedKmh = 0.0,
                    isGap = isGap,
                    pauseState = pauseState
                )
            }
        } else {
            // Already confirmed: accumulate normally
            totalDistanceMeters += distanceToAdd
            movingTimeMs += movingDeltaMs
            currentSpeedKmh = displaySpeedKmh
            if (!pauseState.isPaused) {
                updateMaxSpeed(calculatedSpeedKmh, point.speedAccuracyMps)
            }

            return PointFilterResult.Accepted(
                distanceIncrementMeters = distanceToAdd,
                speedKmh = displaySpeedKmh,
                isGap = isGap,
                pauseState = pauseState
            )
        }
    }

    /**
     * Explicitly pause calculation via manual rider action.
     */
    fun manualPause(): PauseState = autoPauseStateMachine.manualPause()

    /**
     * Explicitly resume calculation via manual rider action.
     */
    fun manualResume(timestampMs: Long? = null): PauseState = autoPauseStateMachine.manualResume(timestampMs)

    /**
     * Determine point speed, preferring GPS Doppler speed when available,
     * falling back to distance / time, and zeroing stationary noise (< 1.5 km/h).
     */
    private fun determineSpeed(point: GpsPoint, segmentDistance: Double, deltaSec: Double): Double {
        val rawSpeedKmh = if (point.speedMps != null && point.speedMps >= 0f) {
            point.speedMps * 3.6
        } else if (deltaSec > 0.0) {
            (segmentDistance / deltaSec) * 3.6
        } else {
            0.0
        }

        return if (rawSpeedKmh < stationarySpeedThresholdKmh) {
            0.0
        } else {
            rawSpeedKmh
        }
    }

    /**
     * Updates max speed robustly.
     * Prevents single noisy GPS glitches from claiming unrealistic top speeds:
     * - If speedAccuracyMps is available and <= 1.5 m/s, accept immediately.
     * - Otherwise, require confirmation across 2 consecutive points within 15% tolerance.
     */
    private fun updateMaxSpeed(speedKmh: Double, speedAccuracyMps: Float?) {
        if (speedKmh <= maxSpeedKmh) return

        val isHighConfidenceGps = speedAccuracyMps != null && speedAccuracyMps <= 1.5f
        val isConsecutiveConfirmed = previousPotentialMaxSpeedKmh > 0.0 &&
                abs(speedKmh - previousPotentialMaxSpeedKmh) <= (speedKmh * 0.15)

        if (isHighConfidenceGps || isConsecutiveConfirmed) {
            maxSpeedKmh = speedKmh
            previousPotentialMaxSpeedKmh = 0.0
        } else {
            previousPotentialMaxSpeedKmh = speedKmh
        }
    }

    /**
     * Reset calculator state.
     */
    fun reset() {
        firstPointTimestampMs = null
        originPoint = null
        isConfirmed = startConfirmationDistanceMeters <= 0.0
        pendingDistanceMeters = 0.0
        pendingMovingTimeMs = 0L
        lastAcceptedPoint = null
        lastAcceptedSpeedKmh = 0.0
        previousPotentialMaxSpeedKmh = 0.0
        totalDistanceMeters = 0.0
        movingTimeMs = 0L
        currentSpeedKmh = 0.0
        maxSpeedKmh = 0.0
        gapCount = 0
        acceptedPointCount = 0
        rejectedPointCount = 0
        latestTimestampMs = 0L
        autoPauseStateMachine.reset()
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        private fun toRadians(deg: Double): Double = deg / 180.0 * PI

        /**
         * Calculate great-circle distance between two coordinates using the Haversine formula.
         */
        fun haversineDistanceMeters(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val dLat = toRadians(lat2 - lat1)
            val dLon = toRadians(lon2 - lon1)
            val rLat1 = toRadians(lat1)
            val rLat2 = toRadians(lat2)

            val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
                    cos(rLat1) * cos(rLat2) * sin(dLon / 2.0) * sin(dLon / 2.0)
            val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
            return EARTH_RADIUS_METERS * c
        }

        /**
         * Process a full list of GPS points in batch mode.
         */
        fun processAll(
            points: List<GpsPoint>,
            calculator: RideCalculator = RideCalculator()
        ): RideStats {
            calculator.reset()
            for (point in points) {
                calculator.process(point)
            }
            return calculator.stats
        }
    }
}
