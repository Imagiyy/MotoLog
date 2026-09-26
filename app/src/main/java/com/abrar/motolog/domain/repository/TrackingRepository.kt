package com.abrar.motolog.domain.repository

import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining operations and shared state for the ride tracking session.
 */
interface TrackingRepository {

    /** Current shared session state observed by UI. */
    val sessionState: StateFlow<TrackingSessionState>

    /** Initiate a new ride tracking session. Starts foreground service. */
    fun startTracking()

    /** Pause an active ride session (manual pause). */
    fun pauseTracking()

    /** Resume a paused ride session. */
    fun resumeTracking()

    /** Stop tracking, save final stats, and terminate foreground service. */
    fun stopTracking()

    /** Reset state from Stopped to Idle. */
    fun resetToIdle()

    /** Check if there is an unfinished ride in the database from a crash/kill. */
    suspend fun checkActiveRideOnStartup(): RideEntity?

    /** Recover an active ride: reconstructs stats from points and marks RECOVERED. */
    suspend fun recoverRide(activeRide: RideEntity): RideStats

    /** Discard an active ride: deletes the ride and its points from the database. */
    suspend fun discardRide(activeRide: RideEntity)

    /** Update shared session state (called by TrackingService). */
    fun updateState(state: TrackingSessionState)
}
