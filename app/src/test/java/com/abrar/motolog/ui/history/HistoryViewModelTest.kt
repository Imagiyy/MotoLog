package com.abrar.motolog.ui.history

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.data.repository.FakeRideRepository
import com.abrar.motolog.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var fakeRepository: FakeRideRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRideRepository()
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_history_settings.preferences_pb") }
        )
        settingsRepository = SettingsRepository(testDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsOnlyCompletedAndRecoveredRides() = runTest(testDispatcher) {
        val ride1 = RideEntity(id = 1, name = "Morning ride", startTime = 1000L, status = RideStatus.COMPLETED)
        val ride2 = RideEntity(id = 2, name = "Active ride", startTime = 2000L, status = RideStatus.ACTIVE)
        val ride3 = RideEntity(id = 3, name = "Recovered ride", startTime = 3000L, status = RideStatus.RECOVERED)

        fakeRepository.seedRides(listOf(ride1, ride2, ride3))
        viewModel = HistoryViewModel(fakeRepository, settingsRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.rides.size)
        // Sorted newest first
        assertEquals(3L, state.rides[0].id)
        assertEquals(1L, state.rides[1].id)
    }

    @Test
    fun deleteRide_immediatelyHidesRideAndSetsRecentlyDeleted() = runTest(testDispatcher) {
        val ride1 = RideEntity(id = 1, name = "Morning ride", startTime = 1000L, status = RideStatus.COMPLETED)
        val ride2 = RideEntity(id = 2, name = "Evening ride", startTime = 2000L, status = RideStatus.COMPLETED)

        fakeRepository.seedRides(listOf(ride1, ride2))
        viewModel = HistoryViewModel(fakeRepository, settingsRepository)

        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.rides.size)

        viewModel.deleteRide(ride1)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(1, state.rides.size)
        assertEquals(2L, state.rides[0].id)
        assertEquals(ride1, state.recentlyDeletedRide)
    }

    @Test
    fun undoDelete_restoresRideImmediately() = runTest(testDispatcher) {
        val ride1 = RideEntity(id = 1, name = "Morning ride", startTime = 1000L, status = RideStatus.COMPLETED)

        fakeRepository.seedRides(listOf(ride1))
        viewModel = HistoryViewModel(fakeRepository, settingsRepository)

        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.rides.size)

        viewModel.deleteRide(ride1)
        runCurrent()

        assertTrue(viewModel.uiState.value.rides.isEmpty())
        assertEquals(ride1, viewModel.uiState.value.recentlyDeletedRide)

        viewModel.undoDelete()
        runCurrent()

        assertEquals(1, viewModel.uiState.value.rides.size)
        assertEquals(1L, viewModel.uiState.value.rides[0].id)
        assertNull(viewModel.uiState.value.recentlyDeletedRide)
    }

    @Test
    fun deleteRide_afterTimeout_commitsDeletionToDatabase() = runTest(testDispatcher) {
        val ride1 = RideEntity(id = 1, name = "Morning ride", startTime = 1000L, status = RideStatus.COMPLETED)

        fakeRepository.seedRides(listOf(ride1))
        viewModel = HistoryViewModel(fakeRepository, settingsRepository)

        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.rides.size)

        viewModel.deleteRide(ride1)
        runCurrent()
        assertEquals(ride1, viewModel.uiState.value.recentlyDeletedRide)

        // Advance time past 5000ms undo window
        advanceTimeBy(5001L)
        runCurrent()

        assertTrue(fakeRepository.ridesFlow.value.isEmpty())
        assertNull(viewModel.uiState.value.recentlyDeletedRide)
    }
}
