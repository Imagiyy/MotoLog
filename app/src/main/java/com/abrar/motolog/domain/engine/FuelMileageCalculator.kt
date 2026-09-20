package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.domain.model.FuelIntervalResult
import com.abrar.motolog.domain.model.FuelMileageStats

/**
 * Fuel fill record input for mileage calculations.
 */
data class FuelFill(
    val id: Long = 0,
    val timestampEpochMs: Long,
    val odometerKm: Double,
    val litres: Double,
    val totalCost: Double,
    val isFullTank: Boolean
)

/**
 * Pure Kotlin calculator for motorcycle fuel mileage using the standard Full-Tank Method.
 * Free of Android framework dependencies.
 */
object FuelMileageCalculator {

    /**
     * Convenience method to calculate mileage directly from Room entity objects.
     */
    fun calculate(logs: List<FuelLogEntity>): FuelMileageStats {
        return computeMileage(
            logs.map {
                FuelFill(
                    id = it.id,
                    timestampEpochMs = it.timestampEpochMs,
                    odometerKm = it.odometerKm,
                    litres = it.litres,
                    totalCost = it.totalCost,
                    isFullTank = it.isFullTank
                )
            }
        )
    }

    /**
     * Calculates mileage across a sequence of fuel fills.
     *
     * In the standard full-tank method:
     * - An interval is bounded by two full-tank fills (Start Full -> End Full).
     * - Any partial fills between the two full-tanks are accumulated into the end fill's litres.
     * - The first full tank establishes the starting baseline odometer; its litres are not counted
     *   towards this interval (they were consumed in the prior unrecorded period).
     */
    fun computeMileage(entries: List<FuelFill>): FuelMileageStats {
        if (entries.isEmpty()) {
            return FuelMileageStats()
        }

        // Sort chronologically/by odometer
        val sorted = entries.sortedWith(compareBy({ it.odometerKm }, { it.timestampEpochMs }))

        val totalLitres = sorted.sumOf { it.litres }
        val totalCost = sorted.sumOf { it.totalCost }
        val totalDistanceKm = if (sorted.size >= 2) {
            (sorted.last().odometerKm - sorted.first().odometerKm).coerceAtLeast(0.0)
        } else 0.0

        var lastFullTankOdometer: Double? = null
        var accumulatedLitres = 0.0
        val intervals = mutableListOf<FuelIntervalResult>()

        var totalIntervalDistance = 0.0
        var totalIntervalLitres = 0.0

        for (fill in sorted) {
            if (lastFullTankOdometer == null) {
                if (fill.isFullTank) {
                    lastFullTankOdometer = fill.odometerKm
                    accumulatedLitres = 0.0
                }
            } else {
                accumulatedLitres += fill.litres

                if (fill.isFullTank) {
                    val deltaKm = fill.odometerKm - lastFullTankOdometer
                    if (deltaKm > 0.0 && accumulatedLitres > 0.0) {
                        val kmPerLitre = deltaKm / accumulatedLitres
                        intervals.add(
                            FuelIntervalResult(
                                intervalIndex = intervals.size + 1,
                                endLogId = fill.id,
                                startOdometerKm = lastFullTankOdometer,
                                endOdometerKm = fill.odometerKm,
                                distanceKm = deltaKm,
                                litresConsumed = accumulatedLitres,
                                kmPerLitre = kmPerLitre
                            )
                        )
                        totalIntervalDistance += deltaKm
                        totalIntervalLitres += accumulatedLitres
                    }
                    lastFullTankOdometer = fill.odometerKm
                    accumulatedLitres = 0.0
                }
            }
        }

        val averageKmPerLitre = if (totalIntervalLitres > 0.0) {
            totalIntervalDistance / totalIntervalLitres
        } else null

        val latestKmPerLitre = intervals.lastOrNull()?.kmPerLitre

        return FuelMileageStats(
            intervals = intervals,
            averageKmPerLitre = averageKmPerLitre,
            latestKmPerLitre = latestKmPerLitre,
            totalLitres = totalLitres,
            totalCost = totalCost,
            totalRecordedDistanceKm = totalDistanceKm
        )
    }
}
