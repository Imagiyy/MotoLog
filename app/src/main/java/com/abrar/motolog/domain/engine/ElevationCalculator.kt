package com.abrar.motolog.domain.engine

import com.abrar.motolog.domain.TrackingConstants
import kotlin.math.abs

/**
 * Calculates cumulative elevation gain and loss from a sequence of raw elevation samples.
 *
 * Pure Kotlin, no Android imports — fully unit testable on JVM.
 *
 * Algorithm:
 * 1. Smooth the raw elevation values with a simple moving average (configurable window)
 *    to eliminate barometer sensor noise and GPS altitude jitter.
 * 2. Use a dead-band accumulator: maintain a "reference elevation" and only commit
 *    gain/loss when the smoothed elevation changes by more than a minimum threshold.
 *    This prevents sensor noise from inflating cumulative elevation.
 */
object ElevationCalculator {

    /**
     * Result of elevation calculation.
     */
    data class ElevationResult(
        /** Total cumulative elevation gained in meters. */
        val gainMeters: Double,
        /** Total cumulative elevation lost in meters. */
        val lossMeters: Double,
        /** Smoothed elevation profile (same length as input after smoothing). */
        val smoothedProfile: List<Double>
    )

    /**
     * Calculate elevation gain and loss from raw elevation readings.
     *
     * @param rawElevations List of elevation values in meters (one per sample).
     *        Empty or null entries should be filtered out before calling this.
     * @param minimumChangeThreshold Minimum elevation change (meters) from the last
     *        committed reference point to count as real gain or loss.
     *        Use [TrackingConstants.ELEVATION_MIN_CHANGE_BAROMETER_METERS] for barometer,
     *        [TrackingConstants.ELEVATION_MIN_CHANGE_GPS_METERS] for GPS altitude.
     * @param smoothingWindow Number of samples for the moving average window.
     * @return [ElevationResult] with gain, loss, and smoothed profile.
     */
    fun calculate(
        rawElevations: List<Double>,
        minimumChangeThreshold: Double = TrackingConstants.ELEVATION_MIN_CHANGE_BAROMETER_METERS,
        smoothingWindow: Int = TrackingConstants.ELEVATION_SMOOTHING_WINDOW
    ): ElevationResult {
        if (rawElevations.size < 2) {
            return ElevationResult(
                gainMeters = 0.0,
                lossMeters = 0.0,
                smoothedProfile = rawElevations.toList()
            )
        }

        // Step 1: Smooth with simple moving average
        val smoothed = smooth(rawElevations, smoothingWindow)

        // Step 2: Dead-band accumulator for gain/loss
        var totalGain = 0.0
        var totalLoss = 0.0
        var referenceElevation = smoothed.first()

        for (i in 1 until smoothed.size) {
            val delta = smoothed[i] - referenceElevation

            if (abs(delta) >= minimumChangeThreshold) {
                if (delta > 0) {
                    totalGain += delta
                } else {
                    totalLoss += abs(delta)
                }
                referenceElevation = smoothed[i]
            }
        }

        return ElevationResult(
            gainMeters = totalGain,
            lossMeters = totalLoss,
            smoothedProfile = smoothed
        )
    }

    /**
     * Simple moving average smoother.
     *
     * For the first (window-1) elements, uses progressively smaller windows
     * centered at the start to avoid boundary artifacts.
     */
    internal fun smooth(values: List<Double>, window: Int): List<Double> {
        if (window <= 1 || values.size <= 1) return values.toList()

        val halfWindow = window / 2
        return List(values.size) { i ->
            val start = maxOf(0, i - halfWindow)
            val end = minOf(values.size - 1, i + halfWindow)
            var sum = 0.0
            for (j in start..end) {
                sum += values[j]
            }
            sum / (end - start + 1)
        }
    }
}
