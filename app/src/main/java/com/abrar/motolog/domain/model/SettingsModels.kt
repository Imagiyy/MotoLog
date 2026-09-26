package com.abrar.motolog.domain.model

/**
 * Fuel economy display unit options.
 */
enum class FuelUnit(val displayName: String, val unitLabel: String) {
    KM_PER_LITER("km/l", "km/l"),
    L_PER_100KM("l/100km", "l/100km"),
    MPG_US("mpg (US)", "mpg"),
    MPG_UK("mpg (UK)", "mpg")
}

/**
 * GPS tracking mode balancing accuracy vs battery consumption.
 */
enum class TrackingMode(val displayName: String, val description: String) {
    HIGH_ACCURACY(
        displayName = "High Accuracy",
        description = "1s updates, maximum GPS precision. Ideal for twisty roads and short rides."
    ),
    BATTERY_SAVER(
        displayName = "Battery Saver",
        description = "3s updates, ~30-40% reduced battery drain. Recommended for all-day touring."
    )
}

/**
 * Speed alert notification style.
 */
enum class SpeedAlertStyle(val displayName: String) {
    VISUAL_ONLY("Visual Highlight Only"),
    VISUAL_AND_HAPTIC("Visual & Vibration"),
    ALL("Visual, Vibration & Audio Chime")
}

/**
 * Default map background theme option.
 */
enum class MapThemePreference(val displayName: String, val description: String) {
    DARK("Dark", "Dark background map style, optimized for night rides and AMOLED displays"),
    LIGHT("White / Light", "Clean white background map style with high daytime contrast")
}

