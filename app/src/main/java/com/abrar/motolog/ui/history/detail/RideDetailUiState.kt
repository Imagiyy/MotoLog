package com.abrar.motolog.ui.history.detail

import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.domain.model.RideSplit
import com.abrar.motolog.domain.model.GraphData
import com.abrar.motolog.domain.model.RouteMapData

/**
 * Supported distance split intervals for ride breakdown analysis.
 */
enum class SplitInterval(val distanceMultiplier: Int, val labelKm: String, val labelMi: String) {
    SPLIT_1KM(1, "1 km", "1 mi"),
    SPLIT_10KM(10, "10 km", "10 mi"),
    SPLIT_100KM(100, "100 km", "100 mi")
}

/**
 * UI state for the Ride Detail screen.
 */
data class RideDetailUiState(
    val isLoading: Boolean = true,
    val ride: RideEntity? = null,
    val assignedBike: BikeEntity? = null,
    val availableBikes: List<BikeEntity> = emptyList(),
    val isChangeBikeDialogOpen: Boolean = false,
    val isSplitsLoading: Boolean = true,
    val splits: List<RideSplit> = emptyList(),
    val selectedSplitInterval: SplitInterval = SplitInterval.SPLIT_1KM,
    val routeMap: RouteMapData? = null,
    val speedGraph: GraphData? = null,
    val elevationGraph: GraphData? = null,
    val isVisualsLoading: Boolean = true,
    val showOverallStats: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val renameCandidateName: String = ""
)
