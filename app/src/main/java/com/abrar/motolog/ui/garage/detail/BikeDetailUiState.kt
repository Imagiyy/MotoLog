package com.abrar.motolog.ui.garage.detail

import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.domain.model.FuelMileageStats
import com.abrar.motolog.domain.model.MaintenanceEvaluation

data class BikeDetailUiState(
    val isLoading: Boolean = true,
    val bike: BikeEntity? = null,
    val currentOdometerKm: Double = 0.0,
    val recordedRidesCount: Int = 0,
    val recordedRidesDistanceKm: Double = 0.0,
    val maintenanceEvaluations: List<MaintenanceEvaluation> = emptyList(),
    val fuelLogs: List<FuelLogEntity> = emptyList(),
    val fuelStats: FuelMileageStats = FuelMileageStats(),
    val selectedTab: Int = 0, // 0 = Maintenance, 1 = Fuel
    val useMetricUnits: Boolean = true,
    val fuelUnit: com.abrar.motolog.domain.model.FuelUnit = com.abrar.motolog.domain.model.FuelUnit.KM_PER_LITER,
    val currencySymbol: String = "$",

    // Dialogs
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
