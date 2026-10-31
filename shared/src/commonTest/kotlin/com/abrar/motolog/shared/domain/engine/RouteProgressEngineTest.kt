package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.ManeuverType
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteInstruction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RouteProgressEngineTest {

    private fun createPoint(
        lat: Double,
        lon: Double,
        speedMps: Float? = 10f, // ~36 km/h
        accuracyMeters: Float = 5f,
        timestamp: Long = 1000L,
        elapsedTimestampMs: Long? = 1000L
    ) = LocationPoint(
        latitude = lat,
        longitude = lon,
        speedMps = speedMps,
        accuracyMeters = accuracyMeters,
        timestamp = timestamp,
        elapsedTimestampMs = elapsedTimestampMs
    )

    private fun createRoute(
        geometry: List<LocationPoint>,
        totalDistanceMeters: Double = 0.0,
        instructions: List<RouteInstruction> = emptyList()
    ): PlannedRoute {
        return PlannedRoute(
            id = "test_route_1",
            geometry = geometry,
            totalDistanceMeters = totalDistanceMeters,
            expectedDurationSeconds = 600L,
            instructions = instructions,
            providerAttribution = "Test Provider",
            calculatedAtTimestamp = 1000L
        )
    }

    @Test
    fun straightRouteTrackingAndMonotonicProgress() {
        // Simple south-to-north route along 77.50 longitude
        // ~111 meters per 0.001 deg latitude
        val routePoints = listOf(
            createPoint(12.000, 77.500),
            createPoint(12.005, 77.500),
            createPoint(12.010, 77.500)
        )
        val route = createRoute(routePoints)
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        // Point near beginning
        val p1 = createPoint(12.001, 77.500, timestamp = 2000L, elapsedTimestampMs = 2000L)
        val progress1 = engine.onLocationUpdate(p1)

        assertEquals(NavigationStatus.ON_ROUTE, progress1.status)
        assertTrue(progress1.distanceAlongRouteMeters > 50.0)
        assertTrue(progress1.remainingDistanceMeters > 0.0)
        assertTrue(progress1.crossTrackDistanceMeters < 5.0)

        // Point midway
        val p2 = createPoint(12.006, 77.500, timestamp = 4000L, elapsedTimestampMs = 4000L)
        val progress2 = engine.onLocationUpdate(p2)

        assertTrue(progress2.distanceAlongRouteMeters > progress1.distanceAlongRouteMeters)
        assertTrue(progress2.remainingDistanceMeters < progress1.remainingDistanceMeters)
        assertTrue(progress2.progressFraction > progress1.progressFraction)
    }

    @Test
    fun jitterSuppressionKeepsDistanceMonotonic() {
        val routePoints = listOf(
            createPoint(12.000, 77.500),
            createPoint(12.010, 77.500)
        )
        val route = createRoute(routePoints)
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        // Rider reaches latitude 12.005
        val p1 = createPoint(12.00500, 77.500, timestamp = 5000L, elapsedTimestampMs = 5000L)
        val prog1 = engine.onLocationUpdate(p1)
        val dist1 = prog1.distanceAlongRouteMeters

        // Small GPS jitter backwards (~5m, within 10m tolerance)
        val p2 = createPoint(12.00495, 77.500, timestamp = 6000L, elapsedTimestampMs = 6000L)
        val prog2 = engine.onLocationUpdate(p2)

        // Distance should be clamped and not decrease
        assertEquals(dist1, prog2.distanceAlongRouteMeters, 0.001)
    }

    @Test
    fun immediateArrivalDetectionTriggersRegardlessOfSpeed() {
        val destination = createPoint(12.010, 77.500)
        val route = createRoute(listOf(createPoint(12.000, 77.500), destination))
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        // Location 8 meters from destination with high speed 20 m/s (72 km/h)
        val fix = createPoint(
            lat = 12.00993,
            lon = 77.500,
            speedMps = 20f,
            timestamp = 5000L,
            elapsedTimestampMs = 5000L
        )
        val progress = engine.onLocationUpdate(fix)

        assertEquals(NavigationStatus.ARRIVED, progress.status)
        assertEquals(1.0f, progress.progressFraction)
        assertEquals(0.0, progress.remainingDistanceMeters)
    }

    @Test
    fun speedConfirmedArrivalDetection() {
        val destination = createPoint(12.010, 77.500)
        val route = createRoute(listOf(createPoint(12.000, 77.500), destination))
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        // Location 30m from destination with low speed (1 m/s = 3.6 km/h <= 5 km/h)
        val fix = createPoint(
            lat = 12.00973,
            lon = 77.500,
            speedMps = 1f,
            timestamp = 5000L,
            elapsedTimestampMs = 5000L
        )
        val progress = engine.onLocationUpdate(fix)

        assertEquals(NavigationStatus.ARRIVED, progress.status)
    }

    @Test
    fun falseArrivalRejectionAtHighSpeed() {
        val destination = createPoint(12.010, 77.500)
        val route = createRoute(listOf(createPoint(12.000, 77.500), destination))
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        // Location 30m from destination but moving fast (15 m/s = 54 km/h > 5 km/h)
        val fix = createPoint(
            lat = 12.00973,
            lon = 77.500,
            speedMps = 15f,
            timestamp = 5000L,
            elapsedTimestampMs = 5000L
        )
        val progress = engine.onLocationUpdate(fix)

        assertFalse(progress.status == NavigationStatus.ARRIVED)
        assertEquals(NavigationStatus.ON_ROUTE, progress.status)
    }

    @Test
    fun offRouteWarningAfterSustainedDeviation() {
        // Route along longitude 77.500
        val route = createRoute(listOf(
            createPoint(12.000, 77.500),
            createPoint(12.010, 77.500)
        ))
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        // Deviate ~55m east (0.0005 deg longitude at lat 12 is ~54.3m)
        val devLon = 77.50050

        // t = 1000ms: initial deviation fix
        val fix1 = createPoint(12.005, devLon, timestamp = 1000L, elapsedTimestampMs = 1000L)
        val prog1 = engine.onLocationUpdate(fix1)
        assertEquals(NavigationStatus.ON_ROUTE, prog1.status)

        // t = 4000ms (3s elapsed): still within 5s grace period
        val fix2 = createPoint(12.005, devLon, timestamp = 4000L, elapsedTimestampMs = 4000L)
        val prog2 = engine.onLocationUpdate(fix2)
        assertEquals(NavigationStatus.ON_ROUTE, prog2.status)

        // t = 7000ms (6s elapsed): > 5s sustained deviation -> OFF_ROUTE_WARNING
        val fix3 = createPoint(12.005, devLon, timestamp = 7000L, elapsedTimestampMs = 7000L)
        val prog3 = engine.onLocationUpdate(fix3)
        assertEquals(NavigationStatus.OFF_ROUTE_WARNING, prog3.status)

        // Return to route -> instantly recovers to ON_ROUTE
        val recoverFix = createPoint(12.006, 77.500, timestamp = 8000L, elapsedTimestampMs = 8000L)
        val progRecover = engine.onLocationUpdate(recoverFix)
        assertEquals(NavigationStatus.ON_ROUTE, progRecover.status)
    }

    @Test
    fun offRouteRerouteTriggerAfterSustainedDeviation() {
        val route = createRoute(listOf(
            createPoint(12.000, 77.500),
            createPoint(12.010, 77.500)
        ))
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        // Deviate ~108m east (0.001 deg longitude at lat 12 is ~108.7m) > 80m reroute threshold
        val farLon = 77.50100

        // t = 0s
        val fix1 = createPoint(12.005, farLon, timestamp = 1000L, elapsedTimestampMs = 1000L)
        val prog1 = engine.onLocationUpdate(fix1)
        assertEquals(NavigationStatus.OFF_ROUTE_WARNING, prog1.status)

        // t = 8s
        val fix2 = createPoint(12.005, farLon, timestamp = 9000L, elapsedTimestampMs = 9000L)
        val prog2 = engine.onLocationUpdate(fix2)
        assertEquals(NavigationStatus.OFF_ROUTE_WARNING, prog2.status)

        // t = 11s (> 10s delay) -> OFF_ROUTE_REROUTE_NEEDED
        val fix3 = createPoint(12.005, farLon, timestamp = 12000L, elapsedTimestampMs = 12000L)
        val prog3 = engine.onLocationUpdate(fix3)
        assertEquals(NavigationStatus.OFF_ROUTE_REROUTE_NEEDED, prog3.status)
    }

    @Test
    fun rerouteCooldownPreventsImmediateRetrigger() {
        val route = createRoute(listOf(
            createPoint(12.000, 77.500),
            createPoint(12.010, 77.500)
        ))
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        val farLon = 77.50100 // ~108m off-route

        // Trigger reroute at t = 12000L
        engine.onLocationUpdate(createPoint(12.005, farLon, timestamp = 1000L, elapsedTimestampMs = 1000L))
        val rerouteNeeded = engine.onLocationUpdate(createPoint(12.005, farLon, timestamp = 12000L, elapsedTimestampMs = 12000L))
        assertEquals(NavigationStatus.OFF_ROUTE_REROUTE_NEEDED, rerouteNeeded.status)

        // Mark reroute dispatched
        engine.markRerouteTriggered(12000L)

        // 5s later at t = 17000L: sustained off-route, but reroute cooldown (20s) is active
        val cooldownFix = engine.onLocationUpdate(createPoint(12.005, farLon, timestamp = 17000L, elapsedTimestampMs = 17000L))
        assertEquals(NavigationStatus.OFF_ROUTE_WARNING, cooldownFix.status)

        // After cooldown passes (t = 33000L, 21s after last reroute and >10s sustained)
        engine.onLocationUpdate(createPoint(12.005, farLon, timestamp = 23000L, elapsedTimestampMs = 23000L))
        val newReroute = engine.onLocationUpdate(createPoint(12.005, farLon, timestamp = 34000L, elapsedTimestampMs = 34000L))
        assertEquals(NavigationStatus.OFF_ROUTE_REROUTE_NEEDED, newReroute.status)
    }

    @Test
    fun degradedGpsAndSignalLossHandledGracefully() {
        val route = createRoute(listOf(
            createPoint(12.000, 77.500),
            createPoint(12.010, 77.500)
        ))
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        // Good fix
        val goodFix = createPoint(12.002, 77.500, accuracyMeters = 10f, timestamp = 2000L)
        val prog1 = engine.onLocationUpdate(goodFix)
        assertEquals(NavigationStatus.ON_ROUTE, prog1.status)

        // Fix with poor accuracy (35m > 25m)
        val noisyFix = createPoint(12.008, 77.505, accuracyMeters = 35f, timestamp = 3000L)
        val pausedProg = engine.onLocationUpdate(noisyFix)
        assertEquals(NavigationStatus.PAUSED_NO_GPS, pausedProg.status)
        // Previous progress preserved
        assertEquals(prog1.distanceAlongRouteMeters, pausedProg.distanceAlongRouteMeters)

        // Signal lost event
        val lostProg = engine.onGpsLost(4000L)
        assertEquals(NavigationStatus.PAUSED_NO_GPS, lostProg.status)
    }

    @Test
    fun degenerateSegmentWithDuplicateCoordinatesDoesNotCrash() {
        val route = createRoute(listOf(
            createPoint(12.000, 77.500),
            createPoint(12.000, 77.500), // duplicate!
            createPoint(12.010, 77.500)
        ))
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        val fix = createPoint(12.005, 77.500, timestamp = 3000L)
        val progress = engine.onLocationUpdate(fix)

        assertEquals(NavigationStatus.ON_ROUTE, progress.status)
        assertFalse(progress.crossTrackDistanceMeters.isNaN())
        assertFalse(progress.distanceAlongRouteMeters.isNaN())
    }

    @Test
    fun instructionIndexAdvancesAlongRoute() {
        val instructions = listOf(
            RouteInstruction(
                maneuverType = ManeuverType.DEPART,
                streetName = "Main St",
                pointIndex = 0,
                instructionText = "Head north on Main St"
            ),
            RouteInstruction(
                maneuverType = ManeuverType.TURN_RIGHT,
                streetName = "Cross Ave",
                pointIndex = 1,
                instructionText = "Turn right onto Cross Ave"
            ),
            RouteInstruction(
                maneuverType = ManeuverType.ARRIVE,
                streetName = "Destination",
                pointIndex = 2,
                instructionText = "You have arrived"
            )
        )
        val route = createRoute(
            geometry = listOf(
                createPoint(12.000, 77.500),
                createPoint(12.005, 77.500),
                createPoint(12.005, 77.510)
            ),
            instructions = instructions
        )
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        // Near segment 0
        val p1 = createPoint(12.001, 77.500)
        val prog1 = engine.onLocationUpdate(p1)
        assertEquals(0, prog1.currentInstructionIndex)

        // Near segment 1
        val p2 = createPoint(12.005, 77.505)
        val prog2 = engine.onLocationUpdate(p2)
        assertEquals(1, prog2.currentInstructionIndex)
    }

    @Test
    fun resetClearsStateCompletely() {
        val route = createRoute(listOf(
            createPoint(12.000, 77.500),
            createPoint(12.010, 77.500)
        ))
        val engine = RouteProgressEngine()
        engine.setRoute(route)

        engine.onLocationUpdate(createPoint(12.005, 77.500))
        assertNotNull(engine.lastProgress)
        assertNotNull(engine.currentRoute)

        engine.reset()
        assertEquals(null, engine.lastProgress)
        assertEquals(null, engine.currentRoute)
    }
}
