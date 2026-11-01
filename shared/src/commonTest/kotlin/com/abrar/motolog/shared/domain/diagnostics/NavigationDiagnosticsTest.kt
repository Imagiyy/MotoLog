package com.abrar.motolog.shared.domain.diagnostics

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationDiagnosticsTest {

    private lateinit var diagnostics: NavigationDiagnostics

    @BeforeTest
    fun setUp() {
        diagnostics = NavigationDiagnostics()
    }

    @Test
    fun recordsRouteMetricsAccurately() {
        diagnostics.recordRouteRequest()
        diagnostics.recordRouteRequest()
        diagnostics.recordRouteSuccess()
        diagnostics.recordRouteFailure(NavigationDiagnostics.FailureCategory.RATE_LIMIT)

        val snapshot = diagnostics.getSnapshot(1000L)
        assertEquals(2L, snapshot.totalRouteRequests)
        assertEquals(1L, snapshot.successfulRouteRequests)
        assertEquals(1L, snapshot.failedRouteRequests)
        assertEquals(1L, snapshot.rateLimitErrors)
        assertEquals(0L, snapshot.networkErrors)
    }

    @Test
    fun recordsReroutesAndLifecycleEvents() {
        diagnostics.recordRerouteTriggered()
        diagnostics.recordRerouteSuccess()
        diagnostics.recordArrival()
        diagnostics.recordVoiceAnnouncement()
        diagnostics.recordGpsSignalLoss()
        diagnostics.recordOffRouteWarning()

        val snapshot = diagnostics.getSnapshot()
        assertEquals(1L, snapshot.totalReroutesTriggered)
        assertEquals(1L, snapshot.successfulReroutes)
        assertEquals(1L, snapshot.arrivalsConfirmed)
        assertEquals(1L, snapshot.voiceAnnouncementsSpoken)
        assertEquals(1L, snapshot.gpsSignalLossEvents)
        assertEquals(1L, snapshot.offRouteWarningCount)
    }

    @Test
    fun exportedJsonContainsNoCoordinatesOrSecrets() {
        diagnostics.recordRouteRequest()
        diagnostics.recordRouteSuccess()

        val json = diagnostics.exportDiagnosticsJson(5000L)
        assertTrue(json.contains("\"successfulRouteRequests\": 1"))
        assertFalse(json.contains("latitude"))
        assertFalse(json.contains("longitude"))
        assertFalse(json.contains("apiKey"))
    }

    @Test
    fun resetClearsAllCounters() {
        diagnostics.recordRouteRequest()
        diagnostics.recordArrival()
        diagnostics.reset()

        val snapshot = diagnostics.getSnapshot()
        assertEquals(0L, snapshot.totalRouteRequests)
        assertEquals(0L, snapshot.arrivalsConfirmed)
    }
}
