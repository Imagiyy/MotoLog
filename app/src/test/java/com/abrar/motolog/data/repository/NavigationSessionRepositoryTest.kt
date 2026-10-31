package com.abrar.motolog.data.repository

import com.abrar.motolog.data.local.dao.NavigationSessionDao
import com.abrar.motolog.data.local.dao.SavedDestinationDao
import com.abrar.motolog.data.local.entity.NavigationSessionEntity
import com.abrar.motolog.data.local.entity.SavedDestinationEntity
import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.DestinationSourceType
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.NavigationSession
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NavigationSessionRepositoryTest {

    private class FakeNavigationSessionDao : NavigationSessionDao {
        val sessions = mutableMapOf<String, NavigationSessionEntity>()
        val activeFlow = MutableStateFlow<NavigationSessionEntity?>(null)

        override suspend fun upsertSession(session: NavigationSessionEntity) {
            sessions[session.sessionId] = session
            if (session.endedAtTimestamp == null) {
                activeFlow.value = session
            } else if (activeFlow.value?.sessionId == session.sessionId) {
                activeFlow.value = null
            }
        }

        override suspend fun getSession(sessionId: String): NavigationSessionEntity? =
            sessions[sessionId]

        override suspend fun getActiveSession(): NavigationSessionEntity? =
            sessions.values.firstOrNull { it.endedAtTimestamp == null }

        override fun observeActiveSession(): Flow<NavigationSessionEntity?> =
            activeFlow

        override suspend fun updateProgress(sessionId: String, progressJson: String, status: String) {
            val existing = sessions[sessionId] ?: return
            val updated = existing.copy(lastKnownProgressJson = progressJson, status = status)
            sessions[sessionId] = updated
            if (updated.endedAtTimestamp == null) {
                activeFlow.value = updated
            }
        }

        override suspend fun endSession(sessionId: String, status: String, endedAt: Long) {
            val existing = sessions[sessionId] ?: return
            val updated = existing.copy(status = status, endedAtTimestamp = endedAt)
            sessions[sessionId] = updated
            if (activeFlow.value?.sessionId == sessionId) {
                activeFlow.value = null
            }
        }

        override suspend fun deleteSession(sessionId: String) {
            sessions.remove(sessionId)
            if (activeFlow.value?.sessionId == sessionId) {
                activeFlow.value = null
            }
        }

        override suspend fun pruneOldSessions(cutoffTimestamp: Long) {
            sessions.entries.removeIf { (_, entity) ->
                entity.endedAtTimestamp != null && entity.endedAtTimestamp < cutoffTimestamp
            }
        }
    }

    private class FakeSavedDestinationDao : SavedDestinationDao {
        val destinations = mutableMapOf<String, SavedDestinationEntity>()
        val flow = MutableStateFlow<List<SavedDestinationEntity>>(emptyList())

        override suspend fun insertDestination(destination: SavedDestinationEntity) {
            destinations[destination.id] = destination
            emit()
        }

        override fun getAllDestinations(): Flow<List<SavedDestinationEntity>> = flow

        override fun getFavoriteDestinations(): Flow<List<SavedDestinationEntity>> =
            flow.map { list -> list.filter { it.isFavorite } }

        override suspend fun getDestinationById(id: String): SavedDestinationEntity? =
            destinations[id]

        override suspend fun deleteDestinationById(id: String) {
            destinations.remove(id)
            emit()
        }

        override suspend fun setFavorite(id: String, isFavorite: Boolean) {
            destinations[id]?.let {
                destinations[id] = it.copy(isFavorite = isFavorite)
                emit()
            }
        }

        override suspend fun clearAll() {
            destinations.clear()
            emit()
        }

        private fun emit() {
            flow.value = destinations.values.toList()
        }
    }

    private lateinit var sessionDao: FakeNavigationSessionDao
    private lateinit var destinationDao: FakeSavedDestinationDao
    private lateinit var repository: NavigationSessionRepositoryImpl

    private fun createSampleRoute(): PlannedRoute = PlannedRoute(
        id = "route_123",
        geometry = listOf(
            LocationPoint(12.0, 77.0, null, 5f, 1000L),
            LocationPoint(12.1, 77.1, null, 5f, 2000L)
        ),
        totalDistanceMeters = 15000.0,
        expectedDurationSeconds = 1200L,
        calculatedAtTimestamp = 1000L
    )

    @Before
    fun setUp() {
        sessionDao = FakeNavigationSessionDao()
        destinationDao = FakeSavedDestinationDao()
        repository = NavigationSessionRepositoryImpl(sessionDao, destinationDao)
    }

    @Test
    fun saveActiveSessionAndRestoreOnProcessRecovery() = runTest {
        val route = createSampleRoute()
        val session = NavigationSession(
            sessionId = "session_abc",
            plannedRouteId = route.id,
            rideId = 42L,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = 1000L
        )

        repository.saveActiveSession(session, route)

        val active = repository.getActiveSession()
        assertNotNull(active)
        val (restoredSession, restoredRoute) = active!!

        assertEquals("session_abc", restoredSession.sessionId)
        assertEquals(42L, restoredSession.rideId)
        assertEquals(NavigationStatus.ON_ROUTE, restoredSession.status)

        assertNotNull(restoredRoute)
        assertEquals(route.id, restoredRoute!!.id)
        assertEquals(2, restoredRoute.geometry.size)
        assertEquals(15000.0, restoredRoute.totalDistanceMeters, 0.01)
    }

    @Test
    fun checkpointProgressUpdatesActiveSession() = runTest {
        val route = createSampleRoute()
        val session = NavigationSession(
            sessionId = "session_abc",
            plannedRouteId = route.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = 1000L
        )
        repository.saveActiveSession(session, route)

        val progress = RouteProgress(
            nearestPointIndex = 1,
            distanceAlongRouteMeters = 5000.0,
            remainingDistanceMeters = 10000.0,
            progressFraction = 0.33f,
            crossTrackDistanceMeters = 3.5,
            status = NavigationStatus.ON_ROUTE,
            timestamp = 2000L
        )

        repository.checkpointProgress("session_abc", progress)

        val active = repository.getActiveSession()
        assertNotNull(active)
        val restoredProgress = active!!.first.lastKnownProgress
        assertNotNull(restoredProgress)
        assertEquals(1, restoredProgress!!.nearestPointIndex)
        assertEquals(5000.0, restoredProgress.distanceAlongRouteMeters, 0.01)
    }

    @Test
    fun finishSessionMarksSessionEndedAndClearsActive() = runTest {
        val route = createSampleRoute()
        val session = NavigationSession(
            sessionId = "session_abc",
            plannedRouteId = route.id,
            status = NavigationStatus.ON_ROUTE,
            startedAtTimestamp = 1000L
        )
        repository.saveActiveSession(session, route)

        repository.finishSession("session_abc", NavigationStatus.ARRIVED)

        val active = repository.getActiveSession()
        assertNull(active)
    }

    @Test
    fun destinationSaveFavoriteAndDelete() = runTest {
        val dest = Destination(
            id = "dest_1",
            latitude = 12.9716,
            longitude = 77.5946,
            displayName = "Cubbon Park",
            sourceType = DestinationSourceType.SEARCH,
            createdAtTimestamp = 1000L
        )

        repository.saveDestination(dest, isFavorite = false)
        val saved = repository.getSavedDestinations().first()
        assertEquals(1, saved.size)
        assertEquals("Cubbon Park", saved[0].displayName)

        repository.toggleFavoriteDestination("dest_1", isFavorite = true)
        val favorites = repository.getFavoriteDestinations().first()
        assertEquals(1, favorites.size)

        repository.deleteSavedDestination("dest_1")
        val empty = repository.getSavedDestinations().first()
        assertTrue(empty.isEmpty())
    }
}
