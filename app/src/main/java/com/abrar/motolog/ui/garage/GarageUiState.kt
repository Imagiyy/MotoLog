package com.abrar.motolog.ui.garage

import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.domain.model.MaintenanceEvaluation
import com.abrar.motolog.domain.model.MaintenanceStatus

data class BikeListItem(
    val bike: BikeEntity,
    val currentOdometerKm: Double,
    val isCurrentBike: Boolean,
    val maintenanceEvaluations: List<MaintenanceEvaluation> = emptyList()
) {
    val overdueCount: Int get() = maintenanceEvaluations.count { it.status == MaintenanceStatus.OVERDUE }
    val dueSoonCount: Int get() = maintenanceEvaluations.count { it.status == MaintenanceStatus.DUE_SOON }
}

data class GarageUiState(
    val isLoading: Boolean = true,
    val bikes: List<BikeListItem> = emptyList(),
    val currentBikeId: Long? = null,
    val isAddBikeDialogOpen: Boolean = false,
    val bikeToArchiveOrDelete: BikeEntity? = null,
    val bikeHasRides: Boolean = false,
    val useMetricUnits: Boolean = true
)
