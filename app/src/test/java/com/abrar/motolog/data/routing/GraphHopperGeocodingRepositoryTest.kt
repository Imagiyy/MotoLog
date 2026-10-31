package com.abrar.motolog.data.routing

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.shared.domain.model.DestinationSourceType
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.RoutingError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class GraphHopperGeocodingRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var settingsRepository: SettingsRepository

    private class FakeHttpTransport : HttpTransport {
        var requestedUrl: String? = null
        var responseToReturn: HttpResponse = HttpResponse(200, "{}")

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
            requestedUrl = url
            return responseToReturn
        }
    }

    private lateinit var fakeTransport: FakeHttpTransport
    private lateinit var repository: GraphHopperGeocodingRepository

    @Before
    fun setUp() {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_settings.preferences_pb") }
        )
        settingsRepository = SettingsRepository(testDataStore)
        fakeTransport = FakeHttpTransport()
        repository = GraphHopperGeocodingRepository(fakeTransport, settingsRepository)
    }

    @Test
    fun searchDestinationsSuccessParsesAddressAndCoordinates() = runTest {
        val jsonResponse = """
            {
              "hits": [
                {
                  "point": {
                    "lat": 12.9716,
                    "lng": 77.5946
                  },
                  "name": "Cubbon Park",
                  "street": "Kasturba Road",
                  "housenumber": "1",
                  "city": "Bengaluru",
                  "state": "Karnataka",
                  "country": "India",
                  "osm_id": "osm_12345"
                }
              ]
            }
        """.trimIndent()
        fakeTransport.responseToReturn = HttpResponse(200, jsonResponse)

        val result = repository.searchDestinations(
            query = "Cubbon Park",
            nearLocation = LocationPoint(12.97, 77.59, 0f, 5f, 1000L)
        )

        assertTrue(result.isSuccess)
        val destinations = result.getOrThrow()
        assertEquals(1, destinations.size)

        val dest = destinations.first()
        assertEquals(12.9716, dest.latitude, 0.0001)
        assertEquals(77.5946, dest.longitude, 0.0001)
        assertEquals("osm_12345", dest.providerPlaceId)
        assertEquals(DestinationSourceType.SEARCH, dest.sourceType)
        assertTrue(dest.displayName.contains("Cubbon Park"))
        assertTrue(dest.displayName.contains("Bengaluru"))
        assertTrue(dest.displayName.contains("Karnataka"))

        assertTrue(fakeTransport.requestedUrl!!.contains("q=Cubbon+Park"))
        assertTrue(fakeTransport.requestedUrl!!.contains("point=12.97,77.59"))
    }

    @Test
    fun searchDestinationsEmptyQueryReturnsEmptyListWithoutNetworkCall() = runTest {
        val result = repository.searchDestinations("   ")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
        assertNull(fakeTransport.requestedUrl)
    }

    @Test
    fun reverseGeocodeSuccessReturnsFormattedPinDestination() = runTest {
        val jsonResponse = """
            {
              "hits": [
                {
                  "point": {
                    "lat": 12.9716,
                    "lng": 77.5946
                  },
                  "name": "MG Road Metro Station",
                  "city": "Bengaluru",
                  "state": "Karnataka",
                  "country": "India"
                }
              ]
            }
        """.trimIndent()
        fakeTransport.responseToReturn = HttpResponse(200, jsonResponse)

        val result = repository.reverseGeocode(12.9716, 77.5946)
        assertTrue(result.isSuccess)

        val dest = result.getOrThrow()
        assertEquals(12.9716, dest.latitude, 0.0001)
        assertEquals(77.5946, dest.longitude, 0.0001)
        assertEquals(DestinationSourceType.MAP_PIN, dest.sourceType)
        assertTrue(dest.displayName.contains("MG Road Metro Station"))
        assertTrue(fakeTransport.requestedUrl!!.contains("reverse=true"))
    }

    @Test
    fun reverseGeocodeEmptyHitsReturnsCoordinateFallback() = runTest {
        val jsonResponse = """{"hits": []}"""
        fakeTransport.responseToReturn = HttpResponse(200, jsonResponse)

        val result = repository.reverseGeocode(12.9716, 77.5946)
        assertTrue(result.isSuccess)

        val dest = result.getOrThrow()
        assertEquals(DestinationSourceType.MAP_PIN, dest.sourceType)
        assertTrue(dest.displayName.contains("12.97160"))
        assertTrue(dest.displayName.contains("77.59460"))
    }

    @Test
    fun reverseGeocodeInvalidCoordinatesReturnsError() = runTest {
        val result = repository.reverseGeocode(Double.NaN, 77.5946)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoutingError.InvalidCoordinates)
    }

    @Test
    fun searchDestinationsRateLimitReturnsRateLimitError() = runTest {
        fakeTransport.responseToReturn = HttpResponse(429, "Rate limit exceeded")

        val result = repository.searchDestinations("Mumbai")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoutingError.RateLimitExceeded)
    }
}
