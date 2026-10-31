package com.abrar.motolog.data.routing

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.ManeuverType
import com.abrar.motolog.shared.domain.model.RouteProfile
import com.abrar.motolog.shared.domain.model.RouteRequest
import com.abrar.motolog.shared.domain.model.RoutingError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class GraphHopperRoutingRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var settingsRepository: SettingsRepository

    private class FakeHttpTransport : HttpTransport {
        var requestedUrl: String? = null
        var responseToReturn: HttpResponse = HttpResponse(200, "{}")
        var exceptionToThrow: Throwable? = null

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
            requestedUrl = url
            exceptionToThrow?.let { throw it }
            return responseToReturn
        }
    }

    private lateinit var fakeTransport: FakeHttpTransport
    private lateinit var repository: GraphHopperRoutingRepository

    private fun createPoint(lat: Double, lon: Double) = LocationPoint(
        latitude = lat,
        longitude = lon,
        speedMps = 0f,
        accuracyMeters = 5f,
        timestamp = 1000L
    )

    @Before
    fun setUp() {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_settings.preferences_pb") }
        )
        settingsRepository = SettingsRepository(testDataStore)
        fakeTransport = FakeHttpTransport()
        repository = GraphHopperRoutingRepository(fakeTransport, settingsRepository)
    }

    @Test
    fun calculateRouteSuccessParsesGeometryAndInstructions() = runTest {
        val sampleResponse = """
            {
              "paths": [
                {
                  "distance": 1250.0,
                  "time": 90000,
                  "points": "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                  "bbox": [-126.453, 38.5, -120.2, 43.252],
                  "instructions": [
                    {
                      "text": "Head north on Main St",
                      "street_name": "Main St",
                      "distance": 500.0,
                      "time": 30000,
                      "interval": [0, 1],
                      "sign": 0,
                      "heading": 45.0
                    },
                    {
                      "text": "Turn right onto Cross Ave",
                      "street_name": "Cross Ave",
                      "distance": 750.0,
                      "time": 60000,
                      "interval": [1, 2],
                      "sign": 2,
                      "heading": 90.0
                    },
                    {
                      "text": "You have arrived",
                      "distance": 0.0,
                      "time": 0,
                      "interval": [2, 2],
                      "sign": 4
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        fakeTransport.responseToReturn = HttpResponse(200, sampleResponse)

        val request = RouteRequest(
            origin = createPoint(38.5, -120.2),
            destination = createPoint(43.252, -126.453),
            travelProfile = RouteProfile.MOTORCYCLE
        )

        val result = repository.calculateRoute(request)
        assertTrue(result.isSuccess)

        val routes = result.getOrThrow()
        assertEquals(1, routes.size)
        val route = routes.first()

        assertEquals(1250.0, route.totalDistanceMeters, 0.01)
        assertEquals(90L, route.expectedDurationSeconds)
        assertEquals(3, route.geometry.size)

        assertNotNull(route.bounds)
        assertEquals(38.5, route.bounds!!.minLatitude, 0.001)
        assertEquals(-126.453, route.bounds!!.minLongitude, 0.001)

        assertEquals(3, route.instructions.size)
        assertEquals(ManeuverType.CONTINUE, route.instructions[0].maneuverType)
        assertEquals("Main St", route.instructions[0].streetName)
        assertEquals(ManeuverType.TURN_RIGHT, route.instructions[1].maneuverType)
        assertEquals(ManeuverType.ARRIVE, route.instructions[2].maneuverType)

        assertTrue(fakeTransport.requestedUrl!!.contains("profile=motorcycle"))
    }

    @Test
    fun calculateRouteNoRouteFoundReturnsDomainError() = runTest {
        val errorResponse = """{"message": "Connection between locations not found"}"""
        fakeTransport.responseToReturn = HttpResponse(400, errorResponse)

        val request = RouteRequest(
            origin = createPoint(12.0, 77.0),
            destination = createPoint(13.0, 78.0)
        )

        val result = repository.calculateRoute(request)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoutingError.NoRouteFound)
    }

    @Test
    fun calculateRouteRateLimitReturnsRateLimitError() = runTest {
        fakeTransport.responseToReturn = HttpResponse(429, "Rate limit exceeded")

        val request = RouteRequest(
            origin = createPoint(12.0, 77.0),
            destination = createPoint(13.0, 78.0)
        )

        val result = repository.calculateRoute(request)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoutingError.RateLimitExceeded)
    }

    @Test
    fun calculateRouteUnauthorizedReturnsProviderError() = runTest {
        fakeTransport.responseToReturn = HttpResponse(401, """{"message": "Your key is invalid"}""")

        val request = RouteRequest(
            origin = createPoint(12.0, 77.0),
            destination = createPoint(13.0, 78.0)
        )

        val result = repository.calculateRoute(request)
        assertTrue(result.isFailure)
        val err = result.exceptionOrNull() as RoutingError.ProviderError
        assertEquals(401, err.statusCode)
    }

    @Test
    fun calculateRouteTimeoutReturnsTimeoutError() = runTest {
        fakeTransport.exceptionToThrow = SocketTimeoutException("Read timed out")

        val request = RouteRequest(
            origin = createPoint(12.0, 77.0),
            destination = createPoint(13.0, 78.0)
        )

        val result = repository.calculateRoute(request)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoutingError.Timeout)
    }

    @Test
    fun calculateRouteMalformedJsonReturnsProviderError() = runTest {
        fakeTransport.responseToReturn = HttpResponse(200, "<html>Bad Gateway</html>")

        val request = RouteRequest(
            origin = createPoint(12.0, 77.0),
            destination = createPoint(13.0, 78.0)
        )

        val result = repository.calculateRoute(request)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RoutingError.ProviderError)
    }

    @Test
    fun refreshRouteUsesPreviousDestinationWithNewOrigin() = runTest {
        val sampleResponse = """
            {
              "paths": [
                {
                  "distance": 800.0,
                  "time": 60000,
                  "points": "_p~iF~ps|U"
                }
              ]
            }
        """.trimIndent()
        fakeTransport.responseToReturn = HttpResponse(200, sampleResponse)

        val originalRequest = RouteRequest(
            origin = createPoint(12.000, 77.000),
            destination = createPoint(12.050, 77.050)
        )
        repository.calculateRoute(originalRequest)

        // Reroute from new intermediate position
        val newOrigin = createPoint(12.020, 77.020)
        val rerouteResult = repository.refreshRoute("prev_id", newOrigin)

        assertTrue(rerouteResult.isSuccess)
        assertTrue(fakeTransport.requestedUrl!!.contains("point=12.02,77.02"))
        assertTrue(fakeTransport.requestedUrl!!.contains("point=12.05,77.05"))
    }
}
