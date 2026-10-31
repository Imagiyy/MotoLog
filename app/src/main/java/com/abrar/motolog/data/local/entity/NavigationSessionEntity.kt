package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an active or completed navigation session.
 *
 * Enables crash recovery, resuming after process death, and inspecting planned routes.
 */
@Entity(
    tableName = "navigation_sessions",
    indices = [
        Index(value = ["rideId"]),
        Index(value = ["startedAtTimestamp"])
    ]
)
data class NavigationSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val plannedRouteId: String,
    val rideId: Long? = null,
    val status: String, // NavigationStatus.name
    val startedAtTimestamp: Long,
    val endedAtTimestamp: Long? = null,
    val lastKnownProgressJson: String? = null, // serialized RouteProgress
    val routeSnapshotJson: String? = null, // serialized PlannedRoute
    val rerouteCount: Int = 0
)
