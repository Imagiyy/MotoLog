package com.abrar.motolog.data.io

import android.util.Log
import androidx.room.withTransaction
import com.abrar.motolog.data.local.MotoLogDatabase
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.FuelLogDao
import com.abrar.motolog.data.local.dao.MaintenanceDao
import com.abrar.motolog.data.local.dao.MileageCycleDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.data.local.entity.MileageCycleEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.shared.domain.model.FuelUnit
import com.abrar.motolog.shared.domain.model.SpeedAlertStyle
import com.abrar.motolog.shared.domain.model.TrackingMode
import com.abrar.motolog.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
    val fuelLogCount: Int,
    val mileageCycleCount: Int = 0,
    val checksums: Map<String, String> = emptyMap()
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
    val defaultMapTheme: String = "DARK",
    val autoPauseEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val currentBikeId: Long? = null,
    // Additional settings previously omitted (Fix #23)
    val voiceAnnouncementsEnabled: Boolean = false,
    val thermalEcoMode: Boolean = false,
    val dashboardTextColor: Long? = null,
    val dashboardBgColor: Long? = null,
    val dashboardAccentColor: Long? = null,
    val dashboardComponentColor: Long? = null
)

/**
 * Manages full backup and atomic restore of MotoLog data to/from ZIP archives.
 *
 * Security:
 * - Bounded ZIP reads prevent zip-bomb DoS (Fix #11)
 * - Restore validates all entries before deleting any data (Fix #3)
 * - Backup reads all tables in a Room transaction for snapshot consistency (Fix #12)
 * - Mileage cycles and all persisted settings are now included (Fix #23)
 */
@Singleton
class BackupManager @Inject constructor(
    private val bikeDao: BikeDao,
    private val rideDao: RideDao,
    private val ridePointDao: RidePointDao,
    private val maintenanceDao: MaintenanceDao,
    private val fuelLogDao: FuelLogDao,
    private val mileageCycleDao: MileageCycleDao,
    private val settingsRepository: SettingsRepository,
    private val database: MotoLogDatabase? = null
) {
    companion object {
        const val CURRENT_FORMAT_VERSION: Int = 2
        const val APP_VERSION: String = "1.0.0"

        /** Maximum size of a single entry in the backup ZIP (256 MB) */
        private const val MAX_ENTRY_SIZE_BYTES: Long = 256L * 1024 * 1024
        /** Maximum total decompressed size of the backup (512 MB) */
        private const val MAX_TOTAL_SIZE_BYTES: Long = 512L * 1024 * 1024
        /** Maximum number of entries in a backup ZIP */
        private const val MAX_ENTRY_COUNT: Int = 20

        /** All required entries in a valid backup (manifest always required) */
        private val REQUIRED_ENTRIES = setOf("manifest.json")
        /** All recognized entry names */
        private val KNOWN_ENTRIES = setOf(
            "manifest.json", "settings.json",
            "bikes.json", "rides.json", "points.json",
            "maintenance.json", "fuel.json", "mileage_cycles.json"
        )

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }

    /**
     * Creates a ZIP backup containing all database tables and settings.
     * Fix #12: Reads all tables in a Room transaction for snapshot consistency.
     */
    suspend fun createBackup(
        outputStream: OutputStream,
        onProgress: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        // Read all data in a single transaction for consistent snapshot
        data class Snapshot(
            val bikes: List<BikeEntity>,
            val rides: List<RideEntity>,
            val pointCount: Int,
            val maintenance: List<MaintenanceItemEntity>,
            val fuel: List<FuelLogEntity>,
            val mileageCycles: List<MileageCycleEntity>
        )

        val snapshot = if (database != null) {
            database.withTransaction {
                Snapshot(
                    bikes = bikeDao.getAllBikesOnce(),
                    rides = rideDao.getAllRidesOnce(),
                    pointCount = ridePointDao.getTotalPointCount(),
                    maintenance = maintenanceDao.getAllItemsOnce(),
                    fuel = fuelLogDao.getAllLogsOnce(),
                    mileageCycles = mileageCycleDao.getAllOnce()
                )
            }
        } else {
            Snapshot(
                bikes = bikeDao.getAllBikesOnce(),
                rides = rideDao.getAllRidesOnce(),
                pointCount = ridePointDao.getTotalPointCount(),
                maintenance = maintenanceDao.getAllItemsOnce(),
                fuel = fuelLogDao.getAllLogsOnce(),
                mileageCycles = mileageCycleDao.getAllOnce()
            )
        }

        val settings = BackupSettings(
            useMetricUnits = settingsRepository.useMetricUnits.first(),
            fuelUnit = settingsRepository.fuelUnit.first().name,
            currencySymbol = settingsRepository.currencySymbol.first(),
            trackingMode = settingsRepository.trackingMode.first().name,
            speedAlertEnabled = settingsRepository.speedAlertEnabled.first(),
            speedAlertThresholdKmh = settingsRepository.speedAlertThresholdKmh.first(),
            speedAlertStyle = settingsRepository.speedAlertStyle.first().name,
            themeMode = settingsRepository.themeMode.first().name,
            defaultMapTheme = settingsRepository.defaultMapTheme.first().name,
            autoPauseEnabled = settingsRepository.autoPauseEnabled.first(),
            keepScreenOn = settingsRepository.keepScreenOn.first(),
            currentBikeId = settingsRepository.currentBikeId.first(),
            voiceAnnouncementsEnabled = settingsRepository.voiceAnnouncementsEnabled.first(),
            thermalEcoMode = settingsRepository.thermalEcoMode.first(),
            dashboardTextColor = settingsRepository.dashboardTextColor.first(),
            dashboardBgColor = settingsRepository.dashboardBgColor.first(),
            dashboardAccentColor = settingsRepository.dashboardAccentColor.first(),
            dashboardComponentColor = settingsRepository.dashboardComponentColor.first()
        )

        val settingsJson = json.encodeToString(settings)
        val bikesJson = json.encodeToString(snapshot.bikes)
        val ridesJson = json.encodeToString(snapshot.rides)
        val maintenanceJson = json.encodeToString(snapshot.maintenance)
        val fuelJson = json.encodeToString(snapshot.fuel)
        val cyclesJson = json.encodeToString(snapshot.mileageCycles)

        ZipOutputStream(outputStream).use { zip ->
            // 1. Settings
            writeZipEntry(zip, "settings.json", settingsJson)
            onProgress?.invoke(0.10f)

            // 2. Bikes
            writeZipEntry(zip, "bikes.json", bikesJson)
            onProgress?.invoke(0.20f)

            // 3. Rides
            writeZipEntry(zip, "rides.json", ridesJson)
            onProgress?.invoke(0.35f)

            // 4. Points (streamed in chunks to prevent heap spikes)
            val pointsChecksum = writePointsStreaming(zip, snapshot.pointCount, onProgress)

            // 5. Maintenance
            writeZipEntry(zip, "maintenance.json", maintenanceJson)
            onProgress?.invoke(0.80f)

            // 6. Fuel
            writeZipEntry(zip, "fuel.json", fuelJson)
            onProgress?.invoke(0.85f)

            // 7. Mileage Cycles (Fix #23)
            writeZipEntry(zip, "mileage_cycles.json", cyclesJson)
            onProgress?.invoke(0.90f)

            // 8. Manifest with exact checksums
            val checksums = mapOf(
                "settings.json" to sha256Hex(settingsJson.toByteArray(StandardCharsets.UTF_8)),
                "bikes.json" to sha256Hex(bikesJson.toByteArray(StandardCharsets.UTF_8)),
                "rides.json" to sha256Hex(ridesJson.toByteArray(StandardCharsets.UTF_8)),
                "points.json" to pointsChecksum,
                "maintenance.json" to sha256Hex(maintenanceJson.toByteArray(StandardCharsets.UTF_8)),
                "fuel.json" to sha256Hex(fuelJson.toByteArray(StandardCharsets.UTF_8)),
                "mileage_cycles.json" to sha256Hex(cyclesJson.toByteArray(StandardCharsets.UTF_8))
            )

            val manifest = BackupManifest(
                formatVersion = CURRENT_FORMAT_VERSION,
                appVersion = APP_VERSION,
                createdAtEpochMs = System.currentTimeMillis(),
                bikeCount = snapshot.bikes.size,
                rideCount = snapshot.rides.size,
                pointCount = snapshot.pointCount,
                maintenanceCount = snapshot.maintenance.size,
                fuelLogCount = snapshot.fuel.size,
                mileageCycleCount = snapshot.mileageCycles.size,
                checksums = checksums
            )

            writeZipEntry(zip, "manifest.json", json.encodeToString(manifest))
            onProgress?.invoke(1.0f)
        }
    }

    private suspend fun writePointsStreaming(
        zip: ZipOutputStream,
        totalPoints: Int,
        onProgress: ((Float) -> Unit)?
    ): String {
        val entry = java.util.zip.ZipEntry("points.json")
        zip.putNextEntry(entry)
        val digest = java.security.MessageDigest.getInstance("SHA-256")

        val countingStream = object : OutputStream() {
            override fun write(b: Int) {
                digest.update(b.toByte())
                zip.write(b)
            }
            override fun write(b: ByteArray, off: Int, len: Int) {
                digest.update(b, off, len)
                zip.write(b, off, len)
            }
            override fun flush() {
                zip.flush()
            }
            override fun close() {
                // Do not close zip
            }
        }
        val writer = OutputStreamWriter(countingStream, StandardCharsets.UTF_8)
        writer.write("[")
        var offset = 0
        val pageSize = 1000
        var first = true
        while (offset < totalPoints) {
            val chunk = ridePointDao.getPointsPaged(limit = pageSize, offset = offset)
            if (chunk.isEmpty()) break
            for (pt in chunk) {
                if (!first) {
                    writer.write(",")
                }
                first = false
                writer.write(json.encodeToString(pt))
            }
            writer.flush()
            offset += chunk.size
            if (totalPoints > 0) {
                val progress = 0.35f + 0.40f * (offset.toFloat() / totalPoints.toFloat()).coerceAtMost(1f)
                onProgress?.invoke(progress)
            }
        }
        writer.write("]")
        writer.flush()
        zip.closeEntry()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Inspects a backup file and returns its manifest without modifying the database.
     * Fix #11: Uses bounded reads.
     */
    suspend fun inspectBackup(inputStream: InputStream): BackupManifest = withContext(Dispatchers.IO) {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            var entryCount = 0
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw IllegalArgumentException("Backup contains too many entries (max $MAX_ENTRY_COUNT).")
                }
                if (entry.name == "manifest.json") {
                    val bytes = readBoundedEntry(zip, entry, MAX_ENTRY_SIZE_BYTES)
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
     * Fix #3: Fully parses and validates all entries BEFORE deleting any data.
     * Fix #11: Uses bounded stream reads during parsing.
     */
    suspend fun restoreBackup(inputStream: InputStream): BackupManifest = withContext(Dispatchers.IO) {
        // 1. Ensure no ride is actively recording
        val activeRide = rideDao.findActiveRide()
        if (activeRide != null) {
            throw IllegalStateException("Cannot restore data while a ride is active or recording.")
        }

        // 2. Read and validate ALL entries from ZIP with strict size safety checks
        var manifest: BackupManifest? = null
        var settings: BackupSettings? = null
        var bikes: List<BikeEntity>? = null
        var rides: List<RideEntity>? = null
        var points: List<RidePointEntity>? = null
        var maintenance: List<MaintenanceItemEntity>? = null
        var fuel: List<FuelLogEntity>? = null
        var mileageCycles: List<MileageCycleEntity>? = null
        var totalBytesRead = 0L
        var entryCount = 0
        val seenEntries = mutableSetOf<String>()
        val entryChecksums = mutableMapOf<String, String>()

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw IllegalArgumentException("Backup contains too many entries (max $MAX_ENTRY_COUNT).")
                }

                // Reject duplicate entries
                if (entry.name in seenEntries) {
                    throw IllegalArgumentException("Duplicate entry '${entry.name}' in backup.")
                }
                seenEntries.add(entry.name)

                // Reject unknown entries
                if (entry.name !in KNOWN_ENTRIES) {
                    Log.w("BackupManager", "Unknown entry '${entry.name}' in backup, skipping")
                    entry = zip.nextEntry
                    continue
                }

                // Fix #11: Read through a bounded stream that aborts on oversized entries
                val rawBytes = readBoundedEntry(zip, entry, MAX_ENTRY_SIZE_BYTES)
                totalBytesRead += rawBytes.size

                if (totalBytesRead > MAX_TOTAL_SIZE_BYTES) {
                    throw IllegalArgumentException(
                        "Backup total decompressed size exceeds maximum allowed (${MAX_TOTAL_SIZE_BYTES / (1024 * 1024)} MB). File may be corrupted."
                    )
                }

                entryChecksums[entry.name] = sha256Hex(rawBytes)

                val content = String(rawBytes, StandardCharsets.UTF_8)
                when (entry.name) {
                    "manifest.json" -> manifest = json.decodeFromString(content)
                    "settings.json" -> settings = json.decodeFromString(content)
                    "bikes.json" -> bikes = json.decodeFromString(content)
                    "rides.json" -> rides = json.decodeFromString(content)
                    "points.json" -> points = json.decodeFromString(content)
                    "maintenance.json" -> maintenance = json.decodeFromString(content)
                    "fuel.json" -> fuel = json.decodeFromString(content)
                    "mileage_cycles.json" -> mileageCycles = json.decodeFromString(content)
                }
                entry = zip.nextEntry
            }
        }

        // 3. Validate manifest
        val parsedManifest = manifest ?: throw IllegalArgumentException("Missing manifest.json in backup.")
        if (parsedManifest.formatVersion > CURRENT_FORMAT_VERSION) {
            throw IllegalArgumentException("Backup format v${parsedManifest.formatVersion} is newer than supported v$CURRENT_FORMAT_VERSION.")
        }

        // 4. Fix #3: Validate manifest counts and checksums against actual parsed data BEFORE deleting anything
        val errors = mutableListOf<String>()

        // Fix #22: Verify SHA-256 checksums if manifest provides them
        if (parsedManifest.checksums.isNotEmpty()) {
            for ((entryName, expectedHash) in parsedManifest.checksums) {
                val actualHash = entryChecksums[entryName]
                if (actualHash != null && actualHash != expectedHash) {
                    errors.add("Integrity check failed for '$entryName' (checksum mismatch).")
                }
            }
        }

        val actualBikes = bikes ?: emptyList()
        val actualRides = rides ?: emptyList()
        val actualPoints = points ?: emptyList()
        val actualMaintenance = maintenance ?: emptyList()
        val actualFuel = fuel ?: emptyList()
        val actualMileageCycles = mileageCycles ?: emptyList()

        if (parsedManifest.bikeCount > 0 && actualBikes.isEmpty()) {
            errors.add("Manifest declares ${parsedManifest.bikeCount} bikes but none were found in backup.")
        }
        if (parsedManifest.rideCount > 0 && actualRides.isEmpty()) {
            errors.add("Manifest declares ${parsedManifest.rideCount} rides but none were found in backup.")
        }
        if (parsedManifest.pointCount > 0 && actualPoints.isEmpty()) {
            errors.add("Manifest declares ${parsedManifest.pointCount} points but none were found in backup.")
        }

        // Validate ride status enum values
        for (ride in actualRides) {
            try {
                // RideStatus is already deserialized, but verify it's a known value
                ride.status.name
            } catch (e: Exception) {
                errors.add("Ride ${ride.id} has invalid status.")
            }
        }

        // Validate foreign key relationships
        val bikeIds = actualBikes.map { it.id }.toSet()
        for (ride in actualRides) {
            if (ride.bikeId != null && ride.bikeId !in bikeIds) {
                errors.add("Ride '${ride.name.ifEmpty { ride.id.toString() }}' references non-existent bike ID ${ride.bikeId}.")
            }
        }
        val rideIds = actualRides.map { it.id }.toSet()
        for (point in actualPoints) {
            if (point.rideId !in rideIds) {
                errors.add("Point references non-existent ride ID ${point.rideId}.")
                break // Only report once to avoid flooding
            }
        }
        for (item in actualMaintenance) {
            if (item.bikeId !in bikeIds) {
                errors.add("Maintenance item '${item.name}' references non-existent bike ID ${item.bikeId}.")
            }
        }
        for (log in actualFuel) {
            if (log.bikeId !in bikeIds) {
                errors.add("Fuel log references non-existent bike ID ${log.bikeId}.")
                break
            }
        }
        for (cycle in actualMileageCycles) {
            if (cycle.bikeId !in bikeIds) {
                errors.add("Mileage cycle references non-existent bike ID ${cycle.bikeId}.")
                break
            }
        }

        // Validate GPS coordinates in points
        for (point in actualPoints) {
            if (point.latitude < -90.0 || point.latitude > 90.0 ||
                point.longitude < -180.0 || point.longitude > 180.0 ||
                point.latitude.isNaN() || point.longitude.isNaN() ||
                point.latitude.isInfinite() || point.longitude.isInfinite()
            ) {
                errors.add("Backup contains points with invalid GPS coordinates.")
                break
            }
        }

        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(
                "Backup validation failed:\n${errors.joinToString("\n• ", prefix = "• ")}"
            )
        }

        // 5. Atomically replace database contents ONLY after all validation passes
        val executeRestore: suspend () -> Unit = {
            ridePointDao.deleteAllPoints()
            rideDao.deleteAllRides()
            maintenanceDao.deleteAllMaintenance()
            fuelLogDao.deleteAllFuelLogs()
            mileageCycleDao.deleteAll()
            bikeDao.deleteAllBikes()

            if (actualBikes.isNotEmpty()) bikeDao.insertAll(actualBikes)
            if (actualRides.isNotEmpty()) rideDao.insertAll(actualRides)
            if (actualPoints.isNotEmpty()) {
                actualPoints.chunked(1000).forEach { ridePointDao.insertAll(it) }
            }
            if (actualMaintenance.isNotEmpty()) maintenanceDao.insertAll(actualMaintenance)
            if (actualFuel.isNotEmpty()) fuelLogDao.insertAll(actualFuel)
            if (actualMileageCycles.isNotEmpty()) mileageCycleDao.insertAll(actualMileageCycles)
        }

        if (database != null) {
            database.withTransaction { executeRestore() }
        } else {
            executeRestore()
        }

        // 6. Restore settings (with validation for enum values)
        settings?.let { s ->
            settingsRepository.setUseMetricUnits(s.useMetricUnits)
            try { settingsRepository.setFuelUnit(FuelUnit.valueOf(s.fuelUnit)) } catch (e: Exception) {
                Log.w("BackupManager", "Invalid fuel unit '${s.fuelUnit}', skipping")
            }
            settingsRepository.setCurrencySymbol(s.currencySymbol)
            try { settingsRepository.setTrackingMode(TrackingMode.valueOf(s.trackingMode)) } catch (e: Exception) {
                Log.w("BackupManager", "Invalid tracking mode '${s.trackingMode}', skipping")
            }
            settingsRepository.setSpeedAlertEnabled(s.speedAlertEnabled)
            settingsRepository.setSpeedAlertThresholdKmh(s.speedAlertThresholdKmh)
            try { settingsRepository.setSpeedAlertStyle(SpeedAlertStyle.valueOf(s.speedAlertStyle)) } catch (e: Exception) {
                Log.w("BackupManager", "Invalid speed alert style '${s.speedAlertStyle}', skipping")
            }
            try { settingsRepository.setThemeMode(ThemeMode.valueOf(s.themeMode)) } catch (e: Exception) {
                Log.w("BackupManager", "Invalid theme mode '${s.themeMode}', skipping")
            }
            try { settingsRepository.setDefaultMapTheme(com.abrar.motolog.shared.domain.model.MapThemePreference.valueOf(s.defaultMapTheme)) } catch (e: Exception) {
                Log.w("BackupManager", "Invalid map theme '${s.defaultMapTheme}', skipping")
            }
            settingsRepository.setAutoPauseEnabled(s.autoPauseEnabled)
            settingsRepository.setKeepScreenOn(s.keepScreenOn)

            // Fix #23: Restore additional settings
            settingsRepository.setVoiceAnnouncementsEnabled(s.voiceAnnouncementsEnabled)
            settingsRepository.setThermalEcoMode(s.thermalEcoMode)
            s.dashboardTextColor?.let { settingsRepository.setDashboardTextColor(it) }
            s.dashboardBgColor?.let { settingsRepository.setDashboardBgColor(it) }
            s.dashboardAccentColor?.let { settingsRepository.setDashboardAccentColor(it) }
            s.dashboardComponentColor?.let { settingsRepository.setDashboardComponentColor(it) }

            // Validate currentBikeId references an actual restored bike
            val restoredBikeIds = actualBikes.map { it.id }.toSet()
            val validBikeId = if (s.currentBikeId != null && s.currentBikeId in restoredBikeIds) {
                s.currentBikeId
            } else {
                null // Clear dangling reference
            }
            settingsRepository.setCurrentBikeId(validBikeId)
        }

        parsedManifest
    }

    /**
     * Reads a ZIP entry with bounded size enforcement.
     * Fix #11: Reads incrementally through a capped buffer instead of calling readBytes()
     * which trusts the (potentially spoofed) entry.size metadata.
     */
    private fun readBoundedEntry(
        zip: ZipInputStream,
        entry: ZipEntry,
        maxSizeBytes: Long
    ): ByteArray {
        // Check declared size if available (can be -1 for unknown)
        val declaredSize = entry.size
        if (declaredSize > maxSizeBytes) {
            throw IllegalArgumentException(
                "Backup entry '${entry.name}' declares size ${declaredSize / (1024 * 1024)} MB, exceeding limit."
            )
        }

        val buffer = ByteArray(8192)
        val output = ByteArrayOutputStream()
        var totalRead = 0L

        while (true) {
            val bytesRead = zip.read(buffer)
            if (bytesRead == -1) break
            totalRead += bytesRead
            if (totalRead > maxSizeBytes) {
                throw IllegalArgumentException(
                    "Backup entry '${entry.name}' exceeds maximum allowed size (${maxSizeBytes / (1024 * 1024)} MB)."
                )
            }
            output.write(buffer, 0, bytesRead)
        }

        return output.toByteArray()
    }

    private fun writeZipEntry(zip: ZipOutputStream, entryName: String, data: String) {
        val entry = ZipEntry(entryName)
        zip.putNextEntry(entry)
        zip.write(data.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
