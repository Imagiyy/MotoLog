package com.abrar.motolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MaintenanceItemEntity): Long

    @Update
    suspend fun update(item: MaintenanceItemEntity)

    @Delete
    suspend fun delete(item: MaintenanceItemEntity)

    @Query("SELECT * FROM maintenance_items WHERE bikeId = :bikeId ORDER BY id ASC")
    fun getItemsForBike(bikeId: Long): Flow<List<MaintenanceItemEntity>>

    @Query("SELECT * FROM maintenance_items WHERE bikeId = :bikeId ORDER BY id ASC")
    suspend fun getItemsForBikeOnce(bikeId: Long): List<MaintenanceItemEntity>

    @Query("SELECT * FROM maintenance_items ORDER BY id ASC")
    suspend fun getAllItemsOnce(): List<MaintenanceItemEntity>

    @Query("SELECT * FROM maintenance_items WHERE id = :id")
    suspend fun getItemById(id: Long): MaintenanceItemEntity?

    @Query("UPDATE maintenance_items SET lastDoneOdometerKm = :odometerKm, lastDoneDateEpochMs = :dateEpochMs, lastNotifiedDueEpochMs = NULL WHERE id = :id")
    suspend fun markDone(id: Long, odometerKm: Double, dateEpochMs: Long)

    @Query("UPDATE maintenance_items SET lastNotifiedDueEpochMs = :notifiedEpochMs WHERE id = :id")
    suspend fun updateLastNotified(id: Long, notifiedEpochMs: Long)
}
