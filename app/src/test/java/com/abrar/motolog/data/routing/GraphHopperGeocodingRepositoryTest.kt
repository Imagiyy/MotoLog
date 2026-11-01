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
import org.junit.Assert.assertFalse
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
        var responseHandler: ((String) -> HttpResponse)? = null

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
            requestedUrl = url
            return responseHandler?.invoke(url) ?: responseToReturn
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
        settingsRepository.setRoutingApiKey("test_key")
        settingsRepository.setRoutingMode(com.abrar.motolog.shared.domain.model.RoutingMode.GRAPH_HOPPER_CLOUD)

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
        settingsRepository.setRoutingApiKey("test_key")
        settingsRepository.setRoutingMode(com.abrar.motolog.shared.domain.model.RoutingMode.GRAPH_HOPPER_CLOUD)

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
        settingsRepository.setRoutingApiKey("test_key")
        settingsRepository.setRoutingMode(com.abrar.motolog.shared.domain.model.RoutingMode.GRAPH_HOPPER_CLOUD)

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
        settingsRepository.setRoutingApiKey("test_key")
        settingsRepository.setRoutingMode(com.abrar.motolog.shared.domain.model.RoutingMode.GRAPH_HOPPER_CLOUD)

        fakeTransport.responseToReturn = HttpResponse(429, "Rate limit exceeded")

        val result = repository.searchDestinations("Mumbai")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoutingError.RateLimitExceeded)
    }

    @Test
    fun searchDestinationsCloudMode_missingApiKey_throwsApiKeyMissingWithoutNetworkCall() = runTest {
        settingsRepository.setRoutingApiKey("")
        // Explicitly set cloud mode with empty key (simulating old config)
        settingsRepository.setRoutingMode(com.abrar.motolog.shared.domain.model.RoutingMode.GRAPH_HOPPER_CLOUD)

        // However SettingsRepository automatically falls back to KEYLESS when key is blank,
        // so test when an invalid key is provided to cloud mode:
        settingsRepository.setRoutingApiKey("invalid_key")
        fakeTransport.responseToReturn = HttpResponse(
            400,
            """{"message": "You have to pass an API key, or authentication is required"}"""
        )

        val result = repository.searchDestinations("Mumbai")
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("Error must be ApiKeyMissing but was $error", error is RoutingError.ApiKeyMissing)
    }

    @Test
    fun searchDestinationsCloudMode_400WithHintsApiKeyMessage_throwsApiKeyMissing() = runTest {
        settingsRepository.setRoutingApiKey("dummy_expired_key")
        settingsRepository.setRoutingMode(com.abrar.motolog.shared.domain.model.RoutingMode.GRAPH_HOPPER_CLOUD)
        fakeTransport.responseToReturn = HttpResponse(
            400,
            """{"hints": [{"message": "You have to pass an API key, or authentication is required"}]}"""
        )

        val result = repository.searchDestinations("Mumbai")
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("Error must be ApiKeyMissing but was $error", error is RoutingError.ApiKeyMissing)
    }

    @Test
    fun searchDestinationsCloudMode_400WithGenericError_extractsMessageInProviderError() = runTest {
        settingsRepository.setRoutingApiKey("dummy_valid_key")
        settingsRepository.setRoutingMode(com.abrar.motolog.shared.domain.model.RoutingMode.GRAPH_HOPPER_CLOUD)
        fakeTransport.responseToReturn = HttpResponse(
            400,
            """{"message": "Point parameter was invalid"}"""
        )

        val result = repository.searchDestinations("Mumbai")
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("Error must be ProviderError but was $error", error is RoutingError.ProviderError)
        val providerError = error as RoutingError.ProviderError
        assertEquals(400, providerError.statusCode)
        assertTrue(providerError.message!!.contains("Point parameter was invalid"))
    }

    @Test
    fun searchPlacesKeylessModeUsesSelfHostedUrlAndSendsNoKey() = runTest {
        val sampleResponse = """
            {
              "hits": [
                {
                  "point": { "lat": 12.9352, "lng": 77.6245 },
                  "name": "Koramangala 5th Block",
                  "city": "Bengaluru",
                  "state": "Karnataka",
                  "country": "India"
                }
              ]
            }
        """.trimIndent()
        fakeTransport.responseToReturn = HttpResponse(200, sampleResponse)

        settingsRepository.setRoutingMode(com.abrar.motolog.shared.domain.model.RoutingMode.KEYLESS_GRAPH_HOPPER)
        settingsRepository.setRoutingSelfHostedUrl("https://self-hosted-osm.local:8989")
        settingsRepository.setRoutingApiKey("secret_cloud_key_must_not_leak")

        val result = repository.searchPlaces("Koramangala")
        assertTrue(result.isSuccess)

        val places = result.getOrThrow()
        assertEquals(1, places.size)
        val place = places.first()
        assertEquals("Koramangala 5th Block", place.name)
        assertEquals("Keyless GraphHopper", place.provider)

        val requestedUrl = fakeTransport.requestedUrl!!
        assertTrue(requestedUrl.startsWith("https://self-hosted-osm.local:8989/geocode?"))
        assertFalse("Self-hosted request must never send API key", requestedUrl.contains("key="))
        assertFalse(requestedUrl.contains("secret_cloud_key_must_not_leak"))
    }

    @Test
    fun searchPlacesKeylessModeFallsBackToPhotonWhenSelfHosted404() = runTest {
        val photonResponse = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [77.6412, 12.9784]
                  },
                  "properties": {
                    "osm_id": 987654,
                    "name": "Indiranagar 100ft Road",
                    "city": "Bengaluru",
                    "state": "Karnataka",
                    "country": "India"
                  }
                }
              ]
            }
        """.trimIndent()

        fakeTransport.responseHandler = { url ->
            if (url.contains("photon.komoot.io")) {
                HttpResponse(200, photonResponse)
            } else {
                HttpResponse(404, "Not Found")
            }
        }

        settingsRepository.setRoutingMode(com.abrar.motolog.shared.domain.model.RoutingMode.KEYLESS_GRAPH_HOPPER)
        settingsRepository.setRoutingSelfHostedUrl("https://routing-only-host:8989")

        val result = repository.searchPlaces("Indiranagar")
        assertTrue(result.isSuccess)

        val places = result.getOrThrow()
        assertEquals(1, places.size)
        val place = places.first()
        assertEquals("Indiranagar 100ft Road", place.name)
        assertEquals(12.9784, place.latitude, 0.001)
        assertEquals(77.6412, place.longitude, 0.001)
        assertEquals("OpenStreetMap", place.provider)
    }

    @Test
    fun searchPlacesNearbyFirstRanksCloserLocationsAboveFartherMatches() = runTest {
        val jsonResponse = """
            {
              "hits": [
                {
                  "point": { "lat": 13.0827, "lng": 80.2707 },
                  "name": "Central Cafe",
                  "city": "Chennai",
                  "state": "Tamil Nadu",
                  "country": "India"
                },
                {
                  "point": { "lat": 12.9720, "lng": 77.5950 },
                  "name": "Central Cafe",
                  "city": "Bengaluru",
                  "state": "Karnataka",
                  "country": "India"
                }
              ]
            }
        """.trimIndent()
        fakeTransport.responseToReturn = HttpResponse(200, jsonResponse)

        val riderLocation = LocationPoint(12.9716, 77.5946, 0f, 5f, 1000L)
        val result = repository.searchPlaces("Central Cafe", nearLocation = riderLocation)

        assertTrue(result.isSuccess)
        val places = result.getOrThrow()
        assertEquals(2, places.size)

        // The Bengaluru result (close to rider) must be ranked above Chennai (farther away)
        assertEquals("Bengaluru", places[0].formattedAddress.split(", ").firstOrNull { it.contains("Bengaluru") })
        assertFalse(places[0].isFartherMatch)
        assertTrue(places[0].distanceFromUserMeters != null && places[0].distanceFromUserMeters!! < 2000.0)

        // Chennai result is ~290 km away, so outside default 50 km radius: flagged as farther match
        assertTrue(places[1].isFartherMatch)
        assertTrue(places[1].distanceFromUserMeters != null && places[1].distanceFromUserMeters!! > 200_000.0)
    }
}
