package com.abrar.motolog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TrackingConstants.
 * Validates that constants are correctly defined and
 * have sensible relationships.
 */
class TrackingConstantsTest {

    @Test
    fun `GPS accuracy threshold is positive`() {
        assertTrue(
            "GPS accuracy threshold must be positive",
            TrackingConstants.MIN_GPS_ACCURACY_METERS > 0
        )
    }

    @Test
    fun `stationary speed threshold is less than low speed distance threshold`() {
        assertTrue(
            "Stationary threshold (${TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH}) " +
                    "must be less than low speed distance threshold " +
                    "(${TrackingConstants.LOW_SPEED_DISTANCE_THRESHOLD_KMH})",
            TrackingConstants.STATIONARY_SPEED_THRESHOLD_KMH <
                    TrackingConstants.LOW_SPEED_DISTANCE_THRESHOLD_KMH
        )
    }

    @Test
    fun `auto-resume threshold is greater than auto-pause threshold`() {
        assertTrue(
            "Auto-resume threshold (${TrackingConstants.AUTO_RESUME_SPEED_THRESHOLD_KMH}) " +
                    "must be greater than auto-pause threshold " +
                    "(${TrackingConstants.AUTO_PAUSE_SPEED_THRESHOLD_KMH}) to prevent cycling",
            TrackingConstants.AUTO_RESUME_SPEED_THRESHOLD_KMH >
                    TrackingConstants.AUTO_PAUSE_SPEED_THRESHOLD_KMH
        )
    }

    @Test
    fun `max plausible speed is reasonable for motorcycles`() {
        assertTrue(
            "Max plausible speed must be at least 100 km/h",
            TrackingConstants.MAX_PLAUSIBLE_SPEED_KMH >= 100.0
        )
        assertTrue(
            "Max plausible speed must be at most 400 km/h",
            TrackingConstants.MAX_PLAUSIBLE_SPEED_KMH <= 400.0
        )
    }

    @Test
    fun `location update intervals are positive`() {
        assertTrue(TrackingConstants.LOCATION_UPDATE_INTERVAL_MS > 0)
        assertTrue(TrackingConstants.LOCATION_FASTEST_INTERVAL_MS > 0)
    }

    @Test
    fun `fastest interval does not exceed regular interval`() {
        assertTrue(
            "Fastest interval must not exceed regular interval",
            TrackingConstants.LOCATION_FASTEST_INTERVAL_MS <=
                    TrackingConstants.LOCATION_UPDATE_INTERVAL_MS
        )
    }

    @Test
    fun `auto-pause delay is positive`() {
        assertTrue(
            "Auto-pause delay must be positive",
            TrackingConstants.AUTO_PAUSE_DELAY_SECONDS > 0
        )
    }

    @Test
    fun `GPS signal lost timeout is positive`() {
        assertTrue(
            "GPS signal lost timeout must be positive",
            TrackingConstants.GPS_SIGNAL_LOST_TIMEOUT_SECONDS > 0
        )
    }

    @Test
    fun `expected constant values match AGENTS md spec`() {
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
