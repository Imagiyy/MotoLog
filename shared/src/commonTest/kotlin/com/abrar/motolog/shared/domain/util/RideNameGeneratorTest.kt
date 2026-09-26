package com.abrar.motolog.shared.domain.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class RideNameGeneratorTest {

    private val timeZone = TimeZone.UTC

    private fun epochForUtcHour(hour: Int): Long {
        val dt = LocalDateTime(2026, 9, 20, hour, 15, 0)
        return dt.toInstant(timeZone).toEpochMilliseconds()
    }

    @Test
    fun defaultNameForTimestamp_categorizesTimeOfDayAccurately() {
        // Morning: 05:00 - 11:59
        assertEquals("Morning ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(5), timeZone))
        assertEquals("Morning ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(8), timeZone))
        assertEquals("Morning ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(11), timeZone))

        // Afternoon: 12:00 - 16:59
        assertEquals("Afternoon ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(12), timeZone))
        assertEquals("Afternoon ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(14), timeZone))
        assertEquals("Afternoon ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(16), timeZone))

        // Evening: 17:00 - 20:59
        assertEquals("Evening ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(17), timeZone))
        assertEquals("Evening ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(19), timeZone))
        assertEquals("Evening ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(20), timeZone))

        // Night: 21:00 - 04:59
        assertEquals("Night ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(21), timeZone))
        assertEquals("Night ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(23), timeZone))
        assertEquals("Night ride", RideNameGenerator.defaultNameForTimestamp(epochForUtcHour(2), timeZone))
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
