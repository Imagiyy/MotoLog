package com.abrar.motolog.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.model.FuelUnit
import com.abrar.motolog.domain.model.SpeedAlertStyle
import com.abrar.motolog.domain.model.TrackingMode
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
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val BATTERY_GUIDANCE_SEEN = booleanPreferencesKey("battery_guidance_seen")
        val CURRENT_BIKE_ID = longPreferencesKey("current_bike_id")
    }

    // ============================================================
    // Theme
    // ============================================================

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE]
        try {
            if (raw != null) ThemeMode.valueOf(raw) else ThemeMode.DARK
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
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
}
