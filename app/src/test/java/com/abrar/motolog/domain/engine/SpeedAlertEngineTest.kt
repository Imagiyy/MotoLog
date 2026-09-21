package com.abrar.motolog.domain.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedAlertEngineTest {

    private var currentTime = 100_000L
    private val testClock = { currentTime }

    @Test
    fun whenDisabled_neverTriggers() {
        val engine = SpeedAlertEngine(clock = testClock)
        val alertFired = engine.evaluate(
            currentSpeedKmh = 120.0,
            thresholdKmh = 80.0,
            isEnabled = false,
            currentTimeMs = currentTime
        )
        assertFalse(alertFired)
        assertTrue(engine.armed)
    }

    @Test
    fun whenBelowThreshold_doesNotTrigger() {
        val engine = SpeedAlertEngine(clock = testClock)
        val alertFired = engine.evaluate(
            currentSpeedKmh = 75.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertFalse(alertFired)
        assertTrue(engine.armed)
    }

    @Test
    fun whenExceedingThreshold_triggersAlertAndDisarms() {
        val engine = SpeedAlertEngine(clock = testClock)
        val alertFired = engine.evaluate(
            currentSpeedKmh = 82.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertTrue(alertFired)
        assertFalse(engine.armed)
    }

    @Test
    fun hysteresis_doesNotRearmUntilDroppedBelowResetThreshold() {
        val engine = SpeedAlertEngine(clock = testClock)

        // 1. Cross threshold of 80 km/h -> triggers
        var alertFired = engine.evaluate(
            currentSpeedKmh = 82.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertTrue(alertFired)
        assertFalse(engine.armed)

        // 2. Drop slightly to 78 km/h (within 5 km/h hysteresis [75.0, 80.0]) -> still disarmed
        currentTime += 1000L
        alertFired = engine.evaluate(
            currentSpeedKmh = 78.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertFalse(alertFired)
        assertFalse(engine.armed)

        // 3. Drop down to 74 km/h (below 75 km/h reset threshold) -> rearms
        currentTime += 1000L
        alertFired = engine.evaluate(
            currentSpeedKmh = 74.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertFalse(alertFired)
        assertTrue(engine.armed)

        // 4. Rise to 78 km/h (below 80 km/h threshold) -> armed, does not trigger
        currentTime += 1000L
        alertFired = engine.evaluate(
            currentSpeedKmh = 78.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertFalse(alertFired)
        assertTrue(engine.armed)

        // 5. Rise above 80 km/h again to 81 km/h (after 15s cooldown) -> fires new alert
        currentTime += 15_000L
        alertFired = engine.evaluate(
            currentSpeedKmh = 81.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertTrue(alertFired)
        assertFalse(engine.armed)
    }

    @Test
    fun cooldown_preventsRepeatedAlertsWithinCooldownWindow() {
        val engine = SpeedAlertEngine(clock = testClock)

        // 1. Trigger alert at t=100,000 ms
        var alertFired = engine.evaluate(
            currentSpeedKmh = 85.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertTrue(alertFired)

        // 2. Drop below reset threshold to rearm at t=102,000 ms (+2s)
        currentTime += 2000L
        engine.evaluate(
            currentSpeedKmh = 70.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertTrue(engine.armed)

        // 3. Exceed threshold again at t=105,000 ms (+5s from initial alert)
        // Armed is true, but cooldown (15s) has NOT passed!
        currentTime += 3000L
        alertFired = engine.evaluate(
            currentSpeedKmh = 85.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertFalse(alertFired) // Blocked by cooldown

        // 4. Exceed threshold at t=116,000 ms (+16s from initial alert)
        // Cooldown satisfied -> triggers alert
        currentTime += 11_000L
        alertFired = engine.evaluate(
            currentSpeedKmh = 85.0,
            thresholdKmh = 80.0,
            isEnabled = true,
            currentTimeMs = currentTime
        )
        assertTrue(alertFired)
    }
}
