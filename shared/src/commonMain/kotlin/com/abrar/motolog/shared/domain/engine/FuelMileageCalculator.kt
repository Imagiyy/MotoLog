package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.FuelFill
import com.abrar.motolog.shared.domain.model.FuelIntervalResult
import com.abrar.motolog.shared.domain.model.FuelMileageStats

/**
 * Pure Kotlin calculator for motorcycle fuel mileage and cost efficiency.
 * Free of Android framework dependencies.
 *
 * Calculates distance/litre and distance/cost (e.g. distance/rupees) across consecutive
 * fuel fills directly without requiring full tanks.
 */
object FuelMileageCalculator {

    fun calculate(entries: List<FuelFill>): FuelMileageStats = computeMileage(entries)

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

        if (sorted.size < 2) {
            return FuelMileageStats(
                totalLitres = totalLitres,
                totalCost = totalCost,
                totalRecordedDistanceKm = totalDistanceKm
            )
        }

        var lastOdometer: Double? = null
        var accumulatedLitres = 0.0
        var accumulatedCost = 0.0
        val intervals = mutableListOf<FuelIntervalResult>()

        for (fill in sorted) {
            if (lastOdometer == null) {
                lastOdometer = fill.odometerKm
            } else {
                val deltaKm = fill.odometerKm - lastOdometer
                accumulatedLitres += fill.litres
                accumulatedCost += fill.totalCost

                if (deltaKm > 0.0) {
                    val kmPerLitre = if (accumulatedLitres > 0.0) deltaKm / accumulatedLitres else 0.0
                    val kmPerCost = if (accumulatedCost > 0.0) deltaKm / accumulatedCost else null
                    val costPerKm = if (deltaKm > 0.0 && accumulatedCost > 0.0) accumulatedCost / deltaKm else null

                    intervals.add(
                        FuelIntervalResult(
                            intervalIndex = intervals.size + 1,
                            endLogId = fill.id,
                            startOdometerKm = lastOdometer,
                            endOdometerKm = fill.odometerKm,
                            distanceKm = deltaKm,
                            litresConsumed = accumulatedLitres,
                            kmPerLitre = kmPerLitre,
                            cost = accumulatedCost,
                            kmPerCost = kmPerCost,
                            costPerKm = costPerKm
                        )
                    )
                    lastOdometer = fill.odometerKm
                    accumulatedLitres = 0.0
                    accumulatedCost = 0.0
                }
            }
        }

        val totalIntervalDistance = intervals.sumOf { it.distanceKm }
        val totalIntervalLitres = intervals.sumOf { it.litresConsumed }
        val totalIntervalCost = intervals.sumOf { it.cost }

        val averageKmPerLitre = if (totalIntervalLitres > 0.0) {
            totalIntervalDistance / totalIntervalLitres
        } else null

        val averageKmPerCost = if (totalIntervalCost > 0.0) {
            totalIntervalDistance / totalIntervalCost
        } else null

        val averageCostPerKm = if (totalIntervalDistance > 0.0 && totalIntervalCost > 0.0) {
            totalIntervalCost / totalIntervalDistance
        } else null

        val latestKmPerLitre = intervals.lastOrNull()?.kmPerLitre
        val latestKmPerCost = intervals.lastOrNull()?.kmPerCost

        return FuelMileageStats(
            intervals = intervals,
            averageKmPerLitre = averageKmPerLitre,
            latestKmPerLitre = latestKmPerLitre,
            averageKmPerCost = averageKmPerCost,
            latestKmPerCost = latestKmPerCost,
            averageCostPerKm = averageCostPerKm,
            totalLitres = totalLitres,
            totalCost = totalCost,
            totalRecordedDistanceKm = totalDistanceKm
        )
    }
}
