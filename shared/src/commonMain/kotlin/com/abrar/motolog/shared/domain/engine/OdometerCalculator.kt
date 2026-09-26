package com.abrar.motolog.shared.domain.engine

/**
 * Pure Kotlin calculator for motorcycle odometer computations and manual calibrations.
 * Free of Android framework dependencies.
 */
object OdometerCalculator {

    /**
     * Calculates the current displayed odometer reading.
     *
     * @param initialOdometerKm Initial odometer reading when bike was registered in MotoLog.
     * @param recordedDistanceMeters Cumulative distance in meters from all completed/recovered rides assigned to this bike.
     * @param odometerOffsetKm Manual calibration offset to reconcile GPS with the motorcycle's physical dash.
     */
    fun calculateCurrentOdometerKm(
        initialOdometerKm: Double,
        recordedDistanceMeters: Double,
        odometerOffsetKm: Double
    ): Double {
        val recordedKm = (recordedDistanceMeters / 1000.0).coerceAtLeast(0.0)
        val total = initialOdometerKm + recordedKm + odometerOffsetKm
        return total.coerceAtLeast(0.0)
    }

    /**
     * Computes the new [odometerOffsetKm] needed when the rider manually sets the odometer to [targetOdometerKm].
     *
     * Invariant: `initialOdometerKm + recordedKm + newOffset == targetOdometerKm`.
     */
    fun computeOffsetForTarget(
        initialOdometerKm: Double,
        recordedDistanceMeters: Double,
        targetOdometerKm: Double
    ): Double {
        val recordedKm = (recordedDistanceMeters / 1000.0).coerceAtLeast(0.0)
        return targetOdometerKm - (initialOdometerKm + recordedKm)
    }
}
