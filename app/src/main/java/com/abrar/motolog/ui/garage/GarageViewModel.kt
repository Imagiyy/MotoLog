package com.abrar.motolog.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.MaintenanceDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.domain.engine.evaluate
import com.abrar.motolog.shared.domain.engine.MaintenanceCalculator
import com.abrar.motolog.shared.domain.engine.OdometerCalculator
import com.abrar.motolog.domain.repository.GarageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val garageRepository: GarageRepository,
    private val bikeDao: BikeDao,
    private val rideDao: RideDao,
    private val maintenanceDao: MaintenanceDao,
    private val settingsRepository: com.abrar.motolog.data.settings.SettingsRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(DialogState())

    private data class DialogState(
        val isAddBikeDialogOpen: Boolean = false,
        val bikeToArchiveOrDelete: BikeEntity? = null,
        val bikeHasRides: Boolean = false
    )

    private val _uiState = MutableStateFlow(GarageUiState())
    val uiState: StateFlow<GarageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                garageRepository.activeBikes,
                garageRepository.currentBikeId,
                settingsRepository.useMetricUnits,
                _dialogState
            ) { bikes, currentBikeId, useMetricUnits, dialogState ->
                val now = System.currentTimeMillis()
                val bikeItems = bikes.map { bike ->
                    val totalDistanceMeters = rideDao.getTotalDistanceMetersForBikeOnce(bike.id) ?: 0.0
                    val currentOdo = OdometerCalculator.calculateCurrentOdometerKm(
                        initialOdometerKm = bike.initialOdometerKm,
                        recordedDistanceMeters = totalDistanceMeters,
                        odometerOffsetKm = bike.odometerOffsetKm
                    )
                    val items = maintenanceDao.getItemsForBikeOnce(bike.id)
                    val evaluations = items.map { item ->
                        MaintenanceCalculator.evaluate(item, currentOdo, now)
                    }
                    BikeListItem(
                        bike = bike,
                        currentOdometerKm = currentOdo,
                        isCurrentBike = bike.id == currentBikeId,
                        maintenanceEvaluations = evaluations
                    )
                }

                GarageUiState(
                    isLoading = false,
                    bikes = bikeItems,
                    currentBikeId = currentBikeId,
                    isAddBikeDialogOpen = dialogState.isAddBikeDialogOpen,
                    bikeToArchiveOrDelete = dialogState.bikeToArchiveOrDelete,
                    bikeHasRides = dialogState.bikeHasRides,
                    useMetricUnits = useMetricUnits
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun selectCurrentBike(bikeId: Long) {
        viewModelScope.launch {
            garageRepository.setCurrentBikeId(bikeId)
        }
    }

    fun openAddBikeDialog() {
        _dialogState.update { it.copy(isAddBikeDialogOpen = true) }
    }

    fun dismissAddBikeDialog() {
        _dialogState.update { it.copy(isAddBikeDialogOpen = false) }
    }

    fun addBike(
        name: String,
        makeModel: String,
        initialOdometerKm: Double,
        registrationNumber: String = ""
    ) {
        viewModelScope.launch {
            garageRepository.addBike(
                name = name.trim(),
                makeModel = makeModel.trim(),
                initialOdometerKm = initialOdometerKm,
                registrationNumber = registrationNumber.trim()
            )
            _dialogState.update { it.copy(isAddBikeDialogOpen = false) }
        }
    }

    fun requestArchiveOrDelete(bike: BikeEntity) {
        viewModelScope.launch {
            val rideCount = bikeDao.getRideCountForBike(bike.id)
            _dialogState.update {
                it.copy(
                    bikeToArchiveOrDelete = bike,
                    bikeHasRides = rideCount > 0
                )
            }
        }
    }

    fun dismissArchiveOrDeleteDialog() {
        _dialogState.update {
            it.copy(bikeToArchiveOrDelete = null, bikeHasRides = false)
        }
    }

    fun confirmArchiveOrDelete() {
        val bike = _dialogState.value.bikeToArchiveOrDelete ?: return
        viewModelScope.launch {
            garageRepository.archiveOrDeleteBike(bike.id)
            dismissArchiveOrDeleteDialog()
        }
    }
}
