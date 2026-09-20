package com.abrar.motolog.ui.garage.detail

import androidx.lifecycle.SavedStateHandle
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.domain.model.FuelMileageStats
import com.abrar.motolog.domain.model.MaintenanceEvaluation
import com.abrar.motolog.domain.model.MaintenanceStatus
import com.abrar.motolog.domain.repository.GarageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BikeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGarageRepository: FakeGarageRepository
    private lateinit var fakeBikeDao: FakeBikeDao
    private lateinit var fakeRideDao: FakeRideDao
    private lateinit var viewModel: BikeDetailViewModel

    private val testBike = BikeEntity(
        id = 1L,
        name = "Royal Enfield Himalayan",
        makeModel = "RE",
        initialOdometerKm = 10_000.0,
        odometerOffsetKm = 0.0
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGarageRepository = FakeGarageRepository(testBike)
        fakeBikeDao = FakeBikeDao()
        fakeRideDao = FakeRideDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(bikeId: Long = 1L): BikeDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("bikeId" to bikeId))
        return BikeDetailViewModel(
            garageRepository = fakeGarageRepository,
            bikeDao = fakeBikeDao,
            rideDao = fakeRideDao,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun initialState_loadsBikeDetailsAndStats() = runTest(testDispatcher) {
        fakeRideDao.distanceMap[1L] = 200_000.0 // 200 km
        fakeBikeDao.rideCounts[1L] = 4
        fakeGarageRepository.bikeOdoFlow.value = 10_200.0

        viewModel = createViewModel(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.bike)
        assertEquals("Royal Enfield Himalayan", state.bike?.name)
        assertEquals(10_200.0, state.currentOdometerKm, 0.001)
        assertEquals(4, state.recordedRidesCount)
        assertEquals(200.0, state.recordedRidesDistanceKm, 0.001)
    }

    @Test
    fun selectTab_updatesSelectedTab() = runTest(testDispatcher) {
        viewModel = createViewModel(1L)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(1)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(2)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun setOdometer_calibratesOdometerViaRepository() = runTest(testDispatcher) {
        viewModel = createViewModel(1L)
        advanceUntilIdle()

        viewModel.openSetOdometerDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSetOdometerDialogOpen)

        viewModel.setOdometer(11_500.0)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSetOdometerDialogOpen)
        assertEquals(11_500.0, fakeGarageRepository.lastSetOdometerKm, 0.001)
    }

    @Test
    fun maintenanceItem_addAndMarkDoneFlow() = runTest(testDispatcher) {
        viewModel = createViewModel(1L)
        advanceUntilIdle()

        // 1. Add item
        viewModel.openAddMaintenanceDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAddMaintenanceDialogOpen)

        viewModel.addMaintenanceItem("Brake Pads", 8_000.0, 180)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAddMaintenanceDialogOpen)
        assertEquals(1, fakeGarageRepository.maintenanceItems.size)
        assertEquals("Brake Pads", fakeGarageRepository.maintenanceItems[0].name)

        // 2. Mark done
        val item = fakeGarageRepository.maintenanceItems[0]
        viewModel.requestMarkDone(item)
        advanceUntilIdle()
        assertEquals(item, viewModel.uiState.value.itemToMarkDone)

        viewModel.confirmMarkDone(item.id, 10_500.0)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.itemToMarkDone)
        assertEquals(10_500.0, fakeGarageRepository.lastMarkedDoneOdoKm, 0.001)
    }

    @Test
    fun fuelLog_addAndEditFlow() = runTest(testDispatcher) {
        viewModel = createViewModel(1L)
        advanceUntilIdle()

        viewModel.openAddFuelDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAddFuelDialogOpen)

        viewModel.addFuelLog(
            odometerKm = 10_350.0,
            litres = 12.5,
            totalCost = 1250.0,
            isFullTank = true,
            notes = "First full fill"
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAddFuelDialogOpen)
        assertEquals(1, fakeGarageRepository.fuelLogs.size)
        val addedLog = fakeGarageRepository.fuelLogs[0]
        assertEquals(10_350.0, addedLog.odometerKm, 0.001)
        assertEquals(12.5, addedLog.litres, 0.001)

        // Request delete
        viewModel.requestDeleteFuelLog(addedLog)
        advanceUntilIdle()
        assertEquals(addedLog, viewModel.uiState.value.fuelLogToDelete)

        viewModel.confirmDeleteFuelLog()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.fuelLogToDelete)
        assertTrue(fakeGarageRepository.fuelLogs.isEmpty())
    }

    private class FakeGarageRepository(val bike: BikeEntity) : GarageRepository {
        val bikeFlow = MutableStateFlow<BikeEntity?>(bike)
        val bikeOdoFlow = MutableStateFlow(bike.initialOdometerKm)
        val maintenanceFlow = MutableStateFlow<List<MaintenanceItemEntity>>(emptyList())
        val maintenanceEvalsFlow = MutableStateFlow<List<MaintenanceEvaluation>>(emptyList())
        val fuelLogsFlow = MutableStateFlow<List<FuelLogEntity>>(emptyList())
        val fuelStatsFlow = MutableStateFlow(FuelMileageStats())

        val maintenanceItems = mutableListOf<MaintenanceItemEntity>()
        val fuelLogs = mutableListOf<FuelLogEntity>()
        var lastSetOdometerKm: Double = 0.0
        var lastMarkedDoneOdoKm: Double = 0.0

        override val activeBikes: Flow<List<BikeEntity>> = MutableStateFlow(listOf(bike))
        override val archivedBikes: Flow<List<BikeEntity>> = MutableStateFlow(emptyList())
        override val currentBikeId: Flow<Long?> = MutableStateFlow(bike.id)

        override suspend fun setCurrentBikeId(bikeId: Long?) {}
        override suspend fun getBikeById(bikeId: Long): BikeEntity? = if (bike.id == bikeId) bike else null
        override fun observeBike(bikeId: Long): Flow<BikeEntity?> = bikeFlow
        override fun observeBikeOdometer(bikeId: Long): Flow<Double> = bikeOdoFlow

        override suspend fun addBike(name: String, makeModel: String, initialOdometerKm: Double): Long = 1L
        override suspend fun updateBike(bike: BikeEntity) {
            bikeFlow.value = bike
        }

        override suspend fun setOdometer(bikeId: Long, targetOdometerKm: Double) {
            lastSetOdometerKm = targetOdometerKm
            bikeOdoFlow.value = targetOdometerKm
        }

        override suspend fun archiveOrDeleteBike(bikeId: Long): Boolean = true

        override fun observeMaintenanceItems(bikeId: Long): Flow<List<MaintenanceItemEntity>> = maintenanceFlow
        override fun observeMaintenanceEvaluations(bikeId: Long): Flow<List<MaintenanceEvaluation>> = maintenanceEvalsFlow

        override suspend fun addMaintenanceItem(bikeId: Long, name: String, intervalKm: Double?, intervalDays: Int?): Long {
            val item = MaintenanceItemEntity(
                id = (maintenanceItems.size + 1).toLong(),
                bikeId = bikeId,
                name = name,
                intervalKm = intervalKm,
                intervalDays = intervalDays,
                lastDoneOdometerKm = bikeOdoFlow.value,
                lastDoneDateEpochMs = System.currentTimeMillis()
            )
            maintenanceItems.add(item)
            maintenanceFlow.value = maintenanceItems.toList()
            return item.id
        }

        override suspend fun updateMaintenanceItem(item: MaintenanceItemEntity) {
            val index = maintenanceItems.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                maintenanceItems[index] = item
                maintenanceFlow.value = maintenanceItems.toList()
            }
        }

        override suspend fun deleteMaintenanceItem(item: MaintenanceItemEntity) {
            maintenanceItems.removeAll { it.id == item.id }
            maintenanceFlow.value = maintenanceItems.toList()
        }

        override suspend fun markMaintenanceDone(itemId: Long, odometerKm: Double, dateEpochMs: Long) {
            lastMarkedDoneOdoKm = odometerKm
            val index = maintenanceItems.indexOfFirst { it.id == itemId }
            if (index >= 0) {
                val updated = maintenanceItems[index].copy(
                    lastDoneOdometerKm = odometerKm,
                    lastDoneDateEpochMs = dateEpochMs
                )
                maintenanceItems[index] = updated
                maintenanceFlow.value = maintenanceItems.toList()
            }
        }

        override fun observeFuelLogs(bikeId: Long): Flow<List<FuelLogEntity>> = fuelLogsFlow
        override fun observeFuelStats(bikeId: Long): Flow<FuelMileageStats> = fuelStatsFlow

        override suspend fun addFuelLog(
            bikeId: Long,
            odometerKm: Double,
            litres: Double,
            totalCost: Double,
            isFullTank: Boolean,
            notes: String,
            timestampEpochMs: Long
        ): Long {
            val log = FuelLogEntity(
                id = (fuelLogs.size + 1).toLong(),
                bikeId = bikeId,
                odometerKm = odometerKm,
                litres = litres,
                totalCost = totalCost,
                isFullTank = isFullTank,
                notes = notes,
                timestampEpochMs = timestampEpochMs
            )
            fuelLogs.add(log)
            fuelLogsFlow.value = fuelLogs.toList()
            return log.id
        }

        override suspend fun updateFuelLog(log: FuelLogEntity) {
            val index = fuelLogs.indexOfFirst { it.id == log.id }
            if (index >= 0) {
                fuelLogs[index] = log
                fuelLogsFlow.value = fuelLogs.toList()
            }
        }

        override suspend fun deleteFuelLog(log: FuelLogEntity) {
            fuelLogs.removeAll { it.id == log.id }
            fuelLogsFlow.value = fuelLogs.toList()
        }

        override suspend fun getLatestFuelLog(bikeId: Long): FuelLogEntity? = fuelLogs.maxByOrNull { it.timestampEpochMs }
        override suspend fun reassignRideBike(rideId: Long, newBikeId: Long?) {}
    }

    private class FakeBikeDao : BikeDao {
        val rideCounts = mutableMapOf<Long, Int>()

        override suspend fun insert(bike: BikeEntity): Long = bike.id
        override suspend fun update(bike: BikeEntity) {}
        override suspend fun delete(bike: BikeEntity) {}
        override fun getAllBikes(): Flow<List<BikeEntity>> = MutableStateFlow(emptyList())
        override fun getActiveBikes(): Flow<List<BikeEntity>> = MutableStateFlow(emptyList())
        override suspend fun getActiveBikesOnce(): List<BikeEntity> = emptyList()
        override fun getArchivedBikes(): Flow<List<BikeEntity>> = MutableStateFlow(emptyList())
        override suspend fun getBikeById(bikeId: Long): BikeEntity? = null
        override suspend fun getActiveBikeCount(): Int = 0
        override suspend fun getRideCountForBike(bikeId: Long): Int = rideCounts[bikeId] ?: 0
        override suspend fun setArchived(bikeId: Long, isArchived: Boolean) {}
        override suspend fun updateOdometerOffset(bikeId: Long, offsetKm: Double) {}
    }

    private class FakeRideDao : RideDao {
        val distanceMap = mutableMapOf<Long, Double>()

        override suspend fun insert(ride: RideEntity): Long = ride.id
        override suspend fun update(ride: RideEntity) {}
        override suspend fun delete(ride: RideEntity) {}
        override fun getAllRides(): Flow<List<RideEntity>> = MutableStateFlow(emptyList())
        override suspend fun getRideById(rideId: Long): RideEntity? = null
        override suspend fun getRidesByStatus(status: RideStatus): List<RideEntity> = emptyList()
        override fun getRidesForBike(bikeId: Long): Flow<List<RideEntity>> = MutableStateFlow(emptyList())
        override suspend fun findActiveRide(): RideEntity? = null
        override fun getFinishedRides(): Flow<List<RideEntity>> = MutableStateFlow(emptyList())
        override suspend fun updateRideName(rideId: Long, name: String) {}
        override suspend fun updateRideBike(rideId: Long, bikeId: Long?) {}
        override fun getTotalDistanceMetersForBike(bikeId: Long): Flow<Double?> = MutableStateFlow(distanceMap[bikeId])
        override suspend fun getTotalDistanceMetersForBikeOnce(bikeId: Long): Double? = distanceMap[bikeId]
    }
}
