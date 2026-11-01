package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlaceResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlaceRankingEngineTest {

    private val engine = PlaceRankingEngine

    // Rider in Bangalore center (MG Road: 12.9716, 77.5946)
    private val riderLocation = LocationPoint(
        latitude = 12.9716,
        longitude = 77.5946,
        speedMps = 0f,
        accuracyMeters = 5f,
        timestamp = 1000L,
        elapsedTimestampMs = 1000L
    )

    @Test
    fun exactNameMatchRanksFirstOverPartialMatches() {
        val places = listOf(
            PlaceResult(
                id = "1",
                name = "Apex Cafe & Grill",
                formattedAddress = "Indiranagar, Bangalore",
                latitude = 12.9750,
                longitude = 77.5980
            ),
            PlaceResult(
                id = "2",
                name = "Apex",
                formattedAddress = "MG Road, Bangalore",
                latitude = 12.9720,
                longitude = 77.5950
            ),
            PlaceResult(
                id = "3",
                name = "Apex Motors Workshop",
                formattedAddress = "Koramangala, Bangalore",
                latitude = 12.9350,
                longitude = 77.6200
            )
        )

        val ranked = engine.rankPlaces(places, query = "Apex", userLocation = riderLocation)
        assertEquals(3, ranked.size)
        assertEquals("Apex", ranked[0].name)
    }

    @Test
    fun closerCandidateRanksAboveFartherCandidateWithIdenticalName() {
        val nearPlace = PlaceResult(
            id = "near",
            name = "Shell Petrol Pump",
            formattedAddress = "Residency Road (1.2 km away)",
            latitude = 12.9650,
            longitude = 77.6000
        )
        val farPlace = PlaceResult(
            id = "far",
            name = "Shell Petrol Pump",
            formattedAddress = "Electronic City (18 km away)",
            latitude = 12.8450,
            longitude = 77.6600
        )

        val ranked = engine.rankPlaces(listOf(farPlace, nearPlace), query = "Shell", userLocation = riderLocation)
        assertEquals(2, ranked.size)
        assertEquals("near", ranked[0].id)
        assertEquals("far", ranked[1].id)
        assertNotNull(ranked[0].distanceFromUserMeters)
        assertNotNull(ranked[1].distanceFromUserMeters)
        assertTrue(ranked[0].distanceFromUserMeters!! < ranked[1].distanceFromUserMeters!!)
    }

    @Test
    fun exactMatchFartherAwayStillOutranksWeakPartialMatchVeryClose() {
        // High relevance (exact match 15 km away) vs Low relevance (partial match 500m away)
        val exactMatchFar = PlaceResult(
            id = "exact",
            name = "Ducati Service Center",
            formattedAddress = "Outer Ring Road (12 km away)",
            latitude = 12.9200,
            longitude = 77.6800
        )
        val weakMatchClose = PlaceResult(
            id = "weak",
            name = "Ducati Fan T-Shirt Stall",
            formattedAddress = "Church Street (300 m away)",
            latitude = 12.9730,
            longitude = 77.5960
        )

        val ranked = engine.rankPlaces(
            listOf(weakMatchClose, exactMatchFar),
            query = "Ducati Service Center",
            userLocation = riderLocation
        )
        assertEquals("exact", ranked[0].id)
    }

    @Test
    fun localityMatchBoostsResultWhenLocalityInQuery() {
        val place1 = PlaceResult(
            id = "1",
            name = "Triumph Motorcycles",
            formattedAddress = "Whitefield, Bangalore",
            latitude = 12.9698,
            longitude = 77.7499
        )
        val place2 = PlaceResult(
            id = "2",
            name = "Triumph Motorcycles",
            formattedAddress = "Koramangala, Bangalore",
            latitude = 12.9350,
            longitude = 77.6200
        )

        val ranked = engine.rankPlaces(
            listOf(place2, place1),
            query = "Triumph Whitefield",
            userLocation = riderLocation
        )
        assertEquals("1", ranked[0].id) // Whitefield locality match ranks first
    }

    @Test
    fun unknownUserLocationOmitsDistanceRankingGracefully() {
        val placeA = PlaceResult(
            id = "a",
            name = "KTM Store",
            formattedAddress = "Indiranagar",
            latitude = 12.9750,
            longitude = 77.6400
        )
        val placeB = PlaceResult(
            id = "b",
            name = "KTM Service & Spares",
            formattedAddress = "Yeshwanthpur",
            latitude = 13.0200,
            longitude = 77.5500
        )

        val ranked = engine.rankPlaces(
            listOf(placeB, placeA),
            query = "KTM Store",
            userLocation = null // No GPS fix
        )
        assertEquals(2, ranked.size)
        assertEquals("a", ranked[0].id) // Exact match on "KTM Store"
        assertEquals(null, ranked[0].distanceFromUserMeters)
    }

    @Test
    fun invalidCoordinatesAreDiscarded() {
        val invalidPlaces = listOf(
            PlaceResult(
                id = "nan",
                name = "Corrupted Lat",
                formattedAddress = "Unknown",
                latitude = Double.NaN,
                longitude = 77.5946
            ),
            PlaceResult(
                id = "out_of_bounds",
                name = "Out of range",
                formattedAddress = "Unknown",
                latitude = 195.0,
                longitude = 77.5946
            ),
            PlaceResult(
                id = "valid",
                name = "Valid Place",
                formattedAddress = "MG Road",
                latitude = 12.9716,
                longitude = 77.5946
            )
        )

        val ranked = engine.rankPlaces(invalidPlaces, query = "Place", userLocation = riderLocation)
        assertEquals(1, ranked.size)
        assertEquals("valid", ranked[0].id)
    }

    @Test
    fun searchRadiusMarksFartherMatchesAndGlobalFallbackBehavesCorrectly() {
        // Search radius: 20 km
        val localPlace = PlaceResult(
            id = "local",
            name = "Highway Dhaba",
            formattedAddress = "Hebbal (10 km)",
            latitude = 13.0400,
            longitude = 77.5900
        )
        val distantPlace = PlaceResult(
            id = "distant",
            name = "Highway Dhaba",
            formattedAddress = "Mysore Road (90 km)",
            latitude = 12.3000,
            longitude = 76.6500
        )

        // With allowGlobalFallback = true
        val withFallback = engine.rankPlaces(
            listOf(distantPlace, localPlace),
            query = "Highway Dhaba",
            userLocation = riderLocation,
            searchRadiusMeters = 20_000.0,
            allowGlobalFallback = true
        )
        assertEquals(2, withFallback.size)
        assertEquals("local", withFallback[0].id)
        assertFalse(withFallback[0].isFartherMatch)
        assertEquals("distant", withFallback[1].id)
        assertTrue(withFallback[1].isFartherMatch)

        // With allowGlobalFallback = false
        val withoutFallback = engine.rankPlaces(
            listOf(distantPlace, localPlace),
            query = "Highway Dhaba",
            userLocation = riderLocation,
            searchRadiusMeters = 20_000.0,
            allowGlobalFallback = false
        )
        assertEquals(1, withoutFallback.size)
        assertEquals("local", withoutFallback[0].id)
    }
}
