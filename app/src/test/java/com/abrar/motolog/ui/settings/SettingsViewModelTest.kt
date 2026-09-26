package com.abrar.motolog.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.abrar.motolog.data.io.BackupManager
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.FuelLogDao
import com.abrar.motolog.data.local.dao.MaintenanceDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.model.FuelUnit
import com.abrar.motolog.domain.model.SpeedAlertStyle
import com.abrar.motolog.domain.model.TrackingMode
import com.abrar.motolog.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import kotlinx.coroutines.flow.launchIn

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var fakeBikeDao: FakeBikeDao
    private lateinit var fakeRideDao: FakeRideDao
    private lateinit var fakeRidePointDao: FakeRidePointDao
    private lateinit var fakeMaintenanceDao: FakeMaintenanceDao
    private lateinit var fakeFuelLogDao: FakeFuelLogDao
    private lateinit var backupManager: BackupManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeBikeDao = FakeBikeDao()
        fakeRideDao = FakeRideDao()
        fakeRidePointDao = FakeRidePointDao()
        fakeMaintenanceDao = FakeMaintenanceDao()
        fakeFuelLogDao = FakeFuelLogDao()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_settings_vm.preferences_pb") }
        )
        settingsRepository = SettingsRepository(testDataStore)

        backupManager = BackupManager(
            bikeDao = fakeBikeDao,
            rideDao = fakeRideDao,
            ridePointDao = fakeRidePointDao,
            maintenanceDao = fakeMaintenanceDao,
            fuelLogDao = fakeFuelLogDao,
            settingsRepository = settingsRepository,
            database = null
        )

        viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            bikeDao = fakeBikeDao,
            rideDao = fakeRideDao,
            ridePointDao = fakeRidePointDao,
            backupManager = backupManager,
            context = null
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultSettings_matchExpectedDefaults() = runTest(testDispatcher) {
        viewModel.useMetricUnits.test {
            assertTrue(awaitItem())
        }
        viewModel.themeMode.test {
            assertEquals(ThemeMode.DARK, awaitItem())
        }
        viewModel.trackingMode.test {
            assertEquals(TrackingMode.HIGH_ACCURACY, awaitItem())
        }
        viewModel.currencySymbol.test {
            assertEquals("$", awaitItem())
        }
        viewModel.fuelUnit.test {
            assertEquals(FuelUnit.KM_PER_LITER, awaitItem())
        }
        viewModel.speedAlertEnabled.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun togglingSettings_updatesStateFlowsImmediately() = runTest(testDispatcher) {
        viewModel.useMetricUnits.launchIn(backgroundScope)
        viewModel.themeMode.launchIn(backgroundScope)
        viewModel.trackingMode.launchIn(backgroundScope)
        viewModel.speedAlertEnabled.launchIn(backgroundScope)
        viewModel.speedAlertThresholdKmh.launchIn(backgroundScope)
        viewModel.speedAlertStyle.launchIn(backgroundScope)
        viewModel.fuelUnit.launchIn(backgroundScope)
        viewModel.currencySymbol.launchIn(backgroundScope)
        viewModel.autoPauseEnabled.launchIn(backgroundScope)
        viewModel.keepScreenOn.launchIn(backgroundScope)
        viewModel.defaultMapTheme.launchIn(backgroundScope)

        // Toggle units to Imperial
        viewModel.setUseMetricUnits(false)
        advanceUntilIdle()
        assertFalse(viewModel.useMetricUnits.value)

        // Change Theme to AMOLED
        viewModel.setThemeMode(ThemeMode.AMOLED)
        advanceUntilIdle()
        assertEquals(ThemeMode.AMOLED, viewModel.themeMode.value)

        // Change Tracking Mode to Battery Saver
        viewModel.setTrackingMode(TrackingMode.BATTERY_SAVER)
        advanceUntilIdle()
        assertEquals(TrackingMode.BATTERY_SAVER, viewModel.trackingMode.value)

        // Enable speed alert and set threshold & style
        viewModel.setSpeedAlertEnabled(true)
        viewModel.setSpeedAlertThresholdUser(110.0, isMetric = true)
        viewModel.setSpeedAlertStyle(SpeedAlertStyle.VISUAL_AND_HAPTIC)
        advanceUntilIdle()
        assertTrue(viewModel.speedAlertEnabled.value)
        assertEquals(110.0, viewModel.speedAlertThresholdKmh.value, 0.01)
        assertEquals(SpeedAlertStyle.VISUAL_AND_HAPTIC, viewModel.speedAlertStyle.value)

        // Change fuel unit and currency
        viewModel.setFuelUnit(FuelUnit.MPG_US)
        viewModel.setCurrencySymbol("£")
        advanceUntilIdle()
        assertEquals(FuelUnit.MPG_US, viewModel.fuelUnit.value)
        assertEquals("£", viewModel.currencySymbol.value)

        // Auto pause and keep screen on
        viewModel.setAutoPauseEnabled(false)
        viewModel.setKeepScreenOn(false)
        viewModel.setDefaultMapTheme(com.abrar.motolog.domain.model.MapThemePreference.LIGHT)
        advanceUntilIdle()
        assertFalse(viewModel.autoPauseEnabled.value)
        assertFalse(viewModel.keepScreenOn.value)
        assertEquals(com.abrar.motolog.domain.model.MapThemePreference.LIGHT, viewModel.defaultMapTheme.value)
    }

    @Test
    fun setVoiceAnnouncementsAndThermalEcoMode_updatesValues() = runTest(testDispatcher) {
        viewModel.voiceAnnouncementsEnabled.launchIn(backgroundScope)
        viewModel.thermalEcoMode.launchIn(backgroundScope)
        advanceUntilIdle()

        assertFalse(viewModel.voiceAnnouncementsEnabled.value)
        assertFalse(viewModel.thermalEcoMode.value)

        viewModel.setVoiceAnnouncementsEnabled(true)
        viewModel.setThermalEcoMode(true)
        advanceUntilIdle()

        assertTrue(viewModel.voiceAnnouncementsEnabled.value)
        assertTrue(viewModel.thermalEcoMode.value)
    }

    @Test
    fun activeBikes_emitsFromDao() = runTest(testDispatcher) {
        val bike1 = BikeEntity(id = 1L, name = "Honda Rebel 500")
        fakeBikeDao.insert(bike1)
        advanceUntilIdle()

        viewModel.activeBikes.test {
            var list = awaitItem()
            if (list.isEmpty()) {
                list = awaitItem()
            }
            assertEquals(1, list.size)
            assertEquals("Honda Rebel 500", list[0].name)
        }
    }

    private class FakeBikeDao : BikeDao {
        val bikes = mutableListOf<BikeEntity>()
        private val bikesFlow = MutableStateFlow<List<BikeEntity>>(emptyList())

        override suspend fun insert(bike: BikeEntity): Long {
            bikes.add(bike)
            bikesFlow.value = bikes.toList()
            return bike.id
        }
        override suspend fun update(bike: BikeEntity) {}
        override suspend fun delete(bike: BikeEntity) {
            bikes.remove(bike)
            bikesFlow.value = bikes.toList()
        }
        override fun getAllBikes(): Flow<List<BikeEntity>> = bikesFlow
        override fun getActiveBikes(): Flow<List<BikeEntity>> = bikesFlow
        override suspend fun getActiveBikesOnce(): List<BikeEntity> = bikes.toList()
        override fun getArchivedBikes(): Flow<List<BikeEntity>> = MutableStateFlow(emptyList())
        override suspend fun getBikeById(bikeId: Long): BikeEntity? = bikes.find { it.id == bikeId }
        override suspend fun getActiveBikeCount(): Int = bikes.size
        override suspend fun getRideCountForBike(bikeId: Long): Int = 0
        override suspend fun setArchived(bikeId: Long, isArchived: Boolean) {}
        override suspend fun updateOdometerOffset(bikeId: Long, offsetKm: Double) {}
        override suspend fun getAllBikesOnce(): List<BikeEntity> = bikes.toList()
        override suspend fun insertAll(bikesList: List<BikeEntity>) {
            bikes.addAll(bikesList)
            bikesFlow.value = bikes.toList()
        }
        override suspend fun deleteAllBikes() {
            bikes.clear()
            bikesFlow.value = emptyList()
        }
    }

    private class FakeRideDao : RideDao {
        val rides = mutableMapOf<Long, RideEntity>()
        override suspend fun insert(ride: RideEntity): Long { rides[ride.id] = ride; return ride.id }
        override suspend fun update(ride: RideEntity) { rides[ride.id] = ride }
        override suspend fun delete(ride: RideEntity) { rides.remove(ride.id) }
        override fun getAllRides(): Flow<List<RideEntity>> = MutableStateFlow(rides.values.toList())
        override suspend fun getRideById(rideId: Long): RideEntity? = rides[rideId]
        override suspend fun getRidesByStatus(status: RideStatus): List<RideEntity> = rides.values.filter { it.status == status }
        override fun getRidesForBike(bikeId: Long): Flow<List<RideEntity>> = MutableStateFlow(rides.values.filter { it.bikeId == bikeId })
        override suspend fun findActiveRide(): RideEntity? = rides.values.find { it.status == RideStatus.ACTIVE }
        override fun getFinishedRides(): Flow<List<RideEntity>> = MutableStateFlow(rides.values.toList())
        override suspend fun updateRideName(rideId: Long, name: String) {}
        override suspend fun updateRideBike(rideId: Long, bikeId: Long?) {}
        override fun getTotalDistanceMetersForBike(bikeId: Long): Flow<Double?> = MutableStateFlow(0.0)
        override suspend fun getTotalDistanceMetersForBikeOnce(bikeId: Long): Double? = 0.0
        override suspend fun findDuplicateRide(minStartTime: Long, maxStartTime: Long, minDistance: Double, maxDistance: Double): RideEntity? = null
        override suspend fun getAllRidesOnce(): List<RideEntity> = rides.values.toList()
        override suspend fun insertAll(ridesList: List<RideEntity>) { ridesList.forEach { rides[it.id] = it } }
        override suspend fun deleteAllRides() { rides.clear() }
    }

    private class FakeRidePointDao : RidePointDao {
        val points = mutableListOf<RidePointEntity>()
        override suspend fun insertAll(pointsList: List<RidePointEntity>) { points.addAll(pointsList) }
        override suspend fun insert(point: RidePointEntity): Long { points.add(point); return point.id }
        override fun getPointsForRide(rideId: Long): Flow<List<RidePointEntity>> = MutableStateFlow(points.filter { it.rideId == rideId })
        override suspend fun getPointsForRideOnce(rideId: Long): List<RidePointEntity> = points.filter { it.rideId == rideId }
        override suspend fun getPointCount(rideId: Long): Int = points.count { it.rideId == rideId }
        override suspend fun deletePointsForRide(rideId: Long) { points.removeAll { it.rideId == rideId } }
        override suspend fun getAllPointsOnce(): List<RidePointEntity> = points.toList()
        override suspend fun deleteAllPoints() { points.clear() }
    }

    private class FakeMaintenanceDao : MaintenanceDao {
        val items = mutableListOf<MaintenanceItemEntity>()
        override suspend fun insert(item: MaintenanceItemEntity): Long { items.add(item); return item.id }
        override suspend fun update(item: MaintenanceItemEntity) {}
        override suspend fun delete(item: MaintenanceItemEntity) { items.remove(item) }
        override fun getItemsForBike(bikeId: Long): Flow<List<MaintenanceItemEntity>> = MutableStateFlow(items.filter { it.bikeId == bikeId })
        override suspend fun getItemsForBikeOnce(bikeId: Long): List<MaintenanceItemEntity> = items.filter { it.bikeId == bikeId }
        override suspend fun getAllItemsOnce(): List<MaintenanceItemEntity> = items.toList()
        override suspend fun getItemById(id: Long): MaintenanceItemEntity? = items.find { it.id == id }
        override suspend fun markDone(id: Long, odometerKm: Double, dateEpochMs: Long) {}
        override suspend fun updateLastNotified(id: Long, notifiedEpochMs: Long) {}
        override suspend fun insertAll(itemsList: List<MaintenanceItemEntity>) { items.addAll(itemsList) }
        override suspend fun deleteAllMaintenance() { items.clear() }
    }

    private class FakeFuelLogDao : FuelLogDao {
        val logs = mutableListOf<FuelLogEntity>()
        override suspend fun insert(log: FuelLogEntity): Long { logs.add(log); return log.id }
        override suspend fun update(log: FuelLogEntity) {}
        override suspend fun delete(log: FuelLogEntity) { logs.remove(log) }
        override fun getLogsForBike(bikeId: Long): Flow<List<FuelLogEntity>> = MutableStateFlow(logs.filter { it.bikeId == bikeId })
        override suspend fun getLogsForBikeAscending(bikeId: Long): List<FuelLogEntity> = logs.filter { it.bikeId == bikeId }
        override suspend fun getLogById(id: Long): FuelLogEntity? = logs.find { it.id == id }
        override suspend fun getLatestLogForBike(bikeId: Long): FuelLogEntity? = logs.lastOrNull { it.bikeId == bikeId }
        override suspend fun getAllLogsOnce(): List<FuelLogEntity> = logs.toList()
        override suspend fun insertAll(logsList: List<FuelLogEntity>) { logs.addAll(logsList) }
        override suspend fun deleteAllFuelLogs() { logs.clear() }
    }
}
