package com.abrar.motolog.domain.repository

import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.shared.domain.model.GpsPoint
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for accessing and managing completed and recovered rides.
 */
interface RideRepository {

    /**
     * Flow of all finished (COMPLETED and RECOVERED) rides, sorted newest first.
     * Excludes currently ACTIVE rides and rides pending deletion in the undo window.
     */
    fun getFinishedRides(): Flow<List<RideEntity>>

    /**
     * Fetches a single ride by ID.
     */
    suspend fun getRideById(rideId: Long): RideEntity?

    /**
     * Loads all recorded GPS points for a ride converted to pure domain [GpsPoint] objects.
     */
    suspend fun getPointsForRide(rideId: Long): List<GpsPoint>

    /**
     * Updates the name of an existing ride.
     */
    suspend fun renameRide(rideId: Long, newName: String)

    /**
     * Marks a ride for deletion (hides it from the finished rides Flow while the Undo snackbar is visible).
     */
    suspend fun markForDeletion(rideId: Long)

    /**
     * Cancels pending deletion, restoring the ride immediately to the finished rides Flow.
     */
    suspend fun undoDeletion(rideId: Long)

    /**
     * Permanently deletes the ride and all associated GPS points from the database.
     */
    suspend fun commitDeletion(rideId: Long)
}
