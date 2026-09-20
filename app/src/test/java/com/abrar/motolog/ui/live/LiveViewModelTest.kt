package com.abrar.motolog.ui.live

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.model.LocationPoint
import com.abrar.motolog.fake.FakeLocationSource
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

    private lateinit var fakeLocationSource: FakeLocationSource
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: LiveViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocationSource = FakeLocationSource()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_settings.preferences_pb") }
        )
        settingsRepository = SettingsRepository(testDataStore)

        viewModel = LiveViewModel(
            locationSource = fakeLocationSource,
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
    fun startTracking_transitionsToWaitingForGps() = runTest(testDispatcher) {
        viewModel.startTracking()
        assertEquals(LiveUiState.WaitingForGps(currentAccuracyMeters = null), viewModel.uiState.value)
    }

    @Test
    fun waitingForGps_inaccurateFix_staysInWaitingWithUpdatedAccuracy() = runTest(testDispatcher) {
        viewModel.startTracking()

        // Accuracy is 30m, which is > 25m threshold
        fakeLocationSource.emitLocation(
            LocationPoint(
                latitude = 12.9716,
                longitude = 77.5946,
                speedMps = 10f,
                accuracyMeters = 30f,
                timestamp = System.currentTimeMillis()
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LiveUiState.WaitingForGps)
        assertEquals(30f, (state as LiveUiState.WaitingForGps).currentAccuracyMeters)
    }

    @Test
    fun waitingForGps_accurateFix_transitionsToTracking() = runTest(testDispatcher) {
        viewModel.startTracking()

        // Fix accuracy is 12m (<= 25m) and speed is 15 m/s (54 km/h)
        fakeLocationSource.emitLocation(
            LocationPoint(
                latitude = 12.9716,
                longitude = 77.5946,
                speedMps = 15f,
                accuracyMeters = 12f,
                timestamp = System.currentTimeMillis()
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LiveUiState.Tracking)
        val trackingState = state as LiveUiState.Tracking
        assertEquals(54.0, trackingState.speedKmh, 0.01)
        assertEquals(12f, trackingState.accuracyMeters)
    }

    @Test
    fun tracking_stationaryNoiseSpeed_treatedAsZero() = runTest(testDispatcher) {
        viewModel.startTracking()

        // 0.3 m/s = 1.08 km/h (< 1.5 km/h stationary threshold)
        fakeLocationSource.emitLocation(
            LocationPoint(
                latitude = 12.9716,
                longitude = 77.5946,
                speedMps = 0.3f,
                accuracyMeters = 10f,
                timestamp = System.currentTimeMillis()
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LiveUiState.Tracking)
        val trackingState = state as LiveUiState.Tracking
        assertEquals(0.0, trackingState.speedKmh, 0.0)
    }

    @Test
    fun stopTracking_stopsLocationUpdatesAndTransitionsToStopped() = runTest(testDispatcher) {
        viewModel.startTracking()

        fakeLocationSource.emitLocation(
            LocationPoint(
                latitude = 12.9716,
                longitude = 77.5946,
                speedMps = 10f,
                accuracyMeters = 15f,
                timestamp = System.currentTimeMillis()
            )
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is LiveUiState.Tracking)

        viewModel.stopTracking()
        advanceUntilIdle()

        assertEquals(LiveUiState.Stopped, viewModel.uiState.value)
        assertTrue(fakeLocationSource.stopCalled)
    }

    @Test
    fun resetToIdle_transitionsFromStoppedToIdle() = runTest(testDispatcher) {
        viewModel.startTracking()
        viewModel.stopTracking()
        advanceUntilIdle()
        assertEquals(LiveUiState.Stopped, viewModel.uiState.value)

        viewModel.resetToIdle()
        assertEquals(LiveUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun acceptDisclaimer_persistsToSettingsRepository() = runTest(testDispatcher) {
        viewModel.isDisclaimerAccepted.test {
            assertFalse(awaitItem())

            viewModel.acceptDisclaimer()
            advanceUntilIdle()

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
