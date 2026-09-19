package com.abrar.motolog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abrar.motolog.ui.navigation.MotoLogNavGraph
import com.abrar.motolog.ui.theme.MotoLogTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity for MotoLog.
 * All UI is built with Jetpack Compose; this activity only sets up
 * the theme and navigation graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotoLogTheme {
                MotoLogNavGraph()
            }
        }
    }
}
