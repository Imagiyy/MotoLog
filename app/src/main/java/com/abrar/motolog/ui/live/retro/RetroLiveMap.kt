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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
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
import com.abrar.motolog.domain.map.MapStyleProvider
import com.abrar.motolog.domain.model.PauseState
import com.abrar.motolog.domain.model.RideStats
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
    defaultMapTheme: com.abrar.motolog.domain.model.MapThemePreference = com.abrar.motolog.domain.model.MapThemePreference.DARK,
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
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
            modifier = modifier
        )
        return
    }

    var mapUnavailable by remember { mutableStateOf(false) }
    var followRider by remember { mutableStateOf(true) }
    var isDarkMap by rememberSaveable(defaultMapTheme) {
        mutableStateOf(defaultMapTheme == com.abrar.motolog.domain.model.MapThemePreference.DARK)
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

    val styleUrl = mapStyleProvider.getStyleUrl(isDarkTheme = isDarkMap)
    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = initialCameraPosition
    ) {
        // 1. Live route trail line (unconditional declaration prevents Compose applier node thrashing)
        val lineGeoJson = if (routeCoordinates.size >= 2) {
            lineStringJson(routeCoordinates)
        } else {
            EMPTY_GEOJSON
        }
        val lineSource = rememberGeoJsonSource(GeoJsonData.JsonString(lineGeoJson))
        LineLayer(
            id = "live-route-trail",
            source = lineSource,
            color = const(palette.primaryAccent),
            width = const(4.dp)
        )

        // 2. Rider current location puck (unconditional declaration with empty feature fallback)
        val puckGeoJson = if (latitude != null && longitude != null) {
            pointJson(latitude, longitude)
        } else {
            EMPTY_GEOJSON
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

    // Auto-center camera onto rider ONLY when followRider is true
    LaunchedEffect(latitude, longitude, followRider) {
        if (followRider && latitude != null && longitude != null) {
            try {
                val currentZoom = if (mapState.cameraPosition.zoom > 1.0) mapState.cameraPosition.zoom else 16.0
                mapState.animateCameraPosition(
                    CameraPosition(
                        bearing = mapState.cameraPosition.bearing,
                        target = Position(longitude, latitude),
                        tilt = mapState.cameraPosition.tilt,
                        zoom = currentZoom
                    ),
                    CameraAnimation.Ease(duration = 500.milliseconds)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Non-fatal animation interrupted or viewport measuring
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

            // Top Right Action Buttons: Theme Toggle & Recenter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Map Light / Dark Theme Mode Button
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            palette.surfaceBorder.copy(alpha = 0.8f),
                            CircleShape
                        )
                        .clickable { isDarkMap = !isDarkMap },
                    color = palette.surface.copy(alpha = 0.92f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDarkMap) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMap) "Switch to Light Map" else "Switch to Dark Map",
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Recenter / Follow GPS Button
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
                            if (latitude != null && longitude != null) {
                                coroutineScope.launch {
                                    try {
                                        mapState.animateCameraPosition(
                                            CameraPosition(
                                                bearing = 0.0,
                                                target = Position(longitude, latitude),
                                                tilt = 0.0,
                                                zoom = 16.0
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
        }

        // Floating Retro Mini-Cockpit HUD Card at Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

                // Minimize / Maximize HUD Toggle Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.2.dp, palette.surfaceBorder.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .clickable { isHudMinimized = !isHudMinimized },
                    color = palette.surface.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isHudMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isHudMinimized) "Maximize HUD" else "Minimize HUD",
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isHudMinimized) "EXPAND HUD" else "MINIMIZE HUD",
                            color = palette.dialText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Full Mini Cockpit Cluster
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
                    palette = palette
                )
            }

            // Compact Minimized HUD Pill
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
                    palette = palette
                )
            }
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
                palette = palette
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

            // Row 3: Glove-friendly Pause & Stop Controls
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

                // Hold-to-Stop Button (56dp min height)
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

            // Right: Pause/Resume + Hold to Stop
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

