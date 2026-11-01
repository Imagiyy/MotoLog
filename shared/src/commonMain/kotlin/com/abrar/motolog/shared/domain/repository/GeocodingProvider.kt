package com.abrar.motolog.shared.domain.repository

import com.abrar.motolog.shared.domain.model.Coordinates
import com.abrar.motolog.shared.domain.model.PlaceResult

/**
 * Domain interface for geocoding search adapters (Cloud or self-hosted keyless).
 */
interface GeocodingProvider {
    /**
     * Search for candidate places matching a query string, biased around a coordinate.
     *
     * @param query Search query text.
     * @param near Optional coordinate bias.
     * @param radiusMeters Optional search radius in meters.
     * @return List of normalized [PlaceResult]s.
     */
    suspend fun search(
        query: String,
        near: Coordinates? = null,
        radiusMeters: Int? = null
    ): Result<List<PlaceResult>>
}
