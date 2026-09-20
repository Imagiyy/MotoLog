package com.abrar.motolog.domain.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Generates human-friendly default ride names based strictly on the local time of day
 * when the ride started.
 *
 * Does not require network access, reverse geocoding, or location permissions.
 */
object RideNameGenerator {

    /**
     * Returns a default name (e.g., "Morning ride", "Afternoon ride") based on the start timestamp.
     *
     * @param startTimestampEpochMs Epoch milliseconds when the ride began.
     * @param zoneId Time zone to use for local hour determination (defaults to system time zone).
     */
    fun defaultNameForTimestamp(
        startTimestampEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTimestampEpochMs), zoneId)
        val hour = zonedDateTime.hour

        return when (hour) {
            in 5..11 -> "Morning ride"
            in 12..16 -> "Afternoon ride"
            in 17..20 -> "Evening ride"
            else -> "Night ride"
        }
    }

    /**
     * Sanitizes and validates a candidate ride name.
     * Trims leading and trailing whitespace. If empty or blank, falls back to the default name.
     * Truncates names exceeding [maxLength] characters.
     */
    fun sanitizeRideName(
        name: String?,
        fallbackDefaultName: String,
        maxLength: Int = 50
    ): String {
        val trimmed = name?.trim().orEmpty()
        val finalName = if (trimmed.isBlank()) fallbackDefaultName else trimmed
        return if (finalName.length > maxLength) finalName.take(maxLength) else finalName
    }
}
