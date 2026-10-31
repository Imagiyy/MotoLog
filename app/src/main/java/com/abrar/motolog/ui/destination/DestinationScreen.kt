package com.abrar.motolog.ui.destination

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrar.motolog.data.map.MapSupport
import com.abrar.motolog.shared.domain.engine.RideCalculator
import com.abrar.motolog.shared.domain.engine.UnitConverter
import com.abrar.motolog.shared.domain.map.MapStyleProvider
import com.abrar.motolog.shared.domain.model.Destination
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteAvoidance
import com.abrar.motolog.shared.domain.model.RouteProfile
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.AndroidRenderMode
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.map.renderMode
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DestinationScreen(
    onNavigateBack: () -> Unit,
    onNavigationStarted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DestinationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val textColor by viewModel.dashboardTextColor.collectAsStateWithLifecycle()
    val bgColor by viewModel.dashboardBgColor.collectAsStateWithLifecycle()
    val accentColor by viewModel.dashboardAccentColor.collectAsStateWithLifecycle()
    val componentColor by viewModel.dashboardComponentColor.collectAsStateWithLifecycle()
    val defaultMapTheme by viewModel.defaultMapTheme.collectAsStateWithLifecycle()

    val palette = remember(textColor, bgColor, accentColor, componentColor, isSystemDark) {
        getCockpitThemePalette(
            isSystemDark = isSystemDark,
            textColor = Color(textColor),
            backgroundColor = Color(bgColor),
            accentColor = Color(accentColor),
            componentColor = Color(componentColor)
        )
    }

    LaunchedEffect(uiState.navigationStarted) {
        if (uiState.navigationStarted) {
            viewModel.onNavigationHandled()
            onNavigationStarted()
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
    ) {
        if (isLandscape) {
            DestinationLandscapeLayout(
                uiState = uiState,
                palette = palette,
                mapStyleProvider = viewModel.mapStyleProvider,
                isDarkMap = defaultMapTheme == com.abrar.motolog.shared.domain.model.MapThemePreference.DARK,
                onNavigateBack = onNavigateBack,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSelectDestination = viewModel::selectDestination,
                onClearSelectedDestination = viewModel::clearSelectedDestination,
                onToggleFavorite = viewModel::toggleFavorite,
                onSetTravelProfile = viewModel::setTravelProfile,
                onToggleAvoidance = viewModel::toggleAvoidance,
                onSelectRouteAlternative = viewModel::selectRouteAlternative,
                onToggleMapPinMode = viewModel::setMapPinMode,
                onConfirmMapPin = viewModel::confirmMapPinDestination,
                onSaveApiKey = viewModel::saveRoutingApiKey,
                onStartNavigation = viewModel::startNavigation,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            DestinationPortraitLayout(
                uiState = uiState,
                palette = palette,
                mapStyleProvider = viewModel.mapStyleProvider,
                isDarkMap = defaultMapTheme == com.abrar.motolog.shared.domain.model.MapThemePreference.DARK,
                onNavigateBack = onNavigateBack,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSelectDestination = viewModel::selectDestination,
                onClearSelectedDestination = viewModel::clearSelectedDestination,
                onToggleFavorite = viewModel::toggleFavorite,
                onSetTravelProfile = viewModel::setTravelProfile,
                onToggleAvoidance = viewModel::toggleAvoidance,
                onSelectRouteAlternative = viewModel::selectRouteAlternative,
                onToggleMapPinMode = viewModel::setMapPinMode,
                onConfirmMapPin = viewModel::confirmMapPinDestination,
                onSaveApiKey = viewModel::saveRoutingApiKey,
                onStartNavigation = viewModel::startNavigation,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DestinationPortraitLayout(
    uiState: DestinationUiState,
    palette: CockpitThemePalette,
    mapStyleProvider: MapStyleProvider?,
    isDarkMap: Boolean,
    onNavigateBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectDestination: (Destination) -> Unit,
    onClearSelectedDestination: () -> Unit,
    onToggleFavorite: (Destination) -> Unit,
    onSetTravelProfile: (RouteProfile) -> Unit,
    onToggleAvoidance: (RouteAvoidance) -> Unit,
    onSelectRouteAlternative: (Int) -> Unit,
    onToggleMapPinMode: (Boolean) -> Unit,
    onConfirmMapPin: (Double, Double) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiKeyInput by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Cockpit Header Bar
        DestinationTopBar(
            title = if (uiState.isMapPinMode) "SELECT PIN ON MAP" else "DESTINATION & ROUTE",
            isMapPinMode = uiState.isMapPinMode,
            palette = palette,
            onNavigateBack = {
                if (uiState.isMapPinMode) {
                    onToggleMapPinMode(false)
                } else if (uiState.selectedDestination != null) {
                    onClearSelectedDestination()
                } else {
                    onNavigateBack()
                }
            },
            onToggleMapPinMode = { onToggleMapPinMode(!uiState.isMapPinMode) }
        )

        // API Key prompt banner
        if (uiState.isApiKeyMissing || showApiKeyInput) {
            ApiKeyPromptCard(
                palette = palette,
                onSaveKey = { key ->
                    onSaveApiKey(key)
                    showApiKeyInput = false
                },
                onDismiss = { showApiKeyInput = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Error message banner
        if (uiState.errorMessage != null && !uiState.isApiKeyMissing) {
            ErrorMessageCard(
                message = uiState.errorMessage,
                palette = palette,
                onConfigureApiKey = { showApiKeyInput = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        when {
            // MAP PIN SELECTION MODE
            uiState.isMapPinMode -> {
                MapPinPickerView(
                    currentLocation = uiState.currentLocation,
                    mapStyleProvider = mapStyleProvider,
                    isDarkMap = isDarkMap,
                    palette = palette,
                    isReverseGeocoding = uiState.isReverseGeocoding,
                    onConfirmLocation = onConfirmMapPin,
                    onCancel = { onToggleMapPinMode(false) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ROUTE PREVIEW MODE (Destination Selected)
            uiState.selectedDestination != null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Map Preview on Top (fills flexible height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        RoutePreviewMapView(
                            currentLocation = uiState.currentLocation,
                            selectedRoute = uiState.selectedRoute,
                            destination = uiState.selectedDestination,
                            mapStyleProvider = mapStyleProvider,
                            isDarkMap = isDarkMap,
                            palette = palette,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Clear destination button over map
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(CircleShape)
                                .clickable { onClearSelectedDestination() },
                            color = palette.surface.copy(alpha = 0.9f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Destination",
                                tint = palette.dialText,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                    }

                    // Route Summary and Actions Card at Bottom
                    RouteSummaryCockpitCard(
                        uiState = uiState,
                        palette = palette,
                        onSetTravelProfile = onSetTravelProfile,
                        onToggleAvoidance = onToggleAvoidance,
                        onSelectAlternative = onSelectRouteAlternative,
                        onStartNavigation = onStartNavigation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

            // SEARCH & FAVORITES MODE
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    // Search Bar
                    DestinationSearchBar(
                        query = uiState.searchQuery,
                        isSearching = uiState.isSearching,
                        palette = palette,
                        onQueryChanged = onSearchQueryChanged,
                        onClearQuery = { onSearchQueryChanged("") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    // Pin on Map button row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onToggleMapPinMode(true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.primaryAccent),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = palette.primaryAccent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Pick on Map",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "PICK ON MAP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Saved & Favorite Destinations Chips
                    if (uiState.savedDestinations.isNotEmpty() && uiState.searchQuery.isBlank()) {
                        Text(
                            text = "SAVED DESTINATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = palette.dialText.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.savedDestinations.forEach { dest ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, palette.surfaceBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        .clickable { onSelectDestination(dest) },
                                    color = palette.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Saved",
                                            tint = palette.primaryAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = dest.displayName,
                                            color = palette.dialText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    // Search Results List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.searchResults, key = { it.id }) { destination ->
                            val isFavorite = uiState.savedDestinations.any { it.id == destination.id }
                            DestinationSearchResultItem(
                                destination = destination,
                                currentLocation = uiState.currentLocation,
                                useMetricUnits = uiState.useMetricUnits,
                                isFavorite = isFavorite,
                                palette = palette,
                                onSelect = { onSelectDestination(destination) },
                                onToggleFavorite = { onToggleFavorite(destination) }
                            )
                        }

                        if (uiState.searchResults.isEmpty() && uiState.searchQuery.isNotBlank() && !uiState.isSearching) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No destinations found for \"${uiState.searchQuery}\"",
                                        color = palette.dialText.copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationLandscapeLayout(
    uiState: DestinationUiState,
    palette: CockpitThemePalette,
    mapStyleProvider: MapStyleProvider?,
    isDarkMap: Boolean,
    onNavigateBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectDestination: (Destination) -> Unit,
    onClearSelectedDestination: () -> Unit,
    onToggleFavorite: (Destination) -> Unit,
    onSetTravelProfile: (RouteProfile) -> Unit,
    onToggleAvoidance: (RouteAvoidance) -> Unit,
    onSelectRouteAlternative: (Int) -> Unit,
    onToggleMapPinMode: (Boolean) -> Unit,
    onConfirmMapPin: (Double, Double) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // Left Half: Map View (either pin picker or route preview)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        ) {
            if (uiState.isMapPinMode) {
                MapPinPickerView(
                    currentLocation = uiState.currentLocation,
                    mapStyleProvider = mapStyleProvider,
                    isDarkMap = isDarkMap,
                    palette = palette,
                    isReverseGeocoding = uiState.isReverseGeocoding,
                    onConfirmLocation = onConfirmMapPin,
                    onCancel = { onToggleMapPinMode(false) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                RoutePreviewMapView(
                    currentLocation = uiState.currentLocation,
                    selectedRoute = uiState.selectedRoute,
                    destination = uiState.selectedDestination,
                    mapStyleProvider = mapStyleProvider,
                    isDarkMap = isDarkMap,
                    palette = palette,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Right Half: Controls, Search, Route Info
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(palette.surface)
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            DestinationTopBar(
                title = if (uiState.selectedDestination != null) "ROUTE PREVIEW" else "SEARCH DESTINATION",
                isMapPinMode = uiState.isMapPinMode,
                palette = palette,
                onNavigateBack = {
                    if (uiState.isMapPinMode) {
                        onToggleMapPinMode(false)
                    } else if (uiState.selectedDestination != null) {
                        onClearSelectedDestination()
                    } else {
                        onNavigateBack()
                    }
                },
                onToggleMapPinMode = { onToggleMapPinMode(!uiState.isMapPinMode) }
            )

            Spacer(Modifier.height(8.dp))

            if (uiState.selectedDestination != null) {
                RouteSummaryCockpitCard(
                    uiState = uiState,
                    palette = palette,
                    onSetTravelProfile = onSetTravelProfile,
                    onToggleAvoidance = onToggleAvoidance,
                    onSelectAlternative = onSelectRouteAlternative,
                    onStartNavigation = onStartNavigation,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                DestinationSearchBar(
                    query = uiState.searchQuery,
                    isSearching = uiState.isSearching,
                    palette = palette,
                    onQueryChanged = onSearchQueryChanged,
                    onClearQuery = { onSearchQueryChanged("") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { onToggleMapPinMode(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceBorder)
                ) {
                    Icon(imageVector = Icons.Default.Place, contentDescription = "Pick Pin")
                    Spacer(Modifier.width(6.dp))
                    Text("CHOOSE PIN ON MAP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))

                uiState.searchResults.forEach { destination ->
                    val isFavorite = uiState.savedDestinations.any { it.id == destination.id }
                    DestinationSearchResultItem(
                        destination = destination,
                        currentLocation = uiState.currentLocation,
                        useMetricUnits = uiState.useMetricUnits,
                        isFavorite = isFavorite,
                        palette = palette,
                        onSelect = { onSelectDestination(destination) },
                        onToggleFavorite = { onToggleFavorite(destination) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DestinationTopBar(
    title: String,
    isMapPinMode: Boolean,
    palette: CockpitThemePalette,
    onNavigateBack: () -> Unit,
    onToggleMapPinMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.primaryAccent
                )
            }
            Text(
                text = title,
                color = palette.dialText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        IconButton(
            onClick = onToggleMapPinMode,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = if (isMapPinMode) Icons.Default.Search else Icons.Default.Map,
                contentDescription = if (isMapPinMode) "Switch to Search" else "Switch to Map Pin",
                tint = palette.primaryAccent
            )
        }
    }
}

@Composable
private fun DestinationSearchBar(
    query: String,
    isSearching: Boolean,
    palette: CockpitThemePalette,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
        color = palette.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = palette.primaryAccent,
                modifier = Modifier.size(20.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = {
                    Text(
                        text = "Search address, city, or landmark...",
                        color = palette.dialText.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = palette.dialText,
                    unfocusedTextColor = palette.dialText,
                    cursorColor = palette.primaryAccent
                )
            )

            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = palette.primaryAccent,
                    strokeWidth = 2.dp
                )
            } else if (query.isNotBlank()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = palette.dialText.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationSearchResultItem(
    destination: Destination,
    currentLocation: LocationPoint?,
    useMetricUnits: Boolean,
    isFavorite: Boolean,
    palette: CockpitThemePalette,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val distanceDisplay = remember(destination, currentLocation, useMetricUnits) {
        if (currentLocation != null) {
            val distMeters = RideCalculator.haversineDistanceMeters(
                lat1 = currentLocation.latitude,
                lon1 = currentLocation.longitude,
                lat2 = destination.latitude,
                lon2 = destination.longitude
            )
            if (useMetricUnits) {
                if (distMeters >= 1000) {
                    String.format(Locale.US, "%.1f km", distMeters / 1000.0)
                } else {
                    String.format(Locale.US, "%d m", distMeters.roundToInt())
                }
            } else {
                val miles = distMeters / 1609.344
                String.format(Locale.US, "%.1f mi", miles)
            }
        } else {
            null
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, palette.surfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSelect()
            },
        color = palette.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(palette.primaryAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.displayName,
                    color = palette.dialText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (distanceDisplay != null) {
                    Text(
                        text = "$distanceDisplay away",
                        color = palette.primaryAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) palette.primaryAccent else palette.dialText.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RouteSummaryCockpitCard(
    uiState: DestinationUiState,
    palette: CockpitThemePalette,
    onSetTravelProfile: (RouteProfile) -> Unit,
    onToggleAvoidance: (RouteAvoidance) -> Unit,
    onSelectAlternative: (Int) -> Unit,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val selectedRoute = uiState.selectedRoute

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, palette.surfaceBorder, RoundedCornerShape(12.dp)),
        color = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Destination Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = uiState.selectedDestination?.displayName ?: "Destination",
                    color = palette.dialText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = palette.surfaceBorder.copy(alpha = 0.4f))

            if (uiState.isCalculatingRoute) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = palette.primaryAccent,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Calculating motorcycle route...",
                        color = palette.dialText,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else if (selectedRoute != null) {
                // Route Candidate Alternatives Tabs
                if (uiState.routeCandidates.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.routeCandidates.forEachIndexed { index, candidate ->
                            val isSelected = index == uiState.selectedRouteIndex
                            val etaMins = (candidate.expectedDurationSeconds / 60).coerceAtLeast(1)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) palette.primaryAccent else palette.surfaceBorder.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectAlternative(index) },
                                color = if (isSelected) palette.primaryAccent.copy(alpha = 0.15f) else palette.background
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (index == 0) "Fastest" else "Alt ${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) palette.primaryAccent else palette.dialText.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "${etaMins}m",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = palette.dialText
                                    )
                                }
                            }
                        }
                    }
                }

                // Primary Metrics Row
                val distKm = selectedRoute.totalDistanceMeters / 1000.0
                val distText = if (uiState.useMetricUnits) {
                    String.format(Locale.US, "%.1f KM", distKm)
                } else {
                    String.format(Locale.US, "%.1f MI", distKm * 0.621371)
                }
                val durationHrs = selectedRoute.expectedDurationSeconds / 3600
                val durationMins = (selectedRoute.expectedDurationSeconds % 3600) / 60
                val timeText = if (durationHrs > 0) {
                    "${durationHrs}h ${durationMins}m"
                } else {
                    "${durationMins}m"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DISTANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = palette.dialText.copy(alpha = 0.6f)
                        )
                        Text(
                            text = distText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = palette.primaryAccent
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ESTIMATED TIME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = palette.dialText.copy(alpha = 0.6f)
                        )
                        Text(
                            text = timeText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = palette.dialText
                        )
                    }
                }

                // Profile and Avoidances Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.background)
                            .padding(2.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onSetTravelProfile(RouteProfile.MOTORCYCLE) },
                            color = if (uiState.travelProfile == RouteProfile.MOTORCYCLE) palette.primaryAccent else Color.Transparent
                        ) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = "Motorcycle",
                                tint = if (uiState.travelProfile == RouteProfile.MOTORCYCLE) palette.background else palette.dialText,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onSetTravelProfile(RouteProfile.CAR) },
                            color = if (uiState.travelProfile == RouteProfile.CAR) palette.primaryAccent else Color.Transparent
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Car",
                                tint = if (uiState.travelProfile == RouteProfile.CAR) palette.background else palette.dialText,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                    }

                    // Avoidance Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AvoidanceChip(
                            label = "TOLLS",
                            isSelected = uiState.avoidances.contains(RouteAvoidance.TOLLS),
                            palette = palette,
                            onClick = { onToggleAvoidance(RouteAvoidance.TOLLS) }
                        )
                        AvoidanceChip(
                            label = "HWY",
                            isSelected = uiState.avoidances.contains(RouteAvoidance.HIGHWAYS),
                            palette = palette,
                            onClick = { onToggleAvoidance(RouteAvoidance.HIGHWAYS) }
                        )
                    }
                }

                // GLOVE-FRIENDLY START NAVIGATION BUTTON (>= 56dp height)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartNavigation()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryAccent,
                        contentColor = palette.background
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Start Navigation",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "START NAVIGATION",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AvoidanceChip(
    label: String,
    isSelected: Boolean,
    palette: CockpitThemePalette,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(
                1.dp,
                if (isSelected) palette.primaryAccent else palette.surfaceBorder.copy(alpha = 0.5f),
                RoundedCornerShape(4.dp)
            )
            .clickable { onClick() },
        color = if (isSelected) palette.primaryAccent.copy(alpha = 0.2f) else palette.background
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isSelected) palette.primaryAccent else palette.dialText.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun RoutePreviewMapView(
    currentLocation: LocationPoint?,
    selectedRoute: PlannedRoute?,
    destination: Destination?,
    mapStyleProvider: MapStyleProvider?,
    isDarkMap: Boolean,
    palette: CockpitThemePalette,
    modifier: Modifier = Modifier
) {
    val isNativeSupported = remember { MapSupport.isNativeSupported }
    if (!isNativeSupported || mapStyleProvider == null) {
        Box(
            modifier = modifier
                .background(palette.surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Map preview unavailable on this device architecture",
                color = palette.dialText.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val styleUrl = mapStyleProvider.getStyleUrl(isDarkTheme = isDarkMap)
    val startLat = currentLocation?.latitude ?: destination?.latitude ?: 0.0
    val startLon = currentLocation?.longitude ?: destination?.longitude ?: 0.0

    val initialCamera = remember {
        CameraPosition(
            bearing = 0.0,
            target = Position(startLon, startLat),
            tilt = 0.0,
            zoom = 12.0
        )
    }

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = initialCamera
    ) {
        // Planned Route Line
        val routeGeoJson = remember(selectedRoute) {
            if (selectedRoute != null && selectedRoute.geometry.size >= 2) {
                lineStringJson(selectedRoute.geometry.map { it.latitude to it.longitude })
            } else {
                EMPTY_GEOJSON
            }
        }
        val routeSource = rememberGeoJsonSource(GeoJsonData.JsonString(routeGeoJson))
        LineLayer(
            id = "preview-route-line",
            source = routeSource,
            color = const(palette.primaryAccent),
            width = const(5.dp)
        )

        // Start point puck
        val startGeoJson = remember(currentLocation) {
            if (currentLocation != null) {
                pointJson(currentLocation.latitude, currentLocation.longitude)
            } else {
                EMPTY_GEOJSON
            }
        }
        val startSource = rememberGeoJsonSource(GeoJsonData.JsonString(startGeoJson))
        CircleLayer(
            id = "preview-start-point",
            source = startSource,
            color = const(Color(0xFF00E676)),
            radius = const(7.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp)
        )

        // Destination point puck
        val endGeoJson = remember(destination) {
            if (destination != null) {
                pointJson(destination.latitude, destination.longitude)
            } else {
                EMPTY_GEOJSON
            }
        }
        val endSource = rememberGeoJsonSource(GeoJsonData.JsonString(endGeoJson))
        CircleLayer(
            id = "preview-end-point",
            source = endSource,
            color = const(Color(0xFFFF1744)),
            radius = const(8.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp)
        )
    }

    // Auto-fit camera to route bounding box
    LaunchedEffect(selectedRoute) {
        val geom = selectedRoute?.geometry
        if (!geom.isNullOrEmpty()) {
            try {
                val minLon = geom.minOf { it.longitude }
                val minLat = geom.minOf { it.latitude }
                val maxLon = geom.maxOf { it.longitude }
                val maxLat = geom.maxOf { it.latitude }
                val delta = 0.003
                val adjMinLon = if (minLon == maxLon) minLon - delta else minLon
                val adjMaxLon = if (minLon == maxLon) maxLon + delta else maxLon
                val adjMinLat = if (minLat == maxLat) minLat - delta else minLat
                val adjMaxLat = if (minLat == maxLat) maxLat + delta else maxLat

                mapState.animateCameraToBounds(
                    boundingBox = BoundingBox(
                        west = adjMinLon,
                        south = adjMinLat,
                        east = adjMaxLon,
                        north = adjMaxLat
                    ),
                    padding = PaddingValues(56.dp),
                    animation = CameraAnimation.Ease(duration = 600.milliseconds)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    val uiOptions = remember {
        MapUiOptions {
            renderMode = AndroidRenderMode.Texture
        }
    }

    MaplibreMap(
        state = mapState,
        uiOptions = uiOptions,
        modifier = modifier
    )
}

@Composable
private fun MapPinPickerView(
    currentLocation: LocationPoint?,
    mapStyleProvider: MapStyleProvider?,
    isDarkMap: Boolean,
    palette: CockpitThemePalette,
    isReverseGeocoding: Boolean,
    onConfirmLocation: (Double, Double) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNativeSupported = remember { MapSupport.isNativeSupported }
    if (!isNativeSupported || mapStyleProvider == null) {
        Box(
            modifier = modifier
                .background(palette.surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Interactive map pin selection requires native OpenGL",
                color = palette.dialText.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val styleUrl = mapStyleProvider.getStyleUrl(isDarkTheme = isDarkMap)
    val startLat = currentLocation?.latitude ?: 20.5937 // Fallback center
    val startLon = currentLocation?.longitude ?: 78.9629

    val initialCamera = remember {
        CameraPosition(
            bearing = 0.0,
            target = Position(startLon, startLat),
            tilt = 0.0,
            zoom = 15.0
        )
    }

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = initialCamera
    )

    val coroutineScope = rememberCoroutineScope()
    val uiOptions = remember {
        MapUiOptions {
            renderMode = AndroidRenderMode.Texture
        }
    }

    Box(modifier = modifier) {
        MaplibreMap(
            state = mapState,
            uiOptions = uiOptions,
            modifier = Modifier.fillMaxSize()
        )

        // Center Target Crosshair / Pin (Fixed at map viewport center)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 24.dp), // offset slightly so tip of pin lands on exact center
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Target Location",
                tint = palette.primaryAccent,
                modifier = Modifier.size(48.dp)
            )
        }

        // Zoom In / Out floating controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingMapControlButton(
                icon = Icons.Default.Add,
                contentDescription = "Zoom In",
                palette = palette,
                onClick = {
                    coroutineScope.launch {
                        mapState.animateCameraPosition(
                            CameraPosition(
                                bearing = mapState.cameraPosition.bearing,
                                target = mapState.cameraPosition.target,
                                tilt = mapState.cameraPosition.tilt,
                                zoom = (mapState.cameraPosition.zoom + 1.0).coerceAtMost(20.0)
                            ),
                            CameraAnimation.Ease(200.milliseconds)
                        )
                    }
                }
            )
            FloatingMapControlButton(
                icon = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                palette = palette,
                onClick = {
                    coroutineScope.launch {
                        mapState.animateCameraPosition(
                            CameraPosition(
                                bearing = mapState.cameraPosition.bearing,
                                target = mapState.cameraPosition.target,
                                tilt = mapState.cameraPosition.tilt,
                                zoom = (mapState.cameraPosition.zoom - 1.0).coerceAtLeast(1.0)
                            ),
                            CameraAnimation.Ease(200.milliseconds)
                        )
                    }
                }
            )
        }

        // Bottom Action Card
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, palette.surfaceBorder, RoundedCornerShape(12.dp)),
            color = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pan map to align pin with your destination",
                    color = palette.dialText.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val target = mapState.cameraPosition.target
                        onConfirmLocation(target.latitude, target.longitude)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryAccent,
                        contentColor = palette.background
                    ),
                    enabled = !isReverseGeocoding
                ) {
                    if (isReverseGeocoding) {
                        CircularProgressIndicator(
                            color = palette.background,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Finding Address...", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SET AS DESTINATION",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingMapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    palette: CockpitThemePalette,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .border(1.dp, palette.surfaceBorder, CircleShape)
            .clickable { onClick() },
        color = palette.surface.copy(alpha = 0.9f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = palette.primaryAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ApiKeyPromptCard(
    palette: CockpitThemePalette,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFFFF9100), RoundedCornerShape(8.dp)),
        color = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = Color(0xFFFF9100),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "GRAPHHOPPER API KEY REQUIRED",
                    color = Color(0xFFFF9100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "Enter your free GraphHopper API key from graphhopper.com to enable routing and destination search:",
                color = palette.dialText.copy(alpha = 0.8f),
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    placeholder = { Text("Paste API Key here...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primaryAccent,
                        unfocusedBorderColor = palette.surfaceBorder,
                        focusedTextColor = palette.dialText,
                        unfocusedTextColor = palette.dialText
                    )
                )

                Button(
                    onClick = { if (apiKeyText.isNotBlank()) onSaveKey(apiKeyText.trim()) },
                    enabled = apiKeyText.isNotBlank(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent)
                ) {
                    Text("Save", color = palette.background, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ErrorMessageCard(
    message: String,
    palette: CockpitThemePalette,
    onConfigureApiKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
        color = Color(0xFFFF1744).copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF1744),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                color = palette.dialText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            if (message.contains("API key", ignoreCase = true)) {
                TextButton(onClick = onConfigureApiKey) {
                    Text("Enter Key", color = palette.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private const val EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

private fun lineStringJson(points: List<Pair<Double, Double>>): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[${points.joinToString { "[${it.second},${it.first}]" }}]}}"

private fun pointJson(latitude: Double, longitude: Double): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[${longitude},${latitude}]}}"
