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

    fun calculate(
        entries: List<FuelFill>,
        currentOdometerKm: Double? = null,
        startOdometerKm: Double? = null
    ): FuelMileageStats = computeMileage(entries, currentOdometerKm, startOdometerKm)

    fun computeMileage(
        entries: List<FuelFill>,
        currentOdometerKm: Double? = null,
        startOdometerKm: Double? = null
    ): FuelMileageStats {
        if (entries.isEmpty()) {
            return FuelMileageStats()
        }

        // Sort chronologically/by odometer
        val sorted = entries.sortedWith(compareBy({ it.odometerKm }, { it.timestampEpochMs }))

        val totalLitres = sorted.sumOf { it.litres }
        val totalCost = sorted.sumOf { it.totalCost }

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

        val totalDistanceKm: Double
        val averageKmPerLitre: Double?
        val averageKmPerCost: Double?
        val averageCostPerKm: Double?

        if (currentOdometerKm != null) {
            val baseOdo = startOdometerKm ?: 0.0
            val effectiveDistance = (maxOf(currentOdometerKm, sorted.lastOrNull()?.odometerKm ?: 0.0) - baseOdo).coerceAtLeast(0.0)
            totalDistanceKm = effectiveDistance
            // Round totalLitres to 1 decimal place to match displayed fuel volume (e.g. 2.77 -> 2.8)
            val displayLitres = kotlin.math.round(totalLitres * 10.0) / 10.0
            averageKmPerLitre = if (displayLitres > 0.0 && effectiveDistance > 0.0) {
                effectiveDistance / displayLitres
            } else if (totalLitres > 0.0 && effectiveDistance > 0.0) {
                effectiveDistance / totalLitres
            } else null
            averageKmPerCost = if (totalCost > 0.0 && effectiveDistance > 0.0) {
                effectiveDistance / totalCost
            } else null
            averageCostPerKm = if (effectiveDistance > 0.0 && totalCost > 0.0) {
                totalCost / effectiveDistance
            } else null
        } else {
            totalDistanceKm = if (sorted.size >= 2) {
                (sorted.last().odometerKm - sorted.first().odometerKm).coerceAtLeast(0.0)
            } else 0.0
            val displayIntervalLitres = kotlin.math.round(totalIntervalLitres * 10.0) / 10.0
            averageKmPerLitre = if (displayIntervalLitres > 0.0) {
                totalIntervalDistance / displayIntervalLitres
            } else if (totalIntervalLitres > 0.0) {
                totalIntervalDistance / totalIntervalLitres
            } else null
            averageKmPerCost = if (totalIntervalCost > 0.0) {
                totalIntervalDistance / totalIntervalCost
            } else null
            averageCostPerKm = if (totalIntervalDistance > 0.0 && totalIntervalCost > 0.0) {
                totalIntervalCost / totalIntervalDistance
            } else null
        }

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
