package com.abrar.motolog.ui.navigation

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.fake.FakeNavigationSessionRepository
import com.abrar.motolog.fake.FakeTrackingRepository
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.ManeuverType
import com.abrar.motolog.shared.domain.model.NavigationSession
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.shared.domain.model.RouteInstruction
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var navSessionRepo: FakeNavigationSessionRepository
    private lateinit var trackingRepo: FakeTrackingRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var viewModel: NavigationViewModel

    private fun createSampleRoute(): PlannedRoute {
        val geometry = listOf(
            LocationPoint(latitude = 12.9716, longitude = 77.5946), // MG Road start
            LocationPoint(latitude = 12.9750, longitude = 77.5980), // Waypoint 1
            LocationPoint(latitude = 12.9800, longitude = 77.6050)  // Destination Indiranagar
        )
        val instructions = listOf(
            RouteInstruction(
                maneuverType = ManeuverType.DEPART,
                streetName = "MG Road",
                instructionText = "Head east on MG Road",
                pointIndex = 0,
                distanceMeters = 500.0
            ),
            RouteInstruction(
                maneuverType = ManeuverType.TURN_LEFT,
                streetName = "100ft Road",
                instructionText = "Turn left onto 100ft Road",
                pointIndex = 1,
                distanceMeters = 800.0
            ),
            RouteInstruction(
                maneuverType = ManeuverType.ARRIVE,
                streetName = "Indiranagar",
                instructionText = "Arrive at Indiranagar",
                pointIndex = 2,
                distanceMeters = 0.0
            )
        )
        return PlannedRoute(
            id = "route_101",
            geometry = geometry,
            totalDistanceMeters = 1300.0,
            expectedDurationSeconds = 300L,
            instructions = instructions,
            providerAttribution = "GraphHopper"
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        navSessionRepo = FakeNavigationSessionRepository()
        trackingRepo = FakeTrackingRepository()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_nav_settings.preferences_pb") }
        )
        settingsRepo = SettingsRepository(testDataStore)

        viewModel = NavigationViewModel(
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
    fun initialStateIsIdleWhenNoActiveSession() = runTest {
        advanceUntilIdle()
        assertEquals(NavigationUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun activeSessionTransitionsStateToNavigating() = runTest {
        val route = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_001",
            plannedRouteId = route.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = System.currentTimeMillis()
        )

        navSessionRepo.saveActiveSession(session, route)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NavigationUiState.Navigating)
        val nav = state as NavigationUiState.Navigating
        assertEquals("sess_001", nav.session.sessionId)
        assertEquals(1300.0, nav.remainingDistanceMeters, 50.0)
        assertFalse(nav.isArrived)
        assertEquals(CameraFollowMode.FOLLOW_HEAD_UP, nav.cameraFollowMode)
        assertNotNull(nav.currentInstruction)
        assertEquals("Head east on MG Road", nav.currentInstruction?.instructionText)
    }

    @Test
    fun locationFixUpdatesRouteProgressAndManeuverDistance() = runTest {
        val route = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_001",
            plannedRouteId = route.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = System.currentTimeMillis()
        )
        navSessionRepo.saveActiveSession(session, route)
        advanceUntilIdle()

        // Move to first waypoint (12.9750, 77.5980)
        trackingRepo.emitSessionState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                stats = RideStats(totalDistanceMeters = 500.0, currentSpeedKmh = 36.0),
                pauseState = PauseState.RECORDING,
                accuracyMeters = 4f,
                latitude = 12.9750,
                longitude = 77.5980,
                altitudeMeters = 900.0,
                bearingDegrees = 45f,
                isGpsLost = false
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NavigationUiState.Navigating)
        val nav = state as NavigationUiState.Navigating
        assertTrue(nav.remainingDistanceMeters < 1300.0)
        assertFalse(nav.isGpsLost)
    }

    @Test
    fun arrivalAtDestinationTriggersArrivedState() = runTest {
        val route = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_001",
            plannedRouteId = route.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = System.currentTimeMillis()
        )
        navSessionRepo.saveActiveSession(session, route)
        advanceUntilIdle()

        // Emit fix right at destination (12.9800, 77.6050) with low speed (stationary confirm)
        trackingRepo.emitSessionState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                stats = RideStats(totalDistanceMeters = 1300.0, currentSpeedKmh = 1.0),
                pauseState = PauseState.RECORDING,
                accuracyMeters = 3f,
                latitude = 12.9800,
                longitude = 77.6050,
                altitudeMeters = 900.0,
                bearingDegrees = 0f,
                isGpsLost = false
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NavigationUiState.Navigating)
        val nav = state as NavigationUiState.Navigating
        assertTrue(nav.isArrived)
        assertEquals(NavigationStatus.ARRIVED, nav.status)
    }

    @Test
    fun cameraFollowModeTogglingAndRecenter() = runTest {
        val route = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_001",
            plannedRouteId = route.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = System.currentTimeMillis()
        )
        navSessionRepo.saveActiveSession(session, route)
        advanceUntilIdle()

        val initial = viewModel.uiState.value as NavigationUiState.Navigating
        assertEquals(CameraFollowMode.FOLLOW_HEAD_UP, initial.cameraFollowMode)

        // Toggle to North-Up
        viewModel.toggleCameraFollowMode()
        assertEquals(CameraFollowMode.FOLLOW_NORTH_UP, (viewModel.uiState.value as NavigationUiState.Navigating).cameraFollowMode)

        // User drags map -> FREE mode
        viewModel.onUserMapInteraction()
        assertEquals(CameraFollowMode.FREE, (viewModel.uiState.value as NavigationUiState.Navigating).cameraFollowMode)

        // Recenter -> resets back to FOLLOW_HEAD_UP
        viewModel.recenterCamera()
        assertEquals(CameraFollowMode.FOLLOW_HEAD_UP, (viewModel.uiState.value as NavigationUiState.Navigating).cameraFollowMode)
    }

    @Test
    fun endNavigationResetsToIdle() = runTest {
        val route = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_001",
            plannedRouteId = route.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = System.currentTimeMillis()
        )
        navSessionRepo.saveActiveSession(session, route)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is NavigationUiState.Navigating)

        viewModel.endNavigation()
        advanceUntilIdle()

        assertEquals(NavigationUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun gpsSignalLostUpdatesNavigatingState() = runTest {
        val route = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_001",
            plannedRouteId = route.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = System.currentTimeMillis()
        )
        navSessionRepo.saveActiveSession(session, route)
        advanceUntilIdle()

        trackingRepo.emitSessionState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                stats = RideStats(),
                pauseState = PauseState.RECORDING,
                accuracyMeters = 40f,
                latitude = null,
                longitude = null,
                isGpsLost = true
            )
        )
        advanceUntilIdle()

        val nav = viewModel.uiState.value as NavigationUiState.Navigating
        assertTrue(nav.isGpsLost)
    }
}
