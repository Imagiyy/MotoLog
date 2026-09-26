package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.shared.domain.model.GraphPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphDataPreparerTest {

    private fun createEntity(
        timestamp: Long,
        speedMs: Number = 10.0,
        altitudeMeters: Double = 100.0,
        isPaused: Boolean = false,
        isGap: Boolean = false
    ) = RidePointEntity(
        rideId = 1L,
        latitude = 12.0,
        longitude = 77.0,
        speedMs = speedMs.toDouble(),
        accuracyMeters = 5f,
        altitudeMeters = altitudeMeters,
        timestamp = timestamp,
        isPaused = isPaused,
        isGap = isGap
    )

    @Test
    fun `insufficient points returns null graph`() {
        assertNull(GraphDataPreparer.prepareSpeedGraph(emptyList(), 0L))
        assertNull(GraphDataPreparer.prepareSpeedGraph(listOf(createEntity(1000L)), 0L))

        assertNull(GraphDataPreparer.prepareElevationGraph(emptyList(), 0L))
        assertNull(GraphDataPreparer.prepareElevationGraph(listOf(createEntity(1000L)), 0L))
    }

    @Test
    fun `speed graph converts speed from m_s to km_h`() {
        val points = listOf(
            createEntity(1000L, speedMs = 10f), // 36 km/h
            createEntity(2000L, speedMs = 20f), // 72 km/h
            createEntity(3000L, speedMs = 15f)  // 54 km/h
        )

        val graph = GraphDataPreparer.prepareSpeedGraph(points, startTimeMs = 1000L)
        assertNotNull(graph)
        assertEquals(1, graph!!.segments.size)
        assertEquals(3, graph.segments[0].points.size)
        assertEquals(36f, graph.segments[0].points[0].y, 0.1f)
        assertEquals(72f, graph.segments[0].points[1].y, 0.1f)
        assertEquals(54f, graph.segments[0].points[2].y, 0.1f)
    }

    @Test
    fun `gaps split line into multiple segments`() {
        val points = listOf(
            createEntity(1000L, speedMs = 10f),
            createEntity(2000L, speedMs = 10f),
            createEntity(3000L, speedMs = 10f, isGap = true), // Gap marker
            createEntity(4000L, speedMs = 15f),
            createEntity(5000L, speedMs = 15f)
        )

        val graph = GraphDataPreparer.prepareSpeedGraph(points, startTimeMs = 1000L)
        assertNotNull(graph)
        assertEquals(2, graph!!.segments.size)
    }

    @Test
    fun `elevation graph ignores points with zero altitude`() {
        val points = listOf(
            createEntity(1000L, altitudeMeters = 0.0),
            createEntity(2000L, altitudeMeters = 0.0)
        )

        assertNull(GraphDataPreparer.prepareElevationGraph(points, startTimeMs = 1000L))
    }

    @Test
    fun `elevation graph correctly extracts altitude and adds padding`() {
        val points = listOf(
            createEntity(1000L, altitudeMeters = 100.0),
            createEntity(2000L, altitudeMeters = 150.0),
            createEntity(3000L, altitudeMeters = 200.0)
        )

        val graph = GraphDataPreparer.prepareElevationGraph(points, startTimeMs = 1000L)
        assertNotNull(graph)
        assertEquals(1, graph!!.segments.size)
        // Range should encompass 100 to 200 with padding
        assertTrue(graph.yRange.start <= 100f)
        assertTrue(graph.yRange.endInclusive >= 200f)
    }

    @Test
    fun `LTTB downsamples large list of points while keeping first and last`() {
        val input = (0..100).map { i ->
            GraphPoint(i.toFloat(), kotlin.math.sin(i * 0.1).toFloat() * 10f)
        }

        val downsampled = GraphDataPreparer.lttbDownsample(input, targetCount = 20)
        assertEquals(20, downsampled.size)
        assertEquals(input.first(), downsampled.first())
        assertEquals(input.last(), downsampled.last())
    }
}
