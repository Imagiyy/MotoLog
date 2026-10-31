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

import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.shared.domain.engine.OdometerCalculator

@OptIn(ExperimentalCoroutinesApi::class)
class RideRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRideDao: FakeRideDao
    private lateinit var fakeRidePointDao: FakeRidePointDao
    private lateinit var fakeBikeDao: FakeBikeDao
    private lateinit var repository: RideRepositoryImpl

    @Before
    fun setUp() {
        fakeRideDao = FakeRideDao()
        fakeRidePointDao = FakeRidePointDao()
        fakeBikeDao = FakeBikeDao()
        repository = RideRepositoryImpl(fakeRideDao, fakeRidePointDao, fakeBikeDao, testDispatcher)
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
    fun commitDeletion_transfersRideDistanceToBikeOdometerOffsetAndPreservesOdometer() = runTest(testDispatcher) {
        val bike = BikeEntity(id = 1L, name = "Honda CB300R", initialOdometerKm = 1000.0, odometerOffsetKm = 10.0)
        fakeBikeDao.bikesMap[1L] = bike

        val ride = RideEntity(
            id = 50L,
            bikeId = 1L,
            name = "Sunday Ride",
            startTime = 1000L,
            distanceMeters = 25000.0, // 25 km
            status = RideStatus.COMPLETED
        )
        fakeRideDao.ridesMap[50L] = ride

        // Initial state before deletion:
        // Recorded distance = 25 km, Initial = 1000 km, Offset = 10 km -> Total Odometer = 1035 km
        val initialOdo = OdometerCalculator.calculateCurrentOdometerKm(
            initialOdometerKm = bike.initialOdometerKm,
            recordedDistanceMeters = ride.distanceMeters,
            odometerOffsetKm = bike.odometerOffsetKm
        )
        assertEquals(1035.0, initialOdo, 0.001)

        // Delete the ride
        repository.commitDeletion(50L)
        advanceUntilIdle()

        // Ride is deleted from rides table
        assertNull(fakeRideDao.ridesMap[50L])

        // Bike offset increased by 25 km (from 10.0 to 35.0)
        val updatedBike = fakeBikeDao.bikesMap[1L]
        assertEquals(35.0, updatedBike?.odometerOffsetKm ?: 0.0, 0.001)

        // New odometer calculation:
        // Recorded distance = 0 km, Initial = 1000 km, Offset = 35 km -> Total Odometer = 1035 km (UNCHANGED!)
        val finalOdo = OdometerCalculator.calculateCurrentOdometerKm(
            initialOdometerKm = updatedBike?.initialOdometerKm ?: 0.0,
            recordedDistanceMeters = 0.0,
            odometerOffsetKm = updatedBike?.odometerOffsetKm ?: 0.0
        )
        assertEquals(initialOdo, finalOdo, 0.001)
    }

    @Test
    fun commitDeletion_importedRideNotCountingTowardsOdometerDoesNotAlterBikeOffset() = runTest(testDispatcher) {
        val bike = BikeEntity(id = 1L, name = "Honda CB300R", initialOdometerKm = 1000.0, odometerOffsetKm = 10.0)
        fakeBikeDao.bikesMap[1L] = bike

        val importedRide = RideEntity(
            id = 51L,
            bikeId = 1L,
            name = "Old GPX Import",
            startTime = 1000L,
            distanceMeters = 50000.0,
            status = RideStatus.COMPLETED,
            isImported = true,
            countsTowardOdometer = false
        )
        fakeRideDao.ridesMap[51L] = importedRide

        repository.commitDeletion(51L)
        advanceUntilIdle()

        assertNull(fakeRideDao.ridesMap[51L])
        // Offset remains 10.0 because the ride never counted towards the odometer
        assertEquals(10.0, fakeBikeDao.bikesMap[1L]?.odometerOffsetKm ?: 0.0, 0.001)
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

    override suspend fun getTotalPointCount(): Int =
        pointsMap.values.sumOf { it.size }

    override suspend fun getPointsPaged(limit: Int, offset: Int): List<RidePointEntity> =
        pointsMap.values.flatten().drop(offset).take(limit)

    override suspend fun deleteAllPoints() {
        pointsMap.clear()
    }
}

private class FakeBikeDao : BikeDao {
    val bikesMap = mutableMapOf<Long, BikeEntity>()

    override suspend fun insert(bike: BikeEntity): Long {
        bikesMap[bike.id] = bike
        return bike.id
    }

    override suspend fun update(bike: BikeEntity) {
        bikesMap[bike.id] = bike
    }

    override suspend fun delete(bike: BikeEntity) {
        bikesMap.remove(bike.id)
    }

    override fun getAllBikes(): Flow<List<BikeEntity>> =
        MutableStateFlow(bikesMap.values.toList())

    override fun getActiveBikes(): Flow<List<BikeEntity>> =
        MutableStateFlow(bikesMap.values.filter { !it.isArchived })

    override suspend fun getActiveBikesOnce(): List<BikeEntity> =
        bikesMap.values.filter { !it.isArchived }

    override fun getArchivedBikes(): Flow<List<BikeEntity>> =
        MutableStateFlow(bikesMap.values.filter { it.isArchived })

    override suspend fun getBikeById(bikeId: Long): BikeEntity? = bikesMap[bikeId]

    override suspend fun getActiveBikeCount(): Int =
        bikesMap.values.count { !it.isArchived }

    override suspend fun getRideCountForBike(bikeId: Long): Int = 0

    override suspend fun setArchived(bikeId: Long, isArchived: Boolean) {
        val existing = bikesMap[bikeId]
        if (existing != null) {
            bikesMap[bikeId] = existing.copy(isArchived = isArchived)
        }
    }

    override suspend fun updateOdometerOffset(bikeId: Long, offsetKm: Double) {
        val existing = bikesMap[bikeId]
        if (existing != null) {
            bikesMap[bikeId] = existing.copy(odometerOffsetKm = offsetKm)
        }
    }

    override suspend fun updateMileageReset(bikeId: Long, resetOdoKm: Double?, resetEpochMs: Long?) {
        val existing = bikesMap[bikeId]
        if (existing != null) {
            bikesMap[bikeId] = existing.copy(mileageResetOdometerKm = resetOdoKm, mileageResetEpochMs = resetEpochMs)
        }
    }

    override suspend fun getAllBikesOnce(): List<BikeEntity> = bikesMap.values.toList()

    override suspend fun insertAll(bikes: List<BikeEntity>) {
        bikes.forEach { bikesMap[it.id] = it }
    }

    override suspend fun deleteAllBikes() {
        bikesMap.clear()
    }
}
