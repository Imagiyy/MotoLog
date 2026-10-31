package com.abrar.motolog.ui.destination

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.fake.FakeGeocodingRepository
import com.abrar.motolog.fake.FakeNavigationSessionRepository
import com.abrar.motolog.fake.FakeRoutingRepository
import com.abrar.motolog.fake.FakeTrackingRepository
import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.DestinationSourceType
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteAvoidance
import com.abrar.motolog.shared.domain.model.RouteProfile
import com.abrar.motolog.shared.domain.model.RoutingError
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DestinationViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var routingRepo: FakeRoutingRepository
    private lateinit var geocodingRepo: FakeGeocodingRepository
    private lateinit var navSessionRepo: FakeNavigationSessionRepository
    private lateinit var trackingRepo: FakeTrackingRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var viewModel: DestinationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        routingRepo = FakeRoutingRepository()
        geocodingRepo = FakeGeocodingRepository()
        navSessionRepo = FakeNavigationSessionRepository()
        trackingRepo = FakeTrackingRepository()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_dest_settings.preferences_pb") }
        )
        settingsRepo = SettingsRepository(testDataStore)

        viewModel = DestinationViewModel(
            routingRepository = routingRepo,
            geocodingRepository = geocodingRepo,
            navigationSessionRepository = navSessionRepo,
            trackingRepository = trackingRepo,
            settingsRepository = settingsRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_emptySearchAndNoRoute() {
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertFalse(state.isSearching)
        assertTrue(state.searchResults.isEmpty())
        assertNull(state.selectedDestination)
        assertNull(state.selectedRoute)
        assertFalse(state.navigationStarted)
    }

    @Test
    fun searchQuery_debounced_triggersGeocodingRepository() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("MG Road")
        assertEquals("MG Road", viewModel.uiState.value.searchQuery)

        // Before debounce delay (300ms) completes
        advanceTimeBy(100L)
        assertNull(geocodingRepo.lastSearchQuery)

        // After debounce delay
        advanceTimeBy(300L)
        advanceUntilIdle()

        assertEquals("MG Road", geocodingRepo.lastSearchQuery)
        assertEquals(1, viewModel.uiState.value.searchResults.size)
        assertEquals("MG Road, Bengaluru", viewModel.uiState.value.searchResults.first().displayName)
    }

    @Test
    fun searchQuery_blank_clearsResultsImmediately() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("Nandi Hills")
        advanceTimeBy(400L)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.searchResults.size)

        viewModel.onSearchQueryChanged("")
        assertEquals(0, viewModel.uiState.value.searchResults.size)
        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun searchQuery_missingApiKey_setsApiKeyMissingState() = runTest(testDispatcher) {
        geocodingRepo.searchResultsToReturn = Result.failure(RoutingError.ApiKeyMissing())

        viewModel.onSearchQueryChanged("Airport")
        advanceTimeBy(400L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isApiKeyMissing)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun selectDestination_withCurrentLocation_calculatesRoute() = runTest(testDispatcher) {
        // Feed current location via tracking repo session state
        trackingRepo.updateState(
            TrackingSessionState.Tracking(
                rideId = 101L,
                isPaused = false,
                latitude = 12.9716,
                longitude = 77.5946,
                accuracyMeters = 8.0f
            )
        )
        advanceUntilIdle()

        val dest = Destination(
            id = "d1",
            latitude = 13.0000,
            longitude = 77.6500,
            displayName = "Target Destination"
        )

        viewModel.selectDestination(dest)
        advanceUntilIdle()

        assertEquals(dest, viewModel.uiState.value.selectedDestination)
        assertEquals(1, viewModel.uiState.value.routeCandidates.size)
        assertEquals("route_1", viewModel.uiState.value.selectedRoute?.id)
        assertNotNull(routingRepo.lastCalculatedRequest)
        assertEquals(12.9716, routingRepo.lastCalculatedRequest?.origin?.latitude ?: 0.0, 0.0001)
        assertEquals(13.0000, routingRepo.lastCalculatedRequest?.destination?.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun selectDestination_withoutCurrentLocation_setsWaitingLocationError() = runTest(testDispatcher) {
        val dest = Destination(id = "d1", latitude = 13.0, longitude = 77.6, displayName = "Place")
        viewModel.selectDestination(dest)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("GPS location unavailable"))
    }

    @Test
    fun travelProfileChange_recalculatesRouteWithNewProfile() = runTest(testDispatcher) {
        trackingRepo.updateState(
            TrackingSessionState.Tracking(rideId = 1L, isPaused = false, latitude = 12.9, longitude = 77.5)
        )
        advanceUntilIdle()

        val dest = Destination(id = "d1", latitude = 13.0, longitude = 77.6, displayName = "Place")
        viewModel.selectDestination(dest)
        advanceUntilIdle()
        assertEquals(RouteProfile.MOTORCYCLE, routingRepo.lastCalculatedRequest?.travelProfile)

        viewModel.setTravelProfile(RouteProfile.CAR)
        advanceUntilIdle()
        assertEquals(RouteProfile.CAR, routingRepo.lastCalculatedRequest?.travelProfile)
    }

    @Test
    fun avoidanceToggle_recalculatesRouteWithUpdatedAvoidances() = runTest(testDispatcher) {
        trackingRepo.updateState(
            TrackingSessionState.Tracking(rideId = 1L, isPaused = false, latitude = 12.9, longitude = 77.5)
        )
        advanceUntilIdle()

        val dest = Destination(id = "d1", latitude = 13.0, longitude = 77.6, displayName = "Place")
        viewModel.selectDestination(dest)
        advanceUntilIdle()

        viewModel.toggleAvoidance(RouteAvoidance.TOLLS)
        advanceUntilIdle()
        assertTrue(routingRepo.lastCalculatedRequest?.avoidances?.contains(RouteAvoidance.TOLLS) == true)

        viewModel.toggleAvoidance(RouteAvoidance.TOLLS)
        advanceUntilIdle()
        assertFalse(routingRepo.lastCalculatedRequest?.avoidances?.contains(RouteAvoidance.TOLLS) == true)
    }

    @Test
    fun selectRouteAlternative_updatesSelectedCandidate() = runTest(testDispatcher) {
        trackingRepo.updateState(
            TrackingSessionState.Tracking(rideId = 1L, isPaused = false, latitude = 12.9, longitude = 77.5)
        )
        advanceUntilIdle()

        routingRepo.routeToReturn = Result.success(
            listOf(
                PlannedRoute(id = "fastest", geometry = emptyList(), totalDistanceMeters = 5000.0, expectedDurationSeconds = 600L),
                PlannedRoute(id = "alternative", geometry = emptyList(), totalDistanceMeters = 5500.0, expectedDurationSeconds = 700L)
            )
        )

        val dest = Destination(id = "d1", latitude = 13.0, longitude = 77.6, displayName = "Place")
        viewModel.selectDestination(dest)
        advanceUntilIdle()

        assertEquals("fastest", viewModel.uiState.value.selectedRoute?.id)

        viewModel.selectRouteAlternative(1)
        assertEquals("alternative", viewModel.uiState.value.selectedRoute?.id)
    }

    @Test
    fun confirmMapPinDestination_reverseGeocodesAndCalculatesRoute() = runTest(testDispatcher) {
        trackingRepo.updateState(
            TrackingSessionState.Tracking(rideId = 1L, isPaused = false, latitude = 12.9, longitude = 77.5)
        )
        advanceUntilIdle()

        geocodingRepo.reverseGeocodeToReturn = Result.success(
            Destination(
                id = "pin_dest",
                latitude = 12.9500,
                longitude = 77.5800,
                displayName = "Pinned Hill Top",
                sourceType = DestinationSourceType.MAP_PIN
            )
        )

        viewModel.confirmMapPinDestination(12.9500, 77.5800)
        advanceUntilIdle()

        assertEquals("Pinned Hill Top", viewModel.uiState.value.selectedDestination?.displayName)
        assertEquals(DestinationSourceType.MAP_PIN, viewModel.uiState.value.selectedDestination?.sourceType)
        assertFalse(viewModel.uiState.value.isMapPinMode)
        assertNotNull(viewModel.uiState.value.selectedRoute)
    }

    @Test
    fun toggleFavorite_savesAndRemovesFavoriteDestination() = runTest(testDispatcher) {
        val dest = Destination(id = "fav_1", latitude = 12.9, longitude = 77.5, displayName = "Favorite Spot")

        viewModel.toggleFavorite(dest)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.savedDestinations.any { it.id == "fav_1" })

        viewModel.toggleFavorite(dest)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.savedDestinations.any { it.id == "fav_1" })
    }

    @Test
    fun saveRoutingApiKey_updatesSettingsAndRetriesRouteCalculation() = runTest(testDispatcher) {
        trackingRepo.updateState(
            TrackingSessionState.Tracking(rideId = 1L, isPaused = false, latitude = 12.9, longitude = 77.5)
        )
        advanceUntilIdle()

        routingRepo.routeToReturn = Result.failure(RoutingError.ApiKeyMissing())
        val dest = Destination(id = "d1", latitude = 13.0, longitude = 77.6, displayName = "Place")
        viewModel.selectDestination(dest)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isApiKeyMissing)

        // User saves key
        routingRepo.routeToReturn = Result.success(
            listOf(PlannedRoute(id = "recovered_route", geometry = emptyList(), totalDistanceMeters = 1000.0, expectedDurationSeconds = 120L))
        )
        viewModel.saveRoutingApiKey("my_valid_api_key_123")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isApiKeyMissing)
        assertEquals("recovered_route", viewModel.uiState.value.selectedRoute?.id)
    }

    @Test
    fun startNavigation_savesActiveSessionAndFlagsNavigationStarted() = runTest(testDispatcher) {
        trackingRepo.updateState(
            TrackingSessionState.Tracking(rideId = 999L, isPaused = false, latitude = 12.9, longitude = 77.5)
        )
        advanceUntilIdle()

        val dest = Destination(id = "d1", latitude = 13.0, longitude = 77.6, displayName = "Place")
        viewModel.selectDestination(dest)
        advanceUntilIdle()

        viewModel.startNavigation()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.navigationStarted)
        assertNotNull(navSessionRepo.activeSession)
        assertEquals(999L, navSessionRepo.activeSession?.rideId)
        assertEquals("route_1", navSessionRepo.activeSession?.plannedRouteId)

        viewModel.onNavigationHandled()
        assertFalse(viewModel.uiState.value.navigationStarted)
    }
}
