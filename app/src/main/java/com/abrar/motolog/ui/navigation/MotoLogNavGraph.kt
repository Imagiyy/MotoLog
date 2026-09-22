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

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TwoWheeler
import com.abrar.motolog.ui.garage.GarageScreen
import com.abrar.motolog.ui.garage.detail.BikeDetailScreen
import com.abrar.motolog.ui.settings.SettingsScreen

@Serializable
data object LiveRoute

@Serializable
data object GarageRoute

@Serializable
data object HistoryRoute

@Serializable
data object SettingsRoute

@Serializable
data class BikeDetailRoute(val bikeId: Long)

@Serializable
data class RideDetailRoute(val rideId: Long)

/**
 * Bottom navigation destinations for MotoLog.
 */
enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Any
) {
    LIVE("Live", Icons.Default.Speed, LiveRoute),
    GARAGE("Garage", Icons.Default.TwoWheeler, GarageRoute),
    HISTORY("History", Icons.Default.History, HistoryRoute),
    SETTINGS("Settings", Icons.Default.Settings, SettingsRoute)
}

/**
 * Main navigation graph with bottom navigation bar.
 */
@Composable
fun MotoLogNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isLiveScreen = currentDestination?.hasRoute(LiveRoute::class) == true

    Scaffold(
        bottomBar = {
            if (!isLandscape || !isLiveScreen) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LiveRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<LiveRoute> {
                LiveScreen(
                    onNavigateToSettings = {
                        navController.navigate(SettingsRoute)
                    }
                )
            }
            composable<GarageRoute> {
                GarageScreen(
                    onNavigateToBikeDetail = { bikeId ->
                        navController.navigate(BikeDetailRoute(bikeId))
                    }
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    onNavigateToRideDetail = { rideId ->
                        navController.navigate(RideDetailRoute(rideId))
                    }
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable<BikeDetailRoute> {
                BikeDetailScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable<RideDetailRoute> {
                com.abrar.motolog.ui.history.detail.RideDetailScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
