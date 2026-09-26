package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.engine.FuelMileageCalculator
import com.abrar.motolog.shared.domain.engine.MaintenanceCalculator
import com.abrar.motolog.shared.domain.model.FuelFill
import com.abrar.motolog.shared.domain.model.FuelMileageStats
import com.abrar.motolog.shared.domain.model.MaintenanceEvaluation
import com.abrar.motolog.shared.domain.model.MaintenanceStatus
import com.abrar.motolog.shared.domain.model.MaintenanceTask

/**
 * Adapter extensions bridging Android Room entities to shared domain models.
 */
fun MaintenanceItemEntity.toDomain(): MaintenanceTask = MaintenanceTask(
    id = id,
    bikeId = bikeId,
    name = name,
    intervalKm = intervalKm,
    intervalDays = intervalDays,
    lastDoneOdometerKm = lastDoneOdometerKm,
    lastDoneDateEpochMs = lastDoneDateEpochMs,
    lastNotifiedDueEpochMs = lastNotifiedDueEpochMs
)

fun MaintenanceTask.toEntity(): MaintenanceItemEntity = MaintenanceItemEntity(
    id = id,
    bikeId = bikeId,
    name = name,
    intervalKm = intervalKm,
    intervalDays = intervalDays,
    lastDoneOdometerKm = lastDoneOdometerKm,
    lastDoneDateEpochMs = lastDoneDateEpochMs,
    lastNotifiedDueEpochMs = lastNotifiedDueEpochMs
)

fun MaintenanceCalculator.evaluate(
    item: MaintenanceItemEntity,
    currentOdometerKm: Double,
    currentEpochMs: Long,
    dueSoonKmThreshold: Double = TrackingConstants.DUE_SOON_KM_THRESHOLD,
    dueSoonDaysThreshold: Int = TrackingConstants.DUE_SOON_DAYS_THRESHOLD
): MaintenanceEvaluation = evaluate(
    item = item.toDomain(),
    currentOdometerKm = currentOdometerKm,
    currentEpochMs = currentEpochMs,
    dueSoonKmThreshold = dueSoonKmThreshold,
    dueSoonDaysThreshold = dueSoonDaysThreshold
)

fun MaintenanceCalculator.shouldNotify(
    item: MaintenanceItemEntity,
    status: MaintenanceStatus,
    currentEpochMs: Long,
    antiSpamDurationMs: Long = 7L * 24 * 60 * 60 * 1000
): Boolean = shouldNotify(
    item = item.toDomain(),
    status = status,
    currentEpochMs = currentEpochMs,
    antiSpamDurationMs = antiSpamDurationMs
)

fun FuelLogEntity.toDomain(): FuelFill = FuelFill(
    id = id,
    timestampEpochMs = timestampEpochMs,
    odometerKm = odometerKm,
    litres = litres,
    totalCost = totalCost,
    isFullTank = isFullTank
)

fun FuelMileageCalculator.calculate(logs: List<FuelLogEntity>): FuelMileageStats =
    computeMileage(logs.map { it.toDomain() })
