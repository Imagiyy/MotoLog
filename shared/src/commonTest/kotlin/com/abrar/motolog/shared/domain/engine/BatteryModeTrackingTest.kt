package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.GpsPoint
import com.abrar.motolog.shared.domain.model.PauseState
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatteryModeTrackingTest {

    private val originLat = 12.9716
    private val originLon = 77.5946
    private val latPerMeter = (1.0 / 6_371_000.0) * (180.0 / PI)

    @Test
    fun steadyRide_calculatesAccurateDistanceAtBothIntervals() {
        // 1 km ride at constant 20 m/s (72 km/h). Total time = 50 seconds.
        val totalDistanceMeters = 1000.0
        val speedMps = 20f

        // High accuracy: 1 second interval (51 points)
        val highAccuracyCalc = RideCalculator(startConfirmationDistanceMeters = 0.0)
        var t1 = 1_000_000L
        for (i in 0..50) {
            val dist = i * 20.0
            highAccuracyCalc.process(
                GpsPoint(
                    timestampEpochMs = t1,
                    latitude = originLat + dist * latPerMeter,
                    longitude = originLon,
                    speedMps = speedMps,
                    accuracyMeters = 3f
                )
            )
            t1 += 1000L
        }

        // Battery saver: 3 second interval (~17 points)
        val batterySaverCalc = RideCalculator(startConfirmationDistanceMeters = 0.0)
        var t2 = 1_000_000L
        for (i in 0..16) {
            val dist = (i * 3 * 20.0).coerceAtMost(totalDistanceMeters)
            batterySaverCalc.process(
                GpsPoint(
                    timestampEpochMs = t2,
                    latitude = originLat + dist * latPerMeter,
                    longitude = originLon,
                    speedMps = speedMps,
                    accuracyMeters = 3f
                )
            )
            t2 += 3000L
        }
        // Final point at 50s
        batterySaverCalc.process(
            GpsPoint(
                timestampEpochMs = 1_000_000L + 50_000L,
                latitude = originLat + totalDistanceMeters * latPerMeter,
                longitude = originLon,
                speedMps = speedMps,
                accuracyMeters = 3f
            )
        )

        // Both engines should report 1000m within 1% tolerance
        assertEquals(1000.0, highAccuracyCalc.stats.totalDistanceMeters, 5.0)
        assertEquals(1000.0, batterySaverCalc.stats.totalDistanceMeters, 5.0)
    }

    @Test
    fun speedSpikeRejection_functionsIdenticallyAtBothIntervals() {
        // High accuracy: 1s delta, impossible jump of 100m in 1s = 100 m/s = 360 km/h (>250 km/h max)
        val calc1s = RideCalculator(startConfirmationDistanceMeters = 0.0)
        calc1s.process(GpsPoint(1_000_000L, originLat, originLon, speedMps = 10f, accuracyMeters = 3f))
        val res1s = calc1s.process(
            GpsPoint(1_001_000L, originLat + 100.0 * latPerMeter, originLon, speedMps = 10f, accuracyMeters = 3f)
        )
        assertTrue(res1s is PointFilterResult.Rejected)
        assertEquals(RejectionReason.SPIKE_SPEED, (res1s as PointFilterResult.Rejected).reason)

        // Battery saver: 3s delta, jump of 300m in 3s = 100 m/s = 360 km/h (>250 km/h max)
        val calc3s = RideCalculator(startConfirmationDistanceMeters = 0.0)
        calc3s.process(GpsPoint(1_000_000L, originLat, originLon, speedMps = 10f, accuracyMeters = 3f))
        val res3s = calc3s.process(
            GpsPoint(1_003_000L, originLat + 300.0 * latPerMeter, originLon, speedMps = 10f, accuracyMeters = 3f)
        )
        assertTrue(res3s is PointFilterResult.Rejected)
        assertEquals(RejectionReason.SPIKE_SPEED, (res3s as PointFilterResult.Rejected).reason)
    }

    @Test
    fun autoPause_triggersAtEightSecondsAtBothIntervals() {
        // High accuracy: 1s points at 0 km/h
        val sm1s = AutoPauseStateMachine(autoPauseEnabled = true)
        for (sec in 0..7) {
            val state = sm1s.onSpeedUpdate(0.0, timestampMs = sec * 1000L)
            assertEquals(PauseState.RECORDING, state)
        }
        // At 8s -> AUTO_PAUSED
        val stateAt8s = sm1s.onSpeedUpdate(0.0, timestampMs = 8000L)
        assertEquals(PauseState.AUTO_PAUSED, stateAt8s)

        // Battery saver: 3s points at 0 km/h
        val sm3s = AutoPauseStateMachine(autoPauseEnabled = true)
        // t = 0s -> RECORDING
        assertEquals(PauseState.RECORDING, sm3s.onSpeedUpdate(0.0, 0L))
        // t = 3s -> RECORDING
        assertEquals(PauseState.RECORDING, sm3s.onSpeedUpdate(0.0, 3000L))
        // t = 6s -> RECORDING
        assertEquals(PauseState.RECORDING, sm3s.onSpeedUpdate(0.0, 6000L))
        // t = 9s (exceeds 8s window) -> AUTO_PAUSED
        assertEquals(PauseState.AUTO_PAUSED, sm3s.onSpeedUpdate(0.0, 9000L))
    }
}
