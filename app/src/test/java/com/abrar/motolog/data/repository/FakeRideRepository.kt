package com.abrar.motolog.data.repository

import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.shared.domain.model.GpsPoint
import com.abrar.motolog.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakeRideRepository : RideRepository {

    val ridesFlow = MutableStateFlow<List<RideEntity>>(emptyList())
    val pointsMap = mutableMapOf<Long, List<GpsPoint>>()
    val pendingDeletions = MutableStateFlow<Set<Long>>(emptySet())

    fun seedRides(rides: List<RideEntity>) {
        ridesFlow.value = rides
    }

    override fun getFinishedRides(): Flow<List<RideEntity>> {
        return ridesFlow.combine(pendingDeletions) { list, pending ->
            list.filter { (it.status == RideStatus.COMPLETED || it.status == RideStatus.RECOVERED) && it.id !in pending }
                .sortedByDescending { it.startTime }
        }
    }

    override suspend fun getRideById(rideId: Long): RideEntity? {
        return ridesFlow.value.find { it.id == rideId }
    }

    override suspend fun getPointsForRide(rideId: Long): List<GpsPoint> {
        return pointsMap[rideId].orEmpty()
    }

    override suspend fun renameRide(rideId: Long, newName: String) {
        ridesFlow.value = ridesFlow.value.map {
            if (it.id == rideId) it.copy(name = newName) else it
        }
    }

    override suspend fun markForDeletion(rideId: Long) {
        pendingDeletions.value = pendingDeletions.value + rideId
    }

    override suspend fun undoDeletion(rideId: Long) {
        pendingDeletions.value = pendingDeletions.value - rideId
    }

    override suspend fun commitDeletion(rideId: Long) {
        pendingDeletions.value = pendingDeletions.value - rideId
        ridesFlow.value = ridesFlow.value.filter { it.id != rideId }
        pointsMap.remove(rideId)
    }
}
