package com.abrar.motolog.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * A calculated motorcycle navigation route returned by a routing provider.
 *
 * @property id Unique identifier for the route session.
 * @property geometry Ordered list of route coordinates.
 * @property totalDistanceMeters Total route distance in meters.
 * @property expectedDurationSeconds Estimated travel time in seconds.
 * @property bounds Bounding box covering the entire route geometry for map camera framing.
 * @property instructions Turn-by-turn guidance maneuvers along the route.
 * @property providerAttribution Attribution or copyright string required by the routing provider.
 * @property calculatedAtTimestamp UTC timestamp (ms) when route was computed.
 */
@Serializable
data class PlannedRoute(
    val id: String,
    val geometry: List<LocationPoint>,
    val totalDistanceMeters: Double,
    val expectedDurationSeconds: Long,
    val bounds: RouteBoundingBox? = null,
    val instructions: List<RouteInstruction> = emptyList(),
    val providerAttribution: String? = null,
    val calculatedAtTimestamp: Long = 0L
)

/**
 * Latitude/Longitude bounding box encompassing a route.
 */
@Serializable
data class RouteBoundingBox(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
)

/**
 * A discrete navigation maneuver / instruction along a route.
 *
 * @property maneuverType Classification of the maneuver (e.g. turn, continue, roundabout).
 * @property streetName Name of the road or street being entered.
 * @property distanceMeters Distance in meters from this instruction to the next.
 * @property durationSeconds Expected duration in seconds to traverse this step.
 * @property instructionText Human-readable guidance text (e.g., "Turn right onto Ring Road").
 * @property pointIndex Index in [PlannedRoute.geometry] where this maneuver occurs.
 * @property bearingDegrees Expected heading/bearing after executing this maneuver (0..360°).
 */
@Serializable
data class RouteInstruction(
    val maneuverType: ManeuverType,
    val streetName: String? = null,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0L,
    val instructionText: String = "",
    val pointIndex: Int = 0,
    val bearingDegrees: Double? = null
)

/**
 * Categorization of turn-by-turn maneuvers.
 */
@Serializable
enum class ManeuverType {
    DEPART,
    CONTINUE,
    TURN_SLIGHT_LEFT,
    TURN_LEFT,
    TURN_SHARP_LEFT,
    TURN_SLIGHT_RIGHT,
    TURN_RIGHT,
    TURN_SHARP_RIGHT,
    U_TURN,
    ROUNDABOUT,
    MERGE,
    RAMP,
    FORK,
    ARRIVE,
    UNKNOWN
}
