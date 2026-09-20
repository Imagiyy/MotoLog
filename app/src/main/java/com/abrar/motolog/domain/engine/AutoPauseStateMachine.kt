package com.abrar.motolog.domain.engine

import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.model.PauseState

/**
 * Pure Kotlin auto-pause state machine.
 *
 * Implements:
 * 1. Auto-pause when speed stays below [pauseSpeedThresholdKmh] (3 km/h) for [pauseDelaySeconds] (8 s).
 * 2. Auto-resume when speed exceeds [resumeSpeedThresholdKmh] (5 km/h) (hysteresis prevents flapping).
 * 3. Coexistence with manual pause:
 *    - Manual pause always wins and overrides auto-pause.
 *    - Auto-resume never overrides manual pause.
 *    - Manually resuming while stationary does not instantly re-trigger auto-pause without waiting the full 8 s.
 * 4. Toggleable via [autoPauseEnabled]. When disabled, operates as pure manual pause / recording.
 */
class AutoPauseStateMachine(
    var autoPauseEnabled: Boolean = true,
    private val pauseSpeedThresholdKmh: Double = TrackingConstants.AUTO_PAUSE_SPEED_THRESHOLD_KMH,
    private val pauseDelaySeconds: Int = TrackingConstants.AUTO_PAUSE_DELAY_SECONDS,
    private val resumeSpeedThresholdKmh: Double = TrackingConstants.AUTO_RESUME_SPEED_THRESHOLD_KMH
) {
    var pauseState: PauseState = PauseState.RECORDING
        private set

    private var lowSpeedStartTimeMs: Long? = null

    /**
     * Process an incoming speed fix and update pause state accordingly.
     *
     * @param speedKmh Filtered speed in km/h.
     * @param timestampMs Timestamp of the speed measurement.
     * @return Current [PauseState] after evaluation.
     */
    fun onSpeedUpdate(speedKmh: Double, timestampMs: Long): PauseState {
        if (pauseState == PauseState.MANUALLY_PAUSED) {
            // Manual pause always wins; auto-resume NEVER overrides manual pause
            return PauseState.MANUALLY_PAUSED
        }

        if (!autoPauseEnabled) {
            if (pauseState == PauseState.AUTO_PAUSED) {
                pauseState = PauseState.RECORDING
            }
            lowSpeedStartTimeMs = null
            return pauseState
        }

        when (pauseState) {
            PauseState.RECORDING -> {
                if (speedKmh < pauseSpeedThresholdKmh) {
                    val start = lowSpeedStartTimeMs
                    if (start == null) {
                        lowSpeedStartTimeMs = timestampMs
                    } else if (timestampMs - start >= pauseDelaySeconds * 1000L) {
                        pauseState = PauseState.AUTO_PAUSED
                    }
                } else {
                    // Speed is at or above threshold, reset stationary countdown
                    lowSpeedStartTimeMs = null
                }
            }

            PauseState.AUTO_PAUSED -> {
                // Hysteresis: requires speed > resumeSpeedThresholdKmh (5.0 km/h) to resume.
                // Speeds between 3.0 and 5.0 km/h remain AUTO_PAUSED.
                if (speedKmh > resumeSpeedThresholdKmh) {
                    pauseState = PauseState.RECORDING
                    lowSpeedStartTimeMs = null
                }
            }

            PauseState.MANUALLY_PAUSED -> {
                // Handled above
            }
        }

        return pauseState
    }

    /**
     * Rider explicitly paused the ride.
     * Manual pause immediately overrides any current state.
     */
    fun manualPause(): PauseState {
        pauseState = PauseState.MANUALLY_PAUSED
        lowSpeedStartTimeMs = null
        return pauseState
    }

    /**
     * Rider explicitly resumed the ride.
     *
     * Resets stationary delay timer so that resuming while stationary
     * will NOT immediately re-trigger auto-pause without waiting the full 8 seconds.
     */
    fun manualResume(currentTimestampMs: Long? = null): PauseState {
        pauseState = PauseState.RECORDING
        // Setting lowSpeedStartTimeMs to the resume timestamp (or null) guarantees
        // that the full pauseDelaySeconds must elapse from this point forward.
        lowSpeedStartTimeMs = currentTimestampMs
        return pauseState
    }

    /**
     * Reset state machine to initial state.
     */
    fun reset() {
        pauseState = PauseState.RECORDING
        lowSpeedStartTimeMs = null
    }
}
