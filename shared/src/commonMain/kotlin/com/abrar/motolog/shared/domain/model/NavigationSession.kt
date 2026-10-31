package com.abrar.motolog.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Tracks the lifecycle and state of an active or completed navigation session.
 *
 * Persisted to allow crash recovery and post-ride comparison between planned route and actual track.
 *
 * @property sessionId Unique session identifier.
 * @property plannedRouteId Identifier of the [PlannedRoute] being navigated.
 * @property rideId Associated ride recording ID if navigation is paired with an active ride.
 * @property status Current navigation status.
 * @property startedAtTimestamp Monotonic or epoch timestamp (ms) when navigation began.
 * @property endedAtTimestamp Timestamp when navigation was finished or cancelled.
 * @property lastKnownProgress Last computed progress checkpoint for recovery.
 * @property rerouteCount Total number of automatic or manual reroutes triggered during this session.
 */
@Serializable
data class NavigationSession(
    val sessionId: String,
    val plannedRouteId: String,
    val rideId: Long? = null,
    val status: NavigationStatus = NavigationStatus.ON_ROUTE,
    val startedAtTimestamp: Long = 0L,
    val endedAtTimestamp: Long? = null,
    val lastKnownProgress: RouteProgress? = null,
    val rerouteCount: Int = 0
)
