package com.abrar.motolog.fake

import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteRequest
import com.abrar.motolog.shared.domain.repository.RoutingRepository

class FakeRoutingRepository : RoutingRepository {

    var routeToReturn: Result<List<PlannedRoute>> = Result.success(
        listOf(
            PlannedRoute(
                id = "route_1",
                geometry = listOf(
                    LocationPoint(latitude = 12.9716, longitude = 77.5946),
                    LocationPoint(latitude = 12.9750, longitude = 77.6000),
                    LocationPoint(latitude = 13.0000, longitude = 77.6500)
                ),
                totalDistanceMeters = 8500.0,
                expectedDurationSeconds = 900L
            )
        )
    )

    var lastCalculatedRequest: RouteRequest? = null
    var refreshRouteToReturn: Result<PlannedRoute>? = null
    var cancelCalled: Boolean = false

    override suspend fun calculateRoute(request: RouteRequest): Result<List<PlannedRoute>> {
        lastCalculatedRequest = request
        return routeToReturn
    }

    override suspend fun refreshRoute(routeId: String, currentOrigin: LocationPoint): Result<PlannedRoute> {
        return refreshRouteToReturn ?: routeToReturn.map { it.first() }
    }

    override fun cancelActiveRequests() {
        cancelCalled = true
    }
}
