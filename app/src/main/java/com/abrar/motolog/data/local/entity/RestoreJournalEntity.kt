package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Durable marker for a restore whose database transaction committed but whose
 * DataStore settings still need to be applied.
 */
@Entity(tableName = "restore_journal")
data class RestoreJournalEntity(
    @PrimaryKey val id: Int = 1,
    val settingsJson: String
)
