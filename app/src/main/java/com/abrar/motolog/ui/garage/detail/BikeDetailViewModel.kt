package com.abrar.motolog.ui.garage.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.domain.repository.GarageRepository
import com.abrar.motolog.ui.navigation.BikeDetailRoute
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
class BikeDetailViewModel @Inject constructor(
    private val garageRepository: GarageRepository,
    private val bikeDao: BikeDao,
    private val rideDao: RideDao,
    private val settingsRepository: com.abrar.motolog.data.settings.SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bikeId: Long = runCatching {
        savedStateHandle.toRoute<BikeDetailRoute>().bikeId
    }.getOrElse {
        savedStateHandle.get<Long>("bikeId") ?: -1L
    }

    private val _dialogState = MutableStateFlow(DetailDialogState())

    private data class DetailDialogState(
        val selectedTab: Int = 0,
        val isSetOdometerDialogOpen: Boolean = false,
        val isEditBikeDialogOpen: Boolean = false,
        val isAddMaintenanceDialogOpen: Boolean = false,
        val itemToEdit: MaintenanceItemEntity? = null,
        val itemToDelete: MaintenanceItemEntity? = null,
        val itemToMarkDone: MaintenanceItemEntity? = null,
        val isAddFuelDialogOpen: Boolean = false,
        val fuelLogToEdit: FuelLogEntity? = null,
        val fuelLogToDelete: FuelLogEntity? = null
    )

    private data class BikeData(
        val bike: com.abrar.motolog.data.local.entity.BikeEntity?,
        val odo: Double,
        val maintenanceEvals: List<com.abrar.motolog.domain.model.MaintenanceEvaluation>,
        val fuelLogs: List<com.abrar.motolog.data.local.entity.FuelLogEntity>,
        val fuelStats: com.abrar.motolog.domain.model.FuelMileageStats
    )

    private val bikeDataFlow = combine(
        garageRepository.observeBike(bikeId),
        garageRepository.observeBikeOdometer(bikeId),
        garageRepository.observeMaintenanceEvaluations(bikeId),
        garageRepository.observeFuelLogs(bikeId),
        garageRepository.observeFuelStats(bikeId)
    ) { bike, odo, maintenanceEvals, fuelLogs, fuelStats ->
        BikeData(bike, odo, maintenanceEvals, fuelLogs, fuelStats)
    }

    private val _uiState = MutableStateFlow(BikeDetailUiState())
    val uiState: StateFlow<BikeDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bikeDataFlow,
                settingsRepository.useMetricUnits,
                settingsRepository.fuelUnit,
                settingsRepository.currencySymbol,
                _dialogState
            ) { data, useMetricUnits, fuelUnit, currencySymbol, dialogState ->
                val bike = data.bike
                val rideCount = if (bike != null) bikeDao.getRideCountForBike(bikeId) else 0
                val totalDistanceMeters = if (bike != null) rideDao.getTotalDistanceMetersForBikeOnce(bikeId) ?: 0.0 else 0.0

                BikeDetailUiState(
                    isLoading = false,
                    bike = bike,
                    currentOdometerKm = data.odo,
                    recordedRidesCount = rideCount,
                    recordedRidesDistanceKm = totalDistanceMeters / 1000.0,
                    maintenanceEvaluations = data.maintenanceEvals,
                    fuelLogs = data.fuelLogs,
                    fuelStats = data.fuelStats,
                    selectedTab = dialogState.selectedTab,
                    useMetricUnits = useMetricUnits,
                    fuelUnit = fuelUnit,
                    currencySymbol = currencySymbol,
                    isSetOdometerDialogOpen = dialogState.isSetOdometerDialogOpen,
                    isEditBikeDialogOpen = dialogState.isEditBikeDialogOpen,
                    isAddMaintenanceDialogOpen = dialogState.isAddMaintenanceDialogOpen,
                    itemToEdit = dialogState.itemToEdit,
                    itemToDelete = dialogState.itemToDelete,
                    itemToMarkDone = dialogState.itemToMarkDone,
                    isAddFuelDialogOpen = dialogState.isAddFuelDialogOpen,
                    fuelLogToEdit = dialogState.fuelLogToEdit,
                    fuelLogToDelete = dialogState.fuelLogToDelete
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun selectTab(index: Int) {
        _dialogState.update { it.copy(selectedTab = index) }
    }

    // Odometer
    fun openSetOdometerDialog() {
        _dialogState.update { it.copy(isSetOdometerDialogOpen = true) }
    }

    fun dismissSetOdometerDialog() {
        _dialogState.update { it.copy(isSetOdometerDialogOpen = false) }
    }

    fun setOdometer(targetOdoKm: Double) {
        viewModelScope.launch {
            garageRepository.setOdometer(bikeId, targetOdoKm)
            dismissSetOdometerDialog()
        }
    }

    // Bike Info Edit
    fun openEditBikeDialog() {
        _dialogState.update { it.copy(isEditBikeDialogOpen = true) }
    }

    fun dismissEditBikeDialog() {
        _dialogState.update { it.copy(isEditBikeDialogOpen = false) }
    }

    fun updateBike(name: String, makeModel: String, registrationNumber: String = "") {
        val currentBike = uiState.value.bike ?: return
        viewModelScope.launch {
            garageRepository.updateBike(
                currentBike.copy(
                    name = name.trim(),
                    makeModel = makeModel.trim(),
                    registrationNumber = registrationNumber.trim()
                )
            )
            dismissEditBikeDialog()
        }
    }

    // Maintenance
    fun openAddMaintenanceDialog() {
        _dialogState.update { it.copy(isAddMaintenanceDialogOpen = true) }
    }

    fun dismissAddMaintenanceDialog() {
        _dialogState.update { it.copy(isAddMaintenanceDialogOpen = false) }
    }

    fun addMaintenanceItem(name: String, intervalKm: Double?, intervalDays: Int?) {
        viewModelScope.launch {
            garageRepository.addMaintenanceItem(bikeId, name.trim(), intervalKm, intervalDays)
            dismissAddMaintenanceDialog()
        }
    }

    fun openEditMaintenanceDialog(item: MaintenanceItemEntity) {
        _dialogState.update { it.copy(itemToEdit = item) }
    }

    fun dismissEditMaintenanceDialog() {
        _dialogState.update { it.copy(itemToEdit = null) }
    }

    fun updateMaintenanceItem(item: MaintenanceItemEntity) {
        viewModelScope.launch {
            garageRepository.updateMaintenanceItem(item)
            dismissEditMaintenanceDialog()
        }
    }

    fun requestDeleteMaintenanceItem(item: MaintenanceItemEntity) {
        _dialogState.update { it.copy(itemToDelete = item) }
    }

    fun dismissDeleteMaintenanceDialog() {
        _dialogState.update { it.copy(itemToDelete = null) }
    }

    fun confirmDeleteMaintenanceItem() {
        val item = _dialogState.value.itemToDelete ?: return
        viewModelScope.launch {
            garageRepository.deleteMaintenanceItem(item)
            dismissDeleteMaintenanceDialog()
        }
    }

    fun requestMarkDone(item: MaintenanceItemEntity) {
        _dialogState.update { it.copy(itemToMarkDone = item) }
    }

    fun dismissMarkDoneDialog() {
        _dialogState.update { it.copy(itemToMarkDone = null) }
    }

    fun confirmMarkDone(itemId: Long, odometerKm: Double) {
        viewModelScope.launch {
            garageRepository.markMaintenanceDone(itemId, odometerKm)
            dismissMarkDoneDialog()
        }
    }

    // Fuel
    fun openAddFuelDialog() {
        _dialogState.update { it.copy(isAddFuelDialogOpen = true) }
    }

    fun dismissAddFuelDialog() {
        _dialogState.update { it.copy(isAddFuelDialogOpen = false) }
    }

    fun addFuelLog(
        odometerKm: Double,
        litres: Double,
        totalCost: Double,
        isFullTank: Boolean,
        notes: String,
        timestampEpochMs: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            garageRepository.addFuelLog(
                bikeId = bikeId,
                odometerKm = odometerKm,
                litres = litres,
                totalCost = totalCost,
                isFullTank = isFullTank,
                notes = notes.trim(),
                timestampEpochMs = timestampEpochMs
            )
            dismissAddFuelDialog()
        }
    }

    fun openEditFuelDialog(log: FuelLogEntity) {
        _dialogState.update { it.copy(fuelLogToEdit = log) }
    }

    fun dismissEditFuelDialog() {
        _dialogState.update { it.copy(fuelLogToEdit = null) }
    }

    fun updateFuelLog(log: FuelLogEntity) {
        viewModelScope.launch {
            garageRepository.updateFuelLog(log)
            dismissEditFuelDialog()
        }
    }

    fun requestDeleteFuelLog(log: FuelLogEntity) {
        _dialogState.update { it.copy(fuelLogToDelete = log) }
    }

    fun dismissDeleteFuelDialog() {
        _dialogState.update { it.copy(fuelLogToDelete = null) }
    }

    fun confirmDeleteFuelLog() {
        val log = _dialogState.value.fuelLogToDelete ?: return
        viewModelScope.launch {
            garageRepository.deleteFuelLog(log)
            dismissDeleteFuelDialog()
        }
    }
}
