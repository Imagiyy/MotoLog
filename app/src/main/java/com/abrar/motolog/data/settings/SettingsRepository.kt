package com.abrar.motolog.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.model.FuelUnit
import com.abrar.motolog.shared.domain.model.RoutingMode
import com.abrar.motolog.shared.domain.model.SpeedAlertStyle
import com.abrar.motolog.shared.domain.model.TrackingMode
import java.net.URI
import com.abrar.motolog.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository wrapping DataStore Preferences for app-wide settings.
 * Provides type-safe accessors for each setting.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    // ============================================================
    // Preference Keys
    // ============================================================

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_PAUSE_ENABLED = booleanPreferencesKey("auto_pause_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val USE_METRIC_UNITS = booleanPreferencesKey("use_metric_units")
        val FUEL_UNIT = stringPreferencesKey("fuel_unit")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val TRACKING_MODE = stringPreferencesKey("tracking_mode")
        val SPEED_ALERT_ENABLED = booleanPreferencesKey("speed_alert_enabled")
        val SPEED_ALERT_THRESHOLD_KMH = doublePreferencesKey("speed_alert_threshold_kmh")
        val SPEED_ALERT_STYLE = stringPreferencesKey("speed_alert_style")
        val VOICE_ANNOUNCEMENTS_ENABLED = booleanPreferencesKey("voice_announcements_enabled")
        val THERMAL_ECO_MODE = booleanPreferencesKey("thermal_eco_mode")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val BATTERY_GUIDANCE_SEEN = booleanPreferencesKey("battery_guidance_seen")
        val CURRENT_BIKE_ID = longPreferencesKey("current_bike_id")
        val DEFAULT_MAP_THEME = stringPreferencesKey("default_map_theme")
        val DASHBOARD_TEXT_COLOR = longPreferencesKey("dashboard_text_color")
        val DASHBOARD_BG_COLOR = longPreferencesKey("dashboard_bg_color")
        val DASHBOARD_ACCENT_COLOR = longPreferencesKey("dashboard_accent_color")
        val DASHBOARD_COMPONENT_COLOR = longPreferencesKey("dashboard_component_color")
        val ROUTING_API_KEY = stringPreferencesKey("routing_api_key")
        val ROUTING_PROVIDER_URL = stringPreferencesKey("routing_provider_url")
        val ROUTING_AUTO_REROUTE_ENABLED = booleanPreferencesKey("routing_auto_reroute_enabled")
        val NAVIGATION_FEATURE_ENABLED = booleanPreferencesKey("navigation_feature_enabled")
        val ROUTING_MODE = stringPreferencesKey("routing_mode")
        val ROUTING_SELF_HOSTED_URL = stringPreferencesKey("routing_self_hosted_url")
        val NEARBY_SEARCH_ENABLED = booleanPreferencesKey("nearby_search_enabled")
        val INITIAL_SEARCH_RADIUS_KM = intPreferencesKey("initial_search_radius_km")
        val GLOBAL_SEARCH_FALLBACK = booleanPreferencesKey("global_search_fallback")
        val ROUTING_AVOID_TOLLS = booleanPreferencesKey("routing_avoid_tolls")
        val ROUTING_AVOID_FERRIES = booleanPreferencesKey("routing_avoid_ferries")
        val ROUTING_AVOID_HIGHWAYS = booleanPreferencesKey("routing_avoid_highways")
        val ROUTING_AVOID_UNPAVED = booleanPreferencesKey("routing_avoid_unpaved")
    }

    // ============================================================
    // Theme & Custom Colors
    // ============================================================

    companion object {
        const val DEFAULT_DASHBOARD_TEXT_COLOR = 0xFFFFFFFFL
        const val DEFAULT_DASHBOARD_BG_COLOR = 0xFF030508L
        const val DEFAULT_DASHBOARD_ACCENT_COLOR = 0xFF00D4E5L
        const val DEFAULT_DASHBOARD_COMPONENT_COLOR = 0xFF0D0F14L
        const val DEFAULT_ROUTING_PROVIDER_URL = "https://graphhopper.com/api/1"
        const val DEFAULT_SELF_HOSTED_URL = "http://localhost:8989"
        const val DEFAULT_SEARCH_RADIUS_KM = 50
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE]
        try {
            if (raw != null) ThemeMode.valueOf(raw) else ThemeMode.RACING_TFT
        } catch (_: Exception) {
            ThemeMode.RACING_TFT
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    val defaultMapTheme: Flow<com.abrar.motolog.shared.domain.model.MapThemePreference> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.DEFAULT_MAP_THEME]
        try {
            if (raw != null) com.abrar.motolog.shared.domain.model.MapThemePreference.valueOf(raw) else com.abrar.motolog.shared.domain.model.MapThemePreference.LIGHT
        } catch (_: Exception) {
            com.abrar.motolog.shared.domain.model.MapThemePreference.LIGHT
        }
    }

    suspend fun setDefaultMapTheme(pref: com.abrar.motolog.shared.domain.model.MapThemePreference) {
        dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_MAP_THEME] = pref.name
        }
    }

    val dashboardTextColor: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.DASHBOARD_TEXT_COLOR] ?: DEFAULT_DASHBOARD_TEXT_COLOR
    }

    suspend fun setDashboardTextColor(color: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.DASHBOARD_TEXT_COLOR] = color
        }
    }

    val dashboardBgColor: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.DASHBOARD_BG_COLOR] ?: DEFAULT_DASHBOARD_BG_COLOR
    }

    suspend fun setDashboardBgColor(color: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.DASHBOARD_BG_COLOR] = color
        }
    }

    val dashboardAccentColor: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.DASHBOARD_ACCENT_COLOR] ?: DEFAULT_DASHBOARD_ACCENT_COLOR
    }

    suspend fun setDashboardAccentColor(color: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.DASHBOARD_ACCENT_COLOR] = color
        }
    }

    val dashboardComponentColor: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.DASHBOARD_COMPONENT_COLOR] ?: DEFAULT_DASHBOARD_COMPONENT_COLOR
    }

    suspend fun setDashboardComponentColor(color: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.DASHBOARD_COMPONENT_COLOR] = color
        }
    }

    suspend fun resetDashboardColors() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.DASHBOARD_TEXT_COLOR)
            prefs.remove(Keys.DASHBOARD_BG_COLOR)
            prefs.remove(Keys.DASHBOARD_ACCENT_COLOR)
            prefs.remove(Keys.DASHBOARD_COMPONENT_COLOR)
        }
    }

    // ============================================================
    // Auto-Pause
    // ============================================================

    val autoPauseEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PAUSE_ENABLED] ?: true // Enabled by default
    }

    suspend fun setAutoPauseEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_PAUSE_ENABLED] = enabled
        }
    }

    // ============================================================
    // Keep Screen On
    // ============================================================

    val keepScreenOn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.KEEP_SCREEN_ON] ?: true // On by default for riding
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.KEEP_SCREEN_ON] = enabled
        }
    }

    // ============================================================
    // Units (Speed & Distance)
    // ============================================================

    val useMetricUnits: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.USE_METRIC_UNITS] ?: true // Metric by default
    }

    suspend fun setUseMetricUnits(metric: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.USE_METRIC_UNITS] = metric
        }
    }

    // ============================================================
    // Fuel Economy Units
    // ============================================================

    val fuelUnit: Flow<FuelUnit> = dataStore.data.map { prefs ->
        try {
            prefs[Keys.FUEL_UNIT]?.let { FuelUnit.valueOf(it) } ?: FuelUnit.KM_PER_LITER
        } catch (_: IllegalArgumentException) {
            FuelUnit.KM_PER_LITER
        }
    }

    suspend fun setFuelUnit(unit: FuelUnit) {
        dataStore.edit { prefs ->
            prefs[Keys.FUEL_UNIT] = unit.name
        }
    }

    // ============================================================
    // Currency Symbol (Display only)
    // ============================================================

    val currencySymbol: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CURRENCY_SYMBOL] ?: "$"
    }

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_SYMBOL] = symbol.trim().ifEmpty { "$" }
        }
    }

    // ============================================================
    // Tracking Mode (Accuracy vs Battery)
    // ============================================================

    val trackingMode: Flow<TrackingMode> = dataStore.data.map { prefs ->
        try {
            prefs[Keys.TRACKING_MODE]?.let { TrackingMode.valueOf(it) } ?: TrackingMode.HIGH_ACCURACY
        } catch (_: IllegalArgumentException) {
            TrackingMode.HIGH_ACCURACY
        }
    }

    suspend fun setTrackingMode(mode: TrackingMode) {
        dataStore.edit { prefs ->
            prefs[Keys.TRACKING_MODE] = mode.name
        }
    }

    // ============================================================
    // Speed Alert
    // ============================================================

    val speedAlertEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SPEED_ALERT_ENABLED] ?: false
    }

    suspend fun setSpeedAlertEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SPEED_ALERT_ENABLED] = enabled
        }
    }

    val speedAlertThresholdKmh: Flow<Double> = dataStore.data.map { prefs ->
        prefs[Keys.SPEED_ALERT_THRESHOLD_KMH] ?: TrackingConstants.SPEED_ALERT_DEFAULT_THRESHOLD_KMH
    }

    suspend fun setSpeedAlertThresholdKmh(thresholdKmh: Double) {
        dataStore.edit { prefs ->
            prefs[Keys.SPEED_ALERT_THRESHOLD_KMH] = thresholdKmh
        }
    }

    val speedAlertStyle: Flow<SpeedAlertStyle> = dataStore.data.map { prefs ->
        try {
            prefs[Keys.SPEED_ALERT_STYLE]?.let { SpeedAlertStyle.valueOf(it) } ?: SpeedAlertStyle.ALL
        } catch (_: IllegalArgumentException) {
            SpeedAlertStyle.ALL
        }
    }

    suspend fun setSpeedAlertStyle(style: SpeedAlertStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.SPEED_ALERT_STYLE] = style.name
        }
    }

    // ============================================================
    // Voice Announcements & Thermal Eco Mode
    // ============================================================

    val voiceAnnouncementsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.VOICE_ANNOUNCEMENTS_ENABLED] ?: false
    }

    suspend fun setVoiceAnnouncementsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.VOICE_ANNOUNCEMENTS_ENABLED] = enabled
        }
    }

    val thermalEcoMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.THERMAL_ECO_MODE] ?: false
    }

    suspend fun setThermalEcoMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.THERMAL_ECO_MODE] = enabled
        }
    }

    // ============================================================
    // Disclaimer
    // ============================================================

    val disclaimerAccepted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DISCLAIMER_ACCEPTED] ?: false
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DISCLAIMER_ACCEPTED] = accepted
        }
    }

    // ============================================================
    // Battery Optimization Guidance
    // ============================================================

    val batteryGuidanceSeen: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.BATTERY_GUIDANCE_SEEN] ?: false
    }

    suspend fun setBatteryGuidanceSeen(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.BATTERY_GUIDANCE_SEEN] = seen
        }
    }

    // ============================================================
    // Active Bike Selection
    // ============================================================

    val currentBikeId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_BIKE_ID]
    }

    suspend fun setCurrentBikeId(bikeId: Long?) {
        dataStore.edit { prefs ->
            if (bikeId != null) {
                prefs[Keys.CURRENT_BIKE_ID] = bikeId
            } else {
                prefs.remove(Keys.CURRENT_BIKE_ID)
            }
        }
    }

    // ============================================================
    // Routing & Navigation Preferences
    // ============================================================

    val routingApiKey: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.ROUTING_API_KEY] ?: ""
    }

    suspend fun setRoutingApiKey(apiKey: String) {
        dataStore.edit { prefs ->
            prefs[Keys.ROUTING_API_KEY] = apiKey.trim()
        }
    }

    val routingProviderUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.ROUTING_PROVIDER_URL] ?: DEFAULT_ROUTING_PROVIDER_URL
    }

    suspend fun setRoutingProviderUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[Keys.ROUTING_PROVIDER_URL] = url.trim()
        }
    }

    val routingAutoRerouteEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ROUTING_AUTO_REROUTE_ENABLED] ?: true
    }

    suspend fun setRoutingAutoRerouteEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ROUTING_AUTO_REROUTE_ENABLED] = enabled
        }
    }

    val navigationFeatureEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.NAVIGATION_FEATURE_ENABLED] ?: true
    }

    suspend fun setNavigationFeatureEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NAVIGATION_FEATURE_ENABLED] = enabled
        }
    }

    // ============================================================
    // Routing Mode & Keyless Self-Hosted Configuration
    // ============================================================

    val routingMode: Flow<RoutingMode> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.ROUTING_MODE]
        try {
            if (raw != null) RoutingMode.valueOf(raw) else RoutingMode.GRAPH_HOPPER_CLOUD
        } catch (_: Exception) {
            RoutingMode.GRAPH_HOPPER_CLOUD
        }
    }

    suspend fun setRoutingMode(mode: RoutingMode) {
        dataStore.edit { prefs ->
            prefs[Keys.ROUTING_MODE] = mode.name
        }
    }

    val routingSelfHostedUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.ROUTING_SELF_HOSTED_URL] ?: DEFAULT_SELF_HOSTED_URL
    }

    /**
     * Validates and saves the self-hosted server URL. Returns true if valid and saved, false otherwise.
     */
    suspend fun setRoutingSelfHostedUrl(url: String): Boolean {
        if (!isValidServerUrl(url)) {
            return false
        }
        dataStore.edit { prefs ->
            prefs[Keys.ROUTING_SELF_HOSTED_URL] = url.trim()
        }
        return true
    }

    val nearbySearchEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.NEARBY_SEARCH_ENABLED] ?: true
    }

    suspend fun setNearbySearchEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NEARBY_SEARCH_ENABLED] = enabled
        }
    }

    val initialSearchRadiusKm: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.INITIAL_SEARCH_RADIUS_KM] ?: DEFAULT_SEARCH_RADIUS_KM
    }

    suspend fun setInitialSearchRadiusKm(radiusKm: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.INITIAL_SEARCH_RADIUS_KM] = radiusKm.coerceIn(5, 500)
        }
    }

    val globalSearchFallback: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.GLOBAL_SEARCH_FALLBACK] ?: true
    }

    suspend fun setGlobalSearchFallback(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_SEARCH_FALLBACK] = enabled
        }
    }

    val routingAvoidTolls: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ROUTING_AVOID_TOLLS] ?: false
    }

    suspend fun setRoutingAvoidTolls(avoid: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ROUTING_AVOID_TOLLS] = avoid
        }
    }

    val routingAvoidFerries: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ROUTING_AVOID_FERRIES] ?: false
    }

    suspend fun setRoutingAvoidFerries(avoid: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ROUTING_AVOID_FERRIES] = avoid
        }
    }

    val routingAvoidHighways: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ROUTING_AVOID_HIGHWAYS] ?: false
    }

    suspend fun setRoutingAvoidHighways(avoid: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ROUTING_AVOID_HIGHWAYS] = avoid
        }
    }

    val routingAvoidUnpaved: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ROUTING_AVOID_UNPAVED] ?: false
    }

    suspend fun setRoutingAvoidUnpaved(avoid: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ROUTING_AVOID_UNPAVED] = avoid
        }
    }

    /**
     * Verifies that the given server URL has a valid scheme and host.
     */
    fun isValidServerUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return false
        }
        return try {
            val uri = URI(trimmed)
            !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
