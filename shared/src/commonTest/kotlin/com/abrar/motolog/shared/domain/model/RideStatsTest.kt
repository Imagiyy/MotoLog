package com.abrar.motolog.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RideStatsTest {

    @Test
    fun defaultRideStats_isZeroRideReturnsTrue() {
        val stats = RideStats()
        assertTrue(stats.isZeroRide(), "Default RideStats with 0 accepted points must be considered a zero ride")
    }

    @Test
    fun acceptedPointsWithZeroDistanceAndZeroSpeed_isZeroRideReturnsTrue() {
        val stats = RideStats(
            acceptedPointCount = 5,
            totalDistanceMeters = 0.0,
            movingTimeMs = 0L,
            maxSpeedKmh = 0.0
        )
        assertTrue(stats.isZeroRide(), "Points recorded while stationary with zero movement must be considered a zero ride")
    }

    @Test
    fun stationaryJitterBelowThreshold_isZeroRideReturnsTrue() {
        val stats = RideStats(
            acceptedPointCount = 20,
            totalDistanceMeters = 4.5, // Less than 10m threshold
            movingTimeMs = 0L,
            maxSpeedKmh = 1.0 // Below stationary threshold (1.5 km/h)
        )
        assertTrue(stats.isZeroRide(), "Stationary jitter below saveable threshold must be considered a zero ride")
    }

    @Test
    fun actualRideWithDistance_isZeroRideReturnsFalse() {
        val stats = RideStats(
            acceptedPointCount = 10,
            totalDistanceMeters = 150.0,
            movingTimeMs = 15000L,
            maxSpeedKmh = 40.0
        )
        assertFalse(stats.isZeroRide(), "Real ride with recorded distance and moving time must not be considered a zero ride")
    }

    @Test
    fun actualRideWithMovementAboveStationarySpeed_isZeroRideReturnsFalse() {
        val stats = RideStats(
            acceptedPointCount = 10,
            totalDistanceMeters = 25.0,
            movingTimeMs = 3000L,
            maxSpeedKmh = 15.0
        )
        assertFalse(stats.isZeroRide(), "Ride with speed > 1.5 km/h and distance > 10m must not be considered a zero ride")
    }
}
