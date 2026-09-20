package com.abrar.motolog.ui.history

import com.abrar.motolog.data.local.entity.RideEntity

/**
 * UI state for the History (ride list) screen.
 */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val rides: List<RideEntity> = emptyList(),
    val recentlyDeletedRide: RideEntity? = null
)
