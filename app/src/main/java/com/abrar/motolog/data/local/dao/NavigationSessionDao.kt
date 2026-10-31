package com.abrar.motolog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abrar.motolog.data.local.entity.NavigationSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NavigationSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: NavigationSessionEntity)

    @Query("SELECT * FROM navigation_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): NavigationSessionEntity?

    @Query("SELECT * FROM navigation_sessions WHERE endedAtTimestamp IS NULL ORDER BY startedAtTimestamp DESC LIMIT 1")
    suspend fun getActiveSession(): NavigationSessionEntity?

    @Query("SELECT * FROM navigation_sessions WHERE endedAtTimestamp IS NULL ORDER BY startedAtTimestamp DESC LIMIT 1")
    fun observeActiveSession(): Flow<NavigationSessionEntity?>

    @Query("UPDATE navigation_sessions SET lastKnownProgressJson = :progressJson, status = :status WHERE sessionId = :sessionId")
    suspend fun updateProgress(sessionId: String, progressJson: String, status: String)

    @Query("UPDATE navigation_sessions SET status = :status, endedAtTimestamp = :endedAt WHERE sessionId = :sessionId")
    suspend fun endSession(sessionId: String, status: String, endedAt: Long)

    @Query("DELETE FROM navigation_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM navigation_sessions WHERE endedAtTimestamp IS NOT NULL AND endedAtTimestamp < :cutoffTimestamp")
    suspend fun pruneOldSessions(cutoffTimestamp: Long)
}
