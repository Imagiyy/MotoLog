package com.abrar.motolog.domain.model

/**
 * Represents the pause state of an active ride tracking session.
 */
enum class PauseState {
    /** Ride is actively recording; moving time and distance accumulate normally. */
    RECORDING,

    /** Ride is automatically paused due to speed < 3.0 km/h for 8 continuous seconds. */
    AUTO_PAUSED,

    /** Ride was manually paused by the rider. Manual pause always overrides auto-pause. */
    MANUALLY_PAUSED;

    val isPaused: Boolean get() = this != RECORDING
}
