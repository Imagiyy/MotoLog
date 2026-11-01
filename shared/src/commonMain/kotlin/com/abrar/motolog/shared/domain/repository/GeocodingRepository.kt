package com.abrar.motolog.shared.domain.repository

import com.abrar.motolog.shared.domain.model.Coordinates
import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlaceResult
import com.abrar.motolog.shared.domain.model.toLocationPoint

/**
 * Provider-agnostic interface for destination search and reverse geocoding.
 */
interface GeocodingRepository : GeocodingProvider {

    /**
     * Search for candidate places matching a free-text search query with nearby-first ranking.
     *
     * @param query Search text entered by the rider (e.g. place, business, address).
     * @param nearLocation Optional coordinate to bias search results toward rider's vicinity.
     * @param radiusMeters Optional search radius in meters.
     * @return List of matching [PlaceResult] candidates.
     */
    suspend fun searchPlaces(
        query: String,
        nearLocation: LocationPoint? = null,
        radiusMeters: Int? = null
    ): Result<List<PlaceResult>>

    override suspend fun search(
        query: String,
        near: Coordinates?,
        radiusMeters: Int?
    ): Result<List<PlaceResult>> =
        searchPlaces(query, near?.toLocationPoint(), radiusMeters)

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
