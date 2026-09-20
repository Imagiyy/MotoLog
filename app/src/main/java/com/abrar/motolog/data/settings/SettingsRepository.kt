package com.abrar.motolog.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val BATTERY_GUIDANCE_SEEN = booleanPreferencesKey("battery_guidance_seen")
        val CURRENT_BIKE_ID = androidx.datastore.preferences.core.longPreferencesKey("current_bike_id")
    }

    // ============================================================
    // Theme
    // ============================================================

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            "AMOLED" -> ThemeMode.AMOLED
            else -> ThemeMode.DARK // Default to dark for rider visibility
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
    // Units
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
