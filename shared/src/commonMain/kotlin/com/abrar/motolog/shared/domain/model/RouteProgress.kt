package com.abrar.motolog.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Status of the rider relative to the planned route.
 */
@Serializable
enum class NavigationStatus {
    /** Rider is progressing along the planned route within tolerance. */
    ON_ROUTE,

    /** Rider has deviated beyond warning threshold (40m) for over 5 seconds. */
    OFF_ROUTE_WARNING,

    /** Rider has deviated beyond reroute threshold (80m) for over 10 seconds. Reroute required. */
    OFF_ROUTE_REROUTE_NEEDED,

    /** Rider has reached the destination within arrival radius. */
    ARRIVED,

    /** GPS accuracy is insufficient or signal is lost; progress calculations paused. */
    PAUSED_NO_GPS
}

/**
 * Real-time progress snapshot along a planned route.
 *
 * @property nearestPointIndex Index in route geometry representing the nearest forward segment start.
 * @property distanceAlongRouteMeters Cumulative distance in meters traveled along the route from start.
 * @property remainingDistanceMeters Estimated remaining distance in meters to destination along the route.
 * @property progressFraction Route completion ratio between 0.0f and 1.0f.
 * @property crossTrackDistanceMeters Perpendicular distance in meters from the current position to the nearest route segment.
 * @property status Current navigational state (on-route, warning, reroute needed, arrived, paused).
 * @property currentInstructionIndex Index of the active / upcoming maneuver in [PlannedRoute.instructions].
 * @property timestamp Monotonic or wall-clock timestamp (ms) when this progress snapshot was calculated.
 */
@Serializable
data class RouteProgress(
    val nearestPointIndex: Int,
    val distanceAlongRouteMeters: Double,
    val remainingDistanceMeters: Double,
    val progressFraction: Float,
    val crossTrackDistanceMeters: Double,
    val status: NavigationStatus,
    val currentInstructionIndex: Int? = null,
    val timestamp: Long = 0L
)
