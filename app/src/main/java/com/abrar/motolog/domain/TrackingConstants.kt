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

    /** Distance (meters) of movement required before activating live ride
     *  metrics, suppressing mounting/driveway drift.
     *  Once reached, this initial 100m is credited into total distance. */
    const val START_CONFIRMATION_DISTANCE_METERS: Double = 100.0

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

    // ============================================================
    // Maintenance & Garage
    // ============================================================

    /** Remaining distance (km) below which a maintenance item is marked DUE_SOON */
    const val DUE_SOON_KM_THRESHOLD: Double = 100.0

    /** Remaining days below which a maintenance item is marked DUE_SOON */
    const val DUE_SOON_DAYS_THRESHOLD: Int = 7

    /** Maintenance reminder notification channel */
    const val MAINTENANCE_NOTIFICATION_CHANNEL_ID: String = "motolog_maintenance_channel"
    const val MAINTENANCE_NOTIFICATION_ID_BASE: Int = 2000

    // ============================================================
    // Elevation Tracking
    // ============================================================

    /** Minimum elevation change from barometer (meters) to count as real gain/loss. */
    const val ELEVATION_MIN_CHANGE_BAROMETER_METERS: Double = 3.0

    /** Minimum elevation change from GPS altitude (meters) to count as real gain/loss. */
    const val ELEVATION_MIN_CHANGE_GPS_METERS: Double = 10.0

    /** Number of consecutive readings to smooth before calculating gain/loss. */
    const val ELEVATION_SMOOTHING_WINDOW: Int = 5

    /** Barometer sample interval in milliseconds (roughly 1 Hz to match GPS). */
    const val BAROMETER_SAMPLE_INTERVAL_MS: Long = 1_000L

    /** Hardware FIFO latency allowance for low-power pressure sampling. */
    const val BAROMETER_MAX_REPORT_LATENCY_MS: Long = 5_000L

    // ============================================================
    // Route Downsampling
    // ============================================================

    /** Tolerance in meters for Ramer-Douglas-Peucker simplification. */
    const val DOWNSAMPLE_TOLERANCE_METERS: Double = 5.0

    /** Maximum number of points after downsampling for map display. */
    const val DOWNSAMPLE_MAX_POINTS: Int = 2000

    // ============================================================
    // Battery Saver Tracking Mode
    // ============================================================

    /** Desired update interval in battery saver mode (milliseconds). */
    const val BATTERY_SAVER_UPDATE_INTERVAL_MS: Long = 3_000L

    /** Fastest acceptable interval in battery saver mode (milliseconds). */
    const val BATTERY_SAVER_FASTEST_INTERVAL_MS: Long = 3_000L

    /** Duration (seconds) without location before signal lost in battery saver mode. */
    const val BATTERY_SAVER_SIGNAL_LOST_TIMEOUT_SECONDS: Int = 15

    /** Maximum interval (milliseconds) between points before gap in battery saver mode. */
    const val BATTERY_SAVER_GAP_THRESHOLD_MS: Long = 15_000L

    // ============================================================
    // Speed Alert
    // ============================================================

    /** Default speed alert threshold in km/h. */
    const val SPEED_ALERT_DEFAULT_THRESHOLD_KMH: Double = 100.0

    /** Drop in speed (km/h) below threshold required to re-arm the speed alert. */
    const val SPEED_ALERT_HYSTERESIS_KMH: Double = 5.0

    /** Minimum duration (seconds) between speed alert triggers to prevent annoyance. */
    const val SPEED_ALERT_COOLDOWN_SECONDS: Long = 15L

    // ============================================================
    // Speed Color Scale
    // ============================================================

    /** If ride speed range is narrower than this (km/h), use fallback window. */
    const val SPEED_COLOR_MIN_RANGE_KMH: Double = 5.0

    /** Fallback half-window (km/h) around median when speed range is too narrow. */
    const val SPEED_COLOR_FALLBACK_WINDOW_KMH: Double = 10.0
}
