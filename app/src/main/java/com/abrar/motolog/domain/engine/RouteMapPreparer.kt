package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.shared.domain.engine.RouteDownsampler
import com.abrar.motolog.shared.domain.engine.SpeedColorScale
import com.abrar.motolog.shared.domain.model.RouteMapData
import com.abrar.motolog.shared.domain.model.RouteMapPoint
import com.abrar.motolog.shared.domain.model.RouteMapSegment

/** Builds speed-bucketed, gap-safe route data for the map. */
object RouteMapPreparer {
    fun prepare(
        points: List<RidePointEntity>,
        maxPoints: Int = 2_000
    ): RouteMapData {
        val chunks = mutableListOf<MutableList<RouteDownsampler.RoutePoint>>()
        var currentChunk = mutableListOf<RouteDownsampler.RoutePoint>()
        points.filter { it.accuracyMeters <= 25f }.forEach { point ->
            if (point.isPaused || point.isGap) {
                if (currentChunk.isNotEmpty()) chunks += currentChunk
                currentChunk = mutableListOf()
            } else {
                currentChunk += RouteDownsampler.RoutePoint(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    speedKmh = (point.speedMs * 3.6).coerceAtLeast(0.0),
                    altitudeMeters = point.altitudeMeters,
                    timestampMs = point.timestamp,
                    isPaused = false,
                    isGap = false
                )
            }
        }
        if (currentChunk.isNotEmpty()) chunks += currentChunk
        val downsampledChunks = chunks.map { RouteDownsampler.downsample(it, maxPoints = maxPoints) }
        val downsampled = downsampledChunks.flatten()
        if (downsampled.size < 2) {
            val point = downsampled.firstOrNull()?.toMapPoint()
            return RouteMapData(emptyList(), point, point, 0.0, 0.0)
        }

        val scale = SpeedColorScale.computeScale(downsampled.map { it.speedKmh })
        val minSpeed = scale?.minSpeedKmh ?: 0.0
        val maxSpeed = scale?.maxSpeedKmh ?: 1.0
        val segments = mutableListOf<RouteMapSegment>()
        downsampledChunks.forEach { chunk ->
            if (chunk.size < 2) return@forEach
            var currentBucket = bucket(chunk[0].speedKmh, minSpeed, maxSpeed)
            var currentPoints = mutableListOf(chunk[0].toMapPoint())
            for (index in 1 until chunk.size) {
                val point = chunk[index]
                val pointBucket = bucket(point.speedKmh, minSpeed, maxSpeed)
                currentPoints.add(point.toMapPoint())
                if (pointBucket != currentBucket) {
                    segments += RouteMapSegment(currentBucket, currentPoints.toList())
                    currentPoints = mutableListOf(currentPoints.last())
                    currentBucket = pointBucket
                }
            }
            if (currentPoints.size >= 2) segments += RouteMapSegment(currentBucket, currentPoints)
        }

        return RouteMapData(
            segments = segments,
            start = downsampled.first().toMapPoint(),
            end = downsampled.last().toMapPoint(),
            minSpeedKmh = minSpeed,
            maxSpeedKmh = maxSpeed
        )
    }

    private fun bucket(speed: Double, min: Double, max: Double): Int {
        if (max <= min) return 0
        return (((speed - min) / (max - min)) * 4.0).toInt().coerceIn(0, 4)
    }

    private fun RouteDownsampler.RoutePoint.toMapPoint() = RouteMapPoint(
        latitude = latitude,
        longitude = longitude,
        speedKmh = speedKmh
    )
}
