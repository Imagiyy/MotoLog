package com.abrar.motolog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abrar.motolog.data.local.entity.SavedDestinationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDestinationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestination(destination: SavedDestinationEntity)

    @Query("SELECT * FROM saved_destinations ORDER BY isFavorite DESC, createdAtTimestamp DESC")
    fun getAllDestinations(): Flow<List<SavedDestinationEntity>>

    @Query("SELECT * FROM saved_destinations WHERE isFavorite = 1 ORDER BY createdAtTimestamp DESC")
    fun getFavoriteDestinations(): Flow<List<SavedDestinationEntity>>

    @Query("SELECT * FROM saved_destinations WHERE id = :id LIMIT 1")
    suspend fun getDestinationById(id: String): SavedDestinationEntity?

    @Query("DELETE FROM saved_destinations WHERE id = :id")
    suspend fun deleteDestinationById(id: String)

    @Query("UPDATE saved_destinations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM saved_destinations")
    suspend fun clearAll()
}
