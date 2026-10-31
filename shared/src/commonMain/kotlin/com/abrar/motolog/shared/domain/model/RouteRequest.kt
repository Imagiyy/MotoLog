package com.abrar.motolog.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Parameters for requesting a calculated route between two points.
 *
 * @property origin Starting location point.
 * @property destination Target location point.
 * @property travelProfile Vehicle mode (default: MOTORCYCLE).
 * @property avoidances Road features to avoid during routing.
 * @property alternatives Whether to request alternative route candidates.
 * @property requestTimestamp Monotonic or wall-clock epoch timestamp (ms) of request dispatch.
 */
@Serializable
data class RouteRequest(
    val origin: LocationPoint,
    val destination: LocationPoint,
    val travelProfile: RouteProfile = RouteProfile.MOTORCYCLE,
    val avoidances: Set<RouteAvoidance> = emptySet(),
    val alternatives: Boolean = false,
    val requestTimestamp: Long = 0L
)

@Serializable
enum class RouteProfile {
    MOTORCYCLE,
    CAR
}

@Serializable
enum class RouteAvoidance {
    TOLLS,
    HIGHWAYS,
    FERRIES,
    UNPAVED
}
