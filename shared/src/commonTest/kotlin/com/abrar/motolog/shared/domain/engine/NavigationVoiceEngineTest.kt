package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.ManeuverType
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.RouteInstruction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigationVoiceEngineTest {

    private lateinit var engine: NavigationVoiceEngine

    @BeforeTest
    fun setUp() {
        engine = NavigationVoiceEngine()
    }

    @Test
    fun formatsMetricPhrasingCorrectly() {
        val instruction = RouteInstruction(
            maneuverType = ManeuverType.TURN_RIGHT,
            streetName = "100 Feet Road",
            instructionText = "Turn right onto 100 Feet Road"
        )

        // Far tier (500m)
        val farText = engine.formatManeuverAnnouncement(
            instruction = instruction,
            tier = AnnouncementTier.FAR,
            distanceMeters = 500.0,
            isMetric = true
        )
        assertEquals("In 500 meters, turn right onto 100 Feet Road.", farText)

        // Near tier (150m)
        val nearText = engine.formatManeuverAnnouncement(
            instruction = instruction,
            tier = AnnouncementTier.NEAR,
            distanceMeters = 150.0,
            isMetric = true
        )
        assertEquals("In 150 meters, turn right onto 100 Feet Road.", nearText)

        // Imminent tier
        val imminentText = engine.formatManeuverAnnouncement(
            instruction = instruction,
            tier = AnnouncementTier.IMMINENT,
            distanceMeters = 25.0,
            isMetric = true
        )
        assertEquals("Turn right onto 100 Feet Road.", imminentText)
    }

    @Test
    fun formatsImperialPhrasingCorrectly() {
        val instruction = RouteInstruction(
            maneuverType = ManeuverType.TURN_LEFT,
            streetName = "Mission Blvd",
            instructionText = "Turn left onto Mission Blvd"
        )

        // Far tier in imperial (~500m = ~1640ft -> 1600 feet)
        val farText = engine.formatManeuverAnnouncement(
            instruction = instruction,
            tier = AnnouncementTier.FAR,
            distanceMeters = 500.0,
            isMetric = false
        )
        assertEquals("In 1600 feet, turn left onto Mission Blvd.", farText)

        // 1 mile
        val mileText = engine.formatManeuverAnnouncement(
            instruction = instruction,
            tier = AnnouncementTier.FAR,
            distanceMeters = 1609.34,
            isMetric = false
        )
        assertEquals("In 1 mile, turn left onto Mission Blvd.", mileText)
    }

    @Test
    fun roundaboutsWithCustomTextPreserveManeuverDetails() {
        val instruction = RouteInstruction(
            maneuverType = ManeuverType.ROUNDABOUT,
            streetName = "Koramangala Ring Rd",
            instructionText = "At the roundabout, take the 2nd exit"
        )

        val imminentText = engine.formatManeuverAnnouncement(
            instruction = instruction,
            tier = AnnouncementTier.IMMINENT,
            distanceMeters = 30.0,
            isMetric = true
        )
        assertEquals("At the roundabout, take the 2nd exit.", imminentText)
    }

    @Test
    fun tieredAnnouncementsTriggerInOrderWithoutDuplicates() {
        val instruction = RouteInstruction(
            maneuverType = ManeuverType.TURN_LEFT,
            streetName = "Outer Ring Rd",
            instructionText = "Turn left onto Outer Ring Rd"
        )

        // 1. Far distance: 480m -> triggers FAR
        val a1 = engine.evaluateAnnouncement(
            instruction = instruction,
            instructionIndex = 1,
            distanceToManeuverMeters = 480.0,
            currentSpeedKmh = 40.0,
            isMetric = true,
            status = NavigationStatus.ON_ROUTE,
            nowMs = 1000L
        )
        assertNotNull(a1)
        assertEquals(AnnouncementTier.FAR, a1.tier)
        assertEquals("In 500 meters, turn left onto Outer Ring Rd.", a1.text)
        assertFalse(a1.isUrgent)

        // 2. Immediate next update at 470m (same tier) -> suppressed!
        val a2 = engine.evaluateAnnouncement(
            instruction = instruction,
            instructionIndex = 1,
            distanceToManeuverMeters = 470.0,
            currentSpeedKmh = 40.0,
            isMetric = true,
            status = NavigationStatus.ON_ROUTE,
            nowMs = 2000L
        )
        assertNull(a2)

        // 3. Near distance: 140m at t=6000L (> 4s cadence) -> triggers NEAR
        val a3 = engine.evaluateAnnouncement(
            instruction = instruction,
            instructionIndex = 1,
            distanceToManeuverMeters = 140.0,
            currentSpeedKmh = 40.0,
            isMetric = true,
            status = NavigationStatus.ON_ROUTE,
            nowMs = 6000L
        )
        assertNotNull(a3)
        assertEquals(AnnouncementTier.NEAR, a3.tier)
        assertEquals("In 150 meters, turn left onto Outer Ring Rd.", a3.text)

        // 4. Imminent distance: 25m at t=8000L -> triggers IMMINENT and is urgent
        val a4 = engine.evaluateAnnouncement(
            instruction = instruction,
            instructionIndex = 1,
            distanceToManeuverMeters = 25.0,
            currentSpeedKmh = 40.0,
            isMetric = true,
            status = NavigationStatus.ON_ROUTE,
            nowMs = 8000L
        )
        assertNotNull(a4)
        assertEquals(AnnouncementTier.IMMINENT, a4.tier)
        assertEquals("Turn left onto Outer Ring Rd.", a4.text)
        assertTrue(a4.isUrgent)
    }

    @Test
    fun maneuverTransitionResetsTiersForNextStep() {
        val step1 = RouteInstruction(
            maneuverType = ManeuverType.TURN_LEFT,
            streetName = "First St"
        )
        val step2 = RouteInstruction(
            maneuverType = ManeuverType.TURN_RIGHT,
            streetName = "Second Ave"
        )

        // Exhaust all tiers on step 1
        engine.evaluateAnnouncement(step1, 0, 450.0, 30.0, true, NavigationStatus.ON_ROUTE, 1000L)
        engine.evaluateAnnouncement(step1, 0, 140.0, 30.0, true, NavigationStatus.ON_ROUTE, 6000L)
        engine.evaluateAnnouncement(step1, 0, 20.0, 30.0, true, NavigationStatus.ON_ROUTE, 8000L)

        // Advance to step 2 (index 1) at far distance
        val nextAnnouncement = engine.evaluateAnnouncement(
            instruction = step2,
            instructionIndex = 1,
            distanceToManeuverMeters = 400.0,
            currentSpeedKmh = 30.0,
            isMetric = true,
            status = NavigationStatus.ON_ROUTE,
            nowMs = 15000L
        )

        assertNotNull(nextAnnouncement)
        assertEquals(AnnouncementTier.FAR, nextAnnouncement.tier)
        assertEquals("In 400 meters, turn right onto Second Ave.", nextAnnouncement.text)
    }

    @Test
    fun arrivalAndOffRouteStatusChangesAnnounceImmediately() {
        // Arrival
        val arrived = engine.evaluateAnnouncement(
            instruction = null,
            instructionIndex = null,
            distanceToManeuverMeters = 5.0,
            currentSpeedKmh = 0.0,
            isMetric = true,
            status = NavigationStatus.ARRIVED,
            nowMs = 1000L
        )
        assertNotNull(arrived)
        assertEquals("You have arrived at your destination.", arrived.text)
        assertTrue(arrived.isUrgent)

        // Consecutive ARRIVED status does not repeat
        val arrivedAgain = engine.evaluateAnnouncement(
            instruction = null,
            instructionIndex = null,
            distanceToManeuverMeters = 5.0,
            currentSpeedKmh = 0.0,
            isMetric = true,
            status = NavigationStatus.ARRIVED,
            nowMs = 2000L
        )
        assertNull(arrivedAgain)

        // Off-route reroute needed
        val rerouteNeeded = engine.evaluateAnnouncement(
            instruction = null,
            instructionIndex = null,
            distanceToManeuverMeters = 100.0,
            currentSpeedKmh = 30.0,
            isMetric = true,
            status = NavigationStatus.OFF_ROUTE_REROUTE_NEEDED,
            nowMs = 3000L
        )
        assertNotNull(rerouteNeeded)
        assertEquals("Off route. Recalculating...", rerouteNeeded.text)
        assertTrue(rerouteNeeded.isUrgent)

        // On reroute completed
        val newRoute = engine.onRerouteCompleted("Airport Road")
        assertEquals("New route found. Continue on Airport Road.", newRoute.text)
        assertTrue(newRoute.isUrgent)
    }
}
