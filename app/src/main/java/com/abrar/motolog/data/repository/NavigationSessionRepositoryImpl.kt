package com.abrar.motolog.data.repository

import com.abrar.motolog.data.local.dao.NavigationSessionDao
import com.abrar.motolog.data.local.dao.SavedDestinationDao
import com.abrar.motolog.data.local.entity.NavigationSessionEntity
import com.abrar.motolog.data.local.entity.SavedDestinationEntity
import com.abrar.motolog.domain.repository.NavigationSessionRepository
import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.DestinationSourceType
import com.abrar.motolog.shared.domain.model.NavigationSession
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationSessionRepositoryImpl @Inject constructor(
    private val navigationSessionDao: NavigationSessionDao,
    private val savedDestinationDao: SavedDestinationDao
) : NavigationSessionRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun saveActiveSession(session: NavigationSession, route: PlannedRoute) {
        val routeJson = try {
            json.encodeToString(route)
        } catch (_: Exception) {
            null
        }

        val progressJson = session.lastKnownProgress?.let {
            try {
                json.encodeToString(it)
            } catch (_: Exception) {
                null
            }
        }

        val entity = NavigationSessionEntity(
            sessionId = session.sessionId,
            plannedRouteId = session.plannedRouteId,
            rideId = session.rideId,
            status = session.status.name,
            startedAtTimestamp = session.startedAtTimestamp,
            endedAtTimestamp = session.endedAtTimestamp,
            lastKnownProgressJson = progressJson,
            routeSnapshotJson = routeJson,
            rerouteCount = session.rerouteCount
        )

        navigationSessionDao.upsertSession(entity)
    }

    override suspend fun checkpointProgress(sessionId: String, progress: RouteProgress) {
        val progressJson = try {
            json.encodeToString(progress)
        } catch (_: Exception) {
            return
        }
        navigationSessionDao.updateProgress(
            sessionId = sessionId,
            progressJson = progressJson,
            status = progress.status.name
        )
    }

    override suspend fun getActiveSession(): Pair<NavigationSession, PlannedRoute?>? {
        val entity = navigationSessionDao.getActiveSession() ?: return null
        return entityToSessionWithRoute(entity)
    }

    override fun observeActiveSession(): Flow<NavigationSession?> {
        return navigationSessionDao.observeActiveSession().map { entity ->
            entity?.let { entityToSession(it) }
        }
    }

    override suspend fun finishSession(sessionId: String, finalStatus: NavigationStatus) {
        navigationSessionDao.endSession(
            sessionId = sessionId,
            status = finalStatus.name,
            endedAt = System.currentTimeMillis()
        )
    }

    override suspend fun discardSession(sessionId: String) {
        navigationSessionDao.deleteSession(sessionId)
    }

    override suspend fun incrementRerouteCount(sessionId: String) {
        val entity = navigationSessionDao.getSession(sessionId) ?: return
        val updated = entity.copy(rerouteCount = entity.rerouteCount + 1)
        navigationSessionDao.upsertSession(updated)
    }

    // ============================================================
    // Saved Destinations
    // ============================================================

    override fun getSavedDestinations(): Flow<List<Destination>> {
        return savedDestinationDao.getAllDestinations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteDestinations(): Flow<List<Destination>> {
        return savedDestinationDao.getFavoriteDestinations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveDestination(destination: Destination, isFavorite: Boolean) {
        savedDestinationDao.insertDestination(
            SavedDestinationEntity(
                id = destination.id.ifEmpty { java.util.UUID.randomUUID().toString() },
                latitude = destination.latitude,
                longitude = destination.longitude,
                displayName = destination.displayName,
                providerPlaceId = destination.providerPlaceId,
                sourceType = destination.sourceType.name,
                createdAtTimestamp = if (destination.createdAtTimestamp > 0) destination.createdAtTimestamp else System.currentTimeMillis(),
                isFavorite = isFavorite
            )
        )
    }

    override suspend fun toggleFavoriteDestination(id: String, isFavorite: Boolean) {
        savedDestinationDao.setFavorite(id, isFavorite)
    }

    override suspend fun deleteSavedDestination(id: String) {
        savedDestinationDao.deleteDestinationById(id)
    }

    private fun entityToSession(entity: NavigationSessionEntity): NavigationSession {
        val progress = entity.lastKnownProgressJson?.let {
            try {
                json.decodeFromString<RouteProgress>(it)
            } catch (_: Exception) {
                null
            }
        }

        val status = try {
            NavigationStatus.valueOf(entity.status)
        } catch (_: Exception) {
            NavigationStatus.ON_ROUTE
        }

        return NavigationSession(
            sessionId = entity.sessionId,
            plannedRouteId = entity.plannedRouteId,
            rideId = entity.rideId,
            status = status,
            startedAtTimestamp = entity.startedAtTimestamp,
            endedAtTimestamp = entity.endedAtTimestamp,
            lastKnownProgress = progress,
            rerouteCount = entity.rerouteCount
        )
    }

    private fun entityToSessionWithRoute(entity: NavigationSessionEntity): Pair<NavigationSession, PlannedRoute?> {
        val session = entityToSession(entity)
        val route = entity.routeSnapshotJson?.let {
            try {
                json.decodeFromString<PlannedRoute>(it)
            } catch (_: Exception) {
                null
            }
        }
        return Pair(session, route)
    }

    private fun SavedDestinationEntity.toDomain(): Destination {
        val src = try {
            DestinationSourceType.valueOf(sourceType)
        } catch (_: Exception) {
            DestinationSourceType.SEARCH
        }
        return Destination(
            id = id,
            latitude = latitude,
            longitude = longitude,
            displayName = displayName,
            providerPlaceId = providerPlaceId,
            sourceType = src,
            createdAtTimestamp = createdAtTimestamp
        )
    }
}
