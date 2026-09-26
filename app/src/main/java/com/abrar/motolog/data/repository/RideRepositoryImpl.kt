package com.abrar.motolog.data.repository

import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.RideEntity
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RideRepository {

    @Inject
    constructor(
        rideDao: RideDao,
        ridePointDao: RidePointDao
    ) : this(rideDao, ridePointDao, Dispatchers.IO)

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
            // Explicitly delete points first, then the ride itself
            ridePointDao.deletePointsForRide(rideId)
            rideDao.delete(ride)
        }
    }
}
