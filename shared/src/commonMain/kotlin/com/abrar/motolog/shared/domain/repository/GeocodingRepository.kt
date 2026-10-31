package com.abrar.motolog.shared.domain.repository

import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.LocationPoint

/**
 * Provider-agnostic interface for destination search and reverse geocoding.
 */
interface GeocodingRepository {

    /**
     * Search for candidate destinations matching a free-text search query.
     *
     * @param query Search text entered by the rider (e.g. place, business, address).
     * @param nearLocation Optional coordinate to bias search results toward rider's vicinity.
     * @return List of matching [Destination] candidates.
     */
    suspend fun searchDestinations(
        query: String,
        nearLocation: LocationPoint? = null
    ): Result<List<Destination>>

    /**
     * Look up place name and locality for a geographical coordinate (e.g. user dropped a map pin).
     */
    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<Destination>
}
