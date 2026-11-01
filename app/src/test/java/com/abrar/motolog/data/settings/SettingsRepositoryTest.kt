package com.abrar.motolog.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.abrar.motolog.shared.domain.model.RoutingMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_settings.preferences_pb") }
        )
        repository = SettingsRepository(testDataStore)
    }

    @Test
    fun defaultRoutingSettings_matchExpectedDefaults() = runTest(testDispatcher) {
        assertEquals(RoutingMode.KEYLESS_GRAPH_HOPPER, repository.routingMode.first())
        assertEquals("http://localhost:8989", repository.routingSelfHostedUrl.first())
        assertTrue(repository.nearbySearchEnabled.first())
        assertEquals(50, repository.initialSearchRadiusKm.first())
        assertTrue(repository.globalSearchFallback.first())
        assertFalse(repository.routingAvoidTolls.first())
        assertFalse(repository.routingAvoidFerries.first())
        assertFalse(repository.routingAvoidHighways.first())
        assertFalse(repository.routingAvoidUnpaved.first())
    }

    @Test
    fun setRoutingMode_persistsCorrectly() = runTest(testDispatcher) {
        repository.setRoutingApiKey("valid_key_123")
        repository.setRoutingMode(RoutingMode.GRAPH_HOPPER_CLOUD)
        assertEquals(RoutingMode.GRAPH_HOPPER_CLOUD, repository.routingMode.first())

        repository.setRoutingMode(RoutingMode.KEYLESS_GRAPH_HOPPER)
        assertEquals(RoutingMode.KEYLESS_GRAPH_HOPPER, repository.routingMode.first())
    }

    @Test
    fun routingMode_fallsBackToKeylessWhenCloudKeyMissing() = runTest(testDispatcher) {
        repository.setRoutingMode(RoutingMode.GRAPH_HOPPER_CLOUD)
        repository.setRoutingApiKey("")
        assertEquals(RoutingMode.KEYLESS_GRAPH_HOPPER, repository.routingMode.first())
    }

    @Test
    fun isValidServerUrl_validatesProtocolsAndHosts() {
        assertTrue(repository.isValidServerUrl("http://localhost:8989"))
        assertTrue(repository.isValidServerUrl("https://graphhopper.example.com"))
        assertTrue(repository.isValidServerUrl("http://192.168.1.100:8080/api"))
        assertTrue(repository.isValidServerUrl("https://my-routing-proxy.org/"))

        // Invalid cases
        assertFalse(repository.isValidServerUrl(""))
        assertFalse(repository.isValidServerUrl("   "))
        assertFalse(repository.isValidServerUrl("ftp://graphhopper.org"))
        assertFalse(repository.isValidServerUrl("localhost:8989")) // Missing scheme
        assertFalse(repository.isValidServerUrl("http://")) // Missing host
        assertFalse(repository.isValidServerUrl("random text"))
    }

    @Test
    fun setRoutingSelfHostedUrl_validatesBeforeSaving() = runTest(testDispatcher) {
        val validUrl = "https://routing.mydomain.com/api"
        val success = repository.setRoutingSelfHostedUrl(validUrl)
        assertTrue(success)
        assertEquals(validUrl, repository.routingSelfHostedUrl.first())

        // Attempting to set an invalid URL must return false and preserve the previous valid URL
        val failure = repository.setRoutingSelfHostedUrl("invalid-url-string")
        assertFalse(failure)
        assertEquals(validUrl, repository.routingSelfHostedUrl.first())
    }

    @Test
    fun searchRadiusAndAvoidances_persistCorrectly() = runTest(testDispatcher) {
        repository.setNearbySearchEnabled(false)
        assertFalse(repository.nearbySearchEnabled.first())

        repository.setInitialSearchRadiusKm(25)
        assertEquals(25, repository.initialSearchRadiusKm.first())

        // Clamping check: below 5 clamped to 5, above 500 clamped to 500
        repository.setInitialSearchRadiusKm(2)
        assertEquals(5, repository.initialSearchRadiusKm.first())

        repository.setInitialSearchRadiusKm(1000)
        assertEquals(500, repository.initialSearchRadiusKm.first())

        repository.setGlobalSearchFallback(false)
        assertFalse(repository.globalSearchFallback.first())

        repository.setRoutingAvoidTolls(true)
        assertTrue(repository.routingAvoidTolls.first())

        repository.setRoutingAvoidFerries(true)
        assertTrue(repository.routingAvoidFerries.first())

        repository.setRoutingAvoidHighways(true)
        assertTrue(repository.routingAvoidHighways.first())

        repository.setRoutingAvoidUnpaved(true)
        assertTrue(repository.routingAvoidUnpaved.first())
    }
}
