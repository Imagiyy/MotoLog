package com.abrar.motolog.fake

import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.DestinationSourceType
import com.abrar.motolog.shared.domain.model.LocationPoint
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
