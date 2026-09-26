package com.abrar.motolog.ui.history.detail

import androidx.lifecycle.SavedStateHandle
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.data.repository.FakeRideRepository
import com.abrar.motolog.shared.domain.model.GpsPoint
import com.abrar.motolog.shared.domain.util.RideNameGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RideDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRideRepository
    private lateinit var fakeRideDao: FakeRideDao
    private lateinit var fakeBikeDao: FakeBikeDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRideRepository()
        fakeRideDao = FakeRideDao()
        fakeBikeDao = FakeBikeDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadRideDetail_loadsRideAndComputesSplits() = runTest(testDispatcher) {
        val ride = RideEntity(
            id = 42L,
            name = "Morning ride",
            startTime = 1000L,
            endTime = 1000L + 120_000L,
            distanceMeters = 2050.0,
            movingTimeMs = 120_000L,
            elapsedTimeMs = 120_000L,
            status = RideStatus.COMPLETED
        )
        fakeRepository.seedRides(listOf(ride))
        fakeRideDao.rides[42L] = ride

        val points = mutableListOf<GpsPoint>()
        var time = 1000L
        var lat = 12.9716
        val latPerKm = 0.009
        for (i in 0 until 100) {
            points.add(
                GpsPoint(
                    timestampEpochMs = time,
                    latitude = lat,
                    longitude = 77.5946,
                    speedMps = 20f,
                    accuracyMeters = 5f
                )
            )
            time += 1000L
            lat += latPerKm / 50.0
        }
        points.add(
            GpsPoint(
                timestampEpochMs = time,
                latitude = lat,
                longitude = 77.5946,
                speedMps = 20f,
                accuracyMeters = 5f
            )
        )
        fakeRepository.pointsMap[42L] = points

        val savedStateHandle = SavedStateHandle(mapOf("rideId" to 42L))
        val viewModel = RideDetailViewModel(fakeRepository, fakeRideDao, fakeBikeDao, savedStateHandle).apply {
            defaultDispatcher = testDispatcher
            loadRideDetail()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSplitsLoading)
        assertNotNull(state.ride)
        assertEquals(42L, state.ride?.id)
        assertEquals(3, state.splits.size)
        assertEquals(1, state.splits[0].splitNumber)
        assertEquals(2, state.splits[1].splitNumber)
    }

    @Test
    fun toggleStatsMode_togglesBetweenMovingAndOverall() = runTest(testDispatcher) {
        val ride = RideEntity(id = 10L, startTime = 1000L, status = RideStatus.COMPLETED)
        fakeRepository.seedRides(listOf(ride))
        fakeRideDao.rides[10L] = ride

        val viewModel = RideDetailViewModel(fakeRepository, fakeRideDao, fakeBikeDao, SavedStateHandle(mapOf("rideId" to 10L)))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showOverallStats)

        viewModel.toggleStatsMode()
        assertTrue(viewModel.uiState.value.showOverallStats)

        viewModel.toggleStatsMode()
        assertFalse(viewModel.uiState.value.showOverallStats)
    }

    @Test
    fun renameRide_updatesNameSuccessfully() = runTest(testDispatcher) {
        val ride = RideEntity(id = 10L, name = "Morning ride", startTime = 1000L, status = RideStatus.COMPLETED)
        fakeRepository.seedRides(listOf(ride))
        fakeRideDao.rides[10L] = ride

        val viewModel = RideDetailViewModel(fakeRepository, fakeRideDao, fakeBikeDao, SavedStateHandle(mapOf("rideId" to 10L)))
        advanceUntilIdle()

        viewModel.openRenameDialog()
        assertTrue(viewModel.uiState.value.isRenameDialogOpen)

        viewModel.onRenameCandidateChanged("Mountain Loop")
        viewModel.confirmRename()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRenameDialogOpen)
        assertEquals("Mountain Loop", viewModel.uiState.value.ride?.name)
        assertEquals("Mountain Loop", fakeRepository.getRideById(10L)?.name)
    }

    @Test
    fun renameRide_blankOrWhitespace_fallsBackToDefaultName() = runTest(testDispatcher) {
        val ride = RideEntity(id = 10L, name = "Old Name", startTime = 1000L, status = RideStatus.COMPLETED)
        fakeRepository.seedRides(listOf(ride))
        fakeRideDao.rides[10L] = ride

        val viewModel = RideDetailViewModel(fakeRepository, fakeRideDao, fakeBikeDao, SavedStateHandle(mapOf("rideId" to 10L)))
        advanceUntilIdle()

        viewModel.openRenameDialog()
        viewModel.onRenameCandidateChanged("    ")
        viewModel.confirmRename()
        advanceUntilIdle()

        val defaultExpected = RideNameGenerator.defaultNameForTimestamp(1000L)
        assertEquals(defaultExpected, viewModel.uiState.value.ride?.name)
    }

    @Test
    fun reassignBike_updatesAssignedBikeAndRide() = runTest(testDispatcher) {
        val bike = BikeEntity(id = 1L, name = "Honda CB300R")
        fakeBikeDao.bikes.add(bike)

        val ride = RideEntity(id = 10L, bikeId = null, name = "Unassigned", startTime = 1000L, status = RideStatus.COMPLETED)
        fakeRepository.seedRides(listOf(ride))
        fakeRideDao.rides[10L] = ride

        val viewModel = RideDetailViewModel(fakeRepository, fakeRideDao, fakeBikeDao, SavedStateHandle(mapOf("rideId" to 10L)))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.assignedBike)

        viewModel.selectBikeForRide(1L)
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.ride?.bikeId)
        assertEquals("Honda CB300R", viewModel.uiState.value.assignedBike?.name)

        // Now unassign
        viewModel.selectBikeForRide(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.ride?.bikeId)
        assertNull(viewModel.uiState.value.assignedBike)
    }

    private class FakeRideDao : RideDao {
        val rides = mutableMapOf<Long, RideEntity>()

        override suspend fun insert(ride: RideEntity): Long { rides[ride.id] = ride; return ride.id }
        override suspend fun update(ride: RideEntity) { rides[ride.id] = ride }
        override suspend fun delete(ride: RideEntity) { rides.remove(ride.id) }
        override fun getAllRides(): Flow<List<RideEntity>> = MutableStateFlow(rides.values.toList())
        override suspend fun getRideById(rideId: Long): RideEntity? = rides[rideId]
        override suspend fun getRidesByStatus(status: RideStatus): List<RideEntity> = rides.values.filter { it.status == status }
        override fun getRidesForBike(bikeId: Long): Flow<List<RideEntity>> = MutableStateFlow(rides.values.filter { it.bikeId == bikeId })
        override suspend fun findActiveRide(): RideEntity? = rides.values.find { it.status == RideStatus.ACTIVE }
        override fun getFinishedRides(): Flow<List<RideEntity>> = MutableStateFlow(rides.values.toList())
        override suspend fun updateRideName(rideId: Long, name: String) {
            rides[rideId] = rides[rideId]?.copy(name = name) ?: return
        }
        override suspend fun updateRideBike(rideId: Long, bikeId: Long?) {
            rides[rideId] = rides[rideId]?.copy(bikeId = bikeId) ?: return
        }
        override fun getTotalDistanceMetersForBike(bikeId: Long): Flow<Double?> =
            MutableStateFlow(rides.values.filter { it.bikeId == bikeId }.sumOf { it.distanceMeters })
        override suspend fun getTotalDistanceMetersForBikeOnce(bikeId: Long): Double? =
            rides.values.filter { it.bikeId == bikeId }.sumOf { it.distanceMeters }
        override suspend fun findDuplicateRide(minStartTime: Long, maxStartTime: Long, minDistance: Double, maxDistance: Double): RideEntity? = null
        override suspend fun getAllRidesOnce(): List<RideEntity> = rides.values.toList()
        override suspend fun insertAll(ridesList: List<RideEntity>) {
            ridesList.forEach { rides[it.id] = it }
        }
        override suspend fun deleteAllRides() {
            rides.clear()
        }
    }

    private class FakeBikeDao : BikeDao {
        val bikes = mutableListOf<BikeEntity>()

        override suspend fun insert(bike: BikeEntity): Long { bikes.add(bike); return bike.id }
        override suspend fun update(bike: BikeEntity) {}
        override suspend fun delete(bike: BikeEntity) { bikes.remove(bike) }
        override fun getAllBikes(): Flow<List<BikeEntity>> = MutableStateFlow(bikes)
        override fun getActiveBikes(): Flow<List<BikeEntity>> = MutableStateFlow(bikes.filter { !it.isArchived })
        override suspend fun getActiveBikesOnce(): List<BikeEntity> = bikes.filter { !it.isArchived }
        override fun getArchivedBikes(): Flow<List<BikeEntity>> = MutableStateFlow(bikes.filter { it.isArchived })
        override suspend fun getBikeById(bikeId: Long): BikeEntity? = bikes.find { it.id == bikeId }
        override suspend fun getActiveBikeCount(): Int = bikes.count { !it.isArchived }
        override suspend fun getRideCountForBike(bikeId: Long): Int = 0
        override suspend fun setArchived(bikeId: Long, isArchived: Boolean) {}
        override suspend fun updateOdometerOffset(bikeId: Long, offsetKm: Double) {}
        override suspend fun getAllBikesOnce(): List<BikeEntity> = bikes.toList()
        override suspend fun insertAll(bikesList: List<BikeEntity>) {
            bikes.addAll(bikesList)
        }
        override suspend fun deleteAllBikes() {
            bikes.clear()
        }
    }
}
