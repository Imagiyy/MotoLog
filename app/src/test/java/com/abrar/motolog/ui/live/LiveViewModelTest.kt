package com.abrar.motolog.ui.live

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import com.abrar.motolog.fake.FakeTrackingRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeRepository: FakeTrackingRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: LiveViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTrackingRepository()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_settings.preferences_pb") }
        )
        settingsRepository = SettingsRepository(testDataStore)

        viewModel = LiveViewModel(
            trackingRepository = fakeRepository,
            settingsRepository = settingsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(LiveUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun startTracking_delegatesToRepository() = runTest(testDispatcher) {
        viewModel.startTracking()
        advanceUntilIdle()
        assertTrue(fakeRepository.startCalled)
        assertEquals(LiveUiState.WaitingForGps(Float.MAX_VALUE), viewModel.uiState.value)
    }

    @Test
    fun sessionState_waitingForGps_updatesUiState() = runTest(testDispatcher) {
        fakeRepository.updateState(TrackingSessionState.WaitingForGps(accuracyMeters = 18f))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LiveUiState.WaitingForGps)
        assertEquals(18f, (state as LiveUiState.WaitingForGps).currentAccuracyMeters)
    }

    @Test
    fun sessionState_tracking_updatesUiStateWithStats() = runTest(testDispatcher) {
        val testStats = RideStats(
            totalDistanceMeters = 5200.0,
            movingTimeMs = 300_000L,
            currentSpeedKmh = 45.0,
            avgMovingSpeedKmh = 62.4
        )
        fakeRepository.updateState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                isPaused = false,
                stats = testStats,
                accuracyMeters = 5.0f
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LiveUiState.Tracking)
        val trackingState = state as LiveUiState.Tracking
        assertEquals(45.0, trackingState.speedKmh, 0.1)
        assertEquals(5200.0, trackingState.stats.totalDistanceMeters, 0.1)
        assertFalse(trackingState.isPaused)
    }

    @Test
    fun pauseAndResume_delegateToRepository() = runTest(testDispatcher) {
        fakeRepository.updateState(
            TrackingSessionState.Tracking(rideId = 1L, isPaused = false)
        )
        advanceUntilIdle()

        viewModel.pauseTracking()
        assertTrue(fakeRepository.pauseCalled)
        advanceUntilIdle()
        assertTrue((viewModel.uiState.value as LiveUiState.Tracking).isPaused)

        viewModel.resumeTracking()
        assertTrue(fakeRepository.resumeCalled)
        advanceUntilIdle()
        assertFalse((viewModel.uiState.value as LiveUiState.Tracking).isPaused)
    }

    @Test
    fun stopTracking_delegatesToRepository() = runTest(testDispatcher) {
        viewModel.stopTracking()
        assertTrue(fakeRepository.stopCalled)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LiveUiState.Stopped)
    }

    @Test
    fun resetToIdle_delegatesToRepository() = runTest(testDispatcher) {
        viewModel.resetToIdle()
        assertTrue(fakeRepository.resetCalled)
        assertEquals(LiveUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun recoveryOnStartup_unfinishedRideDetected_promptsRecovery() = runTest(testDispatcher) {
        val unfinishedRide = RideEntity(
            id = 99L,
            startTime = 1000L,
            distanceMeters = 1500.0,
            status = RideStatus.ACTIVE
        )
        fakeRepository.activeRideToReturn = unfinishedRide

        val newVm = LiveViewModel(
            trackingRepository = fakeRepository,
            settingsRepository = settingsRepository
        )
        advanceUntilIdle()

        val state = newVm.uiState.value
        assertTrue("Expected RecoveryPrompt state, but got $state", state is LiveUiState.RecoveryPrompt)
        assertEquals(99L, (state as LiveUiState.RecoveryPrompt).activeRide.id)
    }

    @Test
    fun recoveryPrompt_recover_delegatesToRepositoryAndStops() = runTest(testDispatcher) {
        val unfinishedRide = RideEntity(
            id = 99L,
            startTime = 1000L,
            distanceMeters = 1500.0,
            status = RideStatus.ACTIVE
        )
        viewModel.recoverRide(unfinishedRide)
        advanceUntilIdle()

        assertTrue(fakeRepository.recoveredRideCalled)
        val state = viewModel.uiState.value
        assertTrue(state is LiveUiState.Stopped)
        assertEquals(1500.0, (state as LiveUiState.Stopped).stats.totalDistanceMeters, 0.1)
    }

    @Test
    fun recoveryPrompt_discard_delegatesToRepositoryAndResetsToIdle() = runTest(testDispatcher) {
        val unfinishedRide = RideEntity(
            id = 99L,
            startTime = 1000L,
            status = RideStatus.ACTIVE
        )
        viewModel.discardRide(unfinishedRide)
        advanceUntilIdle()

        assertTrue(fakeRepository.discardedRideCalled)
        assertEquals(LiveUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun disclaimer_acceptancePersists() = runTest(testDispatcher) {
        viewModel.isDisclaimerAccepted.test {
            assertFalse(awaitItem())
            viewModel.acceptDisclaimer()
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun keepScreenOn_togglePersists() = runTest(testDispatcher) {
        viewModel.keepScreenOn.test {
            assertTrue(awaitItem())
            viewModel.setKeepScreenOn(false)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun batteryGuidance_togglePersists() = runTest(testDispatcher) {
        viewModel.batteryGuidanceSeen.test {
            assertFalse(awaitItem())
            viewModel.setBatteryGuidanceSeen(true)
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sessionState_autoPaused_mapsToAutoPausedUiState() = runTest(testDispatcher) {
        fakeRepository.updateState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                pauseState = com.abrar.motolog.shared.domain.model.PauseState.AUTO_PAUSED,
                isGpsLost = false,
                stats = RideStats(totalDistanceMeters = 1200.0),
                accuracyMeters = 6.0f
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LiveUiState.Tracking)
        val trackingState = state as LiveUiState.Tracking
        assertTrue(trackingState.isPaused)
        assertEquals(com.abrar.motolog.shared.domain.model.PauseState.AUTO_PAUSED, trackingState.pauseState)
        assertFalse(trackingState.isGpsLost)
    }

    @Test
    fun sessionState_gpsLost_mapsToGpsLostUiState() = runTest(testDispatcher) {
        fakeRepository.updateState(
            TrackingSessionState.Tracking(
                rideId = 1L,
                pauseState = com.abrar.motolog.shared.domain.model.PauseState.RECORDING,
                isGpsLost = true,
                stats = RideStats(totalDistanceMeters = 3400.0),
                accuracyMeters = 5.0f
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LiveUiState.Tracking)
        val trackingState = state as LiveUiState.Tracking
        assertTrue(trackingState.isGpsLost)
        assertEquals(com.abrar.motolog.shared.domain.model.PauseState.RECORDING, trackingState.pauseState)
    }
}
