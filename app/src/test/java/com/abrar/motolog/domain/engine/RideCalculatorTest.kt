package com.abrar.motolog.domain.engine

import com.abrar.motolog.domain.model.GpsPoint
import com.abrar.motolog.test.FixtureGenerator
import com.abrar.motolog.test.GpxParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class RideCalculatorTest {

    companion object {
        private val FIXTURES_DIR = File("src/test/resources/fixtures")

        @BeforeClass
        @JvmStatic
        fun setupFixtures() {
            FixtureGenerator.generateAll(FIXTURES_DIR)
        }

        private fun loadFixture(name: String): List<GpsPoint> {
            val file = File(FIXTURES_DIR, name)
            assertTrue("Fixture file must exist: ${file.absolutePath}", file.exists())
            return GpxParser.parse(FileInputStream(file))
        }
    }

    // ============================================================
    // Fixture 1: 5-Minute Stationary Jitter Test
    // Requirement from AGENTS.md: a stationary phone for 5 minutes adds under 10 m of distance.
    // ============================================================

    @Test
    fun stationaryJitter_fiveMinutesAddsUnderTenMeters() {
        val points = loadFixture("stationary_jitter.gpx")
        assertEquals(300, points.size)

        val stats = RideCalculator.processAll(points)

        // AGENTS.md requirement: stationary phone for 5 minutes adds under 10 m
        assertTrue(
            "Stationary drift must add under 10 m, but was ${stats.totalDistanceMeters} m",
            stats.totalDistanceMeters < 10.0
        )
        // Analytical expectation: 0 moving time
        assertEquals(0L, stats.movingTimeMs)
        assertEquals(0.0, stats.maxSpeedKmh, 0.001)
        assertEquals(0.0, stats.avgMovingSpeedKmh, 0.001)
    }

    // ============================================================
    // Fixture 2: 20-Minute Highway Ride Test
    // ============================================================

    @Test
    fun highwayRide_sustainedSpeedComputesExpectedDistanceAndAverages() {
        val points = loadFixture("highway_ride.gpx")
        assertEquals(1200, points.size)

        val stats = RideCalculator.processAll(points)

        // Analytical ground truth: 25.0 m/s * 1199 s = ~29,975m (approx 30 km within 0.5%)
        assertEquals(30_000.0, stats.totalDistanceMeters, 200.0)
        assertEquals(1199_000L, stats.movingTimeMs)
        assertEquals(90.0, stats.maxSpeedKmh, 0.5)
        assertEquals(90.0, stats.avgMovingSpeedKmh, 0.5)
        assertEquals(90.0, stats.avgOverallSpeedKmh, 0.5)
        assertEquals(0, stats.gapCount)
    }

    // ============================================================
    // Fixture 3: Stop-and-Go Ride Test
    // ============================================================

    @Test
    fun stopAndGo_correctlySeparatesMovingVsStoppedTime() {
        val points = loadFixture("stop_and_go.gpx")
        assertEquals(600, points.size)

        val stats = RideCalculator.processAll(points)

        // Analytical ground truth: 3,000m distance, 300s moving, 299s stopped
        assertEquals(3_000.0, stats.totalDistanceMeters, 50.0)
        assertTrue("Moving time must be approx 300s (was ${stats.movingTimeMs}ms)",
            stats.movingTimeMs in 290_000L..310_000L)
        assertTrue("Stopped time must be approx 300s (was ${stats.stoppedTimeMs}ms)",
            stats.stoppedTimeMs in 290_000L..310_000L)

        // Moving avg speed ~ 36 km/h, Overall avg speed ~ 18 km/h
        assertEquals(36.0, stats.avgMovingSpeedKmh, 1.0)
        assertEquals(18.0, stats.avgOverallSpeedKmh, 1.0)
    }

    // ============================================================
    // Fixture 4: Tunnel Gap Test
    // ============================================================

    @Test
    fun tunnelGap_doesNotInventDistanceAcrossSignalBlackout() {
        val points = loadFixture("tunnel_gap.gpx")
        assertEquals(600, points.size)

        val stats = RideCalculator.processAll(points)

        // Must identify the 45s gap
        assertEquals(1, stats.gapCount)
        // Must NOT add distance across the gap: expected ~15,000m (not 16,125m)
        assertEquals(15_000.0, stats.totalDistanceMeters, 100.0)
        // Elapsed time is 645s (including the 45s gap), moving time is ~600s
        assertEquals(644_000L, stats.elapsedTimeMs)
        assertTrue("Moving time should be approx 600s, was ${stats.movingTimeMs}ms",
            stats.movingTimeMs in 595_000L..605_000L)
    }

    // ============================================================
    // Fixture 5: City Ride Test
    // ============================================================

    @Test
    fun cityRide_urbanRouteWithTrafficLightsCalculatesAccurately() {
        val points = loadFixture("city_ride.gpx")
        assertEquals(900, points.size)

        val stats = RideCalculator.processAll(points)

        // 765s moving at 15 m/s (54 km/h) = ~11,475m
        assertEquals(11_475.0, stats.totalDistanceMeters, 150.0)
        assertTrue("Moving time must be approx 765s (was ${stats.movingTimeMs}ms)",
            stats.movingTimeMs in 755_000L..775_000L)
        assertTrue("Stopped time must be approx 135s (was ${stats.stoppedTimeMs}ms)",
            stats.stoppedTimeMs in 125_000L..145_000L)
        assertEquals(54.0, stats.maxSpeedKmh, 1.0)
    }

    // ============================================================
    // Edge Cases & Filtering Unit Tests
    // ============================================================

    @Test
    fun filtering_rejectsPoorAccuracyPoints() {
        val calculator = RideCalculator()

        val good = GpsPoint(1000L, 12.9716, 77.5946, 10f, 15f)
        val bad = GpsPoint(2000L, 12.9726, 77.5946, 10f, 35f) // 35m > 25m threshold

        val res1 = calculator.process(good)
        val res2 = calculator.process(bad)

        assertTrue(res1 is PointFilterResult.Accepted)
        assertTrue(res2 is PointFilterResult.Rejected)
        assertEquals(RejectionReason.POOR_ACCURACY, (res2 as PointFilterResult.Rejected).reason)
        assertEquals(1, calculator.stats.acceptedPointCount)
        assertEquals(1, calculator.stats.rejectedPointCount)
    }

    @Test
    fun filtering_rejectsNonMonotonicTimestamps() {
        val calculator = RideCalculator()

        calculator.process(GpsPoint(5000L, 12.9716, 77.5946, 10f, 10f))
        // Duplicate timestamp
        val dup = calculator.process(GpsPoint(5000L, 12.9720, 77.5946, 10f, 10f))
        // Out of order timestamp
        val backInTime = calculator.process(GpsPoint(4000L, 12.9720, 77.5946, 10f, 10f))

        assertTrue(dup is PointFilterResult.Rejected)
        assertEquals(RejectionReason.NON_MONOTONIC_TIMESTAMP, (dup as PointFilterResult.Rejected).reason)
        assertTrue(backInTime is PointFilterResult.Rejected)
        assertEquals(RejectionReason.NON_MONOTONIC_TIMESTAMP, (backInTime as PointFilterResult.Rejected).reason)
    }

    @Test
    fun filtering_rejectsSpeedSpikesAbove250Kmh() {
        val calculator = RideCalculator()

        calculator.process(GpsPoint(1000L, 12.0000, 77.0000, 20f, 5f))
        // Jump 500 meters in 1 second -> 500 m/s = 1800 km/h (teleportation spike)
        val spike = calculator.process(GpsPoint(2000L, 12.0045, 77.0000, 20f, 5f))

        assertTrue(spike is PointFilterResult.Rejected)
        assertEquals(RejectionReason.SPIKE_SPEED, (spike as PointFilterResult.Rejected).reason)
    }

    @Test
    fun filtering_rejectsAccelerationSpikesAbove15Ms2() {
        val calculator = RideCalculator()

        // Point 1 at 36 km/h (10 m/s)
        calculator.process(GpsPoint(1000L, 12.0000, 77.0000, 10f, 5f))
        // Point 2 1s later claiming 144 km/h (40 m/s): acceleration = (40 - 10) / 1s = 30 m/s^2 > 15 m/s^2
        val spike = calculator.process(GpsPoint(2000L, 12.0002, 77.0000, 40f, 5f))

        assertTrue(spike is PointFilterResult.Rejected)
        assertEquals(RejectionReason.SPIKE_ACCELERATION, (spike as PointFilterResult.Rejected).reason)
    }

    @Test
    fun speed_fallsBackToDistanceOverTimeWhenMissing() {
        val calculator = RideCalculator()

        // Missing speed on first point
        calculator.process(GpsPoint(1000L, 12.0000, 77.0000, null, 5f))
        // 12 meters in 1 second = 12 m/s = 43.2 km/h (within 15 m/s^2 acceleration limit)
        val dLat = 12.0 / 111_139.0
        val res = calculator.process(GpsPoint(2000L, 12.0000 + dLat, 77.0000, null, 5f))

        assertTrue(res is PointFilterResult.Accepted)
        assertEquals(43.2, (res as PointFilterResult.Accepted).speedKmh, 0.5)
        assertEquals(43.2, calculator.stats.currentSpeedKmh, 0.5)
    }

    @Test
    fun maxSpeed_unconfirmedNoisySpikeDoesNotSetMaxSpeed() {
        val calculator = RideCalculator()

        // Normal driving at 60 km/h (16.67 m/s)
        calculator.process(GpsPoint(1000L, 12.0000, 77.0000, 16.67f, 5f))
        val step = 16.67 / 111_139.0
        calculator.process(GpsPoint(2000L, 12.0000 + step, 77.0000, 16.67f, 5f))
        assertEquals(60.0, calculator.stats.maxSpeedKmh, 0.5)

        // A single noisy reading claiming 100 km/h (27.78 m/s, delta v = 11.11 m/s in 1s -> 11.11 m/s^2 <= 15 m/s^2)
        // with poor speed accuracy (3.0 m/s)
        calculator.process(GpsPoint(3000L, 12.0000 + (step * 2), 77.0000, 27.78f, 5f, speedAccuracyMps = 3.0f))

        // Must NOT set max speed on unconfirmed noisy reading!
        assertEquals("Single unconfirmed noisy point must not set max speed", 60.0, calculator.stats.maxSpeedKmh, 0.5)

        // When confirmed on the next reading:
        calculator.process(GpsPoint(4000L, 12.0000 + (step * 3), 77.0000, 27.78f, 5f, speedAccuracyMps = 3.0f))
        assertEquals("Confirmed speed across 2 readings should update max speed", 100.0, calculator.stats.maxSpeedKmh, 0.5)
    }

    @Test
    fun stationaryDrift_zeroSpeedDoesNotAddDistanceEvenIfDisplacementExceedsAccuracy() {
        val calculator = RideCalculator()

        // Initial point stationary (0 speed, 5m accuracy)
        calculator.process(GpsPoint(1000L, 12.0000, 77.0000, 0f, 5f))

        // GPS jumps 8 meters (greater than 5m accuracy), but speed is 0 m/s
        val dLat = 8.0 / 111_139.0
        val res = calculator.process(GpsPoint(2000L, 12.0000 + dLat, 77.0000, 0f, 5f))

        assertTrue(res is PointFilterResult.Accepted)
        // Distance added must strictly be 0.0 because speed is stationary (< 1.5 km/h)
        assertEquals(0.0, (res as PointFilterResult.Accepted).distanceIncrementMeters, 0.001)
        assertEquals(0.0, calculator.stats.totalDistanceMeters, 0.001)
        assertEquals(0L, calculator.stats.movingTimeMs)
        assertEquals(0.0, calculator.stats.avgMovingSpeedKmh, 0.001)
    }

    @Test
    fun avgMovingSpeed_doesNotIncreaseWhileStoppedAtTrafficLight() {
        val calculator = RideCalculator()

        // Ride for 10 seconds at 36 km/h (10 m/s = 100 meters)
        var time = 1000L
        var lat = 12.0000
        val step = 10.0 / 111_139.0
        for (i in 0..10) {
            calculator.process(GpsPoint(time, lat, 77.0000, 10f, 3f))
            time += 1000L
            lat += step
        }

        val movingAvgBeforeStop = calculator.stats.avgMovingSpeedKmh
        assertEquals(36.0, movingAvgBeforeStop, 0.5)

        // Now stopped at a red light for 30 seconds with GPS coordinate drift
        for (i in 0..30) {
            // Drift coordinate slightly
            val driftLat = lat + ((i % 3) * 2.0 / 111_139.0)
            calculator.process(GpsPoint(time, driftLat, 77.0000, 0f, 3f))
            time += 1000L
        }

        val movingAvgAfterStop = calculator.stats.avgMovingSpeedKmh
        // Moving average speed must remain unchanged while stopped; it must NEVER inflate!
        assertEquals("Moving average speed must not increase while stopped", movingAvgBeforeStop, movingAvgAfterStop, 0.01)
        // Overall average speed decreases as elapsed time accumulates while stopped
        assertTrue(calculator.stats.avgOverallSpeedKmh < movingAvgBeforeStop)
    }
}
