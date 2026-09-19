package com.abrar.motolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.abrar.motolog.data.local.entity.BikeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for Bike entities.
 */
@Dao
interface BikeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bike: BikeEntity): Long

    @Update
    suspend fun update(bike: BikeEntity)

    @Delete
    suspend fun delete(bike: BikeEntity)

    @Query("SELECT * FROM bikes ORDER BY createdAt DESC")
    fun getAllBikes(): Flow<List<BikeEntity>>

    @Query("SELECT * FROM bikes WHERE id = :bikeId")
    suspend fun getBikeById(bikeId: Long): BikeEntity?

    @Query("SELECT COUNT(*) FROM bikes")
    suspend fun getBikeCount(): Int
}
