package com.abrar.motolog.domain.repository

import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.NavigationSession
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteProgress
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing persistent navigation sessions,
 * crash recovery checkpoints, and saved/favorite destinations.
 */
interface NavigationSessionRepository {

    /** Save an active navigation session alongside its complete planned route snapshot. */
    suspend fun saveActiveSession(session: NavigationSession, route: PlannedRoute)

    /** Checkpoint live route progress for process death / crash recovery. */
    suspend fun checkpointProgress(sessionId: String, progress: RouteProgress)

    /** Retrieve active unfinished navigation session and route snapshot. */
    suspend fun getActiveSession(): Pair<NavigationSession, PlannedRoute?>?

    /** Observe the current active navigation session. */
    fun observeActiveSession(): Flow<NavigationSession?>

    /** Mark session completed or arrived. */
    suspend fun finishSession(sessionId: String, finalStatus: NavigationStatus)

    /** Delete/discard an unfinished session. */
    suspend fun discardSession(sessionId: String)

    /** Increment reroute counter for an active session. */
    suspend fun incrementRerouteCount(sessionId: String)

    // ============================================================
    // Saved & Favorite Destinations
    // ============================================================

    fun getSavedDestinations(): Flow<List<Destination>>

    fun getFavoriteDestinations(): Flow<List<Destination>>

    suspend fun saveDestination(destination: Destination, isFavorite: Boolean = false)

    suspend fun toggleFavoriteDestination(id: String, isFavorite: Boolean)

    suspend fun deleteSavedDestination(id: String)
}
