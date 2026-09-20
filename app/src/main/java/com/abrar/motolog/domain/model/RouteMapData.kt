package com.abrar.motolog.domain.model

/** Pure route data prepared for map rendering. */
data class RouteMapData(
    val segments: List<RouteMapSegment>,
    val start: RouteMapPoint?,
    val end: RouteMapPoint?,
    val minSpeedKmh: Double,
    val maxSpeedKmh: Double
)

data class RouteMapSegment(
    val speedBucket: Int,
    val points: List<RouteMapPoint>
)

data class RouteMapPoint(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double
)
