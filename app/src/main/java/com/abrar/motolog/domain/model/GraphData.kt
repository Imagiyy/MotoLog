package com.abrar.motolog.domain.model

/**
 * Data model for rendering line graphs with Compose Canvas.
 *
 * Pure Kotlin — no Android or Compose imports.
 * Supports breaking lines at pauses and gaps via multiple segments.
 */
data class GraphData(
    /** Separate line segments (each is a continuous, non-broken section). */
    val segments: List<GraphSegment>,
    /** X-axis range (elapsed time in seconds). */
    val xRange: ClosedFloatingPointRange<Float>,
    /** Y-axis range (speed in km/h or elevation in meters). */
    val yRange: ClosedFloatingPointRange<Float>,
    /** Label for the X-axis (e.g. "Time"). */
    val xLabel: String,
    /** Label for the Y-axis (e.g. "Speed (km/h)"). */
    val yLabel: String
)

/**
 * A single continuous segment of a graph line.
 * Multiple segments allow breaks at pauses and gaps.
 */
data class GraphSegment(val points: List<GraphPoint>)

/**
 * A single data point on a graph.
 */
data class GraphPoint(val x: Float, val y: Float)

/**
 * A reference marker line on a graph (e.g. max speed, average).
 *
 * @property y The Y value for the horizontal marker line.
 * @property label Label text (e.g. "Max: 120 km/h").
 * @property colorArgb Color as ARGB int.
 */
data class GraphMarker(
    val y: Float,
    val label: String,
    val colorArgb: Int
)
