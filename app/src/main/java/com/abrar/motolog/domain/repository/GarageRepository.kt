package com.abrar.motolog.domain.repository

import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.shared.domain.model.FuelMileageStats
import com.abrar.motolog.shared.domain.model.MaintenanceEvaluation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing garage data:
 * motorcycles, odometer readings, maintenance schedules, and fuel logs.
 */
interface GarageRepository {
    val activeBikes: Flow<List<BikeEntity>>
    val archivedBikes: Flow<List<BikeEntity>>
    val currentBikeId: Flow<Long?>

    suspend fun setCurrentBikeId(bikeId: Long?)
    suspend fun getBikeById(bikeId: Long): BikeEntity?
    fun observeBike(bikeId: Long): Flow<BikeEntity?>
    fun observeBikeOdometer(bikeId: Long): Flow<Double>

    suspend fun addBike(
        name: String,
        makeModel: String,
        initialOdometerKm: Double,
        registrationNumber: String = ""
    ): Long
    suspend fun updateBike(bike: BikeEntity)
    suspend fun setOdometer(bikeId: Long, targetOdometerKm: Double)
    suspend fun archiveOrDeleteBike(bikeId: Long): Boolean

    // Maintenance
    fun observeMaintenanceItems(bikeId: Long): Flow<List<MaintenanceItemEntity>>
    fun observeMaintenanceEvaluations(bikeId: Long): Flow<List<MaintenanceEvaluation>>
    suspend fun addMaintenanceItem(bikeId: Long, name: String, intervalKm: Double?, intervalDays: Int?): Long
    suspend fun updateMaintenanceItem(item: MaintenanceItemEntity)
    suspend fun deleteMaintenanceItem(item: MaintenanceItemEntity)
    suspend fun markMaintenanceDone(itemId: Long, odometerKm: Double, dateEpochMs: Long = System.currentTimeMillis())

    // Fuel
    fun observeFuelLogs(bikeId: Long): Flow<List<FuelLogEntity>>
    fun observeFuelStats(bikeId: Long): Flow<FuelMileageStats>
    suspend fun addFuelLog(
        bikeId: Long,
        odometerKm: Double,
        litres: Double,
        totalCost: Double,
        isFullTank: Boolean,
        notes: String = "",
        timestampEpochMs: Long = System.currentTimeMillis()
    ): Long
    suspend fun updateFuelLog(log: FuelLogEntity)
    suspend fun deleteFuelLog(log: FuelLogEntity)
    suspend fun getLatestFuelLog(bikeId: Long): FuelLogEntity?

    // Ride Reassignment
    suspend fun reassignRideBike(rideId: Long, newBikeId: Long?)
}
