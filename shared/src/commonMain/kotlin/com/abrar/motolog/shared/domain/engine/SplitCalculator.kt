package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.model.GpsPoint
import com.abrar.motolog.shared.domain.model.RideSplit
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin calculator for distance-based splits (e.g., per-kilometre or per-mile).
 *
 * Designed to be unit-agnostic: by default [splitDistanceMeters] is 1000.0 m (1 kilometre).
 * For imperial miles, 1609.344 m can be passed without altering domain logic.
 *
 * Respects pauses and signal gaps: paused and gap time do not inflate split duration or distance.
 * Free of Android framework dependencies, fully multiplatform.
 */
object SplitCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    private fun toRadians(deg: Double): Double = deg / 180.0 * PI

    /**
     * Computes distance splits from a chronological sequence of [GpsPoint]s.
     *
     * @param points Chronological list of GPS points recorded during the ride.
     * @param splitDistanceMeters Distance per split in meters (defaults to 1000.0 m for 1 km).
     * @param minAccuracyMeters Max allowed accuracy in meters (points with worse accuracy are ignored).
     * @param stationarySpeedThresholdKmh Speed below which movement is treated as stationary noise (1.5 km/h).
     * @param gapThresholdMs Time threshold for detecting GPS signal blackout gaps (10,000 ms).
     * @return List of completed and partial [RideSplit]s. Returns empty list if no valid movement occurred.
     */
    fun computeSplits(
        points: List<GpsPoint>,
        splitDistanceMeters: Double = 1000.0,
        minAccuracyMeters: Double = TrackingConstants.MIN_GPS_ACCURACY_METERS,
        stationarySpeedThresholdKmh: Double = TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH,
        gapThresholdMs: Long = TrackingConstants.GAP_THRESHOLD_MS
    ): List<RideSplit> {
        if (points.size < 2 || splitDistanceMeters <= 0.0) {
            return emptyList()
        }

        val splits = mutableListOf<RideSplit>()
        var currentSplitDist = 0.0
        var currentSplitDurationMs = 0L

        var lastValidPoint: GpsPoint? = null

        for (point in points) {
            // 1. Accuracy filter
            if (point.accuracyMeters > minAccuracyMeters) {
                continue
            }

            val last = lastValidPoint
            if (last == null) {
                lastValidPoint = point
                continue
            }

            // 2. Monotonic timestamp check
            val deltaMs = point.timestampEpochMs - last.timestampEpochMs
            if (deltaMs <= 0L) {
                continue
            }

            // 3. Gap check
            val isGap = deltaMs > gapThresholdMs || point.isGap

            // 4. Distance & pause check
            val segmentDistance = if (isGap) 0.0 else haversineDistanceMeters(
                last.latitude, last.longitude,
                point.latitude, point.longitude
            )

            // Speed in km/h
            val speedKmh = point.speedMps?.let { it * 3.6 } ?: if (!isGap && deltaMs > 0) {
                (segmentDistance / (deltaMs / 1000.0)) * 3.6
            } else 0.0

            val isStationary = speedKmh < stationarySpeedThresholdKmh
            val isPaused = point.isPaused || isStationary || isGap

            var segDist = if (isPaused) 0.0 else segmentDistance
            var segDuration = if (!isPaused && segDist > 0.0) deltaMs else 0L

            lastValidPoint = point

            // 5. Accumulate into splits with boundary interpolation
            while (segDist > 0.0 && currentSplitDist + segDist >= splitDistanceMeters) {
                val neededDist = splitDistanceMeters - currentSplitDist
                val fraction = if (segDist > 0.0) (neededDist / segDist).coerceIn(0.0, 1.0) else 0.0
                val fractionDurationMs = (segDuration * fraction).toLong()

                val completedDist = splitDistanceMeters
                val completedDurationMs = currentSplitDurationMs + fractionDurationMs

                val avgSpeedKmh = if (completedDurationMs > 0L) {
                    (completedDist / 1000.0) / (completedDurationMs / 3_600_000.0)
                } else 0.0

                splits.add(
                    RideSplit(
                        splitNumber = splits.size + 1,
                        distanceMeters = completedDist,
                        durationMs = completedDurationMs,
                        avgSpeedKmh = avgSpeedKmh,
                        isPartial = false
                    )
                )

                // Reset active split accumulator
                currentSplitDist = 0.0
                currentSplitDurationMs = 0L

                // Subtract consumed portion
                segDist -= neededDist
                segDuration -= fractionDurationMs
            }

            if (segDist > 0.0 || (segDuration > 0L && currentSplitDist > 0.0)) {
                currentSplitDist += segDist
                currentSplitDurationMs += segDuration
            }
        }

        // 6. Handle final partial split if remaining distance >= 1 meter
        if (currentSplitDist >= 1.0) {
            val avgSpeedKmh = if (currentSplitDurationMs > 0L) {
                (currentSplitDist / 1000.0) / (currentSplitDurationMs / 3_600_000.0)
            } else 0.0

            splits.add(
                RideSplit(
                    splitNumber = splits.size + 1,
                    distanceMeters = currentSplitDist,
                    durationMs = currentSplitDurationMs,
                    avgSpeedKmh = avgSpeedKmh,
                    isPartial = true
                )
            )
        }

        return splits
    }

    /**
     * Great-circle distance between two coordinates using Haversine formula.
     */
    private fun haversineDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        val rLat1 = toRadians(lat1)
        val rLat2 = toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
