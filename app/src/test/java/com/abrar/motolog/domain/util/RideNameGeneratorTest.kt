package com.abrar.motolog.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RideNameGeneratorTest {

    private val zoneId = ZoneId.of("UTC")

    private fun epochForUtcHour(hour: Int): Long {
        val dt = LocalDateTime.of(2026, 9, 20, hour, 15, 0)
        return dt.atZone(zoneId).toInstant().toEpochMilli()
    }

    @Test
    fun defaultNameForTimestamp_categorizesTimeOfDayAccurately() {
        // Morning: 05:00 - 11:59
        assertEquals("Morning ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(5), zoneId))
        assertEquals("Morning ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(8), zoneId))
        assertEquals("Morning ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(11), zoneId))

        // Afternoon: 12:00 - 16:59
        assertEquals("Afternoon ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(12), zoneId))
        assertEquals("Afternoon ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(14), zoneId))
        assertEquals("Afternoon ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(16), zoneId))

        // Evening: 17:00 - 20:59
        assertEquals("Evening ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(17), zoneId))
        assertEquals("Evening ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(19), zoneId))
        assertEquals("Evening ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(20), zoneId))

        // Night: 21:00 - 04:59
        assertEquals("Night ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(21), zoneId))
        assertEquals("Night ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(23), zoneId))
        assertEquals("Night ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(2), zoneId))
    }

    @Test
    fun sanitizeRideName_trimsAndAppliesFallback() {
        val fallback = "Morning ride"

        // Blank and whitespace fallback
        assertEquals(fallback, RideNameGenerator.sanitizeRideName("", fallback))
        assertEquals(fallback, RideNameGenerator.sanitizeRideName("   ", fallback))
        assertEquals(fallback, RideNameGenerator.sanitizeRideName(null, fallback))

        // Trimming whitespace
        assertEquals("Mountain Pass", RideNameGenerator.sanitizeRideName("  Mountain Pass  ", fallback))

        // Max length enforcement (50 chars)
        val veryLongName = "A".repeat(60)
        val sanitized = RideNameGenerator.sanitizeRideName(veryLongName, fallback, maxLength = 50)
        assertEquals(50, sanitized.length)
        assertEquals("A".repeat(50), sanitized)
    }
}
