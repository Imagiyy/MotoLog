package com.abrar.motolog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abrar.motolog.data.local.entity.RestoreJournalEntity

@Dao
interface RestoreJournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(journal: RestoreJournalEntity)

    @Query("SELECT * FROM restore_journal WHERE id = 1")
    suspend fun get(): RestoreJournalEntity?

    @Query("DELETE FROM restore_journal WHERE id = 1")
    suspend fun delete()
}
