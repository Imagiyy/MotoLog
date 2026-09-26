package com.abrar.motolog.shared.domain.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class RouteDownsamplerTest {

    private fun createPoint(
        lat: Double,
        lon: Double,
        speedKmh: Double = 50.0,
        altitude: Double? = 100.0,
        timestamp: Long = 1000L,
        isPaused: Boolean = false,
        isGap: Boolean = false
    ) = RouteDownsampler.RoutePoint(
        latitude = lat,
        longitude = lon,
        speedKmh = speedKmh,
        altitudeMeters = altitude,
        timestampMs = timestamp,
        isPaused = isPaused,
        isGap = isGap
    )

    @Test
    fun pointsCountBelowOrEqualtoMaxPointsReturnedUnchanged() {
        val points = (1..50).map { i ->
            createPoint(12.0 + i * 0.001, 77.0 + i * 0.001, timestamp = i * 1000L)
        }

        val result = RouteDownsampler.downsample(points, toleranceMeters = 5.0, maxPoints = 100)
        assertEquals(points.size, result.size)
        assertEquals(points, result)
    }

    @Test
    fun firstAndLastPointsAreAlwaysPreserved() {
        // Collinear straight line of 200 points
        val points = (0..200).map { i ->
            createPoint(12.0 + i * 0.0001, 77.0 + i * 0.0001, timestamp = i * 1000L)
        }

        val result = RouteDownsampler.downsample(points, toleranceMeters = 5.0, maxPoints = 50)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test
    fun gapBoundariesAndPauseTransitionsArePreserved() {
        val points = (0..300).map { i ->
            createPoint(
                lat = 12.0 + i * 0.0001,
                lon = 77.0 + i * 0.0001,
                timestamp = i * 1000L,
                isPaused = i in 100..120,
                isGap = i == 200
            )
        }

        val result = RouteDownsampler.downsample(points, toleranceMeters = 5.0, maxPoints = 50)

        // Point before pause (99) and pause transition (100)
        assertTrue(result.any { it.latitude == points[100].latitude }, "Pause transition 100 should be preserved")
        // Gap point (200)
        assertTrue(result.any { it.isGap }, "Gap point 200 should be preserved")
    }

    @Test
    fun speedExtremesArePreserved() {
        val points = (0..200).map { i ->
            val speed = if (i == 45) 150.0 else 50.0
            createPoint(
                lat = 12.0 + i * 0.0001,
                lon = 77.0 + i * 0.0001,
                speedKmh = speed,
                timestamp = i * 1000L
            )
        }

        val result = RouteDownsampler.downsample(points, toleranceMeters = 10.0, maxPoints = 30)
        assertTrue(result.any { it.speedKmh == 150.0 }, "Max speed spike point 45 should be preserved")
    }

    @Test
    fun synthetic7200PointRideDownsamplesWithinPerformanceBudget() {
        // 2-hour ride at 1 Hz = 7200 points
        val points = (0 until 7200).map { i ->
            val lat = 12.0 + (i * 0.00002) + if (i % 50 == 0) 0.0001 else 0.0
            val lon = 77.0 + (i * 0.00002)
            createPoint(
                lat = lat,
                lon = lon,
                speedKmh = 40.0 + (i % 30),
                altitude = 500.0 + (i * 0.01),
                timestamp = i * 1000L,
                isPaused = i in 1800..1900,
                isGap = i in 3600..3605
            )
        }

        val mark = TimeSource.Monotonic.markNow()
        val result = RouteDownsampler.downsample(points, toleranceMeters = 5.0, maxPoints = 1000)
        val elapsed = mark.elapsedNow().inWholeMilliseconds

        assertTrue(result.size <= 1000, "Result size should be <= maxPoints")
        assertTrue(elapsed < 500, "Downsampling 7200 points took $elapsed ms (expected < 500ms)")
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }
}
