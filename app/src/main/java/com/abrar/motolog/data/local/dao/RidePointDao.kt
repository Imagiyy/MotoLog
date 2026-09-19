package com.abrar.motolog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abrar.motolog.data.local.entity.RidePointEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for RidePoint entities.
 * Points are written in batches during an active ride and
 * read for map rendering and ride detail display.
 */
@Dao
interface RidePointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<RidePointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: RidePointEntity): Long

    @Query("SELECT * FROM ride_points WHERE rideId = :rideId ORDER BY timestamp ASC")
    fun getPointsForRide(rideId: Long): Flow<List<RidePointEntity>>

    @Query("SELECT * FROM ride_points WHERE rideId = :rideId ORDER BY timestamp ASC")
    suspend fun getPointsForRideOnce(rideId: Long): List<RidePointEntity>

    @Query("SELECT COUNT(*) FROM ride_points WHERE rideId = :rideId")
    suspend fun getPointCount(rideId: Long): Int

    @Query("DELETE FROM ride_points WHERE rideId = :rideId")
    suspend fun deletePointsForRide(rideId: Long)
}
