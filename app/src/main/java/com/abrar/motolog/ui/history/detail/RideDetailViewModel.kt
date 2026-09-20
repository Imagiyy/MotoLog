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
import com.abrar.motolog.domain.engine.GraphDataPreparer
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
    private val mapProvider: MapProvider
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
        MapLibreMapProvider(OpenFreeMapStyleProvider())
    )

    val mapStyleProvider: MapStyleProvider
        get() = mapProvider.styleProvider

    internal var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val rideId: Long = runCatching {
        savedStateHandle.toRoute<RideDetailRoute>().rideId
    }.getOrElse {
        savedStateHandle.get<Long>("rideId") ?: -1L
    }

    private val _uiState = MutableStateFlow(RideDetailUiState())
    val uiState: StateFlow<RideDetailUiState> = _uiState.asStateFlow()

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
                Triple(
                    SplitCalculator.computeSplits(points),
                    RouteMapPreparer.prepare(entities),
                    Pair(
                        GraphDataPreparer.prepareSpeedGraph(entities, ride?.startTime ?: 0L),
                        GraphDataPreparer.prepareElevationGraph(entities, ride?.startTime ?: 0L)
                    )
                )
            }

            _uiState.update {
                it.copy(
                    isSplitsLoading = false,
                    isVisualsLoading = false,
                    splits = visuals.first,
                    routeMap = visuals.second,
                    speedGraph = visuals.third.first,
                    elevationGraph = visuals.third.second
                )
            }
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
}
