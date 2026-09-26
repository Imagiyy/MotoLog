package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.GpsPoint
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplitCalculatorTest {

    private val originLat = 12.9716
    private val originLon = 77.5946

    // Earth radius: 6,371,000 m. 1 degree latitude = 2 * PI * 6,371,000 / 360 = 111,194.9266 m
    // 1,000 meters in degrees: (1000.0 / 6_371_000.0) * (180.0 / PI)
    private val latPerKm = (1000.0 / 6_371_000.0) * (180.0 / PI)

    @Test
    fun emptyOrSinglePoint_returnsEmptyList() {
        assertTrue(SplitCalculator.computeSplits(emptyList()).isEmpty())

        val singlePoint = listOf(
            GpsPoint(1000L, originLat, originLon, speedMps = 10f, accuracyMeters = 5f)
        )
        assertTrue(SplitCalculator.computeSplits(singlePoint).isEmpty())
    }

    @Test
    fun steadyRide_producesExpectedSplitsAndDurations() {
        // 5 km ride at constant 20 m/s (72 km/h).
        // 1 km takes 50 seconds (50,000 ms).
        val points = mutableListOf<GpsPoint>()
        var currentLat = originLat
        var timestamp = 1_000_000L

        // Emit 50 intervals per km (5 km = 250 intervals)
        for (i in 0 until 250) {
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = 20f,
                    accuracyMeters = 5f
                )
            )
            timestamp += 1000L
            currentLat += latPerKm / 50.0
        }
        points.add(
            GpsPoint(
                timestampEpochMs = timestamp,
                latitude = currentLat,
                longitude = originLon,
                speedMps = 20f,
                accuracyMeters = 5f
            )
        )

        val splits = SplitCalculator.computeSplits(points)

        assertEquals(5, splits.size)
        for (i in 0 until 5) {
            val split = splits[i]
            assertEquals(i + 1, split.splitNumber)
            assertEquals(1000.0, split.distanceMeters, 2.0)
            assertEquals(50_000.0, split.durationMs.toDouble(), 1500.0)
            assertEquals(72.0, split.avgSpeedKmh, 2.0)
            assertFalse(split.isPartial)
        }
    }

    @Test
    fun rideWithPause_doesNotInflateSplitTimeOrDistance() {
        // 1 km ride, but midway there is a 60-second pause
        val points = mutableListOf<GpsPoint>()
        var currentLat = originLat
        var timestamp = 1_000_000L

        // First 500m (25 intervals)
        for (i in 0..25) {
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = 20f,
                    accuracyMeters = 5f
                )
            )
            timestamp += 1000L
            if (i < 25) {
                currentLat += latPerKm / 50.0
            }
        }

        // 60-second pause (stationary at current position)
        for (i in 1..60) {
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = 0f,
                    accuracyMeters = 5f,
                    isPaused = true
                )
            )
            timestamp += 1000L
        }

        // Second 500m (25 intervals)
        for (i in 1..25) {
            currentLat += latPerKm / 50.0
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = 20f,
                    accuracyMeters = 5f
                )
            )
            timestamp += 1000L
        }

        val splits = SplitCalculator.computeSplits(points)

        assertEquals(1, splits.size)
        val split = splits[0]
        assertEquals(1, split.splitNumber)
        assertEquals(1000.0, split.distanceMeters, 5.0)
        // Moving duration must be ~50 seconds, NOT 110 seconds!
        assertEquals(50_000.0, split.durationMs.toDouble(), 2000.0)
        assertEquals(72.0, split.avgSpeedKmh, 3.0)
    }

    @Test
    fun rideWithTunnelGap_doesNotInflateSplitTimeOrDistance() {
        // Ride with a 45-second GPS blackout gap
        val points = mutableListOf<GpsPoint>()
        var currentLat = originLat
        var timestamp = 1_000_000L

        // First 500m
        for (i in 0..25) {
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = 20f,
                    accuracyMeters = 5f
                )
            )
            timestamp += 1000L
            currentLat += latPerKm / 50.0
        }

        // 45s tunnel gap: next point is 45 seconds later and marked isGap
        timestamp += 45_000L
        points.add(
            GpsPoint(
                timestampEpochMs = timestamp,
                latitude = currentLat + (latPerKm / 50.0), // emerged at some point
                longitude = originLon,
                speedMps = 20f,
                accuracyMeters = 5f,
                isGap = true
            )
        )

        // Remaining 500m
        for (i in 1..25) {
            timestamp += 1000L
            currentLat += latPerKm / 50.0
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = 20f,
                    accuracyMeters = 5f
                )
            )
        }

        val splits = SplitCalculator.computeSplits(points)

        // Must not blow up split duration with 45s blackout
        assertTrue(splits.isNotEmpty())
        val split = splits[0]
        assertTrue(split.durationMs < 60_000L, "Split duration ${split.durationMs}ms should be around ~50s")
    }

    @Test
    fun partialFinalSplit_isMarkedAndComputedCorrectly() {
        // 1.4 km ride (1 full split of 1 km + 1 partial split of 400m)
        val points = mutableListOf<GpsPoint>()
        var currentLat = originLat
        var timestamp = 1_000_000L

        // 70 points * 20m = 1400m
        for (i in 0..70) {
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = 20f,
                    accuracyMeters = 5f
                )
            )
            timestamp += 1000L
            currentLat += latPerKm / 50.0
        }

        val splits = SplitCalculator.computeSplits(points)

        assertEquals(2, splits.size)

        // Split 1
        val split1 = splits[0]
        assertEquals(1, split1.splitNumber)
        assertEquals(1000.0, split1.distanceMeters, 2.0)
        assertFalse(split1.isPartial)

        // Split 2 (partial)
        val split2 = splits[1]
        assertEquals(2, split2.splitNumber)
        assertEquals(400.0, split2.distanceMeters, 15.0)
        assertTrue(split2.isPartial)
        assertEquals(72.0, split2.avgSpeedKmh, 3.0)
    }

    @Test
    fun shortRideUnder1Km_producesSinglePartialSplit() {
        // 400m ride
        val points = mutableListOf<GpsPoint>()
        var currentLat = originLat
        var timestamp = 1_000_000L

        for (i in 0..20) {
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = 20f,
                    accuracyMeters = 5f
                )
            )
            timestamp += 1000L
            currentLat += latPerKm / 50.0
        }

        val splits = SplitCalculator.computeSplits(points)

        assertEquals(1, splits.size)
        val split = splits[0]
        assertEquals(1, split.splitNumber)
        assertTrue(split.isPartial)
        assertEquals(400.0, split.distanceMeters, 10.0)
        assertEquals(20_000.0, split.durationMs.toDouble(), 1000.0)
    }

    @Test
    fun stationaryNoiseOnly_producesEmptyList() {
        // Stationary jitter under 1.5 km/h
        val points = listOf(
            GpsPoint(1_000L, originLat, originLon, speedMps = 0.1f, accuracyMeters = 5f),
            GpsPoint(2_000L, originLat + 0.00001, originLon, speedMps = 0.2f, accuracyMeters = 5f),
            GpsPoint(3_000L, originLat, originLon + 0.00001, speedMps = 0.1f, accuracyMeters = 5f)
        )

        val splits = SplitCalculator.computeSplits(points)
        assertTrue(splits.isEmpty())
    }

    @Test
    fun splitDistanceMeters_tenKmAndHundredKm_producesExpectedSplits() {
        val points = mutableListOf<GpsPoint>()
        var currentLat = originLat
        var timestamp = 1_000_000L

        // 25 km with 50 points per km = 1250 intervals
        for (i in 0 until 1250) {
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = 20f,
                    accuracyMeters = 5f
                )
            )
            timestamp += 1000L
            currentLat += latPerKm / 50.0
        }
        points.add(
            GpsPoint(
                timestampEpochMs = timestamp,
                latitude = currentLat,
                longitude = originLon,
                speedMps = 20f,
                accuracyMeters = 5f
            )
        )

        // 10 km splits: expect 2 full (10 km each) and 1 partial (5 km)
        val splits10k = SplitCalculator.computeSplits(points, splitDistanceMeters = 10_000.0)
        assertEquals(3, splits10k.size)
        assertEquals(1, splits10k[0].splitNumber)
        assertFalse(splits10k[0].isPartial)
        assertEquals(10_000.0, splits10k[0].distanceMeters, 50.0)
        assertEquals(2, splits10k[1].splitNumber)
        assertFalse(splits10k[1].isPartial)
        assertEquals(10_000.0, splits10k[1].distanceMeters, 50.0)
        assertEquals(3, splits10k[2].splitNumber)
        assertTrue(splits10k[2].isPartial)
        assertEquals(5_000.0, splits10k[2].distanceMeters, 50.0)

        // 100 km splits: expect 1 partial (25 km)
        val splits100k = SplitCalculator.computeSplits(points, splitDistanceMeters = 100_000.0)
        assertEquals(1, splits100k.size)
        assertTrue(splits100k[0].isPartial)
        assertEquals(25_000.0, splits100k[0].distanceMeters, 100.0)
    }
}
