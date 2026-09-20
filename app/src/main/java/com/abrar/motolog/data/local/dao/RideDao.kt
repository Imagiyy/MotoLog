package com.abrar.motolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for Ride entities.
 */
@Dao
interface RideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ride: RideEntity): Long

    @Update
    suspend fun update(ride: RideEntity)

    @Delete
    suspend fun delete(ride: RideEntity)

    @Query("SELECT * FROM rides ORDER BY startTime DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun getRideById(rideId: Long): RideEntity?

    @Query("SELECT * FROM rides WHERE status = :status")
    suspend fun getRidesByStatus(status: RideStatus): List<RideEntity>

    @Query("SELECT * FROM rides WHERE bikeId = :bikeId ORDER BY startTime DESC")
    fun getRidesForBike(bikeId: Long): Flow<List<RideEntity>>

    /** Find any ride left in ACTIVE status — used for crash recovery */
    @Query("SELECT * FROM rides WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun findActiveRide(): RideEntity?

    /** Flow of finished rides (COMPLETED and RECOVERED only, excluding ACTIVE), newest first */
    @Query("SELECT * FROM rides WHERE status IN ('COMPLETED', 'RECOVERED') ORDER BY startTime DESC")
    fun getFinishedRides(): Flow<List<RideEntity>>

    /** Updates the name of a ride */
    @Query("UPDATE rides SET name = :name WHERE id = :rideId")
    suspend fun updateRideName(rideId: Long, name: String)

    /** Reassigns a ride to a different bike (or unassigns if null) */
    @Query("UPDATE rides SET bikeId = :bikeId WHERE id = :rideId")
    suspend fun updateRideBike(rideId: Long, bikeId: Long?)

    /** Cumulative distance in meters for a bike from finished rides */
    @Query("SELECT SUM(distanceMeters) FROM rides WHERE bikeId = :bikeId AND status IN ('COMPLETED', 'RECOVERED')")
    fun getTotalDistanceMetersForBike(bikeId: Long): Flow<Double?>

    /** Cumulative distance in meters for a bike (one-shot query) */
    @Query("SELECT SUM(distanceMeters) FROM rides WHERE bikeId = :bikeId AND status IN ('COMPLETED', 'RECOVERED')")
    suspend fun getTotalDistanceMetersForBikeOnce(bikeId: Long): Double?
}
