package com.abrar.motolog.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `points count below or equal to maxPoints returned unchanged`() {
        val points = (1..50).map { i ->
            createPoint(12.0 + i * 0.001, 77.0 + i * 0.001, timestamp = i * 1000L)
        }

        val result = RouteDownsampler.downsample(points, toleranceMeters = 5.0, maxPoints = 100)
        assertEquals(points.size, result.size)
        assertEquals(points, result)
    }

    @Test
    fun `first and last points are always preserved`() {
        // Collinear straight line of 200 points
        val points = (0..200).map { i ->
            createPoint(12.0 + i * 0.0001, 77.0 + i * 0.0001, timestamp = i * 1000L)
        }

        val result = RouteDownsampler.downsample(points, toleranceMeters = 5.0, maxPoints = 50)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test
    fun `gap boundaries and pause transitions are preserved`() {
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
        assertTrue("Pause transition 100 should be preserved", result.any { it.latitude == points[100].latitude })
        // Gap point (200)
        assertTrue("Gap point 200 should be preserved", result.any { it.isGap })
    }

    @Test
    fun `speed extremes are preserved`() {
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
        assertTrue("Max speed spike point 45 should be preserved", result.any { it.speedKmh == 150.0 })
    }

    @Test
    fun `synthetic 7200 point ride downsamples within performance budget`() {
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

        val start = System.currentTimeMillis()
        val result = RouteDownsampler.downsample(points, toleranceMeters = 5.0, maxPoints = 1000)
        val elapsed = System.currentTimeMillis() - start

        assertTrue("Result size should be <= maxPoints", result.size <= 1000)
        assertTrue("Downsampling 7200 points took $elapsed ms (expected < 250ms)", elapsed < 250)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }
}
