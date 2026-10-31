package com.abrar.motolog.ui.destination

import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteAvoidance
import com.abrar.motolog.shared.domain.model.RouteProfile

/**
 * UI state for the Destination Search and Route Planning screen.
 */
data class DestinationUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<Destination> = emptyList(),
    val savedDestinations: List<Destination> = emptyList(),
    val selectedDestination: Destination? = null,
    val isCalculatingRoute: Boolean = false,
    val routeCandidates: List<PlannedRoute> = emptyList(),
    val selectedRouteIndex: Int = 0,
    val travelProfile: RouteProfile = RouteProfile.MOTORCYCLE,
    val avoidances: Set<RouteAvoidance> = emptySet(),
    val requestAlternatives: Boolean = true,
    val errorMessage: String? = null,
    val isApiKeyMissing: Boolean = false,
    val isMapPinMode: Boolean = false,
    val mapCenterLatitude: Double = 0.0,
    val mapCenterLongitude: Double = 0.0,
    val isReverseGeocoding: Boolean = false,
    val currentLocation: LocationPoint? = null,
    val useMetricUnits: Boolean = true,
    val navigationStarted: Boolean = false
) {
    val selectedRoute: PlannedRoute?
        get() = routeCandidates.getOrNull(selectedRouteIndex) ?: routeCandidates.firstOrNull()

    val hasRoutePreview: Boolean
        get() = routeCandidates.isNotEmpty()
}
