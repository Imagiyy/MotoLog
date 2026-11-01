package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.draw.rotate
import com.abrar.motolog.shared.domain.model.ManeuverType
import com.abrar.motolog.ui.navigation.CameraFollowMode
import com.abrar.motolog.ui.navigation.NavigationUiState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.data.map.MapSupport
import com.abrar.motolog.shared.domain.map.MapStyleProvider
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.JewelAmber
import com.abrar.motolog.ui.theme.JewelGreen
import com.abrar.motolog.ui.theme.JewelRed
import com.abrar.motolog.ui.theme.RetroAmber
import com.abrar.motolog.ui.theme.RetroBackground
import com.abrar.motolog.ui.theme.RetroBrass
import com.abrar.motolog.ui.theme.RetroChrome
import com.abrar.motolog.ui.theme.RetroIvory
import com.abrar.motolog.ui.theme.RetroNeedle
import com.abrar.motolog.ui.theme.RetroSurface
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.AndroidRenderMode
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.map.renderMode
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Duration.Companion.milliseconds

/**
 * Live GPS tracking map with real-time rider location puck,
 * breadcrumb trail, and floating vintage cockpit HUD.
 */
@Composable
fun RetroLiveMap(
    latitude: Double?,
    longitude: Double?,
    routeCoordinates: List<Pair<Double, Double>>,
    stats: RideStats,
    speedKmh: Double,
    isMetric: Boolean,
    pauseState: PauseState,
    isGpsLost: Boolean,
    isSpeedAlert: Boolean,
    mapStyleProvider: MapStyleProvider,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopProgressChange: (Float) -> Unit,
    onSwitchToCockpit: () -> Unit,
    onNavigateToDestination: () -> Unit = {},
    navigationUiState: NavigationUiState = NavigationUiState.Idle,
    bearingDegrees: Double? = null,
    onEndNavigation: () -> Unit = {},
    onToggleCameraMode: () -> Unit = {},
    onRecenterCamera: () -> Unit = {},
    onUserMapInteraction: () -> Unit = {},
    onDismissArrival: () -> Unit = {},
    onManualReroute: () -> Unit = {},
    onDismissRerouteError: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    defaultMapTheme: com.abrar.motolog.shared.domain.model.MapThemePreference = com.abrar.motolog.shared.domain.model.MapThemePreference.LIGHT,
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RACING_TFT),
    isIdle: Boolean = false,
    onStartClick: () -> Unit = {}
) {
    val isNativeSupported = remember { MapSupport.isNativeSupported }
    if (!isNativeSupported) {
        RetroMapUnavailableFallback(
            stats = stats,
            speedKmh = speedKmh,
            isMetric = isMetric,
            pauseState = pauseState,
            isGpsLost = isGpsLost,
            isSpeedAlert = isSpeedAlert,
            mapStyleProvider = mapStyleProvider,
            onPauseClick = onPauseClick,
            onResumeClick = onResumeClick,
            onStopProgressChange = onStopProgressChange,
            onSwitchToCockpit = onSwitchToCockpit,
            palette = palette,
            isIdle = isIdle,
            onStartClick = onStartClick,
            modifier = modifier
        )
        return
    }

    var mapUnavailable by remember { mutableStateOf(false) }
    var followRider by remember { mutableStateOf(true) }
    var isDarkMap by rememberSaveable(defaultMapTheme) {
        mutableStateOf(defaultMapTheme == com.abrar.motolog.shared.domain.model.MapThemePreference.DARK)
    }
    var isHudMinimized by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val initialCameraPosition = remember {
        if (latitude != null && longitude != null) {
            CameraPosition(
                bearing = 0.0,
                target = Position(longitude, latitude),
                tilt = 0.0,
                zoom = 16.0
            )
        } else {
            CameraPosition(
                bearing = 0.0,
                target = Position(0.0, 0.0),
                tilt = 0.0,
                zoom = 2.0
            )
        }
    }

    val navState = navigationUiState as? NavigationUiState.Navigating
    val cameraFollowMode = navState?.cameraFollowMode ?: CameraFollowMode.FOLLOW_HEAD_UP

    val styleUrl = mapStyleProvider.getStyleUrl(isDarkTheme = isDarkMap)
    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = initialCameraPosition
    ) {
        // 0. Planned route trail line (vibrant high-contrast electric blue)
        val plannedRoute = navState?.route
        val plannedRouteCoords = remember(plannedRoute?.id) {
            plannedRoute?.geometry?.map { Pair(it.latitude, it.longitude) } ?: emptyList()
        }
        val plannedRouteGeoJson = remember(plannedRouteCoords) {
            if (plannedRouteCoords.size >= 2) {
                lineStringJson(plannedRouteCoords)
            } else {
                EMPTY_GEOJSON
            }
        }
        val plannedRouteSource = rememberGeoJsonSource(GeoJsonData.JsonString(plannedRouteGeoJson))
        LineLayer(
            id = "planned-route-line",
            source = plannedRouteSource,
            color = const(Color(0xFF00E5FF)),
            width = const(6.dp)
        )

        // 1. Destination Marker (Emerald green beacon)
        val destLat = navState?.destinationLatitude
        val destLon = navState?.destinationLongitude
        val destGeoJson = remember(destLat, destLon) {
            if (destLat != null && destLon != null && (destLat != 0.0 || destLon != 0.0)) {
                pointJson(destLat, destLon)
            } else {
                EMPTY_GEOJSON
            }
        }
        val destSource = rememberGeoJsonSource(GeoJsonData.JsonString(destGeoJson))
        CircleLayer(
            id = "destination-pin-halo",
            source = destSource,
            color = const(JewelGreen.copy(alpha = 0.35f)),
            radius = const(18.dp)
        )
        CircleLayer(
            id = "destination-pin-core",
            source = destSource,
            color = const(JewelGreen),
            radius = const(8.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.5.dp)
        )

        // 2. Live completed breadcrumb trail line
        val lineGeoJson = remember(routeCoordinates) {
            if (routeCoordinates.size >= 2) {
                lineStringJson(routeCoordinates)
            } else {
                EMPTY_GEOJSON
            }
        }
        val lineSource = rememberGeoJsonSource(GeoJsonData.JsonString(lineGeoJson))
        LineLayer(
            id = "live-route-trail",
            source = lineSource,
            color = const(palette.primaryAccent),
            width = const(4.dp)
        )

        // 3. Rider current location puck
        val puckGeoJson = remember(latitude, longitude) {
            if (latitude != null && longitude != null) {
                pointJson(latitude, longitude)
            } else {
                EMPTY_GEOJSON
            }
        }
        val puckSource = rememberGeoJsonSource(GeoJsonData.JsonString(puckGeoJson))

        // Pulsing outer halo
        CircleLayer(
            id = "rider-puck-halo",
            source = puckSource,
            color = const(palette.primaryAccent.copy(alpha = 0.35f)),
            radius = const(14.dp)
        )
        // Core location dot
        CircleLayer(
            id = "rider-puck-core",
            source = puckSource,
            color = const(palette.needle),
            radius = const(6.5.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp)
        )
    }

    // Auto-center / follow camera onto rider
    LaunchedEffect(latitude, longitude, followRider, cameraFollowMode, bearingDegrees) {
        if (followRider && latitude != null && longitude != null && cameraFollowMode != CameraFollowMode.FREE) {
            try {
                val currentZoom = if (mapState.cameraPosition.zoom > 1.0) mapState.cameraPosition.zoom else 16.5
                val targetBearing = if (cameraFollowMode == CameraFollowMode.FOLLOW_HEAD_UP && bearingDegrees != null && speedKmh >= 3.0) {
                    bearingDegrees
                } else {
                    0.0
                }
                val targetTilt = if (cameraFollowMode == CameraFollowMode.FOLLOW_HEAD_UP && speedKmh >= 3.0) 35.0 else 0.0

                mapState.animateCameraPosition(
                    CameraPosition(
                        bearing = targetBearing,
                        target = Position(longitude, latitude),
                        tilt = targetTilt,
                        zoom = currentZoom
                    ),
                    CameraAnimation.Ease(duration = 500.milliseconds)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    LaunchedEffect(mapState) {
        mapState.events.collect { event ->
            when (event) {
                is MapEvent.StyleLoadFailed, is MapEvent.SourceDataFailed -> {
                    mapUnavailable = true
                }
                is MapEvent.StyleLoaded -> {
                    mapUnavailable = false
                }
                is MapEvent.CameraMoveStarted -> {
                    // When user manually drags, zooms, or gestures the map, disable auto-lock
                    if (event.animated != true) {
                        followRider = false
                        onUserMapInteraction()
                    }
                }
                else -> {}
            }
        }
    }

    val uiOptions = remember {
        MapUiOptions {
            renderMode = AndroidRenderMode.Texture
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.size > 1 || event.changes.any { it.positionChanged() }) {
                        followRider = false
                        onUserMapInteraction()
                    }
                } while (event.changes.any { it.pressed })
            }
        }
    ) {
        // Base MapLibre map rendered via TextureView
        MaplibreMap(
            state = mapState,
            uiOptions = uiOptions,
            modifier = Modifier.fillMaxSize()
        )

        // Top Header Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Switch to Full Cockpit button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .clickable { onSwitchToCockpit() },
                    color = palette.surface.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Switch to Cockpit",
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "COCKPIT",
                            color = palette.dialText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Destination / Navigation button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .clickable { onNavigateToDestination() },
                    color = palette.surface.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Navigate to Destination",
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "NAV",
                            color = palette.dialText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Top Right Action: Recenter / Follow GPS Button
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(
                        1.5.dp,
                        if (followRider) palette.primaryAccent else palette.surfaceBorder.copy(alpha = 0.6f),
                        CircleShape
                    )
                        .clickable {
                            followRider = true
                            onRecenterCamera()
                            if (latitude != null && longitude != null) {
                                coroutineScope.launch {
                                    try {
                                        val isHeadUp = cameraFollowMode == CameraFollowMode.FOLLOW_HEAD_UP
                                        val targetBearing = if (isHeadUp && bearingDegrees != null && speedKmh >= 3.0) bearingDegrees else 0.0
                                        val targetTilt = if (isHeadUp && speedKmh >= 3.0) 35.0 else 0.0
                                        mapState.animateCameraPosition(
                                            CameraPosition(
                                                bearing = targetBearing,
                                                target = Position(longitude, latitude),
                                                tilt = targetTilt,
                                                zoom = 16.5
                                            ),
                                            CameraAnimation.Ease(duration = 500.milliseconds)
                                        )
                                    } catch (_: Exception) {}
                                }
                            }
                        },
                    color = palette.surface.copy(alpha = 0.92f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Recenter on Me",
                            tint = if (followRider) palette.primaryAccent else palette.dialText.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

        // Turn-by-Turn Navigation Guidance HUD (Top Center)
        if (navState != null) {
            NavigationTurnGuidanceCard(
                navState = navState,
                isMetric = isMetric,
                palette = palette,
                onManualReroute = onManualReroute,
                onDismissRerouteError = onDismissRerouteError,
                onToggleMute = onToggleMute,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp, start = 12.dp, end = 12.dp)
            )
        }

        // Floating Retro Mini-Cockpit HUD Card at Bottom with vertical slide gesture
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 20) {
                            isHudMinimized = true
                        } else if (dragAmount < -20) {
                            isHudMinimized = false
                        }
                    }
                },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Persistent Navigation Metrics Bar (Remaining distance, time, ETA, End Nav)
            if (navState != null) {
                NavigationBottomBar(
                    navState = navState,
                    isMetric = isMetric,
                    palette = palette,
                    onEndNavigation = onEndNavigation,
                    onToggleCameraMode = onToggleCameraMode,
                    onRecenterCamera = {
                        followRider = true
                        onRecenterCamera()
                    }
                )
            }

            // Attribution link & Minimize/Maximize HUD Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapAttributionBadge(
                    attribution = mapStyleProvider.getAttribution(),
                    mapUnavailable = mapUnavailable
                )

                // Minimize / Maximize HUD Toggle Button (Hide / Show Telemetry Values)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.2.dp, palette.surfaceBorder.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .clickable { isHudMinimized = !isHudMinimized },
                    color = palette.surface.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isHudMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isHudMinimized) "Show Telemetry Values" else "Hide Telemetry Values",
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isHudMinimized) "SHOW VALUES" else "HIDE VALUES",
                            color = palette.dialText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Full Mini Cockpit Cluster with Telemetry Values
            AnimatedVisibility(
                visible = !isHudMinimized,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                RetroMiniCockpitCluster(
                    stats = stats,
                    speedKmh = speedKmh,
                    isMetric = isMetric,
                    pauseState = pauseState,
                    isGpsLost = isGpsLost,
                    isSpeedAlert = isSpeedAlert,
                    onPauseClick = onPauseClick,
                    onResumeClick = onResumeClick,
                    onStopProgressChange = onStopProgressChange,
                    palette = palette,
                    isIdle = isIdle,
                    onStartClick = onStartClick
                )
            }

            // Compact Minimized HUD Pill (Telemetry Values Hidden)
            AnimatedVisibility(
                visible = isHudMinimized,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                RetroCompactMinimizedHudPill(
                    speedKmh = speedKmh,
                    isMetric = isMetric,
                    pauseState = pauseState,
                    isGpsLost = isGpsLost,
                    isSpeedAlert = isSpeedAlert,
                    onPauseClick = onPauseClick,
                    onResumeClick = onResumeClick,
                    onStopProgressChange = onStopProgressChange,
                    palette = palette,
                    isIdle = isIdle,
                    onStartClick = onStartClick
                )
            }
        }

        // Arrival Overlay Confirmation
        if (navState != null && navState.isArrived) {
            NavigationArrivalOverlay(
                destinationName = navState.destinationName,
                palette = palette,
                onFinishNavigation = onEndNavigation,
                onDismiss = onDismissArrival,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        }
    }
}

@Composable
private fun RetroMapUnavailableFallback(
    stats: RideStats,
    speedKmh: Double,
    isMetric: Boolean,
    pauseState: PauseState,
    isGpsLost: Boolean,
    isSpeedAlert: Boolean,
    mapStyleProvider: MapStyleProvider,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopProgressChange: (Float) -> Unit,
    onSwitchToCockpit: () -> Unit,
    palette: CockpitThemePalette,
    isIdle: Boolean = false,
    onStartClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(RetroBackground)) {
        // Fallback message in center
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                color = palette.surface.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "MAP UNAVAILABLE",
                        color = palette.primaryAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Map rendering is not supported on this device architecture. All GPS ride telemetry, speedometer readings, and route points continue to be recorded accurately.",
                        color = palette.dialText.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Top Header Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .clickable { onSwitchToCockpit() },
                color = palette.surface.copy(alpha = 0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Switch to Cockpit",
                        tint = palette.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "COCKPIT",
                        color = palette.dialText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Floating Retro Mini-Cockpit HUD Card at Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapAttributionBadge(
                attribution = mapStyleProvider.getAttribution(),
                mapUnavailable = true
            )
            RetroMiniCockpitCluster(
                stats = stats,
                speedKmh = speedKmh,
                isMetric = isMetric,
                pauseState = pauseState,
                isGpsLost = isGpsLost,
                isSpeedAlert = isSpeedAlert,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onStopProgressChange = onStopProgressChange,
                palette = palette,
                isIdle = isIdle,
                onStartClick = onStartClick
            )
        }
    }
}

@Composable
private fun RetroMiniCockpitCluster(
    stats: RideStats,
    speedKmh: Double,
    isMetric: Boolean,
    pauseState: PauseState,
    isGpsLost: Boolean,
    isSpeedAlert: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopProgressChange: (Float) -> Unit,
    palette: CockpitThemePalette,
    isIdle: Boolean = false,
    onStartClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (palette.isLight) {
                        listOf(
                            palette.surface.copy(alpha = 0.97f),
                            Color(0xFFECEFF1).copy(alpha = 0.98f)
                        )
                    } else {
                        listOf(
                            palette.surface.copy(alpha = 0.96f),
                            Color(0xFF12100E).copy(alpha = 0.98f)
                        )
                    }
                )
            )
            .border(2.dp, palette.surfaceBorder.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Status Lights & Live Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jewel status lights
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetroJewelLamp(
                        label = "REC",
                        isActive = !pauseState.isPaused && !isGpsLost,
                        color = JewelColor.GREEN,
                        size = 22.dp
                    )
                    RetroJewelLamp(
                        label = "PAUSE",
                        isActive = pauseState.isPaused,
                        color = JewelColor.AMBER,
                        size = 22.dp,
                        shouldBlink = true
                    )
                    RetroJewelLamp(
                        label = "GPS",
                        isActive = isGpsLost,
                        color = JewelColor.RED,
                        size = 22.dp,
                        shouldBlink = true
                    )
                }

                // Speed display
                val displaySpeed = if (isMetric) speedKmh else speedKmh * 0.621371
                val unitLabel = if (isMetric) "KM/H" else "MPH"
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displaySpeed.toInt().coerceAtLeast(0).toString(),
                        color = if (isSpeedAlert) JewelRed else palette.primaryAccent,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unitLabel,
                        color = palette.secondaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }

            // Row 2: Distance, Moving Time, Avg Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalDistKm = stats.totalDistanceMeters / 1000.0
                val distanceDisplay = if (isMetric) totalDistKm else totalDistKm * 0.621371
                val distanceUnit = if (isMetric) "km" else "mi"
                val avgSpeedDisplay = if (isMetric) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
                val speedUnit = if (isMetric) "km/h" else "mph"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (palette.isLight) Color(0xFFECEFF1) else Color(0xFF151210))
                        .border(1.dp, palette.surfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "TRIP",
                            color = palette.secondaryAccent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${String.format(java.util.Locale.US, "%.1f", distanceDisplay)} $distanceUnit",
                            color = palette.dialText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (palette.isLight) Color(0xFFECEFF1) else Color(0xFF151210))
                        .border(1.dp, palette.surfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "TIME",
                            color = palette.secondaryAccent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            formatDurationMs(stats.movingTimeMs),
                            color = palette.dialText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (palette.isLight) Color(0xFFECEFF1) else Color(0xFF151210))
                        .border(1.dp, palette.surfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "AVG",
                            color = palette.secondaryAccent,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${avgSpeedDisplay.toInt()} $speedUnit",
                            color = palette.dialText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Row 3: Glove-friendly Pause & Stop Controls (or START in Idle)
            if (isIdle) {
                Button(
                    onClick = onStartClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "START RIDE",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pause / Resume Button (56dp min height)
                    val isPaused = pauseState.isPaused
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isPaused) {
                                    Brush.verticalGradient(listOf(JewelGreen, Color(0xFF1A5228)))
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            palette.secondaryAccent.copy(alpha = 0.7f),
                                            palette.surface
                                        )
                                    )
                                }
                            )
                            .border(1.5.dp, palette.surfaceBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                if (isPaused) onResumeClick() else onPauseClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPaused) "RESUME" else "PAUSE",
                            color = palette.dialText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    // Stop Button (56dp min height)
                    RetroHoldToStopButton(
                        onStopConfirmed = { onStopProgressChange(1f) },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MapAttributionBadge(
    attribution: String,
    mapUnavailable: Boolean,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { uriHandler.openUri("https://www.openstreetmap.org/copyright") },
        color = RetroSurface.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (mapUnavailable) "Map offline • $attribution" else attribution,
                fontSize = 9.sp,
                color = RetroBrass,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

private const val EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

private fun lineStringJson(points: List<Pair<Double, Double>>): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[${points.joinToString { "[${it.second},${it.first}]" }}]}}"

private fun pointJson(latitude: Double, longitude: Double): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[${longitude},${latitude}]}}"

private fun formatDurationMs(ms: Long): String {
    val totalSec = ms / 1000
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    }
}

@Composable
private fun RetroCompactMinimizedHudPill(
    speedKmh: Double,
    isMetric: Boolean,
    pauseState: PauseState,
    isGpsLost: Boolean,
    isSpeedAlert: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopProgressChange: (Float) -> Unit,
    palette: CockpitThemePalette,
    isIdle: Boolean = false,
    onStartClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displaySpeed = if (isMetric) speedKmh else speedKmh * 0.621371
    val speedUnit = if (isMetric) "KM/H" else "MPH"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.85f), RoundedCornerShape(12.dp)),
        color = palette.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Status Jewel Lamp & Speed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RetroJewelLamp(
                    label = if (isGpsLost) "GPS" else if (pauseState.isPaused) "PAUSE" else "REC",
                    isActive = true,
                    color = if (isGpsLost) JewelColor.RED else if (pauseState.isPaused) JewelColor.AMBER else JewelColor.GREEN,
                    size = 18.dp,
                    shouldBlink = pauseState.isPaused || isGpsLost
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.0f", displaySpeed),
                        color = if (isSpeedAlert) palette.needle else palette.dialText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = speedUnit,
                        color = palette.secondaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            // Right: Pause/Resume + Stop (or START in Idle)
            if (isIdle) {
                Button(
                    onClick = onStartClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "START RIDE",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            if (pauseState.isPaused) onResumeClick() else onPauseClick()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (pauseState.isPaused) JewelAmber.copy(alpha = 0.2f) else palette.surfaceBorder.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (pauseState.isPaused) JewelAmber else palette.primaryAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (pauseState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = if (pauseState.isPaused) JewelAmber else palette.primaryAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (pauseState.isPaused) "RESUME" else "PAUSE",
                                color = if (pauseState.isPaused) JewelAmber else palette.dialText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    RetroHoldToStopButton(
                        onStopConfirmed = { onStopProgressChange(1f) },
                        modifier = Modifier
                            .width(110.dp)
                            .height(36.dp)
                    )
                }
            }
        }
    }
}

/**
 * Top turn-by-turn guidance card for glanceable rider navigation.
 */
@Composable
fun NavigationTurnGuidanceCard(
    navState: NavigationUiState.Navigating,
    isMetric: Boolean,
    palette: CockpitThemePalette,
    onManualReroute: () -> Unit = {},
    onDismissRerouteError: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
        color = palette.surface.copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Off-route warning / rerouting / signal loss / error status badges
            if (navState.rerouteErrorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JewelRed.copy(alpha = 0.2f))
                        .border(1.dp, JewelRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = navState.rerouteErrorMessage,
                        color = JewelRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, JewelRed.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                .clickable { onManualReroute() },
                            color = JewelRed.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "RETRY",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onDismissRerouteError() },
                            color = palette.surfaceBorder.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "✕",
                                color = palette.dialText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else if (navState.isRerouting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.18f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF00E5FF)
                    )
                    Text(
                        text = "REROUTING... CALCULATING FASTEST PATH",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else if (navState.isOffRouteWarning) {
                val devDist = CockpitUtils.formatDistance(navState.progress.crossTrackDistanceMeters, isMetric)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JewelAmber.copy(alpha = 0.2f))
                        .border(1.dp, JewelAmber.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "⚠️", fontSize = 14.sp)
                        Text(
                            text = "OFF ROUTE ($devDist)",
                            color = JewelAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, JewelAmber.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .clickable { onManualReroute() },
                        color = JewelAmber.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "REROUTE",
                            color = JewelAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else if (navState.isGpsLost) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JewelRed.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "📡", fontSize = 14.sp)
                    Text(
                        text = "GPS SIGNAL LOST — HOLDING POSITION",
                        color = JewelRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Primary Maneuver Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Maneuver Icon in high-contrast neon badge
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.8f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ManeuverIcon(
                            maneuverType = navState.currentInstruction?.maneuverType ?: ManeuverType.CONTINUE,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Maneuver distance + street instruction
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val formattedDist = CockpitUtils.formatDistance(navState.distanceToNextManeuverMeters, isMetric)
                    Text(
                        text = formattedDist,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = navState.currentInstruction?.streetName
                            ?: navState.currentInstruction?.instructionText
                            ?: "Continue along route",
                        color = palette.dialText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }

                // Mute / Unmute Voice Guidance Button (Glove-friendly >= 48dp)
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.5.dp,
                            if (navState.isMuted) JewelAmber.copy(alpha = 0.8f) else Color(0xFF00E5FF).copy(alpha = 0.7f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onToggleMute() },
                    color = if (navState.isMuted) JewelAmber.copy(alpha = 0.2f) else Color(0xFF00E5FF).copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (navState.isMuted) "🔇" else "🔊",
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // Next instruction secondary hint
            navState.nextInstruction?.let { next ->
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "THEN",
                        color = palette.secondaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = next.instructionText,
                        color = palette.dialText.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Bottom navigation metrics card displaying remaining distance, time, ETA,
 * camera perspective toggle, recenter button, and end navigation action.
 */
@Composable
fun NavigationBottomBar(
    navState: NavigationUiState.Navigating,
    isMetric: Boolean,
    palette: CockpitThemePalette,
    onEndNavigation: () -> Unit,
    onToggleCameraMode: () -> Unit,
    onRecenterCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
        color = palette.surface.copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Metrics row: Remaining distance, Duration, ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Remaining Distance
                Column {
                    Text(
                        text = "REMAINING",
                        color = palette.secondaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = CockpitUtils.formatDistance(navState.remainingDistanceMeters, isMetric),
                        color = palette.dialText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Time & ETA
                Column(horizontalAlignment = Alignment.End) {
                    val etaFormatted = CockpitUtils.formatEta(navState.etaTimestampEpochMs)
                    val minutesRemaining = (navState.remainingDurationSeconds / 60).coerceAtLeast(0)
                    Text(
                        text = "ETA • $etaFormatted",
                        color = palette.secondaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (minutesRemaining >= 60) {
                            "${minutesRemaining / 60}h ${minutesRemaining % 60}m"
                        } else {
                            "$minutesRemaining min"
                        },
                        color = Color(0xFF00E5FF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Destination display label
            Text(
                text = "📍 ${navState.destinationName}",
                color = palette.dialText.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            // Controls row: Camera Mode toggle, Recenter, and END NAV button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera Mode button (Head-Up 3D vs North-Up 2D)
                Surface(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, palette.surfaceBorder, RoundedCornerShape(8.dp))
                        .clickable { onToggleCameraMode() },
                    color = palette.background.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Toggle Camera Mode",
                            tint = if (navState.cameraFollowMode == CameraFollowMode.FOLLOW_HEAD_UP) Color(0xFF00E5FF) else palette.dialText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (navState.cameraFollowMode == CameraFollowMode.FOLLOW_HEAD_UP) "3D HEAD-UP" else "2D NORTH",
                            color = palette.dialText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (navState.cameraFollowMode == CameraFollowMode.FREE) {
                    // Recenter button
                    Surface(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, palette.primaryAccent, RoundedCornerShape(8.dp))
                            .clickable { onRecenterCamera() },
                        color = palette.primaryAccent.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Recenter",
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "RECENTER",
                                color = palette.primaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // END NAVIGATION button (Glove-friendly, distinct red tone)
                Button(
                    onClick = onEndNavigation,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JewelRed),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "END NAV",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Arrival confirmation dialog displayed when rider reaches the destination.
 */
@Composable
fun NavigationArrivalOverlay(
    destinationName: String,
    palette: CockpitThemePalette,
    onFinishNavigation: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, JewelGreen, RoundedCornerShape(16.dp)),
        color = palette.surface.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = JewelGreen.copy(alpha = 0.2f),
                border = BorderStroke(2.dp, JewelGreen)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Arrived",
                        tint = JewelGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                text = "ARRIVED AT DESTINATION",
                color = JewelGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = destinationName,
                color = palette.dialText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onFinishNavigation,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JewelGreen)
                ) {
                    Text(
                        text = "FINISH NAV",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Surface(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, palette.surfaceBorder, RoundedCornerShape(8.dp))
                        .clickable { onDismiss() },
                    color = palette.surface
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KEEP MAP",
                            color = palette.dialText,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Directional maneuver icon renderer.
 */
@Composable
fun ManeuverIcon(
    maneuverType: ManeuverType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val (icon, rotation) = when (maneuverType) {
        ManeuverType.DEPART, ManeuverType.CONTINUE -> Icons.Default.ArrowUpward to 0f
        ManeuverType.TURN_SLIGHT_LEFT -> Icons.Default.ArrowUpward to -45f
        ManeuverType.TURN_LEFT -> Icons.AutoMirrored.Filled.ArrowBack to 0f
        ManeuverType.TURN_SHARP_LEFT -> Icons.Default.ArrowUpward to -135f
        ManeuverType.TURN_SLIGHT_RIGHT -> Icons.Default.ArrowUpward to 45f
        ManeuverType.TURN_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward to 0f
        ManeuverType.TURN_SHARP_RIGHT -> Icons.Default.ArrowUpward to 135f
        ManeuverType.U_TURN -> Icons.Default.Refresh to 180f
        ManeuverType.ROUNDABOUT -> Icons.Default.Refresh to 0f
        ManeuverType.MERGE, ManeuverType.RAMP, ManeuverType.FORK -> Icons.Default.ArrowUpward to 0f
        ManeuverType.ARRIVE -> Icons.Default.Place to 0f
        ManeuverType.UNKNOWN -> Icons.Default.Navigation to 0f
    }
    Icon(
        imageVector = icon,
        contentDescription = maneuverType.name,
        tint = tint,
        modifier = modifier.rotate(rotation)
    )
}

