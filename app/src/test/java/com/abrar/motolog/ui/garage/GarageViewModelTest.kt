package com.abrar.motolog.ui.garage

import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.MaintenanceDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.domain.model.FuelMileageStats
import com.abrar.motolog.domain.model.MaintenanceEvaluation
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
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.abrar.motolog.data.settings.SettingsRepository
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GarageViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGarageRepository: FakeGarageRepository
    private lateinit var fakeBikeDao: FakeBikeDao
    private lateinit var fakeRideDao: FakeRideDao
    private lateinit var fakeMaintenanceDao: FakeMaintenanceDao
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: GarageViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGarageRepository = FakeGarageRepository()
        fakeBikeDao = FakeBikeDao()
        fakeRideDao = FakeRideDao()
        fakeMaintenanceDao = FakeMaintenanceDao()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(testDispatcher),
            produceFile = { tempFolder.newFile("garage_settings.preferences_pb") }
        )
        settingsRepository = SettingsRepository(testDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsActiveBikesWithCalculatedOdometer() = runTest(testDispatcher) {
        val bike = BikeEntity(id = 1L, name = "Honda CB300R", makeModel = "Honda", initialOdometerKm = 5000.0)
        fakeGarageRepository.activeBikesFlow.value = listOf(bike)
        fakeGarageRepository.currentBikeIdFlow.value = 1L
        fakeRideDao.distanceMap[1L] = 150_000.0 // 150 km recorded

        viewModel = GarageViewModel(fakeGarageRepository, fakeBikeDao, fakeRideDao, fakeMaintenanceDao, settingsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.bikes.size)
        val bikeItem = state.bikes[0]
        assertEquals("Honda CB300R", bikeItem.bike.name)
        assertEquals(5150.0, bikeItem.currentOdometerKm, 0.001)
        assertTrue(bikeItem.isCurrentBike)
    }

    @Test
    fun selectCurrentBike_updatesCurrentBikeId() = runTest(testDispatcher) {
        val bike1 = BikeEntity(id = 1L, name = "Bike 1", initialOdometerKm = 0.0)
        val bike2 = BikeEntity(id = 2L, name = "Bike 2", initialOdometerKm = 0.0)
        fakeGarageRepository.activeBikesFlow.value = listOf(bike1, bike2)
        fakeGarageRepository.currentBikeIdFlow.value = 1L

        viewModel = GarageViewModel(fakeGarageRepository, fakeBikeDao, fakeRideDao, fakeMaintenanceDao, settingsRepository)
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.currentBikeId)

        viewModel.selectCurrentBike(2L)
        advanceUntilIdle()

        assertEquals(2L, viewModel.uiState.value.currentBikeId)
    }

    @Test
    fun addBikeDialog_lifecycleAndSubmission() = runTest(testDispatcher) {
        viewModel = GarageViewModel(fakeGarageRepository, fakeBikeDao, fakeRideDao, fakeMaintenanceDao, settingsRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAddBikeDialogOpen)

        viewModel.openAddBikeDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAddBikeDialogOpen)

        viewModel.addBike("New Tourer", "Kawasaki Versys", 12000.0)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAddBikeDialogOpen)
        assertEquals(1, fakeGarageRepository.addedBikes.size)
        assertEquals("New Tourer", fakeGarageRepository.addedBikes[0].name)
    }

    @Test
    fun requestArchiveOrDelete_identifiesBikesWithRidesForArchiving() = runTest(testDispatcher) {
        val bikeWithRides = BikeEntity(id = 1L, name = "Ridden Bike")
        fakeBikeDao.rideCounts[1L] = 5

        viewModel = GarageViewModel(fakeGarageRepository, fakeBikeDao, fakeRideDao, fakeMaintenanceDao, settingsRepository)
        advanceUntilIdle()

        viewModel.requestArchiveOrDelete(bikeWithRides)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(bikeWithRides, state.bikeToArchiveOrDelete)
        assertTrue(state.bikeHasRides) // Should offer archive

        viewModel.dismissArchiveOrDeleteDialog()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.bikeToArchiveOrDelete)
    }

    private class FakeGarageRepository : GarageRepository {
        val activeBikesFlow = MutableStateFlow<List<BikeEntity>>(emptyList())
        val archivedBikesFlow = MutableStateFlow<List<BikeEntity>>(emptyList())
        val currentBikeIdFlow = MutableStateFlow<Long?>(null)
        val addedBikes = mutableListOf<BikeEntity>()

        override val activeBikes: Flow<List<BikeEntity>> = activeBikesFlow
        override val archivedBikes: Flow<List<BikeEntity>> = archivedBikesFlow
        override val currentBikeId: Flow<Long?> = currentBikeIdFlow

        override suspend fun setCurrentBikeId(bikeId: Long?) {
            currentBikeIdFlow.value = bikeId
        }

        override suspend fun getBikeById(bikeId: Long): BikeEntity? =
            activeBikesFlow.value.find { it.id == bikeId }

        override fun observeBike(bikeId: Long): Flow<BikeEntity?> =
            MutableStateFlow(activeBikesFlow.value.find { it.id == bikeId })

        override fun observeBikeOdometer(bikeId: Long): Flow<Double> =
            MutableStateFlow(0.0)

        override suspend fun addBike(name: String, makeModel: String, initialOdometerKm: Double): Long {
            val bike = BikeEntity(id = (addedBikes.size + 1).toLong(), name = name, makeModel = makeModel, initialOdometerKm = initialOdometerKm)
            addedBikes.add(bike)
            activeBikesFlow.value = activeBikesFlow.value + bike
            return bike.id
        }

        override suspend fun updateBike(bike: BikeEntity) {}
        override suspend fun setOdometer(bikeId: Long, targetOdometerKm: Double) {}
        override suspend fun archiveOrDeleteBike(bikeId: Long): Boolean = true
        override fun observeMaintenanceItems(bikeId: Long): Flow<List<MaintenanceItemEntity>> = MutableStateFlow(emptyList())
        override fun observeMaintenanceEvaluations(bikeId: Long): Flow<List<MaintenanceEvaluation>> = MutableStateFlow(emptyList())
        override suspend fun addMaintenanceItem(bikeId: Long, name: String, intervalKm: Double?, intervalDays: Int?): Long = 1L
        override suspend fun updateMaintenanceItem(item: MaintenanceItemEntity) {}
        override suspend fun deleteMaintenanceItem(item: MaintenanceItemEntity) {}
        override suspend fun markMaintenanceDone(itemId: Long, odometerKm: Double, dateEpochMs: Long) {}
        override fun observeFuelLogs(bikeId: Long): Flow<List<FuelLogEntity>> = MutableStateFlow(emptyList())
        override fun observeFuelStats(bikeId: Long): Flow<FuelMileageStats> = MutableStateFlow(FuelMileageStats())
        override suspend fun addFuelLog(bikeId: Long, odometerKm: Double, litres: Double, totalCost: Double, isFullTank: Boolean, notes: String, timestampEpochMs: Long): Long = 1L
        override suspend fun updateFuelLog(log: FuelLogEntity) {}
        override suspend fun deleteFuelLog(log: FuelLogEntity) {}
        override suspend fun getLatestFuelLog(bikeId: Long): FuelLogEntity? = null
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
        override suspend fun getAllBikesOnce(): List<BikeEntity> = emptyList()
        override suspend fun insertAll(bikes: List<BikeEntity>) {}
        override suspend fun deleteAllBikes() {}
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
        override suspend fun findDuplicateRide(minStartTime: Long, maxStartTime: Long, minDistance: Double, maxDistance: Double): RideEntity? = null
        override suspend fun getAllRidesOnce(): List<RideEntity> = emptyList()
        override suspend fun insertAll(rides: List<RideEntity>) {}
        override suspend fun deleteAllRides() {}
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        override suspend fun insert(item: MaintenanceItemEntity): Long = item.id
        override suspend fun update(item: MaintenanceItemEntity) {}
        override suspend fun delete(item: MaintenanceItemEntity) {}
        override fun getItemsForBike(bikeId: Long): Flow<List<MaintenanceItemEntity>> = MutableStateFlow(emptyList())
        override suspend fun getItemsForBikeOnce(bikeId: Long): List<MaintenanceItemEntity> = emptyList()
        override suspend fun getAllItemsOnce(): List<MaintenanceItemEntity> = emptyList()
        override suspend fun getItemById(id: Long): MaintenanceItemEntity? = null
        override suspend fun markDone(id: Long, odometerKm: Double, dateEpochMs: Long) {}
        override suspend fun updateLastNotified(id: Long, notifiedEpochMs: Long) {}
        override suspend fun insertAll(items: List<MaintenanceItemEntity>) {}
        override suspend fun deleteAllMaintenance() {}
    }
}
