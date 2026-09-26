package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.GpsPoint
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.test.FixturePoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoPauseTest {

    // ============================================================
    // Test 1: Stop-and-Go Fixture (Moving vs Stopped Time)
    // ============================================================

    @Test
    fun stopAndGoFixture_expectedMovingVsStoppedTimeWithinTolerance() {
        val points = FixturePoints.stopAndGo()
        val calculator = RideCalculator(
            autoPauseStateMachine = AutoPauseStateMachine(autoPauseEnabled = true)
        )
        val stats = RideCalculator.processAll(points, calculator)

        // Analytical ground truth: 3,000m distance, ~300s moving, ~300s stopped
        assertEquals(3_000.0, stats.totalDistanceMeters, 50.0)
        assertTrue(
            stats.movingTimeMs in 290_000L..310_000L,
            "Moving time must be approx 300s (was ${stats.movingTimeMs}ms)"
        )
        assertTrue(
            stats.stoppedTimeMs in 290_000L..310_000L,
            "Stopped time must be approx 300s (was ${stats.stoppedTimeMs}ms)"
        )
        assertEquals(36.0, stats.avgMovingSpeedKmh, 1.0)
    }

    // ============================================================
    // Test 2: Stationary Phone Transitions to Auto-Paused After 8s and Stays
    // ============================================================

    @Test
    fun stationaryPhone_goesToAutoPausedAfter8SecondsAndStaysThere() {
        val calculator = RideCalculator(
            startConfirmationDistanceMeters = 0.0,
            autoPauseStateMachine = AutoPauseStateMachine(autoPauseEnabled = true)
        )

        var t = 1_000L
        // Initial moving point to confirm tracking
        val pInit = calculator.process(
            GpsPoint(timestampEpochMs = t, latitude = 10.0, longitude = 10.0, speedMps = 10f, accuracyMeters = 5f)
        )
        assertTrue(pInit is PointFilterResult.Accepted)
        assertEquals(PauseState.RECORDING, (pInit as PointFilterResult.Accepted).pauseState)

        // Drop speed to 0 km/h at t = 2000ms
        t = 2_000L
        val pFirstStop = calculator.process(
            GpsPoint(timestampEpochMs = t, latitude = 10.0001, longitude = 10.0, speedMps = 0f, accuracyMeters = 5f)
        )
        assertEquals(PauseState.RECORDING, (pFirstStop as PointFilterResult.Accepted).pauseState)

        // Send updates for 7 seconds (t = 3000ms to 9000ms): elapsed since stop = 7s -> still RECORDING
        for (sec in 3..9) {
            t = sec * 1000L
            val p = calculator.process(
                GpsPoint(timestampEpochMs = t, latitude = 10.0001, longitude = 10.0, speedMps = 0f, accuracyMeters = 5f)
            )
            assertEquals(
                PauseState.RECORDING,
                (p as PointFilterResult.Accepted).pauseState,
                "At ${sec}s (${sec - 2}s stationary), must still be RECORDING"
            )
        }

        // At t = 10,000ms: 8 full seconds elapsed since low speed started -> transitions to AUTO_PAUSED!
        t = 10_000L
        val pAutoPaused = calculator.process(
            GpsPoint(timestampEpochMs = t, latitude = 10.0001, longitude = 10.0, speedMps = 0f, accuracyMeters = 5f)
        )
        assertEquals(
            PauseState.AUTO_PAUSED,
            (pAutoPaused as PointFilterResult.Accepted).pauseState,
            "At 8s stationary, must transition to AUTO_PAUSED"
        )

        val movingTimeAtPause = calculator.stats.movingTimeMs

        // Stay stationary for another 300 seconds (5 minutes)
        for (sec in 11..310) {
            t = sec * 1000L
            val p = calculator.process(
                GpsPoint(timestampEpochMs = t, latitude = 10.0001, longitude = 10.0, speedMps = 0f, accuracyMeters = 5f)
            )
            assertEquals(PauseState.AUTO_PAUSED, (p as PointFilterResult.Accepted).pauseState)
        }

        // Moving time must NOT accumulate during auto-pause
        assertEquals(movingTimeAtPause, calculator.stats.movingTimeMs)
        assertEquals(PauseState.AUTO_PAUSED, calculator.autoPauseStateMachine.pauseState)
    }

    // ============================================================
    // Test 3: Slow Crawl Between 3 and 5 km/h (Hysteresis Verification)
    // ============================================================

    @Test
    fun slowCrawlBetween3And5Kmh_noFlappingHysteresis() {
        val sm = AutoPauseStateMachine(autoPauseEnabled = true)
        var t = 1000L

        // Enter AUTO_PAUSED
        sm.onSpeedUpdate(0.0, t)
        t += 8000L
        sm.onSpeedUpdate(0.0, t)
        assertEquals(PauseState.AUTO_PAUSED, sm.pauseState)

        // Slow crawl: test speeds between 3.0 and 5.0 km/h
        val crawlSpeeds = listOf(3.1, 3.5, 4.0, 4.5, 4.9, 5.0)
        for (speed in crawlSpeeds) {
            t += 1000L
            val state = sm.onSpeedUpdate(speed, t)
            assertEquals(
                PauseState.AUTO_PAUSED,
                state,
                "Speed $speed km/h must remain AUTO_PAUSED due to hysteresis"
            )
        }

        // Exceed 5.0 km/h: must auto-resume to RECORDING
        t += 1000L
        val resumedState = sm.onSpeedUpdate(5.1, t)
        assertEquals(PauseState.RECORDING, resumedState)
    }

    // ============================================================
    // Test 4: Manual Pause Plus Auto-Pause Combinations
    // ============================================================

    @Test
    fun manualPausePlusAutoPauseCombinations() {
        val sm = AutoPauseStateMachine(autoPauseEnabled = true)
        var t = 1000L

        // 1. Manual pause while recording
        assertEquals(PauseState.RECORDING, sm.pauseState)
        assertEquals(PauseState.MANUALLY_PAUSED, sm.manualPause())

        // 2. High speed while manually paused never auto-resumes
        val stateAtSpeed = sm.onSpeedUpdate(60.0, t + 1000L)
        assertEquals(
            PauseState.MANUALLY_PAUSED,
            stateAtSpeed,
            "High speed must not override manual pause"
        )

        // 3. Manual pause while auto-paused
        sm.manualResume(t)
        sm.onSpeedUpdate(0.0, t)
        t += 8000L
        sm.onSpeedUpdate(0.0, t)
        assertEquals(PauseState.AUTO_PAUSED, sm.pauseState)

        // Manual pause wins over auto-pause
        assertEquals(PauseState.MANUALLY_PAUSED, sm.manualPause())

        // 4. Manually resuming while stationary does not instantly re-trigger auto-pause
        t += 1000L
        val resumeTimestamp = t
        sm.manualResume(resumeTimestamp)
        assertEquals(PauseState.RECORDING, sm.pauseState)

        // Stationary points for 7 seconds after resume: must remain RECORDING
        for (i in 1..7) {
            t += 1000L
            val state = sm.onSpeedUpdate(0.0, t)
            assertEquals(
                PauseState.RECORDING,
                state,
                "At ${i}s after resume, must still be RECORDING"
            )
        }

        // 8th second after resume: triggers auto-pause
        t += 1000L
        val autoPausedAgain = sm.onSpeedUpdate(0.0, t)
        assertEquals(PauseState.AUTO_PAUSED, autoPausedAgain)
    }

    // ============================================================
    // Test 5: Signal Lost After Timeout and Recovery with No Phantom Distance
    // ============================================================

    @Test
    fun signalLostAfterTimeout_recoversWithoutPhantomDistance() {
        val calculator = RideCalculator(
            startConfirmationDistanceMeters = 0.0,
            gapThresholdMs = 10_000L
        )

        // Point 1: anchor point at t = 1000ms
        val p1 = calculator.process(
            GpsPoint(timestampEpochMs = 1000L, latitude = 52.5200, longitude = 13.4050, speedMps = 15f, accuracyMeters = 5f)
        )
        assertTrue(p1 is PointFilterResult.Accepted)

        // Blackout gap of 45 seconds, phone emerges 800 meters away at t = 46,000ms
        val p2 = calculator.process(
            GpsPoint(timestampEpochMs = 46_000L, latitude = 52.5270, longitude = 13.4050, speedMps = 15f, accuracyMeters = 5f)
        )
        assertTrue(p2 is PointFilterResult.Accepted, "Point after gap must be accepted as new anchor")
        val acceptedP2 = p2 as PointFilterResult.Accepted
        assertTrue(acceptedP2.isGap, "Must be marked as gap")
        assertEquals(0.0, acceptedP2.distanceIncrementMeters, 0.001, "Distance across gap must be strictly 0.0")
        assertEquals(1, calculator.stats.gapCount)

        // Point 3: 1 second later at 15 m/s (~15m distance)
        val p3 = calculator.process(
            GpsPoint(timestampEpochMs = 47_000L, latitude = 52.527135, longitude = 13.4050, speedMps = 15f, accuracyMeters = 5f)
        )
        assertTrue(p3 is PointFilterResult.Accepted)
        val acceptedP3 = p3 as PointFilterResult.Accepted
        assertFalse(acceptedP3.isGap)
        // Must accumulate only the ~15m from point 2 to point 3, NOT the 800m across the tunnel!
        assertEquals(15.0, acceptedP3.distanceIncrementMeters, 2.0)
        assertEquals(15.0, calculator.stats.totalDistanceMeters, 2.0)
    }

    // ============================================================
    // Test 6: Stage 2 Tunnel Gap Fixture Still Passes
    // ============================================================

    @Test
    fun tunnelGapFixture_fromStage2StillPasses() {
        val points = FixturePoints.tunnelGap()
        val stats = RideCalculator.processAll(points)

        assertEquals(1, stats.gapCount)
        assertEquals(15_000.0, stats.totalDistanceMeters, 100.0)
        assertEquals(644_000L, stats.elapsedTimeMs)
        assertTrue(
            stats.movingTimeMs in 595_000L..605_000L,
            "Moving time should be approx 600s, was ${stats.movingTimeMs}ms"
        )
    }

    // ============================================================
    // Test 7: Auto-Pause Toggle Off Matches Stage 2 Results Identically
    // ============================================================

    @Test
    fun autoPauseToggleOff_matchesStage2Results() {
        val points = FixturePoints.stopAndGo()
        val calculatorWithAutoPauseOff = RideCalculator(
            autoPauseStateMachine = AutoPauseStateMachine(autoPauseEnabled = false)
        )
        val statsWithOff = RideCalculator.processAll(points, calculatorWithAutoPauseOff)
        val statsStage2Default = RideCalculator.processAll(points)

        assertEquals(statsStage2Default.totalDistanceMeters, statsWithOff.totalDistanceMeters, 0.001)
        assertEquals(statsStage2Default.movingTimeMs, statsWithOff.movingTimeMs)
        assertEquals(statsStage2Default.stoppedTimeMs, statsWithOff.stoppedTimeMs)
        assertEquals(statsStage2Default.avgMovingSpeedKmh, statsWithOff.avgMovingSpeedKmh, 0.001)
        assertEquals(statsStage2Default.avgOverallSpeedKmh, statsWithOff.avgOverallSpeedKmh, 0.001)
    }

    // ============================================================
    // Test 8: Recovery Compatibility Reflects Auto-Pause and Gaps
    // ============================================================

    @Test
    fun recoveryCompatibility_reflectsAutoPauseAndGaps() {
        val points = mutableListOf<GpsPoint>()
        var t = 1000L
        var lat = 52.5200

        // 1. First 20s: Riding at 15 m/s (54 km/h)
        for (i in 0..19) {
            points.add(
                GpsPoint(timestampEpochMs = t, latitude = lat, longitude = 13.4050, speedMps = 15f, accuracyMeters = 5f)
            )
            t += 1000L
            lat += 0.000135
        }

        // 2. Next 15s: Stationary, marked isPaused = true in database
        for (i in 0..14) {
            points.add(
                GpsPoint(timestampEpochMs = t, latitude = lat, longitude = 13.4050, speedMps = 0f, accuracyMeters = 5f, isPaused = true)
            )
            t += 1000L
        }

        // 3. Next 15s: Gap of 30 seconds underground (tunnel)
        t += 30_000L
        lat += 0.004
        points.add(
            GpsPoint(timestampEpochMs = t, latitude = lat, longitude = 13.4050, speedMps = 15f, accuracyMeters = 5f, isGap = true)
        )

        // 4. Next 20s: Riding again at 15 m/s
        for (i in 0..19) {
            t += 1000L
            lat += 0.000135
            points.add(
                GpsPoint(timestampEpochMs = t, latitude = lat, longitude = 13.4050, speedMps = 15f, accuracyMeters = 5f)
            )
        }

        // Replay recovery
        val calculator = RideCalculator(startConfirmationDistanceMeters = 0.0)
        val recoveredStats = RideCalculator.processAll(points, calculator)

        // Verify: Paused points and gap points did NOT accumulate distance or moving time
        assertTrue(
            recoveredStats.totalDistanceMeters in 550.0..650.0,
            "Distance must reflect only moving segments (~600m), was ${recoveredStats.totalDistanceMeters}"
        )
        // Moving time must be ~39s (20s initial + 19s second segment)
        assertTrue(
            recoveredStats.movingTimeMs in 38_000L..41_000L,
            "Moving time must be approx 39s (was ${recoveredStats.movingTimeMs}ms)"
        )
        // Gap count must be 1
        assertEquals(1, recoveredStats.gapCount)
    }
}
