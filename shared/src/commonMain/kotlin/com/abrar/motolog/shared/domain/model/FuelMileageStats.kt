package com.abrar.motolog.shared.domain.model

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
 * Result of computing mileage and cost efficiency between consecutive fuel fills.
 */
data class FuelIntervalResult(
    val intervalIndex: Int,
    val endLogId: Long = 0L,
    val startOdometerKm: Double,
    val endOdometerKm: Double,
    val distanceKm: Double,
    val litresConsumed: Double,
    val kmPerLitre: Double,
    val cost: Double = 0.0,
    val kmPerCost: Double? = null,
    val costPerKm: Double? = null
) {
    val mileageKml: Double get() = kmPerLitre
}

/**
 * Aggregated fuel mileage and cost efficiency statistics for a motorcycle.
 */
data class FuelMileageStats(
    val intervals: List<FuelIntervalResult> = emptyList(),
    val averageKmPerLitre: Double? = null,
    val latestKmPerLitre: Double? = null,
    val averageKmPerCost: Double? = null,
    val latestKmPerCost: Double? = null,
    val averageCostPerKm: Double? = null,
    val totalLitres: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalRecordedDistanceKm: Double = 0.0
) {
    val measuredIntervalsCount: Int get() = intervals.size
    val averageMileageKml: Double? get() = averageKmPerLitre
    val latestMileageKml: Double? get() = latestKmPerLitre
    val intervalResults: List<FuelIntervalResult> get() = intervals
}
