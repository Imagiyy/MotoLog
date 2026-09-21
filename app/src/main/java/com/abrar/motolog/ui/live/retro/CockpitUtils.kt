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
}
