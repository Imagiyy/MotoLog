package com.abrar.motolog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrar.motolog.data.settings.SettingsRepository
import com.abrar.motolog.ui.navigation.MotoLogNavGraph
import com.abrar.motolog.ui.theme.MotoLogTheme
import com.abrar.motolog.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity for MotoLog.
 * All UI is built with Jetpack Compose; this activity sets up
 * the theme and navigation graph, dynamically reacting to theme changes.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.DARK
            )
            MotoLogTheme(themeMode = themeMode) {
                MotoLogNavGraph()
            }
        }
    }
}
