package com.abrar.motolog.domain.model

/**
 * Result of computing mileage between two consecutive full-tank fuel fills.
 */
data class FuelIntervalResult(
    val intervalIndex: Int,
    val endLogId: Long = 0L,
    val startOdometerKm: Double,
    val endOdometerKm: Double,
    val distanceKm: Double,
    val litresConsumed: Double,
    val kmPerLitre: Double
) {
    val mileageKml: Double get() = kmPerLitre
}

/**
 * Aggregated fuel mileage statistics for a motorcycle.
 */
data class FuelMileageStats(
    val intervals: List<FuelIntervalResult> = emptyList(),
    val averageKmPerLitre: Double? = null,
    val latestKmPerLitre: Double? = null,
    val totalLitres: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalRecordedDistanceKm: Double = 0.0
) {
    val measuredIntervalsCount: Int get() = intervals.size
    val averageMileageKml: Double? get() = averageKmPerLitre
    val latestMileageKml: Double? get() = latestKmPerLitre
    val intervalResults: List<FuelIntervalResult> get() = intervals
}
