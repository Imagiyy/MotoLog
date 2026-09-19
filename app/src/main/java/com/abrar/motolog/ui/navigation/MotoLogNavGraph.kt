package com.abrar.motolog.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abrar.motolog.ui.history.HistoryScreen
import com.abrar.motolog.ui.live.LiveScreen
import kotlinx.serialization.Serializable

// ============================================================
// Type-safe navigation routes
// ============================================================

@Serializable
data object LiveRoute

@Serializable
data object HistoryRoute

/**
 * Bottom navigation destinations for MotoLog.
 */
enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Any
) {
    LIVE("Live", Icons.Default.Speed, LiveRoute),
    HISTORY("History", Icons.Default.History, HistoryRoute)
}

/**
 * Main navigation graph with bottom navigation bar.
 */
@Composable
fun MotoLogNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        selected = currentDestination?.hasRoute(destination.route::class) == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LiveRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<LiveRoute> {
                LiveScreen()
            }
            composable<HistoryRoute> {
                HistoryScreen()
            }
        }
    }
}
