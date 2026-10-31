package com.abrar.motolog.shared.domain.repository

import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteRequest

/**
 * Provider-agnostic interface for motorcycle route calculation and rerouting.
 */
interface RoutingRepository {

    /**
     * Calculate route alternatives between origin and destination based on request parameters.
     *
     * Returns a list of candidate routes with the primary route as the first element.
     */
    suspend fun calculateRoute(request: RouteRequest): Result<List<PlannedRoute>>

    /**
     * Request a fast recalculation / reroute from the current position towards the original destination.
     */
    suspend fun refreshRoute(routeId: String, currentOrigin: LocationPoint): Result<PlannedRoute>

    /**
     * Cancel any currently active or pending routing network requests.
     */
    fun cancelActiveRequests()
}
