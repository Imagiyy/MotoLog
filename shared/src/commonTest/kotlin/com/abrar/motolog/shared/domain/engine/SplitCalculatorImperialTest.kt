package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.GpsPoint
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplitCalculatorImperialTest {

    private val originLat = 12.9716
    private val originLon = 77.5946

    // 1 mile in meters
    private val oneMileMeters = UnitConverter.METERS_PER_MILE // 1609.344

    // 1 meter in degrees latitude
    private val latPerMeter = (1.0 / 6_371_000.0) * (180.0 / PI)

    @Test
    fun rawPointsComputedAtOneMileIntervals() {
        // Construct a ride of exactly 3.5 miles at 20 m/s (72 km/h = ~44.7 mph)
        // 3.5 miles = 5,632.704 meters
        val totalDistanceMeters = 3.5 * oneMileMeters
        val speedMps = 20f
        val points = mutableListOf<GpsPoint>()

        var currentLat = originLat
        var timestamp = 1_000_000L
        var accumulatedMeters = 0.0
        val stepMeters = 20.0 // 1 point per second at 20 m/s

        while (accumulatedMeters <= totalDistanceMeters) {
            points.add(
                GpsPoint(
                    timestampEpochMs = timestamp,
                    latitude = currentLat,
                    longitude = originLon,
                    speedMps = speedMps,
                    accuracyMeters = 4f
                )
            )
            accumulatedMeters += stepMeters
            currentLat += stepMeters * latPerMeter
            timestamp += 1000L
        }

        // Compute splits with 1-mile distance target (1609.344 m)
        val splits = SplitCalculator.computeSplits(points, splitDistanceMeters = oneMileMeters)

        // Should have 3 full mile splits and 1 partial 0.5 mile split
        assertEquals(4, splits.size)

        // Split 1
        assertEquals(1, splits[0].splitNumber)
        assertFalse(splits[0].isPartial)
        assertEquals(oneMileMeters, splits[0].distanceMeters, 25.0)

        // Split 2
        assertEquals(2, splits[1].splitNumber)
        assertFalse(splits[1].isPartial)
        assertEquals(oneMileMeters, splits[1].distanceMeters, 25.0)

        // Split 3
        assertEquals(3, splits[2].splitNumber)
        assertFalse(splits[2].isPartial)
        assertEquals(oneMileMeters, splits[2].distanceMeters, 25.0)

        // Split 4 (Partial 0.5 mile = ~804.67 m)
        assertEquals(4, splits[3].splitNumber)
        assertTrue(splits[3].isPartial)
        assertEquals(0.5 * oneMileMeters, splits[3].distanceMeters, 35.0)
    }
}
