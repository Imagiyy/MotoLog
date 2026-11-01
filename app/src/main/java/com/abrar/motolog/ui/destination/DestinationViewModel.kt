package com.abrar.motolog.ui.destination

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.repository.NavigationSessionRepository
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.shared.domain.map.MapStyleProvider
import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.DestinationSourceType
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.NavigationSession
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.RouteAvoidance
import com.abrar.motolog.shared.domain.model.RouteProfile
import com.abrar.motolog.shared.domain.model.RouteRequest
import com.abrar.motolog.shared.domain.model.RoutingError
import com.abrar.motolog.shared.domain.repository.GeocodingRepository
import com.abrar.motolog.shared.domain.repository.RoutingRepository
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import com.abrar.motolog.shared.domain.model.RoutingMode
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel orchestrating destination search, map-pin selection, route candidates calculation,
 * and navigation session activation.
 */
@HiltViewModel
class DestinationViewModel @Inject constructor(
    private val routingRepository: RoutingRepository,
    private val geocodingRepository: GeocodingRepository,
    private val navigationSessionRepository: NavigationSessionRepository,
    private val trackingRepository: TrackingRepository,
    private val settingsRepository: SettingsRepository,
    private val fusedLocationClient: FusedLocationProviderClient? = null,
    @ApplicationContext private val context: Context? = null,
    val mapStyleProvider: MapStyleProvider? = null
) : ViewModel() {

    constructor(
        routingRepository: RoutingRepository,
        geocodingRepository: GeocodingRepository,
        navigationSessionRepository: NavigationSessionRepository,
        trackingRepository: TrackingRepository,
        settingsRepository: SettingsRepository
    ) : this(
        routingRepository = routingRepository,
        geocodingRepository = geocodingRepository,
        navigationSessionRepository = navigationSessionRepository,
        trackingRepository = trackingRepository,
        settingsRepository = settingsRepository,
        fusedLocationClient = null,
        context = null,
        mapStyleProvider = null
    )

    private val _uiState = MutableStateFlow(DestinationUiState())
    val uiState: StateFlow<DestinationUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var roadDistanceJob: Job? = null

    val routingMode: StateFlow<RoutingMode> = settingsRepository.routingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), RoutingMode.GRAPH_HOPPER_CLOUD)

    val routingApiKey: StateFlow<String> = settingsRepository.routingApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), "")

    val useMetricUnits: StateFlow<Boolean> = settingsRepository.useMetricUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

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

    init {
        // Observe saved destinations
        viewModelScope.launch {
            navigationSessionRepository.getSavedDestinations().collect { savedList ->
                _uiState.update { it.copy(savedDestinations = savedList) }
            }
        }

        // Observe routing mode & API key state
        viewModelScope.launch {
            combine(routingMode, routingApiKey) { mode, key ->
                Pair(mode, key)
            }.collect { (mode, key) ->
                val missing = (mode == RoutingMode.GRAPH_HOPPER_CLOUD && key.isBlank())
                _uiState.update { it.copy(routingMode = mode, isApiKeyMissing = missing) }
            }
        }

        // Observe unit preference
        viewModelScope.launch {
            useMetricUnits.collect { metric ->
                _uiState.update { it.copy(useMetricUnits = metric) }
            }
        }

        // Observe tracking session location if active
        viewModelScope.launch {
            trackingRepository.sessionState.collect { sessionState ->
                if (sessionState is TrackingSessionState.Tracking) {
                    val lat = sessionState.latitude
                    val lon = sessionState.longitude
                    if (lat != null && lon != null) {
                        val point = LocationPoint(
                            latitude = lat,
                            longitude = lon,
                            accuracyMeters = sessionState.accuracyMeters,
                            timestamp = System.currentTimeMillis()
                        )
                        _uiState.update { current ->
                            current.copy(
                                currentLocation = point,
                                mapCenterLatitude = if (current.mapCenterLatitude == 0.0) lat else current.mapCenterLatitude,
                                mapCenterLongitude = if (current.mapCenterLongitude == 0.0) lon else current.mapCenterLongitude
                            )
                        }
                    }
                }
            }
        }

        fetchCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val ctx = context ?: return
        val client = fusedLocationClient ?: return

        val hasFine = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        applyFetchedLocation(loc)
                    } else {
                        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { curLoc ->
                                if (curLoc != null) {
                                    applyFetchedLocation(curLoc)
                                }
                            }
                    }
                }
            } catch (_: Exception) {
                // Ignore failure to fetch last known location
            }
        }
    }

    private fun applyFetchedLocation(loc: android.location.Location) {
        val point = LocationPoint(
            latitude = loc.latitude,
            longitude = loc.longitude,
            speedMps = if (loc.hasSpeed()) loc.speed else null,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else Float.MAX_VALUE,
            timestamp = if (loc.time > 0L) loc.time else System.currentTimeMillis(),
            altitudeMeters = if (loc.hasAltitude()) loc.altitude else null,
            elapsedTimestampMs = loc.elapsedRealtimeNanos / 1_000_000L
        )
        _uiState.update { current ->
            current.copy(
                currentLocation = point,
                mapCenterLatitude = if (current.mapCenterLatitude == 0.0) point.latitude else current.mapCenterLatitude,
                mapCenterLongitude = if (current.mapCenterLongitude == 0.0) point.longitude else current.mapCenterLongitude,
                errorMessage = if (current.errorMessage?.contains("location", ignoreCase = true) == true) null else current.errorMessage
            )
        }
        if (_uiState.value.selectedDestination != null && _uiState.value.routeCandidates.isEmpty() && !_uiState.value.isCalculatingRoute) {
            calculateRouteForSelectedDestination()
        }
        if (_uiState.value.searchResults.isNotEmpty() && _uiState.value.searchResults.any { it.roadDistanceMeters == null }) {
            fetchRoadDistances(point, _uiState.value.searchResults)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, errorMessage = null) }
        searchJob?.cancel()
        roadDistanceJob?.cancel()

        if (query.trim().isEmpty()) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300L)
            _uiState.update { it.copy(isSearching = true) }
            val currentLoc = _uiState.value.currentLocation
            val result = geocodingRepository.searchDestinations(
                query = query.trim(),
                nearLocation = currentLoc
            )
            result.fold(
                onSuccess = { destinations ->
                    _uiState.update { it.copy(isSearching = false, searchResults = destinations) }
                    if (currentLoc != null && destinations.isNotEmpty()) {
                        fetchRoadDistances(currentLoc, destinations)
                    }
                },
                onFailure = { error ->
                    val apiKeyMissing = error is RoutingError.ApiKeyMissing ||
                            (_uiState.value.routingMode == RoutingMode.GRAPH_HOPPER_CLOUD && routingApiKey.value.isBlank())
                    val msg = if (apiKeyMissing) {
                        "GraphHopper API key missing. Please enter your API key or switch to Keyless mode."
                    } else {
                        error.message ?: "Search failed. Check your network connection."
                    }
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            isApiKeyMissing = apiKeyMissing,
                            errorMessage = msg
                        )
                    }
                }
            )
        }
    }

    private fun fetchRoadDistances(origin: LocationPoint, destinations: List<Destination>) {
        roadDistanceJob?.cancel()
        roadDistanceJob = viewModelScope.launch {
            val destPoints = destinations.map {
                LocationPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = System.currentTimeMillis()
                )
            }
            val result = routingRepository.calculateRoadDistances(origin, destPoints)
            result.getOrNull()?.let { distances ->
                if (distances.size == destinations.size) {
                    _uiState.update { current ->
                        if (current.searchResults.map { it.id } == destinations.map { it.id }) {
                            val updated = current.searchResults.mapIndexed { index, dest ->
                                val roadDist = distances.getOrNull(index)
                                if (roadDist != null) dest.copy(roadDistanceMeters = roadDist) else dest
                            }
                            current.copy(searchResults = updated)
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    fun selectDestination(destination: Destination) {
        _uiState.update {
            it.copy(
                selectedDestination = destination,
                searchResults = emptyList(),
                isMapPinMode = false,
                errorMessage = null
            )
        }
        calculateRouteForSelectedDestination()
    }

    fun clearSelectedDestination() {
        _uiState.update {
            it.copy(
                selectedDestination = null,
                routeCandidates = emptyList(),
                selectedRouteIndex = 0,
                errorMessage = null
            )
        }
    }

    fun calculateRouteForSelectedDestination() {
        val dest = _uiState.value.selectedDestination ?: return
        val origin = _uiState.value.currentLocation
        if (origin == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Current GPS location unavailable. Please ensure location services are enabled."
                )
            }
            fetchCurrentLocation()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCalculatingRoute = true, errorMessage = null) }
            val request = RouteRequest(
                origin = origin,
                destination = LocationPoint(
                    latitude = dest.latitude,
                    longitude = dest.longitude,
                    accuracyMeters = 0f,
                    timestamp = System.currentTimeMillis()
                ),
                travelProfile = _uiState.value.travelProfile,
                avoidances = _uiState.value.avoidances,
                alternatives = _uiState.value.requestAlternatives,
                requestTimestamp = System.currentTimeMillis()
            )

            val result = routingRepository.calculateRoute(request)
            result.fold(
                onSuccess = { routes ->
                    _uiState.update {
                        it.copy(
                            isCalculatingRoute = false,
                            routeCandidates = routes,
                            selectedRouteIndex = 0,
                            errorMessage = if (routes.isEmpty()) "No route found to destination" else null
                        )
                    }
                },
                onFailure = { error ->
                    val isSelfHostedIssue = _uiState.value.routingMode == RoutingMode.KEYLESS_GRAPH_HOPPER &&
                            (error.message?.contains("self-hosted", ignoreCase = true) == true ||
                             error.message?.contains("localhost", ignoreCase = true) == true ||
                             error is RoutingError.NetworkError)
                    val apiKeyMissing = error is RoutingError.ApiKeyMissing ||
                            (_uiState.value.routingMode == RoutingMode.GRAPH_HOPPER_CLOUD && routingApiKey.value.isBlank()) ||
                            isSelfHostedIssue
                    val msg = when {
                        isSelfHostedIssue ->
                            "Cannot connect to local server (localhost:8989). Please enter your GraphHopper Cloud API key below to calculate the route."
                        apiKeyMissing -> "GraphHopper API key is missing. Please enter your API key or switch to Keyless mode."
                        error is RoutingError.NoRouteFound -> "No route found between your location and the destination."
                        error is RoutingError.RateLimitExceeded -> "Routing rate limit exceeded. Please wait a moment and try again."
                        error is RoutingError.NetworkError -> error.message ?: "Network connection error while calculating route."
                        else -> error.message ?: "Failed to calculate route."
                    }
                    _uiState.update {
                        it.copy(
                            isCalculatingRoute = false,
                            routeCandidates = emptyList(),
                            isApiKeyMissing = apiKeyMissing,
                            errorMessage = msg
                        )
                    }
                }
            )
        }
    }

    fun setTravelProfile(profile: RouteProfile) {
        if (_uiState.value.travelProfile == profile) return
        _uiState.update { it.copy(travelProfile = profile) }
        calculateRouteForSelectedDestination()
    }

    fun toggleAvoidance(avoidance: RouteAvoidance) {
        val current = _uiState.value.avoidances
        val updated = if (current.contains(avoidance)) current - avoidance else current + avoidance
        _uiState.update { it.copy(avoidances = updated) }
        calculateRouteForSelectedDestination()
    }

    fun selectRouteAlternative(index: Int) {
        _uiState.update { it.copy(selectedRouteIndex = index) }
    }

    fun setMapPinMode(enabled: Boolean) {
        _uiState.update { it.copy(isMapPinMode = enabled, errorMessage = null) }
    }

    fun updateMapCenter(lat: Double, lon: Double) {
        _uiState.update { it.copy(mapCenterLatitude = lat, mapCenterLongitude = lon) }
    }

    fun confirmMapPinDestination(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReverseGeocoding = true, errorMessage = null) }
            val result = geocodingRepository.reverseGeocode(lat, lon)
            val dest = result.getOrElse {
                Destination(
                    id = "pin_${System.currentTimeMillis()}",
                    latitude = lat,
                    longitude = lon,
                    displayName = String.format(Locale.US, "%.5f, %.5f", lat, lon),
                    sourceType = DestinationSourceType.MAP_PIN,
                    createdAtTimestamp = System.currentTimeMillis()
                )
            }
            _uiState.update {
                it.copy(
                    isReverseGeocoding = false,
                    isMapPinMode = false,
                    selectedDestination = dest
                )
            }
            calculateRouteForSelectedDestination()
        }
    }

    fun toggleFavorite(destination: Destination) {
        viewModelScope.launch {
            val isSaved = _uiState.value.savedDestinations.any { it.id == destination.id }
            if (isSaved) {
                navigationSessionRepository.deleteSavedDestination(destination.id)
            } else {
                navigationSessionRepository.saveDestination(destination, isFavorite = true)
            }
        }
    }

    fun saveRoutingApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.setRoutingApiKey(apiKey)
            settingsRepository.setRoutingMode(RoutingMode.GRAPH_HOPPER_CLOUD)
            _uiState.update { it.copy(routingMode = RoutingMode.GRAPH_HOPPER_CLOUD, isApiKeyMissing = false, errorMessage = null) }
            if (_uiState.value.selectedDestination != null) {
                calculateRouteForSelectedDestination()
            }
        }
    }

    fun switchToKeylessMode() {
        viewModelScope.launch {
            settingsRepository.setRoutingMode(RoutingMode.KEYLESS_GRAPH_HOPPER)
            _uiState.update {
                it.copy(
                    routingMode = RoutingMode.KEYLESS_GRAPH_HOPPER,
                    isApiKeyMissing = false,
                    errorMessage = null
                )
            }
            val query = _uiState.value.searchQuery
            if (query.isNotBlank()) {
                onSearchQueryChanged(query)
            } else if (_uiState.value.selectedDestination != null) {
                calculateRouteForSelectedDestination()
            }
        }
    }

    fun startNavigation() {
        val selectedRoute = _uiState.value.selectedRoute ?: return
        viewModelScope.launch {
            val sessionId = UUID.randomUUID().toString()
            val currentRideId = (trackingRepository.sessionState.value as? TrackingSessionState.Tracking)?.rideId
            val session = NavigationSession(
                sessionId = sessionId,
                plannedRouteId = selectedRoute.id,
                rideId = currentRideId,
                status = NavigationStatus.ON_ROUTE,
                startedAtTimestamp = System.currentTimeMillis()
            )
            navigationSessionRepository.saveActiveSession(session, selectedRoute)
            _uiState.update { it.copy(navigationStarted = true) }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigationStarted = false) }
    }
}
