package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlaceResult
import kotlin.math.max

/**
 * Pure Kotlin search ranking engine for destination geocoding results.
 *
 * Implements deterministic multi-factor weighted scoring:
 *   rankingScore = (relevanceScore * 0.65) + (localityMatchScore * 0.15) + (distanceScore * 0.20)
 *
 * Ensures that distance improves ordering among relevant results without allowing an
 * unrelated nearby result to outrank an exact name match.
 */
object PlaceRankingEngine {

    const val DEFAULT_SEARCH_RADIUS_METERS = 50_000.0 // 50 km
    const val RELEVANCE_WEIGHT = 0.65
    const val LOCALITY_WEIGHT = 0.15
    const val DISTANCE_WEIGHT = 0.20

    /**
     * Score, enrich with straight-line distance, and rank candidate places relative to the rider's query and position.
     *
     * @param places Raw candidate places returned by geocoding providers.
     * @param query The text query entered by the rider.
     * @param userLocation The latest known valid fix of the rider, or null if GPS fix is unavailable.
     * @param searchRadiusMeters Initial local search radius threshold (e.g. 50 km).
     * @param allowGlobalFallback When true, results beyond the search radius are retained and flagged as farther matches.
     * @return Sorted, validated [PlaceResult] items ordered by descending ranking score.
     */
    fun rankPlaces(
        places: List<PlaceResult>,
        query: String,
        userLocation: LocationPoint?,
        searchRadiusMeters: Double = DEFAULT_SEARCH_RADIUS_METERS,
        allowGlobalFallback: Boolean = true
    ): List<PlaceResult> {
        val trimmedQuery = query.trim().lowercase()
        if (trimmedQuery.isEmpty()) {
            return emptyList()
        }

        // 1. Filter out invalid/corrupted coordinates
        val validPlaces = places.filter { it.hasValidCoordinates() }
        if (validPlaces.isEmpty()) {
            return emptyList()
        }

        val hasValidUserLocation = userLocation != null &&
                !userLocation.latitude.isNaN() &&
                !userLocation.longitude.isNaN() &&
                userLocation.latitude in -90.0..90.0 &&
                userLocation.longitude in -180.0..180.0

        // 2. Compute distances and individual scores
        val scoredPlaces = validPlaces.map { place ->
            val distanceMeters = if (hasValidUserLocation) {
                RideCalculator.haversineDistanceMeters(
                    lat1 = userLocation.latitude,
                    lon1 = userLocation.longitude,
                    lat2 = place.latitude,
                    lon2 = place.longitude
                )
            } else {
                null
            }

            val relevanceScore = computeRelevanceScore(trimmedQuery, place.name.lowercase())
            val localityScore = computeLocalityScore(trimmedQuery, place.name.lowercase(), place.formattedAddress.lowercase())

            val distanceScore = if (distanceMeters != null) {
                computeDistanceScore(distanceMeters, searchRadiusMeters)
            } else {
                0.0
            }

            val totalScore = if (hasValidUserLocation) {
                (relevanceScore * RELEVANCE_WEIGHT) +
                        (localityScore * LOCALITY_WEIGHT) +
                        (distanceScore * DISTANCE_WEIGHT)
            } else {
                // If user location is unknown, omit distance component and re-weight text relevance
                (relevanceScore * 0.80) + (localityScore * 0.20)
            }

            val isFarther = if (distanceMeters != null) {
                distanceMeters > searchRadiusMeters
            } else {
                false
            }

            ScoredPlace(
                place = place.copy(
                    distanceFromUserMeters = distanceMeters,
                    isFartherMatch = isFarther
                ),
                score = totalScore,
                distanceMeters = distanceMeters ?: Double.MAX_VALUE,
                isFarther = isFarther
            )
        }

        // 3. Apply global fallback policy
        val hasNearbyMatches = scoredPlaces.any { !it.isFarther }
        val candidatePlaces = if (!allowGlobalFallback && hasValidUserLocation) {
            scoredPlaces.filter { !it.isFarther }
        } else {
            scoredPlaces
        }

        // 4. Sort: when nearby matches exist, nearby places come first sorted by score,
        // then farther fallback places sorted by score.
        return candidatePlaces
            .sortedWith(
                compareBy<ScoredPlace> { scored ->
                    // 0 for nearby, 1 for farther fallback
                    if (hasNearbyMatches && scored.isFarther) 1 else 0
                }.thenByDescending { it.score }
                    .thenBy { it.distanceMeters }
                    .thenBy { it.place.name }
            )
            .map { it.place }
    }

    /**
     * Compute textual relevance score in range [0.0, 1.0].
     * Strongly favors exact matches and prefix matches.
     */
    fun computeRelevanceScore(query: String, name: String): Double {
        if (query == name) {
            return 1.0 // Exact match
        }
        if (name.startsWith(query)) {
            return 0.85 // Starts-with match
        }
        if (name.contains(query)) {
            return 0.65 // Substring match
        }

        val queryTokens = query.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val nameTokens = name.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (queryTokens.isEmpty() || nameTokens.isEmpty()) {
            return 0.05
        }

        val matchedCount = queryTokens.count { qToken ->
            nameTokens.any { nToken -> nToken.contains(qToken) || qToken.contains(nToken) }
        }

        val matchFraction = matchedCount.toDouble() / queryTokens.size.toDouble()
        return (matchFraction * 0.50).coerceIn(0.05, 0.50)
    }

    /**
     * Compute locality match score in range [0.0, 1.0].
     * Rewards places whose city, district, or formatted address matches query tokens.
     */
    fun computeLocalityScore(query: String, name: String, formattedAddress: String): Double {
        if (formattedAddress.isEmpty()) {
            return 0.0
        }

        val queryTokens = query.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val remainingTokens = queryTokens.filter { !name.contains(it) }

        if (remainingTokens.isNotEmpty()) {
            val matchedInAddress = remainingTokens.count { formattedAddress.contains(it) }
            val fraction = matchedInAddress.toDouble() / remainingTokens.size.toDouble()
            return (fraction * 0.80 + 0.20).coerceIn(0.0, 1.0)
        }

        // If all query tokens matched the name, check if any query token also appears in the locality
        val anyTokenInAddress = queryTokens.any { formattedAddress.contains(it) }
        return if (anyTokenInAddress) 0.50 else 0.10
    }

    /**
     * Compute distance proximity score in range [0.0, 1.0].
     * Results closer to the user receive higher scores; decays gradually beyond search radius.
     */
    fun computeDistanceScore(distanceMeters: Double, searchRadiusMeters: Double): Double {
        val radius = max(1_000.0, searchRadiusMeters)
        return if (distanceMeters <= radius) {
            (1.0 - (distanceMeters / (radius * 1.25))).coerceIn(0.20, 1.0)
        } else {
            // Decay beyond search radius asymptotically
            val excessDistance = distanceMeters - radius
            (0.20 / (1.0 + (excessDistance / 50_000.0))).coerceIn(0.0, 0.20)
        }
    }

    private data class ScoredPlace(
        val place: PlaceResult,
        val score: Double,
        val distanceMeters: Double,
        val isFarther: Boolean
    )
}
