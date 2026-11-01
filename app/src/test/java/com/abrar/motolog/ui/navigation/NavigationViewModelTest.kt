package com.abrar.motolog.ui.navigation

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.fake.FakeNavigationSessionRepository
import com.abrar.motolog.fake.FakeRoutingRepository
import com.abrar.motolog.fake.FakeTrackingRepository
import com.abrar.motolog.shared.domain.model.RoutingError
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
    private lateinit var routingRepo: FakeRoutingRepository
    private lateinit var fakeClock: FakeClock
    private lateinit var viewModel: NavigationViewModel

    class FakeClock(var time: Long = 1000L) : com.abrar.motolog.shared.domain.time.Clock {
        override fun currentTimeMillis(): Long = time
    }

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
        routingRepo = FakeRoutingRepository()
        fakeClock = FakeClock(1000L)

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_nav_settings.preferences_pb") }
        )
        settingsRepo = SettingsRepository(testDataStore)

        viewModel = NavigationViewModel(
            navigationSessionRepository = navSessionRepo,
            trackingRepository = trackingRepo,
            settingsRepository = settingsRepo,
            routingRepository = routingRepo,
            clock = fakeClock
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

    @Test
    fun autoRerouteTriggeredAfterSustainedDeviation() = runTest {
        val originalRoute = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_reroute_01",
            plannedRouteId = originalRoute.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = 1000L
        )
        navSessionRepo.saveActiveSession(session, originalRoute)
        advanceUntilIdle()

        // Set up the recalculated route that routing repo will return
        val reroutedRoute = PlannedRoute(
            id = "route_rerouted_202",
            geometry = listOf(
                LocationPoint(latitude = 12.9780, longitude = 77.6010),
                LocationPoint(latitude = 12.9800, longitude = 77.6050)
            ),
            totalDistanceMeters = 600.0,
            expectedDurationSeconds = 150L,
            instructions = listOf(
                RouteInstruction(
                    maneuverType = ManeuverType.DEPART,
                    instructionText = "Continue to Indiranagar",
                    distanceMeters = 600.0
                )
            ),
            providerAttribution = "GraphHopper"
        )
        routingRepo.routeToReturn = Result.success(listOf(reroutedRoute))

        // Deviate well beyond 80m reroute threshold (~2km away)
        // t = 1000L
        fakeClock.time = 1000L
        trackingRepo.emitSessionState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                stats = RideStats(),
                pauseState = PauseState.RECORDING,
                accuracyMeters = 5f,
                latitude = 12.9600,
                longitude = 77.5800
            )
        )
        advanceUntilIdle()

        // Off-route warning initially
        val state1 = viewModel.uiState.value as NavigationUiState.Navigating
        assertTrue(state1.isOffRouteWarning)

        // t = 13000L (> 10s sustained off route)
        fakeClock.time = 13000L
        trackingRepo.emitSessionState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                stats = RideStats(totalDistanceMeters = 20.0),
                pauseState = PauseState.RECORDING,
                accuracyMeters = 5f,
                latitude = 12.9601,
                longitude = 77.5801
            )
        )
        advanceUntilIdle()

        // Verify reroute was requested and applied
        assertNotNull(routingRepo.lastCalculatedRequest)
        assertEquals(12.9601, routingRepo.lastCalculatedRequest!!.origin.latitude, 0.001)

        val updatedState = viewModel.uiState.value as NavigationUiState.Navigating
        assertEquals("route_rerouted_202", updatedState.route.id)
        assertEquals(600.0, updatedState.remainingDistanceMeters, 0.1)
        assertFalse(updatedState.isRerouting)
        assertFalse(updatedState.isOffRouteWarning)

        // Verify session in Room was updated with new route and incremented rerouteCount
        val active = navSessionRepo.activeSession
        assertNotNull(active)
        assertEquals("route_rerouted_202", active!!.plannedRouteId)
        assertEquals(1, active.rerouteCount)
    }

    @Test
    fun autoRerouteDisabledInSettingsSuppressesAutomaticReroute() = runTest {
        settingsRepo.setRoutingAutoRerouteEnabled(false)
        advanceUntilIdle()

        val originalRoute = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_reroute_off",
            plannedRouteId = originalRoute.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = 1000L
        )
        navSessionRepo.saveActiveSession(session, originalRoute)
        advanceUntilIdle()

        // Sustained deviation > 80m for > 10s
        fakeClock.time = 1000L
        trackingRepo.emitSessionState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                stats = RideStats(),
                pauseState = PauseState.RECORDING,
                accuracyMeters = 5f,
                latitude = 12.9600,
                longitude = 77.5800
            )
        )
        advanceUntilIdle()

        fakeClock.time = 14000L
        trackingRepo.emitSessionState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                stats = RideStats(totalDistanceMeters = 20.0),
                pauseState = PauseState.RECORDING,
                accuracyMeters = 5f,
                latitude = 12.9601,
                longitude = 77.5801
            )
        )
        advanceUntilIdle()

        // Auto reroute was NOT requested
        assertTrue(routingRepo.lastCalculatedRequest == null)
        val state = viewModel.uiState.value as NavigationUiState.Navigating
        assertEquals(originalRoute.id, state.route.id)
    }

    @Test
    fun manualRerouteTriggersRecalculationEvenIfAutoRerouteDisabled() = runTest {
        settingsRepo.setRoutingAutoRerouteEnabled(false)
        advanceUntilIdle()

        val originalRoute = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_manual",
            plannedRouteId = originalRoute.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = 1000L
        )
        navSessionRepo.saveActiveSession(session, originalRoute)
        advanceUntilIdle()

        // Emit current fix
        fakeClock.time = 2000L
        trackingRepo.emitSessionState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                stats = RideStats(),
                pauseState = PauseState.RECORDING,
                accuracyMeters = 5f,
                latitude = 12.9740,
                longitude = 77.5960
            )
        )
        advanceUntilIdle()

        // User taps manual "REROUTE"
        viewModel.requestManualReroute()
        advanceUntilIdle()

        // Recalculation was dispatched with the rider's latest coordinates
        assertNotNull(routingRepo.lastCalculatedRequest)
        assertEquals(12.9740, routingRepo.lastCalculatedRequest!!.origin.latitude, 0.001)
    }

    @Test
    fun rerouteFailureRetainsExistingRouteAndSetsErrorMessage() = runTest {
        val originalRoute = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_fail",
            plannedRouteId = originalRoute.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = 1000L
        )
        navSessionRepo.saveActiveSession(session, originalRoute)
        advanceUntilIdle()

        fakeClock.time = 2000L
        trackingRepo.emitSessionState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                stats = RideStats(),
                pauseState = PauseState.RECORDING,
                accuracyMeters = 5f,
                latitude = 12.9740,
                longitude = 77.5960
            )
        )
        advanceUntilIdle()

        // Simulate network failure during reroute
        routingRepo.routeToReturn = Result.failure(RoutingError.NetworkError("No route returned"))

        viewModel.requestManualReroute()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NavigationUiState.Navigating
        // Original route is retained! Never erased on network failure!
        assertEquals(originalRoute.id, state.route.id)
        assertFalse(state.isRerouting)
        assertNotNull(state.rerouteErrorMessage)

        // Dismiss error
        viewModel.dismissRerouteError()
        val dismissedState = viewModel.uiState.value as NavigationUiState.Navigating
        assertTrue(dismissedState.rerouteErrorMessage == null)
    }

    @Test
    fun endNavigationCancelsActiveRerouteRequest() = runTest {
        val originalRoute = createSampleRoute()
        val session = NavigationSession(
            sessionId = "sess_cancel",
            plannedRouteId = originalRoute.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = 1000L
        )
        navSessionRepo.saveActiveSession(session, originalRoute)
        advanceUntilIdle()

        viewModel.endNavigation()
        advanceUntilIdle()

        assertTrue(routingRepo.cancelCalled)
        assertEquals(NavigationUiState.Idle, viewModel.uiState.value)
    }
}
