package com.abrar.motolog.fake

import com.abrar.motolog.domain.repository.NavigationSessionRepository
import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.NavigationSession
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeNavigationSessionRepository : NavigationSessionRepository {

    var activeSession: NavigationSession? = null
    var activeRoute: PlannedRoute? = null
    private val activeSessionFlow = MutableStateFlow<NavigationSession?>(null)

    private val savedDestinations = MutableStateFlow<List<Destination>>(emptyList())
    private val favoriteDestinations = MutableStateFlow<List<Destination>>(emptyList())

    override suspend fun saveActiveSession(session: NavigationSession, route: PlannedRoute) {
        activeSession = session
        activeRoute = route
        activeSessionFlow.value = session
    }

    override suspend fun checkpointProgress(sessionId: String, progress: RouteProgress) {
        activeSession = activeSession?.copy(lastKnownProgress = progress)
        activeSessionFlow.value = activeSession
    }

    override suspend fun getActiveSession(): Pair<NavigationSession, PlannedRoute?>? {
        val s = activeSession ?: return null
        return s to activeRoute
    }

    override fun observeActiveSession(): Flow<NavigationSession?> {
        return activeSessionFlow.asStateFlow()
    }

    override suspend fun finishSession(sessionId: String, finalStatus: NavigationStatus) {
        activeSession = activeSession?.copy(status = finalStatus)
        activeSessionFlow.value = activeSession
    }

    override suspend fun discardSession(sessionId: String) {
        activeSession = null
        activeRoute = null
        activeSessionFlow.value = null
    }

    override suspend fun incrementRerouteCount(sessionId: String) {
        activeSession = activeSession?.copy(rerouteCount = (activeSession?.rerouteCount ?: 0) + 1)
        activeSessionFlow.value = activeSession
    }

    override fun getSavedDestinations(): Flow<List<Destination>> = savedDestinations.asStateFlow()

    override fun getFavoriteDestinations(): Flow<List<Destination>> = favoriteDestinations.asStateFlow()

    override suspend fun saveDestination(destination: Destination, isFavorite: Boolean) {
        val current = savedDestinations.value.toMutableList()
        current.removeAll { it.id == destination.id }
        current.add(0, destination)
        savedDestinations.value = current

        if (isFavorite) {
            val favs = favoriteDestinations.value.toMutableList()
            favs.removeAll { it.id == destination.id }
            favs.add(0, destination)
            favoriteDestinations.value = favs
        }
    }

    override suspend fun toggleFavoriteDestination(id: String, isFavorite: Boolean) {
        if (!isFavorite) {
            val favs = favoriteDestinations.value.filter { it.id != id }
            favoriteDestinations.value = favs
        }
    }

    override suspend fun deleteSavedDestination(id: String) {
        savedDestinations.value = savedDestinations.value.filter { it.id != id }
        favoriteDestinations.value = favoriteDestinations.value.filter { it.id != id }
    }
}
