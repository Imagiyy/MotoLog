package com.abrar.motolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.abrar.motolog.data.local.entity.FuelLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FuelLogEntity): Long

    @Update
    suspend fun update(log: FuelLogEntity)

    @Delete
    suspend fun delete(log: FuelLogEntity)

    @Query("SELECT * FROM fuel_logs WHERE bikeId = :bikeId ORDER BY odometerKm DESC, timestampEpochMs DESC")
    fun getLogsForBike(bikeId: Long): Flow<List<FuelLogEntity>>

    @Query("SELECT * FROM fuel_logs WHERE bikeId = :bikeId ORDER BY odometerKm ASC, timestampEpochMs ASC")
    suspend fun getLogsForBikeAscending(bikeId: Long): List<FuelLogEntity>

    @Query("SELECT * FROM fuel_logs WHERE id = :id")
    suspend fun getLogById(id: Long): FuelLogEntity?

    @Query("SELECT * FROM fuel_logs WHERE bikeId = :bikeId ORDER BY odometerKm DESC, timestampEpochMs DESC LIMIT 1")
    suspend fun getLatestLogForBike(bikeId: Long): FuelLogEntity?

    @Query("SELECT * FROM fuel_logs ORDER BY id ASC")
    suspend fun getAllLogsOnce(): List<FuelLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<FuelLogEntity>)

    @Query("DELETE FROM fuel_logs")
    suspend fun deleteAllFuelLogs()
}
