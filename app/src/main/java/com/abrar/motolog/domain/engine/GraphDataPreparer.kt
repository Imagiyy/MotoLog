package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.domain.model.GraphData
import com.abrar.motolog.domain.model.GraphPoint
import com.abrar.motolog.domain.model.GraphSegment
import kotlin.math.roundToInt

/**
 * Prepares [GraphData] from ride points for speed-over-time and
 * elevation-over-time graph rendering.
 *
 * Pure Kotlin logic — no Android or Compose imports.
 * Uses [RidePointEntity] directly to avoid unnecessary mapping.
 */
object GraphDataPreparer {

    /**
     * Prepare speed-over-time graph data from ride points.
     *
     * @param points Ride points in chronological order.
     * @param startTimeMs Ride start time (epoch ms) for elapsed time calculation.
     * @param targetPointCount Desired max number of points per segment for display
     *        (downsampled if necessary to avoid over-rendering).
     */
    fun prepareSpeedGraph(
        points: List<RidePointEntity>,
        startTimeMs: Long,
        targetPointCount: Int = 500
    ): GraphData? {
        if (points.size < 2) return null

        val segments = buildSegments(points, startTimeMs) { point ->
            // speedMs is stored in m/s, convert to km/h
            (point.speedMs * 3.6).toFloat()
        }

        if (segments.isEmpty()) return null

        val allPoints = segments.flatMap { it.points }
        val xMin = allPoints.minOf { it.x }
        val xMax = allPoints.maxOf { it.x }
        val yMin = 0f
        val yMax = allPoints.maxOf { it.y }.coerceAtLeast(10f)

        // Downsample segments to target point count
        val downsampledSegments = segments.map { segment ->
            if (segment.points.size > targetPointCount) {
                GraphSegment(lttbDownsample(segment.points, targetPointCount))
            } else {
                segment
            }
        }

        return GraphData(
            segments = downsampledSegments,
            xRange = xMin..xMax,
            yRange = yMin..yMax,
            xLabel = "Time",
            yLabel = "Speed (km/h)"
        )
    }

    /**
     * Prepare elevation profile graph data from ride points.
     *
     * @param points Ride points in chronological order.
     * @param startTimeMs Ride start time (epoch ms).
     * @param targetPointCount Max points per segment.
     */
    fun prepareElevationGraph(
        points: List<RidePointEntity>,
        startTimeMs: Long,
        targetPointCount: Int = 500
    ): GraphData? {
        val pointsWithElevation = points.filter { it.altitudeMeters != 0.0 }
        if (pointsWithElevation.size < 2) return null

        val segments = buildSegments(pointsWithElevation, startTimeMs) { point ->
            point.altitudeMeters.toFloat()
        }

        if (segments.isEmpty()) return null

        val allPoints = segments.flatMap { it.points }
        val xMin = allPoints.minOf { it.x }
        val xMax = allPoints.maxOf { it.x }
        val yMin = allPoints.minOf { it.y }
        val yMax = allPoints.maxOf { it.y }

        // Add padding to Y range
        val yPadding = ((yMax - yMin) * 0.1f).coerceAtLeast(5f)

        val downsampledSegments = segments.map { segment ->
            if (segment.points.size > targetPointCount) {
                GraphSegment(lttbDownsample(segment.points, targetPointCount))
            } else {
                segment
            }
        }

        return GraphData(
            segments = downsampledSegments,
            xRange = xMin..xMax,
            yRange = (yMin - yPadding)..(yMax + yPadding),
            xLabel = "Time",
            yLabel = "Elevation (m)"
        )
    }

    /**
     * Build graph segments from points, breaking at gaps.
     *
     * @param points Filtered points (already excluding paused if desired).
     * @param startTimeMs Ride start timestamp.
     * @param yValue Extractor function for the Y-axis value.
     */
    private fun buildSegments(
        points: List<RidePointEntity>,
        startTimeMs: Long,
        yValue: (RidePointEntity) -> Float
    ): List<GraphSegment> {
        if (points.isEmpty()) return emptyList()

        val segments = mutableListOf<GraphSegment>()
        var currentSegmentPoints = mutableListOf<GraphPoint>()

        for (i in points.indices) {
            val point = points[i]
            val elapsedSeconds = ((point.timestamp - startTimeMs) / 1000f).coerceAtLeast(0f)

            // Break at pauses and gaps; never interpolate unknown intervals.
            if ((point.isGap || point.isPaused) && currentSegmentPoints.isNotEmpty()) {
                if (currentSegmentPoints.size >= 2) {
                    segments.add(GraphSegment(currentSegmentPoints))
                }
                currentSegmentPoints = mutableListOf()
                continue
            }

            currentSegmentPoints.add(GraphPoint(x = elapsedSeconds, y = yValue(point)))
        }

        // Add the final segment
        if (currentSegmentPoints.size >= 2) {
            segments.add(GraphSegment(currentSegmentPoints))
        }

        return segments
    }

    /**
     * Largest-Triangle-Three-Buckets (LTTB) downsampling for time-series graphs.
     *
     * Preserves visual appearance better than simple nth-point sampling by
     * selecting the most visually significant point in each bucket.
     */
    internal fun lttbDownsample(
        points: List<GraphPoint>,
        targetCount: Int
    ): List<GraphPoint> {
        if (points.size <= targetCount) return points
        if (targetCount < 3) return listOf(points.first(), points.last())

        val result = mutableListOf<GraphPoint>()
        result.add(points.first()) // Always keep first

        val bucketSize = (points.size - 2).toDouble() / (targetCount - 2)

        var prevSelectedIndex = 0

        for (bucket in 0 until targetCount - 2) {
            val bucketStart = ((bucket * bucketSize) + 1).roundToInt()
            val bucketEnd = (((bucket + 1) * bucketSize) + 1).roundToInt().coerceAtMost(points.size - 1)

            // Calculate average of next bucket for triangle area
            val nextBucketStart = bucketEnd
            val nextBucketEnd = (((bucket + 2) * bucketSize) + 1).roundToInt().coerceAtMost(points.size - 1)
            var avgX = 0f
            var avgY = 0f
            val nextBucketLen = nextBucketEnd - nextBucketStart + 1
            for (i in nextBucketStart..nextBucketEnd.coerceAtMost(points.size - 1)) {
                avgX += points[i].x
                avgY += points[i].y
            }
            avgX /= nextBucketLen
            avgY /= nextBucketLen

            // Select point with largest triangle area
            var maxArea = -1f
            var selectedIndex = bucketStart
            val prevPoint = points[prevSelectedIndex]

            for (i in bucketStart until bucketEnd.coerceAtMost(points.size)) {
                val area = kotlin.math.abs(
                    (prevPoint.x - avgX) * (points[i].y - prevPoint.y) -
                            (prevPoint.x - points[i].x) * (avgY - prevPoint.y)
                ) * 0.5f
                if (area > maxArea) {
                    maxArea = area
                    selectedIndex = i
                }
            }

            result.add(points[selectedIndex])
            prevSelectedIndex = selectedIndex
        }

        result.add(points.last()) // Always keep last
        return result
    }
}
