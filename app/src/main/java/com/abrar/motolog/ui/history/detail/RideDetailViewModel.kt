package com.abrar.motolog.ui.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.map.MapLibreMapProvider
import com.abrar.motolog.data.map.OpenFreeMapStyleProvider
import com.abrar.motolog.domain.engine.RouteMapPreparer
import com.abrar.motolog.domain.engine.SplitCalculator
import com.abrar.motolog.domain.repository.RideRepository
import com.abrar.motolog.domain.map.MapStyleProvider
import com.abrar.motolog.domain.map.MapProvider
import com.abrar.motolog.domain.util.RideNameGenerator
import com.abrar.motolog.ui.navigation.RideDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.abrar.motolog.data.io.CsvExporter
import com.abrar.motolog.data.io.GpxExporter
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.engine.UnitConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.io.FileOutputStream

/**
 * ViewModel for the Ride Detail screen.
 *
 * Loads the RideEntity summary from Room and computes distance splits off
 * the main thread on [Dispatchers.Default] to ensure 60/120fps UI responsiveness.
 */
@HiltViewModel
class RideDetailViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val rideDao: RideDao,
    private val bikeDao: BikeDao,
    savedStateHandle: SavedStateHandle,
    private val mapProvider: MapProvider,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    constructor(
        rideRepository: RideRepository,
        rideDao: RideDao,
        bikeDao: BikeDao,
        savedStateHandle: SavedStateHandle
    ) : this(
        rideRepository,
        rideDao,
        bikeDao,
        savedStateHandle,
        MapLibreMapProvider(OpenFreeMapStyleProvider()),
        null
    )

    val mapStyleProvider: MapStyleProvider
        get() = mapProvider.styleProvider

    internal var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    val useMetricUnits: StateFlow<Boolean> = settingsRepository?.useMetricUnits
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)
        ?: MutableStateFlow(true).asStateFlow()

    val defaultMapTheme: StateFlow<com.abrar.motolog.domain.model.MapThemePreference> = settingsRepository?.defaultMapTheme
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), com.abrar.motolog.domain.model.MapThemePreference.DARK)
        ?: MutableStateFlow(com.abrar.motolog.domain.model.MapThemePreference.DARK).asStateFlow()

    private val rideId: Long = runCatching {
        savedStateHandle.toRoute<RideDetailRoute>().rideId
    }.getOrElse {
        savedStateHandle.get<Long>("rideId") ?: -1L
    }

    private val _uiState = MutableStateFlow(RideDetailUiState())
    val uiState: StateFlow<RideDetailUiState> = _uiState.asStateFlow()

    private var cachedGpsPoints: List<com.abrar.motolog.domain.model.GpsPoint> = emptyList()

    init {
        loadRideDetail()
    }

    fun loadRideDetail() {
        if (rideId <= 0L) {
            _uiState.update { it.copy(isLoading = false, isSplitsLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSplitsLoading = true, isVisualsLoading = true) }
            val ride = rideRepository.getRideById(rideId)
            val assignedBike = ride?.bikeId?.let { bikeDao.getBikeById(it) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    ride = ride,
                    assignedBike = assignedBike,
                    renameCandidateName = ride?.name.orEmpty()
                )
            }

            val visuals = withContext(defaultDispatcher) {
                val points = rideRepository.getPointsForRide(rideId)
                cachedGpsPoints = points
                val entities = points.map { point ->
                    RidePointEntity(
                        rideId = rideId,
                        timestamp = point.timestampEpochMs,
                        latitude = point.latitude,
                        longitude = point.longitude,
                        speedMs = point.speedMps?.toDouble() ?: 0.0,
                        accuracyMeters = point.accuracyMeters,
                        altitudeMeters = point.altitudeMeters ?: 0.0,
                        isPaused = point.isPaused,
                        isGap = point.isGap
                    )
                }
                val isMetric = settingsRepository?.useMetricUnits?.firstOrNull() ?: true
                val baseDistance = if (isMetric) 1000.0 else UnitConverter.METERS_PER_MILE
                val splitDistanceMeters = baseDistance * _uiState.value.selectedSplitInterval.distanceMultiplier

                Pair(
                    SplitCalculator.computeSplits(points, splitDistanceMeters = splitDistanceMeters),
                    RouteMapPreparer.prepare(entities)
                )
            }

            _uiState.update {
                it.copy(
                    isSplitsLoading = false,
                    isVisualsLoading = false,
                    splits = visuals.first,
                    routeMap = visuals.second,
                    speedGraph = null,
                    elevationGraph = null
                )
            }
        }
    }

    fun setSplitInterval(interval: SplitInterval) {
        _uiState.update { it.copy(selectedSplitInterval = interval) }
        val points = cachedGpsPoints
        if (points.isEmpty()) return
        viewModelScope.launch(defaultDispatcher) {
            val isMetric = settingsRepository?.useMetricUnits?.firstOrNull() ?: true
            val baseDistance = if (isMetric) 1000.0 else UnitConverter.METERS_PER_MILE
            val splitDistanceMeters = baseDistance * interval.distanceMultiplier
            val splits = SplitCalculator.computeSplits(points, splitDistanceMeters = splitDistanceMeters)
            _uiState.update { it.copy(splits = splits) }
        }
    }

    fun toggleStatsMode() {
        _uiState.update { it.copy(showOverallStats = !it.showOverallStats) }
    }

    fun openRenameDialog() {
        val currentName = _uiState.value.ride?.name.orEmpty()
        _uiState.update {
            it.copy(
                isRenameDialogOpen = true,
                renameCandidateName = currentName
            )
        }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(isRenameDialogOpen = false) }
    }

    fun onRenameCandidateChanged(name: String) {
        _uiState.update { it.copy(renameCandidateName = name) }
    }

    fun confirmRename() {
        val currentRide = _uiState.value.ride ?: return
        val defaultName = RideNameGenerator.defaultNameForTimestamp(currentRide.startTime)
        val sanitized = RideNameGenerator.sanitizeRideName(
            name = _uiState.value.renameCandidateName,
            fallbackDefaultName = defaultName,
            maxLength = 50
        )

        viewModelScope.launch {
            rideRepository.renameRide(currentRide.id, sanitized)
            val updatedRide = currentRide.copy(name = sanitized)
            _uiState.update {
                it.copy(
                    ride = updatedRide,
                    isRenameDialogOpen = false,
                    renameCandidateName = sanitized
                )
            }
        }
    }

    fun openChangeBikeDialog() {
        viewModelScope.launch {
            val bikes = bikeDao.getActiveBikesOnce()
            _uiState.update {
                it.copy(
                    isChangeBikeDialogOpen = true,
                    availableBikes = bikes
                )
            }
        }
    }

    fun dismissChangeBikeDialog() {
        _uiState.update { it.copy(isChangeBikeDialogOpen = false) }
    }

    fun selectBikeForRide(newBikeId: Long?) {
        viewModelScope.launch {
            rideDao.updateRideBike(rideId, newBikeId)
            val updatedBike = newBikeId?.let { bikeDao.getBikeById(it) }
            val updatedRide = _uiState.value.ride?.copy(bikeId = newBikeId)
            _uiState.update {
                it.copy(
                    ride = updatedRide,
                    assignedBike = updatedBike,
                    isChangeBikeDialogOpen = false
                )
            }
        }
    }

    fun exportRideGpx(context: Context, uri: Uri) {
        val currentRide = _uiState.value.ride ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val points = rideRepository.getPointsForRide(currentRide.id)
                val entities = points.map { pt ->
                    RidePointEntity(
                        rideId = currentRide.id,
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
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    GpxExporter.exportRide(currentRide, entities, stream)
                }
            } catch (_: Exception) {}
        }
    }

    fun exportRideCsv(context: Context, uri: Uri) {
        val currentRide = _uiState.value.ride ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val points = rideRepository.getPointsForRide(currentRide.id)
                val entities = points.map { pt ->
                    RidePointEntity(
                        rideId = currentRide.id,
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
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    CsvExporter.exportRidePoints(entities, stream)
                }
            } catch (_: Exception) {}
        }
    }

    fun shareRideGpx(context: Context) {
        val currentRide = _uiState.value.ride ?: return
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val points = rideRepository.getPointsForRide(currentRide.id)
                val entities = points.map { pt ->
                    RidePointEntity(
                        rideId = currentRide.id,
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

                val exportDir = File(appContext.cacheDir, "exports").apply { mkdirs() }
                val exportFile = File(exportDir, "ride_${currentRide.id}.gpx")

                FileOutputStream(exportFile).use { stream ->
                    GpxExporter.exportRide(currentRide, entities, stream)
                }

                val contentUri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    exportFile
                )

                val rideName = currentRide.name.ifBlank { "Motorcycle Ride" }
                val totalDistKm = currentRide.distanceMeters / 1000.0
                val maxSpeedKmh = currentRide.maxSpeedMs * 3.6
                val shareSummary = "$rideName • %.1f km • Max %.1f km/h".format(
                    java.util.Locale.US,
                    totalDistKm,
                    maxSpeedKmh
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    clipData = android.content.ClipData.newRawUri("ride_${currentRide.id}.gpx", contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_SUBJECT, rideName)
                    putExtra(Intent.EXTRA_TEXT, shareSummary)
                }

                val chooser = Intent.createChooser(shareIntent, "Share GPX Track").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Failed to share GPX: ${e.localizedMessage ?: "Unknown error"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
