package com.abrar.motolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abrar.motolog.data.local.entity.MileageCycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MileageCycleDao {

    @Query("SELECT * FROM mileage_cycles WHERE bikeId = :bikeId ORDER BY resetTimestampEpochMs DESC")
    fun observeCyclesForBike(bikeId: Long): Flow<List<MileageCycleEntity>>

    @Query("SELECT * FROM mileage_cycles WHERE bikeId = :bikeId ORDER BY resetTimestampEpochMs DESC")
    suspend fun getCyclesForBike(bikeId: Long): List<MileageCycleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: MileageCycleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cycles: List<MileageCycleEntity>)

    @Delete
    suspend fun delete(cycle: MileageCycleEntity)

    @Query("DELETE FROM mileage_cycles WHERE bikeId = :bikeId")
    suspend fun deleteCyclesForBike(bikeId: Long)

    @Query("DELETE FROM mileage_cycles")
    suspend fun deleteAll()

    @Query("SELECT * FROM mileage_cycles ORDER BY id ASC")
    suspend fun getAllOnce(): List<MileageCycleEntity>
}
