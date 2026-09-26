package com.abrar.motolog.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for TrackingConstants in commonTest.
 * Validates that constants are correctly defined and have sensible relationships.
 */
class TrackingConstantsTest {

    @Test
    fun gpsAccuracyThresholdIsPositive() {
        assertTrue(
            TrackingConstants.MIN_GPS_ACCURACY_METERS > 0,
            "GPS accuracy threshold must be positive"
        )
    }

    @Test
    fun stationarySpeedThresholdIsLessThanLowSpeedDistanceThreshold() {
        assertTrue(
            TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH <
                    TrackingConstants.LOW_SPEED_DISTANCE_THRESHOLD_KMH,
            "Stationary threshold (${TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH}) " +
                    "must be less than low speed distance threshold " +
                    "(${TrackingConstants.LOW_SPEED_DISTANCE_THRESHOLD_KMH})"
        )
    }

    @Test
    fun autoResumeThresholdIsGreaterThanAutoPauseThreshold() {
        assertTrue(
            TrackingConstants.AUTO_RESUME_SPEED_THRESHOLD_KMH >
                    TrackingConstants.AUTO_PAUSE_SPEED_THRESHOLD_KMH,
            "Auto-resume threshold (${TrackingConstants.AUTO_RESUME_SPEED_THRESHOLD_KMH}) " +
                    "must be greater than auto-pause threshold " +
                    "(${TrackingConstants.AUTO_PAUSE_SPEED_THRESHOLD_KMH}) to prevent cycling"
        )
    }

    @Test
    fun maxPlausibleSpeedIsReasonableForMotorcycles() {
        assertTrue(
            TrackingConstants.MAX_PLAUSIBLE_SPEED_KMH >= 100.0,
            "Max plausible speed must be at least 100 km/h"
        )
        assertTrue(
            TrackingConstants.MAX_PLAUSIBLE_SPEED_KMH <= 400.0,
            "Max plausible speed must be at most 400 km/h"
        )
    }

    @Test
    fun locationUpdateIntervalsArePositive() {
        assertTrue(TrackingConstants.LOCATION_UPDATE_INTERVAL_MS > 0)
        assertTrue(TrackingConstants.LOCATION_FASTEST_INTERVAL_MS > 0)
    }

    @Test
    fun fastestIntervalDoesNotExceedRegularInterval() {
        assertTrue(
            TrackingConstants.LOCATION_FASTEST_INTERVAL_MS <=
                    TrackingConstants.LOCATION_UPDATE_INTERVAL_MS,
            "Fastest interval must not exceed regular interval"
        )
    }

    @Test
    fun autoPauseDelayIsPositive() {
        assertTrue(
            TrackingConstants.AUTO_PAUSE_DELAY_SECONDS > 0,
            "Auto-pause delay must be positive"
        )
    }

    @Test
    fun gpsSignalLostTimeoutIsPositive() {
        assertTrue(
            TrackingConstants.GPS_SIGNAL_LOST_TIMEOUT_SECONDS > 0,
            "GPS signal lost timeout must be positive"
        )
    }

    @Test
    fun expectedConstantValuesMatchAgentsMdSpec() {
        // These verify the constants match the values specified in AGENTS.md
        assertEquals(25.0, TrackingConstants.MIN_GPS_ACCURACY_METERS, 0.001)
        assertEquals(1.5, TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH, 0.001)
        assertEquals(3.0, TrackingConstants.LOW_SPEED_DISTANCE_THRESHOLD_KMH, 0.001)
        assertEquals(250.0, TrackingConstants.MAX_PLAUSIBLE_SPEED_KMH, 0.001)
        assertEquals(15.0, TrackingConstants.MAX_ACCELERATION_MS2, 0.001)
        assertEquals(3.0, TrackingConstants.AUTO_PAUSE_SPEED_THRESHOLD_KMH, 0.001)
        assertEquals(8, TrackingConstants.AUTO_PAUSE_DELAY_SECONDS)
        assertEquals(5.0, TrackingConstants.AUTO_RESUME_SPEED_THRESHOLD_KMH, 0.001)
        assertEquals(1000L, TrackingConstants.LOCATION_UPDATE_INTERVAL_MS)
        assertEquals(1000L, TrackingConstants.LOCATION_FASTEST_INTERVAL_MS)
    }
}
