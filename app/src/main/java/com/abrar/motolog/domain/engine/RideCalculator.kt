package com.abrar.motolog.domain.engine

import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.model.GpsPoint
import com.abrar.motolog.domain.model.RideStats
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin calculation engine for motorcycle ride tracking.
 *
 * Implements strict GPS point filtering, Haversine distance computation,
 * stationary jitter elimination, signal gap handling, and robust speed smoothing.
 *
 * Completely free of Android framework dependencies, making it directly testable on the JVM.
 */
class RideCalculator(
    private val minAccuracyMeters: Double = TrackingConstants.MIN_GPS_ACCURACY_METERS,
    private val stationarySpeedThresholdKmh: Double = TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH,
    private val lowSpeedDistanceThresholdKmh: Double = TrackingConstants.LOW_SPEED_DISTANCE_THRESHOLD_KMH,
    private val maxPlausibleSpeedKmh: Double = TrackingConstants.MAX_PLAUSIBLE_SPEED_KMH,
    private val maxAccelerationMs2: Double = TrackingConstants.MAX_ACCELERATION_MS2,
    private val gapThresholdMs: Long = TrackingConstants.GAP_THRESHOLD_MS
) {

    private var firstPointTimestampMs: Long? = null
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
            val elapsed = if (firstPointTimestampMs != null) {
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
            acceptedPointCount++

            val speed = determineSpeed(point, 0.0, 0.0)
            currentSpeedKmh = speed
            lastAcceptedSpeedKmh = speed
            updateMaxSpeed(speed, point.speedAccuracyMps)
            return PointFilterResult.Accepted(
                distanceIncrementMeters = 0.0,
                speedKmh = speed,
                isGap = false
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

        // 3. Speed Spike Check (Implied distance / time)
        val impliedSpeedKmh = (segmentDistance / deltaSec) * 3.6
        if (impliedSpeedKmh > maxPlausibleSpeedKmh) {
            rejectedPointCount++
            return PointFilterResult.Rejected(RejectionReason.SPIKE_SPEED)
        }

        // 4. Current Speed Calculation
        val calculatedSpeedKmh = determineSpeed(point, segmentDistance, deltaSec)

        // 5. Acceleration Spike Check
        val lastSpeedMps = lastAcceptedSpeedKmh / 3.6
        val currentSpeedMps = calculatedSpeedKmh / 3.6
        val impliedAccelMs2 = kotlin.math.abs(currentSpeedMps - lastSpeedMps) / deltaSec
        if (impliedAccelMs2 > maxAccelerationMs2) {
            rejectedPointCount++
            return PointFilterResult.Rejected(RejectionReason.SPIKE_ACCELERATION)
        }

        // 6. Signal Gap Check
        val isGap = deltaMs > gapThresholdMs
        if (isGap) {
            gapCount++
        }

        // 7. Distance & Stationary Noise Filtering
        val distanceToAdd = when {
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

        // 8. Time Accumulation
        latestTimestampMs = point.timestampEpochMs
        if (!isGap && calculatedSpeedKmh >= stationarySpeedThresholdKmh && distanceToAdd > 0.0) {
            movingTimeMs += deltaMs
        }

        totalDistanceMeters += distanceToAdd
        currentSpeedKmh = calculatedSpeedKmh

        // 9. Robust Max Speed Update
        updateMaxSpeed(calculatedSpeedKmh, point.speedAccuracyMps)

        lastAcceptedPoint = point
        lastAcceptedSpeedKmh = calculatedSpeedKmh
        acceptedPointCount++

        return PointFilterResult.Accepted(
            distanceIncrementMeters = distanceToAdd,
            speedKmh = calculatedSpeedKmh,
            isGap = isGap
        )
    }

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
                kotlin.math.abs(speedKmh - previousPotentialMaxSpeedKmh) <= (speedKmh * 0.15)

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
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        /**
         * Calculate great-circle distance between two coordinates using the Haversine formula.
         */
        fun haversineDistanceMeters(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val rLat1 = Math.toRadians(lat1)
            val rLat2 = Math.toRadians(lat2)

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
