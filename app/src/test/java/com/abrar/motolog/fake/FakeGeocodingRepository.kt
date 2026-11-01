package com.abrar.motolog.fake

import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.DestinationSourceType
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlaceResult
import com.abrar.motolog.shared.domain.repository.GeocodingRepository

class FakeGeocodingRepository : GeocodingRepository {

    var searchResultsToReturn: Result<List<Destination>> = Result.success(
        listOf(
            Destination(
                id = "dest_1",
                latitude = 12.9716,
                longitude = 77.5946,
                displayName = "MG Road, Bengaluru",
                sourceType = DestinationSourceType.SEARCH
            )
        )
    )

    var placeResultsToReturn: Result<List<PlaceResult>> = Result.success(
        listOf(
            PlaceResult(
                id = "dest_1",
                name = "MG Road",
                formattedAddress = "Bengaluru, Karnataka",
                latitude = 12.9716,
                longitude = 77.5946,
                distanceFromUserMeters = 1500.0,
                provider = "Fake"
            )
        )
    )

    var reverseGeocodeToReturn: Result<Destination> = Result.success(
        Destination(
            id = "rev_1",
            latitude = 12.9716,
            longitude = 77.5946,
            displayName = "Pinned Location, Bengaluru",
            sourceType = DestinationSourceType.MAP_PIN
        )
    )

    var lastSearchQuery: String? = null
    var lastNearLocation: LocationPoint? = null

    override suspend fun searchPlaces(
        query: String,
        nearLocation: LocationPoint?,
        radiusMeters: Int?
    ): Result<List<PlaceResult>> {
        lastSearchQuery = query
        lastNearLocation = nearLocation
        return placeResultsToReturn
    }

    override suspend fun searchDestinations(
        query: String,
        nearLocation: LocationPoint?
    ): Result<List<Destination>> {
        lastSearchQuery = query
        lastNearLocation = nearLocation
        return searchResultsToReturn
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<Destination> {
        return reverseGeocodeToReturn
    }
}
