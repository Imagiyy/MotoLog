package com.abrar.motolog.shared.domain.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElevationCalculatorTest {

    @Test
    fun emptyAndSinglePointInputsProduceZeroGainAndLoss() {
        val emptyResult = ElevationCalculator.calculate(emptyList())
        assertEquals(0.0, emptyResult.gainMeters, 0.001)
        assertEquals(0.0, emptyResult.lossMeters, 0.001)
        assertTrue(emptyResult.smoothedProfile.isEmpty())

        val singleResult = ElevationCalculator.calculate(listOf(100.0))
        assertEquals(0.0, singleResult.gainMeters, 0.001)
        assertEquals(0.0, singleResult.lossMeters, 0.001)
        assertEquals(listOf(100.0), singleResult.smoothedProfile)
    }

    @Test
    fun flatElevationWithNoiseBelowThresholdProducesZeroGainAndLoss() {
        // Base elevation 50m with +/- 1.0m noise (below 3.0m threshold)
        val elevations = listOf(50.0, 50.8, 49.5, 50.3, 50.0, 49.8, 50.2, 50.1, 49.9)
        val result = ElevationCalculator.calculate(
            rawElevations = elevations,
            minimumChangeThreshold = 3.0,
            smoothingWindow = 3
        )

        assertEquals(0.0, result.gainMeters, 0.001)
        assertEquals(0.0, result.lossMeters, 0.001)
    }

    @Test
    fun steadyClimbProducesAccurateCumulativeGainAndZeroLoss() {
        // Ascending 100m to 200m in steps of 10m
        val elevations = (100..200 step 10).map { it.toDouble() }
        val result = ElevationCalculator.calculate(
            rawElevations = elevations,
            minimumChangeThreshold = 3.0,
            smoothingWindow = 1
        )

        assertEquals(100.0, result.gainMeters, 0.001)
        assertEquals(0.0, result.lossMeters, 0.001)
    }

    @Test
    fun steadyDescentProducesAccurateCumulativeLossAndZeroGain() {
        // Descending 200m to 100m in steps of 10m
        val elevations = (200 downTo 100 step 10).map { it.toDouble() }
        val result = ElevationCalculator.calculate(
            rawElevations = elevations,
            minimumChangeThreshold = 3.0,
            smoothingWindow = 1
        )

        assertEquals(0.0, result.gainMeters, 0.001)
        assertEquals(100.0, result.lossMeters, 0.001)
    }

    @Test
    fun climbAndDescentProducesBothGainAndLoss() {
        // 50 -> 100 -> 50
        val up = (50..100 step 5).map { it.toDouble() }
        val down = (95 downTo 50 step 5).map { it.toDouble() }
        val elevations = up + down

        val result = ElevationCalculator.calculate(
            rawElevations = elevations,
            minimumChangeThreshold = 3.0,
            smoothingWindow = 1
        )

        assertEquals(50.0, result.gainMeters, 0.001)
        assertEquals(50.0, result.lossMeters, 0.001)
    }

    @Test
    fun smoothingMovingAverageReducesNoiseSpikes() {
        val noisy = listOf(10.0, 10.0, 50.0, 10.0, 10.0) // isolated spike
        val smoothed = ElevationCalculator.smooth(noisy, window = 3)

        assertEquals(noisy.size, smoothed.size)
        // Middle spike 50.0 smoothed with neighbors (10 + 50 + 10) / 3 = 23.33
        assertEquals(23.333, smoothed[2], 0.01)
    }
}
