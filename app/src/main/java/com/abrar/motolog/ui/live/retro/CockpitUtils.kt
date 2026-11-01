package com.abrar.motolog.ui.live.retro

import java.util.Locale

/**
 * Shared utility functions for cockpit dashboards.
 */
object CockpitUtils {
    /**
     * Formats duration milliseconds into MM:SS or HH:MM:SS.
     */
    fun formatDurationMs(ms: Long): String {
        val totalSec = ms / 1000
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    /**
     * Formats distance in meters into m/km or ft/mi depending on metric preference.
     */
    fun formatDistance(meters: Double, isMetric: Boolean): String {
        return if (isMetric) {
            if (meters < 1000.0) {
                "${meters.toInt()} m"
            } else {
                String.format(Locale.US, "%.1f km", meters / 1000.0)
            }
        } else {
            val feet = meters * 3.28084
            val miles = meters * 0.000621371
            if (feet < 1000.0) {
                "${feet.toInt()} ft"
            } else {
                String.format(Locale.US, "%.1f mi", miles)
            }
        }
    }

    /**
     * Formats an epoch timestamp into local 12-hour or 24-hour time (e.g., "10:45 AM").
     */
    fun formatEta(epochMs: Long): String {
        if (epochMs <= 0L) return "--:--"
        val date = java.util.Date(epochMs)
        val format = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(date)
    }
}
