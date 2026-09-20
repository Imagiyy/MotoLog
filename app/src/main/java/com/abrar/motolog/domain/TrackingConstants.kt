package com.abrar.motolog.domain

/**
 * All tunable tracking thresholds for MotoLog.
 *
 * These values control GPS point filtering, speed classification,
 * auto-pause behavior, and update intervals. They are centralized
 * here so they can be adjusted without touching any logic code.
 *
 * No Android imports — this is pure Kotlin and fully unit-testable.
 */
object TrackingConstants {

    // ============================================================
    // GPS Point Filtering
    // ============================================================

    /** Maximum acceptable GPS accuracy in meters.
     *  Points with accuracy worse than this are rejected. */
    const val MIN_GPS_ACCURACY_METERS: Double = 25.0

    /** GPS speed below this threshold (km/h) is treated as zero
     *  to filter stationary GPS noise/jitter. */
    const val STATIONARY_SPEED_THRESHOLD_KMH: Double = 1.5

    /** When speed is under this threshold (km/h), do not add distance
     *  for segments shorter than the point's accuracy radius. */
    const val LOW_SPEED_DISTANCE_THRESHOLD_KMH: Double = 3.0

    /** Maximum plausible motorcycle speed (km/h).
     *  Implied speeds above this between consecutive points are
     *  rejected as GPS spikes. */
    const val MAX_PLAUSIBLE_SPEED_KMH: Double = 250.0

    /** Maximum plausible acceleration (m/s²).
     *  Speed changes implying acceleration above this between
     *  consecutive points are rejected as GPS spikes. */
    const val MAX_ACCELERATION_MS2: Double = 15.0

    // ============================================================
    // Auto-Pause
    // ============================================================

    /** Speed threshold (km/h) below which auto-pause timer starts. */
    const val AUTO_PAUSE_SPEED_THRESHOLD_KMH: Double = 3.0

    /** Duration (seconds) speed must stay below the auto-pause
     *  threshold before pausing moving time. */
    const val AUTO_PAUSE_DELAY_SECONDS: Int = 8

    /** Speed threshold (km/h) above which auto-pause resumes
     *  tracking. This is higher than the pause threshold to create
     *  hysteresis and avoid rapid pause/resume cycling. */
    const val AUTO_RESUME_SPEED_THRESHOLD_KMH: Double = 5.0

    // ============================================================
    // Location Update Intervals
    // ============================================================

    /** Desired interval between location updates (milliseconds). */
    const val LOCATION_UPDATE_INTERVAL_MS: Long = 1_000L

    /** Fastest acceptable interval between location updates (milliseconds).
     *  The system will not deliver updates faster than this. */
    const val LOCATION_FASTEST_INTERVAL_MS: Long = 1_000L

    // ============================================================
    // GPS Signal Loss
    // ============================================================

    /** Duration (seconds) without a location update before
     *  declaring GPS signal lost. */
    const val GPS_SIGNAL_LOST_TIMEOUT_SECONDS: Int = 10

    /** Maximum interval (milliseconds) between consecutive points before
     *  treating the segment as a signal gap (no distance accumulated across it). */
    const val GAP_THRESHOLD_MS: Long = 10_000L

    // ============================================================
    // Database Batching & Checkpointing
    // ============================================================

    /** Interval (milliseconds) for batching location point writes
     *  to the Room database during an active ride. */
    const val DB_BATCH_WRITE_INTERVAL_MS: Long = 3_000L

    /** Interval (milliseconds) for checkpointing ride summary stats
     *  to the rides table to ensure crash-resilience. */
    const val CHECKPOINT_INTERVAL_MS: Long = 10_000L

    // ============================================================
    // Notification
    // ============================================================

    /** Minimum interval (milliseconds) between notification updates
     *  to prevent notification churn and system UI overhead. */
    const val NOTIFICATION_UPDATE_INTERVAL_MS: Long = 2_000L

    const val NOTIFICATION_CHANNEL_ID: String = "motolog_tracking_channel"
    const val NOTIFICATION_ID: Int = 1001
}
