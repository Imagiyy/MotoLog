package com.abrar.motolog.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrar.motolog.data.io.BackupManager
import com.abrar.motolog.data.io.BackupManifest
import com.abrar.motolog.data.io.CsvExporter
import com.abrar.motolog.data.io.GpxExporter
import com.abrar.motolog.data.io.GpxImporter
import com.abrar.motolog.data.local.MotoLogDatabase
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.engine.ElevationCalculator
import com.abrar.motolog.shared.domain.engine.RideCalculator
import com.abrar.motolog.shared.domain.engine.UnitConverter
import com.abrar.motolog.shared.domain.model.FuelUnit
import com.abrar.motolog.shared.domain.model.SpeedAlertStyle
import com.abrar.motolog.shared.domain.model.TrackingMode
import androidx.room.withTransaction
import com.abrar.motolog.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class PendingGpxImport(
    val uri: Uri,
    val tracks: List<GpxImporter.ParsedTrack>,
    val isDuplicateDetected: Boolean = false,
    val warnings: List<String> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val bikeDao: BikeDao,
    private val rideDao: RideDao,
    private val ridePointDao: RidePointDao,
    private val backupManager: BackupManager,
    @ApplicationContext private val context: Context? = null,
    private val database: MotoLogDatabase? = null
) : ViewModel() {

    // ============================================================
    // Settings StateFlows
    // ============================================================

    val useMetricUnits: StateFlow<Boolean> = settingsRepository.useMetricUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    val fuelUnit: StateFlow<FuelUnit> = settingsRepository.fuelUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), FuelUnit.KM_PER_LITER)

    val currencySymbol: StateFlow<String> = settingsRepository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), "$")

    val trackingMode: StateFlow<TrackingMode> = settingsRepository.trackingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), TrackingMode.HIGH_ACCURACY)

    val speedAlertEnabled: StateFlow<Boolean> = settingsRepository.speedAlertEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    val speedAlertThresholdKmh: StateFlow<Double> = settingsRepository.speedAlertThresholdKmh
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), TrackingConstants.SPEED_ALERT_DEFAULT_THRESHOLD_KMH)

    val speedAlertStyle: StateFlow<SpeedAlertStyle> = settingsRepository.speedAlertStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SpeedAlertStyle.ALL)

    val autoPauseEnabled: StateFlow<Boolean> = settingsRepository.autoPauseEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    val keepScreenOn: StateFlow<Boolean> = settingsRepository.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ThemeMode.RACING_TFT)

    val defaultMapTheme: StateFlow<com.abrar.motolog.shared.domain.model.MapThemePreference> = settingsRepository.defaultMapTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), com.abrar.motolog.shared.domain.model.MapThemePreference.LIGHT)

    val dashboardTextColor: StateFlow<Long> = settingsRepository.dashboardTextColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SettingsRepository.DEFAULT_DASHBOARD_TEXT_COLOR)

    val dashboardBgColor: StateFlow<Long> = settingsRepository.dashboardBgColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SettingsRepository.DEFAULT_DASHBOARD_BG_COLOR)

    val dashboardAccentColor: StateFlow<Long> = settingsRepository.dashboardAccentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SettingsRepository.DEFAULT_DASHBOARD_ACCENT_COLOR)

    val dashboardComponentColor: StateFlow<Long> = settingsRepository.dashboardComponentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SettingsRepository.DEFAULT_DASHBOARD_COMPONENT_COLOR)

    val voiceAnnouncementsEnabled: StateFlow<Boolean> = settingsRepository.voiceAnnouncementsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    val thermalEcoMode: StateFlow<Boolean> = settingsRepository.thermalEcoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    val activeBikes: StateFlow<List<BikeEntity>> = bikeDao.getActiveBikes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val routingApiKey: StateFlow<String> = settingsRepository.routingApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), "")

    val routingProviderUrl: StateFlow<String> = settingsRepository.routingProviderUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), SettingsRepository.DEFAULT_ROUTING_PROVIDER_URL)

    val routingAutoRerouteEnabled: StateFlow<Boolean> = settingsRepository.routingAutoRerouteEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    // UI Dialog & Snack State
    private val _messageChannel = Channel<String>(Channel.BUFFERED)
    val messageFlow = _messageChannel.receiveAsFlow()

    private val _pendingImport = MutableStateFlow<PendingGpxImport?>(null)
    val pendingImport: StateFlow<PendingGpxImport?> = _pendingImport.asStateFlow()

    private val _pendingRestoreManifest = MutableStateFlow<Pair<Uri, BackupManifest>?>(null)
    val pendingRestoreManifest: StateFlow<Pair<Uri, BackupManifest>?> = _pendingRestoreManifest.asStateFlow()

    // ============================================================
    // Setting Mutators
    // ============================================================

    fun setUseMetricUnits(metric: Boolean) {
        viewModelScope.launch { settingsRepository.setUseMetricUnits(metric) }
    }

    fun setFuelUnit(unit: FuelUnit) {
        viewModelScope.launch { settingsRepository.setFuelUnit(unit) }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch { settingsRepository.setCurrencySymbol(symbol) }
    }

    fun setTrackingMode(mode: TrackingMode) {
        viewModelScope.launch { settingsRepository.setTrackingMode(mode) }
    }

    fun setSpeedAlertEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSpeedAlertEnabled(enabled) }
    }

    fun setSpeedAlertThresholdUser(userValue: Double, isMetric: Boolean) {
        val kmh = UnitConverter.userSpeedToKmh(userValue, isMetric)
        viewModelScope.launch { settingsRepository.setSpeedAlertThresholdKmh(kmh) }
    }

    fun setSpeedAlertStyle(style: SpeedAlertStyle) {
        viewModelScope.launch { settingsRepository.setSpeedAlertStyle(style) }
    }

    fun setAutoPauseEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPauseEnabled(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    fun setRoutingApiKey(apiKey: String) {
        viewModelScope.launch { settingsRepository.setRoutingApiKey(apiKey) }
    }

    fun setRoutingProviderUrl(url: String) {
        viewModelScope.launch { settingsRepository.setRoutingProviderUrl(url) }
    }

    fun setRoutingAutoRerouteEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRoutingAutoRerouteEnabled(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDefaultMapTheme(pref: com.abrar.motolog.shared.domain.model.MapThemePreference) {
        viewModelScope.launch { settingsRepository.setDefaultMapTheme(pref) }
    }

    fun setDashboardTextColor(color: Long) {
        viewModelScope.launch { settingsRepository.setDashboardTextColor(color) }
    }

    fun setDashboardBgColor(color: Long) {
        viewModelScope.launch { settingsRepository.setDashboardBgColor(color) }
    }

    fun setDashboardAccentColor(color: Long) {
        viewModelScope.launch { settingsRepository.setDashboardAccentColor(color) }
    }

    fun setDashboardComponentColor(color: Long) {
        viewModelScope.launch { settingsRepository.setDashboardComponentColor(color) }
    }

    fun resetDashboardColors() {
        viewModelScope.launch { settingsRepository.resetDashboardColors() }
    }

    fun setVoiceAnnouncementsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVoiceAnnouncementsEnabled(enabled) }
    }

    fun setThermalEcoMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setThermalEcoMode(enabled) }
    }

    // ============================================================
    // GPX & CSV Export
    // ============================================================

    fun exportAllRidesGpx(uri: Uri) {
        val ctx = context ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rides = rideDao.getAllRidesOnce()
                ctx.contentResolver.openOutputStream(uri)?.use { stream ->
                    GpxExporter.exportRidesStreaming(
                        rides = rides,
                        pointsProvider = { rideId -> ridePointDao.getPointsForRideOnce(rideId) },
                        outputStream = stream
                    )
                }
                _messageChannel.send("Exported ${rides.size} rides to GPX successfully.")
            } catch (e: Exception) {
                _messageChannel.send("GPX Export failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun exportRidesSummaryCsv(uri: Uri) {
        val ctx = context ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rides = rideDao.getAllRidesOnce()
                ctx.contentResolver.openOutputStream(uri)?.use { stream ->
                    CsvExporter.exportRidesSummary(rides, stream)
                }
                _messageChannel.send("Exported ${rides.size} rides to CSV successfully.")
            } catch (e: Exception) {
                _messageChannel.send("CSV Export failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun shareAllRidesGpx() {
        val ctx = context ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rides = rideDao.getAllRidesOnce()
                val exportDir = File(ctx.cacheDir, "exports").apply { mkdirs() }
                val exportFile = File(exportDir, "motolog_all_rides.gpx")

                FileOutputStream(exportFile).use { stream ->
                    GpxExporter.exportRidesStreaming(
                        rides = rides,
                        pointsProvider = { rideId -> ridePointDao.getPointsForRideOnce(rideId) },
                        outputStream = stream
                    )
                }

                val contentUri = FileProvider.getUriForFile(
                    ctx.applicationContext,
                    "${ctx.applicationContext.packageName}.fileprovider",
                    exportFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    clipData = android.content.ClipData.newRawUri("motolog_all_rides.gpx", contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_SUBJECT, "MotoLog All Rides Export")
                    putExtra(Intent.EXTRA_TEXT, "Export of all motorcycle rides from MotoLog.")
                }

                val chooser = Intent.createChooser(shareIntent, "Share GPX Export").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                withContext(Dispatchers.Main) {
                    ctx.startActivity(chooser)
                }
            } catch (e: Exception) {
                _messageChannel.send("Share failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    // ============================================================
    // GPX Import Flow
    // ============================================================

    fun prepareGpxImport(uri: Uri) {
        val ctx = context ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parseResult = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    GpxImporter.parse(stream)
                } ?: throw IllegalArgumentException("Could not open file.")

                if (parseResult.errors.isNotEmpty()) {
                    _messageChannel.send("Import error: ${parseResult.errors.first()}")
                    return@launch
                }

                if (parseResult.tracks.isEmpty()) {
                    _messageChannel.send("No valid GPS tracks found in file.")
                    return@launch
                }

                // Check for potential duplicates
                var duplicateFound = false
                for (track in parseResult.tracks) {
                    val firstPtTime = track.points.firstOrNull()?.timestampEpochMs
                    if (firstPtTime != null && track.hasTimestamps) {
                        val stats = RideCalculator.processAll(track.points)
                        val match = rideDao.findDuplicateRide(
                            minStartTime = firstPtTime - 60_000L,
                            maxStartTime = firstPtTime + 60_000L,
                            minDistance = stats.totalDistanceMeters * 0.95,
                            maxDistance = stats.totalDistanceMeters * 1.05
                        )
                        if (match != null) {
                            duplicateFound = true
                            break
                        }
                    }
                }

                _pendingImport.value = PendingGpxImport(
                    uri = uri,
                    tracks = parseResult.tracks,
                    isDuplicateDetected = duplicateFound,
                    warnings = parseResult.warnings
                )
            } catch (e: Exception) {
                _messageChannel.send("Import preparation failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun dismissImportDialog() {
        _pendingImport.value = null
    }

    fun executeImport(bikeId: Long?, countsTowardOdometer: Boolean) {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val importBlock: suspend () -> Int = {
                    var importedCount = 0
                    val createdRideIds = mutableListOf<Long>()
                    try {
                        for (track in pending.tracks) {
                            val stats = RideCalculator.processAll(track.points)
                            val startTime = track.points.firstOrNull()?.timestampEpochMs ?: System.currentTimeMillis()
                            val endTime = track.points.lastOrNull()?.timestampEpochMs ?: (startTime + stats.elapsedTimeMs)

                            val rawElevations = track.points.mapNotNull { it.altitudeMeters }
                            val elevResult = if (rawElevations.size >= 2) {
                                ElevationCalculator.calculate(rawElevations, TrackingConstants.ELEVATION_MIN_CHANGE_GPS_METERS)
                            } else null

                            val ride = RideEntity(
                                bikeId = bikeId,
                                name = track.name.ifBlank { "Imported Ride" },
                                startTime = startTime,
                                endTime = endTime,
                                distanceMeters = stats.totalDistanceMeters,
                                elapsedTimeMs = stats.elapsedTimeMs,
                                movingTimeMs = stats.movingTimeMs,
                                avgMovingSpeedMs = stats.avgMovingSpeedKmh / 3.6,
                                overallAvgSpeedMs = stats.avgOverallSpeedKmh / 3.6,
                                maxSpeedMs = stats.maxSpeedKmh / 3.6,
                                elevationGainMeters = elevResult?.gainMeters ?: 0.0,
                                elevationLossMeters = elevResult?.lossMeters ?: 0.0,
                                elevationSource = if (elevResult != null) "gps" else "",
                                status = RideStatus.COMPLETED,
                                isImported = true,
                                countsTowardOdometer = countsTowardOdometer
                            )

                            val rideId = rideDao.insert(ride)
                            createdRideIds.add(rideId)

                            val pointEntities = track.points.map { pt ->
                                RidePointEntity(
                                    rideId = rideId,
                                    timestamp = pt.timestampEpochMs,
                                    latitude = pt.latitude,
                                    longitude = pt.longitude,
                                    speedMs = pt.speedMps?.toDouble() ?: 0.0,
                                    accuracyMeters = pt.accuracyMeters,
                                    altitudeMeters = pt.altitudeMeters ?: 0.0,
                                    isPaused = pt.isPaused,
                                    isGap = pt.isGap
                                )
                            }

                            ridePointDao.insertAll(pointEntities)
                            importedCount++
                        }
                        importedCount
                    } catch (e: Exception) {
                        for (id in createdRideIds) {
                            try {
                                ridePointDao.deletePointsForRide(id)
                                rideDao.getRideById(id)?.let { rideDao.delete(it) }
                            } catch (_: Exception) {}
                        }
                        throw e
                    }
                }

                val count = if (database != null) {
                    database.withTransaction { importBlock() }
                } else {
                    importBlock()
                }

                _messageChannel.send("Successfully imported $count ride(s).")
            } catch (e: Exception) {
                _messageChannel.send("Failed to save imported rides: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    // ============================================================
    // Backup & Restore
    // ============================================================

    fun createBackup(uri: Uri) {
        val ctx = context ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { stream ->
                    backupManager.createBackup(stream)
                }
                _messageChannel.send("Backup archive created successfully.")
            } catch (e: Exception) {
                _messageChannel.send("Backup creation failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun prepareRestore(uri: Uri) {
        val ctx = context ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val manifest = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    backupManager.inspectBackup(stream)
                } ?: throw IllegalArgumentException("Could not read backup file.")

                _pendingRestoreManifest.value = uri to manifest
            } catch (e: Exception) {
                _messageChannel.send("Restore inspection failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun dismissRestoreDialog() {
        _pendingRestoreManifest.value = null
    }

    fun confirmRestore() {
        val pending = _pendingRestoreManifest.value ?: return
        val uri = pending.first
        _pendingRestoreManifest.value = null

        val ctx = context ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val restoredManifest = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    backupManager.restoreBackup(stream)
                } ?: throw IllegalArgumentException("Could not open restore archive.")

                _messageChannel.send(
                    "Restore complete: ${restoredManifest.rideCount} rides, ${restoredManifest.bikeCount} bikes restored."
                )
            } catch (e: Exception) {
                _messageChannel.send("Restore failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}
