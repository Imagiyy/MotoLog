package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.domain.model.GpsPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for crash/force-kill recovery and point reconstruction.
 */
class RideRecoveryTest {

    @Test
    fun recovery_rebuildsAccurateStatsFromStoredPoints() {
        val rideId = 42L
        val startTime = 1774000000000L

        // Simulate an interrupted ride with 60 seconds of riding at 10 m/s (36 km/h) = ~600m
        val storedPoints = mutableListOf<RidePointEntity>()
        var time = startTime
        var lat = 12.0000
        val step = 10.0 / 111_139.0

        for (i in 0..60) {
            storedPoints.add(
                RidePointEntity(
                    id = i.toLong(),
                    rideId = rideId,
                    timestamp = time,
                    latitude = lat,
                    longitude = 77.0000,
                    speedMs = 10.0,
                    accuracyMeters = 5.0f,
                    isPaused = false,
                    isGap = false
                )
            )
            time += 1000L
            lat += step
        }

        // Active ride row left in DB before sudden kill
        val unfinalizedRide = RideEntity(
            id = rideId,
            startTime = startTime,
            endTime = 0L,
            status = RideStatus.ACTIVE
        )

        // Recovery process: map stored entities to GpsPoints and replay through engine
        val gpsPoints = storedPoints.map { p ->
            GpsPoint(
                timestampEpochMs = p.timestamp,
                latitude = p.latitude,
                longitude = p.longitude,
                speedMps = p.speedMs.toFloat(),
                accuracyMeters = p.accuracyMeters
            )
        }
        val reconstructedStats = RideCalculator.processAll(gpsPoints)

        val recoveredRide = unfinalizedRide.copy(
            endTime = storedPoints.last().timestamp,
            distanceMeters = reconstructedStats.totalDistanceMeters,
            elapsedTimeMs = reconstructedStats.elapsedTimeMs,
            movingTimeMs = reconstructedStats.movingTimeMs,
            avgMovingSpeedMs = reconstructedStats.avgMovingSpeedKmh / 3.6,
            overallAvgSpeedMs = reconstructedStats.avgOverallSpeedKmh / 3.6,
            maxSpeedMs = reconstructedStats.maxSpeedKmh / 3.6,
            status = RideStatus.RECOVERED
        )

        assertEquals(RideStatus.RECOVERED, recoveredRide.status)
        assertEquals(storedPoints.last().timestamp, recoveredRide.endTime)
        assertEquals(600.0, recoveredRide.distanceMeters, 20.0)
        assertEquals(60_000L, recoveredRide.movingTimeMs)
        assertEquals(10.0, recoveredRide.avgMovingSpeedMs, 0.5) // ~36 km/h = 10 m/s
        assertEquals(10.0, recoveredRide.maxSpeedMs, 0.5)
    }

    @Test
    fun recovery_abruptStopDoesNotLoseAcceptedPoints() {
        val points = listOf(
            GpsPoint(1000L, 12.0000, 77.0000, 10f, 5f),
            GpsPoint(2000L, 12.0001, 77.0000, 10f, 5f),
            GpsPoint(3000L, 12.0002, 77.0000, 10f, 5f)
        )

        val calculator = RideCalculator()
        val results = points.map { calculator.process(it) }

        assertTrue(results.all { it is PointFilterResult.Accepted })
        assertEquals(3, calculator.stats.acceptedPointCount)
        assertNotEquals(0.0, calculator.stats.totalDistanceMeters)
    }
}
