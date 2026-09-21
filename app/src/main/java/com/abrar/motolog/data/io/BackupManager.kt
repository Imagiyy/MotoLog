package com.abrar.motolog.data.io

import androidx.room.withTransaction
import com.abrar.motolog.data.local.MotoLogDatabase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val createdAtEpochMs: Long,
    val bikeCount: Int,
    val rideCount: Int,
    val pointCount: Int,
    val maintenanceCount: Int,
    val fuelLogCount: Int
)

@Serializable
data class BackupSettings(
    val useMetricUnits: Boolean = true,
    val fuelUnit: String = "KM_PER_LITER",
    val currencySymbol: String = "$",
    val trackingMode: String = "HIGH_ACCURACY",
    val speedAlertEnabled: Boolean = false,
    val speedAlertThresholdKmh: Double = 100.0,
    val speedAlertStyle: String = "ALL",
    val themeMode: String = "DARK",
    val autoPauseEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val currentBikeId: Long? = null
)

/**
 * Manages full backup and atomic restore of MotoLog data to/from ZIP archives.
 */
@Singleton
class BackupManager @Inject constructor(
    private val bikeDao: BikeDao,
    private val rideDao: RideDao,
    private val ridePointDao: RidePointDao,
    private val maintenanceDao: MaintenanceDao,
    private val fuelLogDao: FuelLogDao,
    private val settingsRepository: SettingsRepository,
    private val database: MotoLogDatabase? = null
) {
    companion object {
        const val CURRENT_FORMAT_VERSION: Int = 1
        const val APP_VERSION: String = "1.0.0"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }

    /**
     * Creates a ZIP backup containing all database tables and settings.
     */
    suspend fun createBackup(
        outputStream: OutputStream,
        onProgress: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val bikes = bikeDao.getAllBikesOnce()
        val rides = rideDao.getAllRidesOnce()
        val points = ridePointDao.getAllPointsOnce()
        val maintenance = maintenanceDao.getAllItemsOnce()
        val fuel = fuelLogDao.getAllLogsOnce()

        val manifest = BackupManifest(
            formatVersion = CURRENT_FORMAT_VERSION,
            appVersion = APP_VERSION,
            createdAtEpochMs = System.currentTimeMillis(),
            bikeCount = bikes.size,
            rideCount = rides.size,
            pointCount = points.size,
            maintenanceCount = maintenance.size,
            fuelLogCount = fuel.size
        )

        val settings = BackupSettings(
            useMetricUnits = settingsRepository.useMetricUnits.first(),
            fuelUnit = settingsRepository.fuelUnit.first().name,
            currencySymbol = settingsRepository.currencySymbol.first(),
            trackingMode = settingsRepository.trackingMode.first().name,
            speedAlertEnabled = settingsRepository.speedAlertEnabled.first(),
            speedAlertThresholdKmh = settingsRepository.speedAlertThresholdKmh.first(),
            speedAlertStyle = settingsRepository.speedAlertStyle.first().name,
            themeMode = settingsRepository.themeMode.first().name,
            autoPauseEnabled = settingsRepository.autoPauseEnabled.first(),
            keepScreenOn = settingsRepository.keepScreenOn.first(),
            currentBikeId = settingsRepository.currentBikeId.first()
        )

        ZipOutputStream(outputStream).use { zip ->
            // 1. Manifest
            writeZipEntry(zip, "manifest.json", json.encodeToString(manifest))
            onProgress?.invoke(0.15f)

            // 2. Settings
            writeZipEntry(zip, "settings.json", json.encodeToString(settings))
            onProgress?.invoke(0.25f)

            // 3. Bikes
            writeZipEntry(zip, "bikes.json", json.encodeToString(bikes))
            onProgress?.invoke(0.40f)

            // 4. Rides
            writeZipEntry(zip, "rides.json", json.encodeToString(rides))
            onProgress?.invoke(0.60f)

            // 5. Points
            writeZipEntry(zip, "points.json", json.encodeToString(points))
            onProgress?.invoke(0.85f)

            // 6. Maintenance
            writeZipEntry(zip, "maintenance.json", json.encodeToString(maintenance))
            onProgress?.invoke(0.92f)

            // 7. Fuel
            writeZipEntry(zip, "fuel.json", json.encodeToString(fuel))
            onProgress?.invoke(1.0f)
        }
    }

    /**
     * Inspects a backup file and returns its manifest without modifying the database.
     */
    suspend fun inspectBackup(inputStream: InputStream): BackupManifest = withContext(Dispatchers.IO) {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "manifest.json") {
                    val bytes = zip.readBytes()
                    val manifestStr = String(bytes, StandardCharsets.UTF_8)
                    val manifest = json.decodeFromString<BackupManifest>(manifestStr)
                    if (manifest.formatVersion > CURRENT_FORMAT_VERSION) {
                        throw IllegalArgumentException(
                            "Backup was created with a newer version of MotoLog (format v${manifest.formatVersion}). Please update the app first."
                        )
                    }
                    return@withContext manifest
                }
                entry = zip.nextEntry
            }
        }
        throw IllegalArgumentException("Invalid backup file: manifest.json is missing.")
    }

    /**
     * Atomically restores database and settings from a backup file.
     * Replaces existing data upon explicit confirmation.
     */
    suspend fun restoreBackup(inputStream: InputStream): BackupManifest = withContext(Dispatchers.IO) {
        // 1. Ensure no ride is actively recording
        val activeRide = rideDao.findActiveRide()
        if (activeRide != null) {
            throw IllegalStateException("Cannot restore data while a ride is active or recording.")
        }

        // 2. Read entries from ZIP
        var manifest: BackupManifest? = null
        var settings: BackupSettings? = null
        var bikes: List<BikeEntity>? = null
        var rides: List<RideEntity>? = null
        var points: List<RidePointEntity>? = null
        var maintenance: List<MaintenanceItemEntity>? = null
        var fuel: List<FuelLogEntity>? = null

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val content = String(zip.readBytes(), StandardCharsets.UTF_8)
                when (entry.name) {
                    "manifest.json" -> manifest = json.decodeFromString(content)
                    "settings.json" -> settings = json.decodeFromString(content)
                    "bikes.json" -> bikes = json.decodeFromString(content)
                    "rides.json" -> rides = json.decodeFromString(content)
                    "points.json" -> points = json.decodeFromString(content)
                    "maintenance.json" -> maintenance = json.decodeFromString(content)
                    "fuel.json" -> fuel = json.decodeFromString(content)
                }
                entry = zip.nextEntry
            }
        }

        val parsedManifest = manifest ?: throw IllegalArgumentException("Missing manifest.json in backup.")
        if (parsedManifest.formatVersion > CURRENT_FORMAT_VERSION) {
            throw IllegalArgumentException("Backup format v${parsedManifest.formatVersion} is newer than supported v$CURRENT_FORMAT_VERSION.")
        }

        // 3. Atomically replace database contents
        val executeRestore: suspend () -> Unit = {
            ridePointDao.deleteAllPoints()
            rideDao.deleteAllRides()
            maintenanceDao.deleteAllMaintenance()
            fuelLogDao.deleteAllFuelLogs()
            bikeDao.deleteAllBikes()

            bikes?.let { if (it.isNotEmpty()) bikeDao.insertAll(it) }
            rides?.let { if (it.isNotEmpty()) rideDao.insertAll(it) }
            points?.let { if (it.isNotEmpty()) ridePointDao.insertAll(it) }
            maintenance?.let { if (it.isNotEmpty()) maintenanceDao.insertAll(it) }
            fuel?.let { if (it.isNotEmpty()) fuelLogDao.insertAll(it) }
        }

        if (database != null) {
            database.withTransaction { executeRestore() }
        } else {
            executeRestore()
        }

        // 4. Restore settings
        settings?.let { s ->
            settingsRepository.setUseMetricUnits(s.useMetricUnits)
            try { settingsRepository.setFuelUnit(FuelUnit.valueOf(s.fuelUnit)) } catch (_: Exception) {}
            settingsRepository.setCurrencySymbol(s.currencySymbol)
            try { settingsRepository.setTrackingMode(TrackingMode.valueOf(s.trackingMode)) } catch (_: Exception) {}
            settingsRepository.setSpeedAlertEnabled(s.speedAlertEnabled)
            settingsRepository.setSpeedAlertThresholdKmh(s.speedAlertThresholdKmh)
            try { settingsRepository.setSpeedAlertStyle(SpeedAlertStyle.valueOf(s.speedAlertStyle)) } catch (_: Exception) {}
            try { settingsRepository.setThemeMode(ThemeMode.valueOf(s.themeMode)) } catch (_: Exception) {}
            settingsRepository.setAutoPauseEnabled(s.autoPauseEnabled)
            settingsRepository.setKeepScreenOn(s.keepScreenOn)
            settingsRepository.setCurrentBikeId(s.currentBikeId)
        }

        parsedManifest
    }

    private fun writeZipEntry(zip: ZipOutputStream, entryName: String, data: String) {
        val entry = ZipEntry(entryName)
        zip.putNextEntry(entry)
        zip.write(data.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}
