package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.time.DefaultClock

/**
 * Pure Kotlin state machine for triggering speed threshold alerts.
 *
 * Designed with hysteresis and cooldown:
 * - Fires when speed reaches or exceeds [thresholdKmh].
 * - Disarms after firing, requiring speed to drop below (threshold - [hysteresisKmh])
 *   before it can fire again.
 * - Enforces a minimum cooldown ([cooldownMs]) between alerts.
 * - Uses an injectable clock lambda ([clock]) for deterministic unit testing.
 */
class SpeedAlertEngine(
    private val hysteresisKmh: Double = TrackingConstants.SPEED_ALERT_HYSTERESIS_KMH,
    private val cooldownMs: Long = TrackingConstants.SPEED_ALERT_COOLDOWN_SECONDS * 1000L,
    private val clock: () -> Long = { DefaultClock.currentTimeMillis() }
) {
    private var isArmed: Boolean = true
    private var lastAlertTimestampMs: Long = 0L

    val armed: Boolean
        get() = isArmed

    val lastAlertTimeMs: Long
        get() = lastAlertTimestampMs

    /**
     * Resets the internal state of the alert engine (e.g. at the start of a ride).
     */
    fun reset() {
        isArmed = true
        lastAlertTimestampMs = 0L
    }

    /**
     * Evaluates current speed against the threshold.
     *
     * @param currentSpeedKmh Current smoothed GPS speed in km/h.
     * @param thresholdKmh Configured alert threshold in km/h.
     * @param isEnabled Whether the speed alert setting is currently enabled.
     * @param currentTimeMs Current timestamp in epoch ms (optional, defaults to [clock]).
     * @return True if a new alert should be presented to the rider right now.
     */
    fun evaluate(
        currentSpeedKmh: Double,
        thresholdKmh: Double,
        isEnabled: Boolean,
        currentTimeMs: Long = clock()
    ): Boolean {
        if (!isEnabled || thresholdKmh <= 0.0) {
            isArmed = true
            return false
        }

        val resetThreshold = thresholdKmh - hysteresisKmh
        if (currentSpeedKmh <= resetThreshold) {
            isArmed = true
        }

        if (currentSpeedKmh >= thresholdKmh) {
            val cooldownSatisfied = (currentTimeMs - lastAlertTimestampMs) >= cooldownMs
            if (isArmed && cooldownSatisfied) {
                isArmed = false
                lastAlertTimestampMs = currentTimeMs
                return true
            }
        }

        return false
    }
}
