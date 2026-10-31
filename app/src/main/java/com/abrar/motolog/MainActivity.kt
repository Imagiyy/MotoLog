package com.abrar.motolog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.ui.navigation.MotoLogNavGraph
import com.abrar.motolog.ui.navigation.NavIntentEvent
import com.abrar.motolog.ui.theme.MotoLogTheme
import com.abrar.motolog.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * Single activity for MotoLog.
 * All UI is built with Jetpack Compose; this activity sets up
 * the theme and navigation graph, dynamically reacting to theme changes
 * and handling notification action/navigation intents (Fix #33).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val navEventFlow = MutableStateFlow<NavIntentEvent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.RACING_TFT
            )
            val dashboardTextColor by settingsRepository.dashboardTextColor.collectAsStateWithLifecycle(
                initialValue = SettingsRepository.DEFAULT_DASHBOARD_TEXT_COLOR
            )
            val dashboardBgColor by settingsRepository.dashboardBgColor.collectAsStateWithLifecycle(
                initialValue = SettingsRepository.DEFAULT_DASHBOARD_BG_COLOR
            )
            val dashboardAccentColor by settingsRepository.dashboardAccentColor.collectAsStateWithLifecycle(
                initialValue = SettingsRepository.DEFAULT_DASHBOARD_ACCENT_COLOR
            )
            val dashboardComponentColor by settingsRepository.dashboardComponentColor.collectAsStateWithLifecycle(
                initialValue = SettingsRepository.DEFAULT_DASHBOARD_COMPONENT_COLOR
            )
            val navEvent by navEventFlow.collectAsStateWithLifecycle()

            MotoLogTheme(
                themeMode = themeMode,
                textColor = androidx.compose.ui.graphics.Color(dashboardTextColor),
                backgroundColor = androidx.compose.ui.graphics.Color(dashboardBgColor),
                accentColor = androidx.compose.ui.graphics.Color(dashboardAccentColor),
                componentColor = androidx.compose.ui.graphics.Color(dashboardComponentColor)
            ) {
                MotoLogNavGraph(
                    navEvent = navEvent,
                    onNavEventConsumed = { navEventFlow.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val bikeId = intent.getLongExtra("EXTRA_NAV_BIKE_ID", -1L)
        val requestStop = intent.getBooleanExtra("EXTRA_REQUEST_STOP", false)
        when {
            bikeId > 0L -> navEventFlow.value = NavIntentEvent.NavigateToBike(bikeId)
            requestStop -> navEventFlow.value = NavIntentEvent.RequestStopRide
        }
    }
}
