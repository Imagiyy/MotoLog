package com.abrar.motolog.domain.engine

import com.abrar.motolog.domain.TrackingConstants
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Downsamples a route polyline for efficient map rendering using the
 * Ramer-Douglas-Peucker (RDP) algorithm with speed-extreme and
 * structure-aware preservation.
 *
 * Pure Kotlin, no Android imports — fully unit testable on JVM.
 *
 * Preservation rules:
 * 1. First and last points are always kept.
 * 2. Gap boundaries (point before and after each gap) are always kept.
 * 3. Pause transition points are always kept.
 * 4. Speed extremes (local max/min within windows) are always kept.
 * 5. RDP applied to remaining points with configurable tolerance.
 * 6. If result exceeds maxPoints, tolerance is doubled and re-run.
 */
object RouteDownsampler {

    /**
     * A single route point for downsampling purposes.
     */
    data class RoutePoint(
        val latitude: Double,
        val longitude: Double,
        val speedKmh: Double,
        val altitudeMeters: Double?,
        val timestampMs: Long,
        val isPaused: Boolean,
        val isGap: Boolean
    )

    /**
     * Downsample a list of route points for map display.
     *
     * @param points Full list of ride points in chronological order.
     * @param toleranceMeters RDP simplification tolerance in meters.
     * @param maxPoints Maximum number of output points.
     * @return Simplified list preserving route shape and key structural points.
     */
    fun downsample(
        points: List<RoutePoint>,
        toleranceMeters: Double = TrackingConstants.DOWNSAMPLE_TOLERANCE_METERS,
        maxPoints: Int = TrackingConstants.DOWNSAMPLE_MAX_POINTS
    ): List<RoutePoint> {
        if (points.size <= maxPoints) return points

        // Step 1: Mark protected indices
        val protectedIndices = mutableSetOf<Int>()

        // First and last
        if (points.isNotEmpty()) {
            protectedIndices.add(0)
            protectedIndices.add(points.size - 1)
        }

        // Gap boundaries
        for (i in points.indices) {
            if (points[i].isGap) {
                protectedIndices.add(i)
                if (i > 0) protectedIndices.add(i - 1)
                if (i < points.size - 1) protectedIndices.add(i + 1)
            }
        }

        // Pause transitions
        for (i in 1 until points.size) {
            if (points[i].isPaused != points[i - 1].isPaused) {
                protectedIndices.add(i)
                protectedIndices.add(i - 1)
            }
        }

        // Speed extremes (local max/min in 20-point windows)
        val windowSize = 20
        if (points.size > windowSize) {
            for (start in points.indices step windowSize) {
                val end = minOf(start + windowSize, points.size)
                val window = points.subList(start, end)
                if (window.isNotEmpty()) {
                    val maxIdx = start + window.indices.maxByOrNull { window[it].speedKmh }!!
                    val minIdx = start + window.indices.minByOrNull { window[it].speedKmh }!!
                    protectedIndices.add(maxIdx)
                    protectedIndices.add(minIdx)
                }
            }
        }

        // Step 2: Apply RDP with escalating tolerance
        var currentTolerance = toleranceMeters
        var result = rdpSimplify(points, protectedIndices, currentTolerance)

        // Retry with doubled tolerance if too many points
        var attempts = 0
        while (result.size > maxPoints && attempts < 5) {
            currentTolerance *= 2.0
            result = rdpSimplify(points, protectedIndices, currentTolerance)
            attempts++
        }

        return result
    }

    /**
     * Ramer-Douglas-Peucker simplification with protected index preservation.
     *
     * Uses an iterative stack-based approach to avoid stack overflow on large inputs.
     */
    private fun rdpSimplify(
        points: List<RoutePoint>,
        protectedIndices: Set<Int>,
        tolerance: Double
    ): List<RoutePoint> {
        if (points.size <= 2) return points.toList()

        val keep = BooleanArray(points.size) { it in protectedIndices }

        // Iterative RDP using explicit stack
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to points.size - 1)

        while (stack.isNotEmpty()) {
            val (start, end) = stack.removeLast()
            if (end - start < 2) continue

            var maxDist = 0.0
            var maxIndex = start

            for (i in (start + 1) until end) {
                val dist = perpendicularDistance(
                    points[i], points[start], points[end]
                )
                if (dist > maxDist) {
                    maxDist = dist
                    maxIndex = i
                }
            }

            if (maxDist > tolerance || keep[maxIndex]) {
                keep[maxIndex] = true
                stack.addLast(start to maxIndex)
                stack.addLast(maxIndex to end)
            }
        }

        return points.filterIndexed { index, _ -> keep[index] }
    }

    /**
     * Calculate perpendicular distance from a point to a line segment
     * defined by two endpoints, using geographic (lat/lon) coordinates.
     *
     * Approximation using equirectangular projection — accurate enough
     * for nearby points on a route.
     */
    private fun perpendicularDistance(
        point: RoutePoint,
        lineStart: RoutePoint,
        lineEnd: RoutePoint
    ): Double {
        // Convert to local planar coordinates (meters) using equirectangular projection
        val avgLat = Math.toRadians((lineStart.latitude + lineEnd.latitude) / 2.0)
        val cosLat = cos(avgLat)

        val x = Math.toRadians(point.longitude - lineStart.longitude) * cosLat * EARTH_RADIUS
        val y = Math.toRadians(point.latitude - lineStart.latitude) * EARTH_RADIUS
        val x1 = 0.0
        val y1 = 0.0
        val x2 = Math.toRadians(lineEnd.longitude - lineStart.longitude) * cosLat * EARTH_RADIUS
        val y2 = Math.toRadians(lineEnd.latitude - lineStart.latitude) * EARTH_RADIUS

        val lineLen = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
        if (lineLen < 1e-10) {
            return sqrt(x * x + y * y)
        }

        return abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1) / lineLen
    }

    private const val EARTH_RADIUS = 6_371_000.0 // meters
}
