package com.abrar.motolog.data.repository

import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.shared.domain.model.GpsPoint
import com.abrar.motolog.domain.repository.RideRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepositoryImpl(
    private val rideDao: RideDao,
    private val ridePointDao: RidePointDao,
    private val bikeDao: BikeDao?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RideRepository {

    @Inject
    constructor(
        rideDao: RideDao,
        ridePointDao: RidePointDao,
        bikeDao: BikeDao
    ) : this(rideDao, ridePointDao, bikeDao, Dispatchers.IO)

    constructor(
        rideDao: RideDao,
        ridePointDao: RidePointDao,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(rideDao, ridePointDao, null, ioDispatcher)

    /** In-memory set of ride IDs pending deletion during the undo window */
    private val _pendingDeletionRideIds = MutableStateFlow<Set<Long>>(emptySet())

    override fun getFinishedRides(): Flow<List<RideEntity>> {
        return rideDao.getFinishedRides().combine(_pendingDeletionRideIds) { rides, pendingIds ->
            rides.filter { it.id !in pendingIds }
        }
    }

    override suspend fun getRideById(rideId: Long): RideEntity? = withContext(ioDispatcher) {
        rideDao.getRideById(rideId)
    }

    override suspend fun getPointsForRide(rideId: Long): List<GpsPoint> = withContext(ioDispatcher) {
        val entities = ridePointDao.getPointsForRideOnce(rideId)
        entities.map { entity ->
            GpsPoint(
                timestampEpochMs = entity.timestamp,
                latitude = entity.latitude,
                longitude = entity.longitude,
                speedMps = entity.speedMs.toFloat(),
                accuracyMeters = entity.accuracyMeters,
                speedAccuracyMps = null,
                altitudeMeters = entity.altitudeMeters,
                isPaused = entity.isPaused,
                isGap = entity.isGap
            )
        }
    }

    override suspend fun renameRide(rideId: Long, newName: String) = withContext(ioDispatcher) {
        rideDao.updateRideName(rideId, newName)
    }

    override suspend fun markForDeletion(rideId: Long) {
        _pendingDeletionRideIds.update { it + rideId }
    }

    override suspend fun undoDeletion(rideId: Long) {
        _pendingDeletionRideIds.update { it - rideId }
    }

    override suspend fun commitDeletion(rideId: Long) = withContext(ioDispatcher) {
        _pendingDeletionRideIds.update { it - rideId }
        val ride = rideDao.getRideById(rideId)
        if (ride != null) {
            // When a ride is deleted from ride history, its distance should NOT reduce
            // the vehicle's odometer in the garage. Instead, transfer its distance into
            // the bike's unlogged/calibration offset (odometerOffsetKm).
            val bikeId = ride.bikeId
            if (bikeId != null && bikeDao != null &&
                ride.status in listOf(RideStatus.COMPLETED, RideStatus.RECOVERED) &&
                (!ride.isImported || ride.countsTowardOdometer) &&
                ride.distanceMeters > 0.0
            ) {
                val bike = bikeDao.getBikeById(bikeId)
                if (bike != null) {
                    val rideKm = ride.distanceMeters / 1000.0
                    val newOffset = bike.odometerOffsetKm + rideKm
                    bikeDao.updateOdometerOffset(bikeId, newOffset)
                }
            }

            // Explicitly delete points first, then the ride itself
            ridePointDao.deletePointsForRide(rideId)
            rideDao.delete(ride)
        }
    }
}
