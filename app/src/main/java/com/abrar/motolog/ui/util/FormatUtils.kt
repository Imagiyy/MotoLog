package com.abrar.motolog.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared formatting utilities for MotoLog UI.
 */
object FormatUtils {

    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", Locale.getDefault())
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    /**
     * Formats an epoch timestamp in milliseconds to a human-readable date and time.
     */
    fun formatDateTime(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (epochMs <= 0L) return "Unknown date"
        val instant = Instant.ofEpochMilli(epochMs)
        return DATE_TIME_FORMATTER.format(instant.atZone(zoneId))
    }

    /**
     * Formats an epoch timestamp in milliseconds to date only (e.g. "Sep 20, 2026").
     */
    fun formatDate(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (epochMs <= 0L) return "Unknown date"
        val instant = Instant.ofEpochMilli(epochMs)
        return DATE_FORMATTER.format(instant.atZone(zoneId))
    }

    /**
     * Formats distance in meters to kilometres string (e.g., "12.45 km").
     */
    fun formatDistanceKm(distanceMeters: Double): String {
        return String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)
    }

    /**
     * Formats speed in km/h to string (e.g., "45.2 km/h").
     */
    fun formatSpeedKmh(speedKmh: Double): String {
        val safeSpeed = if (speedKmh.isNaN() || speedKmh.isInfinite()) 0.0 else speedKmh
        return String.format(Locale.US, "%.1f km/h", safeSpeed)
    }

    /**
     * Formats speed in m/s to km/h string.
     */
    fun formatSpeedMsToKmh(speedMs: Double): String {
        val safeKmh = if (speedMs.isNaN() || speedMs.isInfinite()) 0.0 else speedMs * 3.6
        return formatSpeedKmh(safeKmh)
    }

    /**
     * Formats duration in milliseconds to "HH:MM:SS" or "MM:SS".
     */
    fun formatDuration(durationMs: Long): String {
        val totalSecs = (durationMs / 1000).coerceAtLeast(0L)
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60

        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    /**
     * Formats duration in milliseconds to a concise text description (e.g., "1h 24m" or "42m 10s").
     */
    fun formatDurationDescriptive(durationMs: Long): String {
        val totalSecs = (durationMs / 1000).coerceAtLeast(0L)
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60

        return when {
            hrs > 0 -> "${hrs}h ${mins}m"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }
}
