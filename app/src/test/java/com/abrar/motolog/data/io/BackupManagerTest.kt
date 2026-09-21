package com.abrar.motolog.data.io

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var bikeDao: FakeBikeDao
    private lateinit var rideDao: FakeRideDao
    private lateinit var ridePointDao: FakeRidePointDao
    private lateinit var maintenanceDao: FakeMaintenanceDao
    private lateinit var fuelLogDao: FakeFuelLogDao
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        bikeDao = FakeBikeDao()
        rideDao = FakeRideDao()
        ridePointDao = FakeRidePointDao()
        maintenanceDao = FakeMaintenanceDao()
        fuelLogDao = FakeFuelLogDao()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_backup_settings.preferences_pb") }
        )
        settingsRepository = SettingsRepository(testDataStore)

        backupManager = BackupManager(
            bikeDao = bikeDao,
            rideDao = rideDao,
            ridePointDao = ridePointDao,
            maintenanceDao = maintenanceDao,
            fuelLogDao = fuelLogDao,
            settingsRepository = settingsRepository,
            database = null
        )
    }

    @Test
    fun createBackupAndInspect_returnsCorrectManifestAndSummary() = runTest(testDispatcher) {
        val bikes = listOf(BikeEntity(id = 1L, name = "Ninja 400", makeModel = "Kawasaki"))
        val rides = listOf(
            RideEntity(
                id = 10L,
                startTime = 1700000000000L,
                endTime = 1700003600000L,
                distanceMeters = 50000.0,
                elapsedTimeMs = 3600000L,
                movingTimeMs = 3000000L,
                avgMovingSpeedMs = 16.6,
                overallAvgSpeedMs = 13.8,
                maxSpeedMs = 25.0,
                name = "Weekend Loop",
                status = RideStatus.COMPLETED
            )
        )
        val points = listOf(
            RidePointEntity(1L, 10L, 1700000000000L, 12.9716, 77.5946, 20.0, 4f, 900.0, false, false)
        )

        bikeDao.insertAll(bikes)
        rideDao.insertAll(rides)
        ridePointDao.insertAll(points)

        settingsRepository.setUseMetricUnits(false)
        settingsRepository.setCurrencySymbol("€")
        settingsRepository.setSpeedAlertThresholdKmh(120.0)

        val out = ByteArrayOutputStream()
        backupManager.createBackup(out)

        val bytes = out.toByteArray()
        assertTrue(bytes.isNotEmpty())

        val inspectedManifest = backupManager.inspectBackup(ByteArrayInputStream(bytes))
        assertEquals(BackupManager.CURRENT_FORMAT_VERSION, inspectedManifest.formatVersion)
        assertEquals(1, inspectedManifest.bikeCount)
        assertEquals(1, inspectedManifest.rideCount)
        assertEquals(1, inspectedManifest.pointCount)
    }

    @Test
    fun restoreBackup_restoresAllEntitiesAndSettings() = runTest(testDispatcher) {
        val bikes = listOf(BikeEntity(id = 1L, name = "Ninja 400", makeModel = "Kawasaki"))
        val rides = listOf(
            RideEntity(
                id = 10L,
                startTime = 1700000000000L,
                endTime = 1700003600000L,
                distanceMeters = 50000.0,
                elapsedTimeMs = 3600000L,
                movingTimeMs = 3000000L,
                name = "Weekend Loop",
                status = RideStatus.COMPLETED
            )
        )
        val points = listOf(
            RidePointEntity(1L, 10L, 1700000000000L, 12.9716, 77.5946, 20.0, 4f, 900.0, false, false)
        )
        val maintenance = listOf(
            MaintenanceItemEntity(id = 1L, bikeId = 1L, name = "Oil Change", intervalKm = 5000.0)
        )
        val fuel = listOf(
            FuelLogEntity(id = 1L, bikeId = 1L, timestampEpochMs = 1700000000000L, odometerKm = 1500.0, litres = 10.0, totalCost = 20.0, isFullTank = true)
        )

        bikeDao.insertAll(bikes)
        rideDao.insertAll(rides)
        ridePointDao.insertAll(points)
        maintenanceDao.insertAll(maintenance)
        fuelLogDao.insertAll(fuel)

        settingsRepository.setUseMetricUnits(false)
        settingsRepository.setCurrencySymbol("£")
        settingsRepository.setTrackingMode(TrackingMode.BATTERY_SAVER)
        settingsRepository.setThemeMode(ThemeMode.AMOLED)

        val out = ByteArrayOutputStream()
        backupManager.createBackup(out)

        // Wipe all DAOs and reset settings
        bikeDao.deleteAllBikes()
        rideDao.deleteAllRides()
        ridePointDao.deleteAllPoints()
        maintenanceDao.deleteAllMaintenance()
        fuelLogDao.deleteAllFuelLogs()
        settingsRepository.setUseMetricUnits(true)
        settingsRepository.setCurrencySymbol("$")

        assertEquals(0, bikeDao.getAllBikesOnce().size)
        assertEquals(0, rideDao.getAllRidesOnce().size)

        // Restore
        val restoredManifest = backupManager.restoreBackup(ByteArrayInputStream(out.toByteArray()))
        assertEquals(1, restoredManifest.bikeCount)
        assertEquals(1, restoredManifest.rideCount)

        // Verify DAOs have restored data
        assertEquals(1, bikeDao.getAllBikesOnce().size)
        assertEquals("Ninja 400", bikeDao.getAllBikesOnce()[0].name)
        assertEquals(1, rideDao.getAllRidesOnce().size)
        assertEquals("Weekend Loop", rideDao.getAllRidesOnce()[0].name)
        assertEquals(1, ridePointDao.getAllPointsOnce().size)
        assertEquals(1, maintenanceDao.getAllItemsOnce().size)
        assertEquals(1, fuelLogDao.getAllLogsOnce().size)

        // Verify settings restored
        assertEquals(false, settingsRepository.useMetricUnits.first())
        assertEquals("£", settingsRepository.currencySymbol.first())
        assertEquals(TrackingMode.BATTERY_SAVER, settingsRepository.trackingMode.first())
        assertEquals(ThemeMode.AMOLED, settingsRepository.themeMode.first())
    }

    @Test
    fun inspectFutureVersion_isRejectedGracefully() = runTest(testDispatcher) {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            val futureManifestJson = """
                {
                    "formatVersion": 999,
                    "appVersion": "99.0",
                    "createdAtEpochMs": 1700000000000,
                    "bikeCount": 0,
                    "rideCount": 0,
                    "pointCount": 0,
                    "maintenanceCount": 0,
                    "fuelLogCount": 0
                }
            """.trimIndent()
            zip.write(futureManifestJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        var thrown = false
        try {
            backupManager.inspectBackup(ByteArrayInputStream(out.toByteArray()))
        } catch (e: IllegalArgumentException) {
            thrown = true
            assertTrue(e.message?.contains("newer version") == true)
        }
        assertTrue(thrown)
    }

    @Test
    fun inspectCorruptedFile_throwsError() = runTest(testDispatcher) {
        val corruptedBytes = "This is not a zip file".toByteArray(Charsets.UTF_8)
        var thrown = false
        try {
            backupManager.inspectBackup(ByteArrayInputStream(corruptedBytes))
        } catch (e: Exception) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun restoreActiveRide_throwsIllegalStateException() = runTest(testDispatcher) {
        rideDao.insert(
            RideEntity(
                id = 1L,
                startTime = 1000L,
                status = RideStatus.ACTIVE
            )
        )

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            val manifestJson = """
                {
                    "formatVersion": 1,
                    "appVersion": "1.0.0",
                    "createdAtEpochMs": 1700000000000,
                    "bikeCount": 0,
                    "rideCount": 0,
                    "pointCount": 0,
                    "maintenanceCount": 0,
                    "fuelLogCount": 0
                }
            """.trimIndent()
            zip.write(manifestJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        var thrown = false
        try {
            backupManager.restoreBackup(ByteArrayInputStream(out.toByteArray()))
        } catch (e: IllegalStateException) {
            thrown = true
            assertTrue(e.message?.contains("active") == true)
        }
        assertTrue(thrown)
    }

    private class FakeBikeDao : BikeDao {
        val bikes = mutableListOf<BikeEntity>()
        override suspend fun insert(bike: BikeEntity): Long { bikes.add(bike); return bike.id }
        override suspend fun update(bike: BikeEntity) {}
        override suspend fun delete(bike: BikeEntity) { bikes.remove(bike) }
        override fun getAllBikes(): Flow<List<BikeEntity>> = MutableStateFlow(bikes)
        override fun getActiveBikes(): Flow<List<BikeEntity>> = MutableStateFlow(bikes)
        override suspend fun getActiveBikesOnce(): List<BikeEntity> = bikes.toList()
        override fun getArchivedBikes(): Flow<List<BikeEntity>> = MutableStateFlow(emptyList())
        override suspend fun getBikeById(bikeId: Long): BikeEntity? = bikes.find { it.id == bikeId }
        override suspend fun getActiveBikeCount(): Int = bikes.size
        override suspend fun getRideCountForBike(bikeId: Long): Int = 0
        override suspend fun setArchived(bikeId: Long, isArchived: Boolean) {}
        override suspend fun updateOdometerOffset(bikeId: Long, offsetKm: Double) {}
        override suspend fun getAllBikesOnce(): List<BikeEntity> = bikes.toList()
        override suspend fun insertAll(bikesList: List<BikeEntity>) { bikes.addAll(bikesList) }
        override suspend fun deleteAllBikes() { bikes.clear() }
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
