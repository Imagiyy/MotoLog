package com.abrar.motolog.data.repository

import app.cash.turbine.test
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.RideStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RideRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRideDao: FakeRideDao
    private lateinit var fakeRidePointDao: FakeRidePointDao
    private lateinit var repository: RideRepositoryImpl

    @Before
    fun setUp() {
        fakeRideDao = FakeRideDao()
        fakeRidePointDao = FakeRidePointDao()
        repository = RideRepositoryImpl(fakeRideDao, fakeRidePointDao, testDispatcher)
    }

    @Test
    fun getFinishedRides_hidesPendingDeletionsAndRestoresOnUndo() = runTest(testDispatcher) {
        val ride1 = RideEntity(id = 1, name = "Ride 1", startTime = 1000L, status = RideStatus.COMPLETED)
        val ride2 = RideEntity(id = 2, name = "Ride 2", startTime = 2000L, status = RideStatus.COMPLETED)
        fakeRideDao.finishedRidesFlow.value = listOf(ride2, ride1)

        repository.getFinishedRides().test {
            val initial = awaitItem()
            assertEquals(2, initial.size)

            // Mark ride 1 for deletion
            repository.markForDeletion(1L)
            val afterHide = awaitItem()
            assertEquals(1, afterHide.size)
            assertEquals(2L, afterHide[0].id)

            // Undo deletion
            repository.undoDeletion(1L)
            val afterUndo = awaitItem()
            assertEquals(2, afterUndo.size)
            assertEquals(listOf(2L, 1L), afterUndo.map { it.id })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun commitDeletion_deletesPointsAndRideLeavingNoOrphans() = runTest(testDispatcher) {
        val ride = RideEntity(id = 5, name = "Ride 5", startTime = 1000L, status = RideStatus.COMPLETED)
        fakeRideDao.ridesMap[5L] = ride
        fakeRidePointDao.pointsMap[5L] = mutableListOf(
            RidePointEntity(id = 101, rideId = 5L, timestamp = 1000L, latitude = 10.0, longitude = 20.0),
            RidePointEntity(id = 102, rideId = 5L, timestamp = 2000L, latitude = 10.1, longitude = 20.1)
        )

        repository.commitDeletion(5L)
        advanceUntilIdle()

        assertNull(fakeRideDao.ridesMap[5L])
        assertTrue("RidePoints must be wiped on commit deletion", fakeRidePointDao.pointsMap[5L].isNullOrEmpty())
    }

    @Test
    fun renameRide_updatesRideNameInDao() = runTest(testDispatcher) {
        val ride = RideEntity(id = 10, name = "Original", startTime = 1000L, status = RideStatus.COMPLETED)
        fakeRideDao.ridesMap[10L] = ride

        repository.renameRide(10L, "New Highway Name")
        advanceUntilIdle()

        assertEquals("New Highway Name", fakeRideDao.ridesMap[10L]?.name)
    }

    @Test
    fun getPointsForRide_convertsEntitiesToDomainGpsPointsAccurately() = runTest(testDispatcher) {
        val pointEntity = RidePointEntity(
            id = 1,
            rideId = 20,
            timestamp = 5000L,
            latitude = 12.34,
            longitude = 56.78,
            speedMs = 15.5,
            accuracyMeters = 4.2f,
            altitudeMeters = 120.0,
            isPaused = true,
            isGap = false
        )
        fakeRidePointDao.pointsMap[20L] = mutableListOf(pointEntity)

        val points = repository.getPointsForRide(20L)
        advanceUntilIdle()

        assertEquals(1, points.size)
        val p = points[0]
        assertEquals(5000L, p.timestampEpochMs)
        assertEquals(12.34, p.latitude, 0.0001)
        assertEquals(56.78, p.longitude, 0.0001)
        assertEquals(15.5f, p.speedMps)
        assertEquals(4.2f, p.accuracyMeters)
        assertEquals(120.0, p.altitudeMeters ?: 0.0, 0.1)
        assertTrue(p.isPaused)
    }
}

private class FakeRideDao : RideDao {
    val ridesMap = mutableMapOf<Long, RideEntity>()
    val finishedRidesFlow = MutableStateFlow<List<RideEntity>>(emptyList())

    override suspend fun insert(ride: RideEntity): Long {
        ridesMap[ride.id] = ride
        return ride.id
    }

    override suspend fun update(ride: RideEntity) {
        ridesMap[ride.id] = ride
    }

    override suspend fun delete(ride: RideEntity) {
        ridesMap.remove(ride.id)
    }

    override fun getAllRides(): Flow<List<RideEntity>> = finishedRidesFlow

    override suspend fun getRideById(rideId: Long): RideEntity? = ridesMap[rideId]

    override suspend fun getRidesByStatus(status: RideStatus): List<RideEntity> =
        ridesMap.values.filter { it.status == status }

    override fun getRidesForBike(bikeId: Long): Flow<List<RideEntity>> = finishedRidesFlow

    override suspend fun findActiveRide(): RideEntity? =
        ridesMap.values.find { it.status == RideStatus.ACTIVE }

    override fun getFinishedRides(): Flow<List<RideEntity>> = finishedRidesFlow

    override suspend fun updateRideName(rideId: Long, name: String) {
        val existing = ridesMap[rideId]
        if (existing != null) {
            ridesMap[rideId] = existing.copy(name = name)
        }
    }

    override suspend fun updateRideBike(rideId: Long, bikeId: Long?) {
        val existing = ridesMap[rideId]
        if (existing != null) {
            ridesMap[rideId] = existing.copy(bikeId = bikeId)
        }
    }

    override fun getTotalDistanceMetersForBike(bikeId: Long): Flow<Double?> =
        MutableStateFlow(ridesMap.values.filter { it.bikeId == bikeId }.sumOf { it.distanceMeters })

    override suspend fun getTotalDistanceMetersForBikeOnce(bikeId: Long): Double? =
        ridesMap.values.filter { it.bikeId == bikeId }.sumOf { it.distanceMeters }

    override suspend fun findDuplicateRide(minStartTime: Long, maxStartTime: Long, minDistance: Double, maxDistance: Double): RideEntity? = null
    override suspend fun getAllRidesOnce(): List<RideEntity> = ridesMap.values.toList()
    override suspend fun insertAll(rides: List<RideEntity>) { rides.forEach { ridesMap[it.id] = it } }
    override suspend fun deleteAllRides() { ridesMap.clear() }
}

private class FakeRidePointDao : RidePointDao {
    val pointsMap = mutableMapOf<Long, MutableList<RidePointEntity>>()

    override suspend fun insertAll(points: List<RidePointEntity>) {
        points.forEach { insert(it) }
    }

    override suspend fun insert(point: RidePointEntity): Long {
        val list = pointsMap.getOrPut(point.rideId) { mutableListOf() }
        list.add(point)
        return point.id
    }

    override fun getPointsForRide(rideId: Long): Flow<List<RidePointEntity>> =
        MutableStateFlow(pointsMap[rideId].orEmpty())

    override suspend fun getPointsForRideOnce(rideId: Long): List<RidePointEntity> =
        pointsMap[rideId].orEmpty()

    override suspend fun getPointCount(rideId: Long): Int =
        pointsMap[rideId]?.size ?: 0

    override suspend fun deletePointsForRide(rideId: Long) {
        pointsMap.remove(rideId)
    }

    override suspend fun getAllPointsOnce(): List<RidePointEntity> =
        pointsMap.values.flatten()

    override suspend fun deleteAllPoints() {
        pointsMap.clear()
    }
}
