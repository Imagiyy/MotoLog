package com.abrar.motolog.shared.domain.repository

import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteRequest

/**
 * Domain interface for route planning adapters (Cloud or self-hosted keyless).
 */
interface RoutingProvider {
    /**
     * Calculate route alternatives between origin and destination based on request parameters.
     */
    suspend fun calculateRoute(request: RouteRequest): Result<List<PlannedRoute>>
}
