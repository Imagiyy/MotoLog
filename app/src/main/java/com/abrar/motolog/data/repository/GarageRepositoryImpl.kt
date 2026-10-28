package com.abrar.motolog.data.repository

import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.FuelLogDao
import com.abrar.motolog.data.local.dao.MaintenanceDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.engine.calculate
import com.abrar.motolog.domain.engine.evaluate
import com.abrar.motolog.shared.domain.engine.FuelMileageCalculator
import com.abrar.motolog.shared.domain.engine.MaintenanceCalculator
import com.abrar.motolog.shared.domain.engine.OdometerCalculator
import com.abrar.motolog.shared.domain.model.FuelMileageStats
import com.abrar.motolog.shared.domain.model.MaintenanceEvaluation
import com.abrar.motolog.domain.repository.GarageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import com.abrar.motolog.data.local.dao.MileageCycleDao
import com.abrar.motolog.data.local.entity.MileageCycleEntity
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarageRepositoryImpl @Inject constructor(
    private val bikeDao: BikeDao,
    private val rideDao: RideDao,
    private val maintenanceDao: MaintenanceDao,
    private val fuelLogDao: FuelLogDao,
    private val mileageCycleDao: MileageCycleDao,
    private val settingsRepository: SettingsRepository
) : GarageRepository {

    override val activeBikes: Flow<List<BikeEntity>> = bikeDao.getActiveBikes()
    override val archivedBikes: Flow<List<BikeEntity>> = bikeDao.getArchivedBikes()
    override val currentBikeId: Flow<Long?> = settingsRepository.currentBikeId

    override suspend fun setCurrentBikeId(bikeId: Long?) {
        settingsRepository.setCurrentBikeId(bikeId)
    }

    override suspend fun getBikeById(bikeId: Long): BikeEntity? = withContext(Dispatchers.IO) {
        bikeDao.getBikeById(bikeId)
    }

    override fun observeBike(bikeId: Long): Flow<BikeEntity?> {
        return bikeDao.getAllBikes().map { bikes ->
            bikes.find { it.id == bikeId }
        }
    }

    override fun observeBikeOdometer(bikeId: Long): Flow<Double> {
        return combine(
            observeBike(bikeId),
            rideDao.getTotalDistanceMetersForBike(bikeId)
        ) { bike, distanceMeters ->
            if (bike == null) 0.0
            else OdometerCalculator.calculateCurrentOdometerKm(
                initialOdometerKm = bike.initialOdometerKm,
                recordedDistanceMeters = distanceMeters ?: 0.0,
                odometerOffsetKm = bike.odometerOffsetKm
            )
        }
    }

    override suspend fun addBike(
        name: String,
        makeModel: String,
        initialOdometerKm: Double,
        registrationNumber: String
    ): Long = withContext(Dispatchers.IO) {
        val bike = BikeEntity(
            name = name,
            makeModel = makeModel,
            initialOdometerKm = initialOdometerKm,
            odometerOffsetKm = 0.0,
            createdAt = System.currentTimeMillis(),
            isArchived = false,
            registrationNumber = registrationNumber.trim()
        )
        val newId = bikeDao.insert(bike)
        // If this is the only active bike, automatically make it current
        val activeCount = bikeDao.getActiveBikeCount()
        if (activeCount == 1) {
            settingsRepository.setCurrentBikeId(newId)
        }
        newId
    }

    override suspend fun updateBike(bike: BikeEntity): Unit = withContext(Dispatchers.IO) {
        bikeDao.update(bike)
    }

    override suspend fun setOdometer(bikeId: Long, targetOdometerKm: Double): Unit = withContext(Dispatchers.IO) {
        val bike = bikeDao.getBikeById(bikeId) ?: return@withContext
        val totalDistanceMeters = rideDao.getTotalDistanceMetersForBikeOnce(bikeId) ?: 0.0
        val newOffset = OdometerCalculator.computeOffsetForTarget(
            initialOdometerKm = bike.initialOdometerKm,
            recordedDistanceMeters = totalDistanceMeters,
            targetOdometerKm = targetOdometerKm
        )
        bikeDao.updateOdometerOffset(bikeId, newOffset)
    }

    override suspend fun archiveOrDeleteBike(bikeId: Long): Boolean = withContext(Dispatchers.IO) {
        val rideCount = bikeDao.getRideCountForBike(bikeId)
        val bike = bikeDao.getBikeById(bikeId) ?: return@withContext false
        val currentSelected = settingsRepository.currentBikeId.firstOrNull()

        if (rideCount > 0) {
            bikeDao.setArchived(bikeId, true)
            if (currentSelected == bikeId) {
                settingsRepository.setCurrentBikeId(null)
            }
            false // archived
        } else {
            bikeDao.delete(bike)
            if (currentSelected == bikeId) {
                settingsRepository.setCurrentBikeId(null)
            }
            true // deleted
        }
    }

    // Maintenance
    override fun observeMaintenanceItems(bikeId: Long): Flow<List<MaintenanceItemEntity>> {
        return maintenanceDao.getItemsForBike(bikeId)
    }

    override fun observeMaintenanceEvaluations(bikeId: Long): Flow<List<MaintenanceEvaluation>> {
        return combine(
            observeBikeOdometer(bikeId),
            maintenanceDao.getItemsForBike(bikeId)
        ) { currentOdo, items ->
            val now = System.currentTimeMillis()
            items.map { item ->
                MaintenanceCalculator.evaluate(item, currentOdo, now)
            }
        }
    }

    override suspend fun addMaintenanceItem(
        bikeId: Long,
        name: String,
        intervalKm: Double?,
        intervalDays: Int?
    ): Long = withContext(Dispatchers.IO) {
        val bike = bikeDao.getBikeById(bikeId)
        val totalDistanceMeters = rideDao.getTotalDistanceMetersForBikeOnce(bikeId) ?: 0.0
        val currentOdo = if (bike != null) {
            OdometerCalculator.calculateCurrentOdometerKm(
                initialOdometerKm = bike.initialOdometerKm,
                recordedDistanceMeters = totalDistanceMeters,
                odometerOffsetKm = bike.odometerOffsetKm
            )
        } else 0.0

        val item = MaintenanceItemEntity(
            bikeId = bikeId,
            name = name,
            intervalKm = intervalKm,
            intervalDays = intervalDays,
            lastDoneOdometerKm = currentOdo,
            lastDoneDateEpochMs = System.currentTimeMillis()
        )
        maintenanceDao.insert(item)
    }

    override suspend fun updateMaintenanceItem(item: MaintenanceItemEntity): Unit = withContext(Dispatchers.IO) {
        maintenanceDao.update(item)
    }

    override suspend fun deleteMaintenanceItem(item: MaintenanceItemEntity): Unit = withContext(Dispatchers.IO) {
        maintenanceDao.delete(item)
    }

    override suspend fun markMaintenanceDone(
        itemId: Long,
        odometerKm: Double,
        dateEpochMs: Long
    ): Unit = withContext(Dispatchers.IO) {
        maintenanceDao.markDone(itemId, odometerKm, dateEpochMs)
    }

    // Fuel
    override fun observeAllFuelLogs(): Flow<List<FuelLogEntity>> {
        return fuelLogDao.getAllFuelLogsFlow()
    }

    override fun observeFuelLogs(bikeId: Long): Flow<List<FuelLogEntity>> {
        return fuelLogDao.getLogsForBike(bikeId)
    }

    override fun observeFuelStats(bikeId: Long): Flow<FuelMileageStats> {
        return combine(
            fuelLogDao.getLogsForBike(bikeId),
            observeBike(bikeId),
            observeBikeOdometer(bikeId)
        ) { logs, bike, currentOdo ->
            val resetOdo = bike?.mileageResetOdometerKm
            val resetEpoch = bike?.mileageResetEpochMs
            val activeLogs = if (resetEpoch != null) {
                logs.filter { it.timestampEpochMs >= resetEpoch }
            } else if (resetOdo != null) {
                logs.filter { it.odometerKm >= resetOdo }
            } else {
                logs
            }
            FuelMileageCalculator.calculate(
                logs = activeLogs,
                currentOdometerKm = currentOdo,
                startOdometerKm = resetOdo
            )
        }
    }

    override suspend fun addFuelLog(
        bikeId: Long,
        odometerKm: Double,
        litres: Double,
        totalCost: Double,
        isFullTank: Boolean,
        notes: String,
        timestampEpochMs: Long
    ): Long = withContext(Dispatchers.IO) {
        val log = FuelLogEntity(
            bikeId = bikeId,
            timestampEpochMs = timestampEpochMs,
            odometerKm = odometerKm,
            litres = litres,
            totalCost = totalCost,
            isFullTank = isFullTank,
            notes = notes
        )
        fuelLogDao.insert(log)
    }

    override suspend fun updateFuelLog(log: FuelLogEntity): Unit = withContext(Dispatchers.IO) {
        fuelLogDao.update(log)
    }

    override suspend fun deleteFuelLog(log: FuelLogEntity): Unit = withContext(Dispatchers.IO) {
        fuelLogDao.delete(log)
    }

    override suspend fun getLatestFuelLog(bikeId: Long): FuelLogEntity? = withContext(Dispatchers.IO) {
        fuelLogDao.getLatestLogForBike(bikeId)
    }

    override suspend fun resetMileage(bikeId: Long, currentOdometerKm: Double, clearLogs: Boolean): Unit = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val bike = bikeDao.getBikeById(bikeId)
        val logs = fuelLogDao.getLogsForBikeAscending(bikeId)
        val prevResetEpoch = bike?.mileageResetEpochMs
        val prevResetOdo = bike?.mileageResetOdometerKm ?: bike?.initialOdometerKm ?: 0.0
        val activeLogs = if (prevResetEpoch != null) {
            logs.filter { it.timestampEpochMs >= prevResetEpoch }
        } else {
            logs.filter { it.odometerKm >= prevResetOdo }
        }
        val currentStats = FuelMileageCalculator.calculate(
            logs = activeLogs,
            currentOdometerKm = currentOdometerKm,
            startOdometerKm = prevResetOdo
        )

        val distance = (currentOdometerKm - prevResetOdo).coerceAtLeast(0.0)
        if (distance > 0.0 || currentStats.totalLitres > 0.0 || currentStats.totalCost > 0.0) {
            mileageCycleDao.insert(
                MileageCycleEntity(
                    bikeId = bikeId,
                    resetTimestampEpochMs = now,
                    startOdometerKm = prevResetOdo,
                    endOdometerKm = currentOdometerKm,
                    distanceKm = distance,
                    totalLitres = currentStats.totalLitres,
                    totalCost = currentStats.totalCost,
                    averageMileageKml = currentStats.averageMileageKml
                )
            )
        }

        bikeDao.updateMileageReset(bikeId, 0.0, now)
        setOdometer(bikeId, 0.0)
        if (clearLogs) {
            fuelLogDao.deleteLogsForBike(bikeId)
        }
    }

    override suspend fun clearMileageReset(bikeId: Long): Unit = withContext(Dispatchers.IO) {
        bikeDao.updateMileageReset(bikeId, null, null)
    }

    override fun observeMileageCycles(bikeId: Long): Flow<List<MileageCycleEntity>> {
        return mileageCycleDao.observeCyclesForBike(bikeId)
    }

    override suspend fun deleteMileageCycle(cycle: MileageCycleEntity): Unit = withContext(Dispatchers.IO) {
        mileageCycleDao.delete(cycle)
    }

    // Ride Reassignment
    override suspend fun reassignRideBike(rideId: Long, newBikeId: Long?): Unit = withContext(Dispatchers.IO) {
        rideDao.updateRideBike(rideId, newBikeId)
    }
}
