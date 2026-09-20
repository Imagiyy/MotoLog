package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.RidePointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMapPreparerTest {
    private fun point(index: Int, speedMs: Double, paused: Boolean = false, gap: Boolean = false) =
        RidePointEntity(
            rideId = 1L,
            timestamp = index * 1_000L,
            latitude = 12.0 + index * 0.001,
            longitude = 77.0 + index * 0.001,
            speedMs = speedMs,
            accuracyMeters = 5f,
            altitudeMeters = 100.0,
            isPaused = paused,
            isGap = gap
        )

    @Test
    fun `route preserves endpoints and excludes paused and gap points`() {
        val route = RouteMapPreparer.prepare(
            listOf(
                point(0, 5.0),
                point(1, 10.0),
                point(2, 15.0, paused = true),
                point(3, 20.0, gap = true),
                point(4, 25.0)
            )
        )

        assertEquals(12.0, route.start!!.latitude, 0.0)
        assertEquals(12.004, route.end!!.latitude, 0.0)
        assertTrue(route.segments.isNotEmpty())
        assertTrue(route.segments.all { it.points.size >= 2 })
    }

    @Test
    fun `route keeps speed scale bounded against noisy maximum`() {
        val points = (0..20).map { index ->
            point(index, if (index == 10) 300.0 else 20.0)
        }
        val route = RouteMapPreparer.prepare(points)

        assertTrue(route.maxSpeedKmh < 1_100.0)
        assertTrue(route.maxSpeedKmh >= route.minSpeedKmh)
    }
}
