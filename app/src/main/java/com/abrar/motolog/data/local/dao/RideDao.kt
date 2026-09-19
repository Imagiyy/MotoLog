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
}
