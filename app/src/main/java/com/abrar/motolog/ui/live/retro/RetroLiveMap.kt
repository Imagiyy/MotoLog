package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.GpsFixed
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
import androidx.compose.runtime.setValue
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
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
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
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    var mapUnavailable by remember { mutableStateOf(false) }
    var followRider by remember { mutableStateOf(true) }

    val styleUrl = mapStyleProvider.getStyleUrl(isDarkTheme = !palette.isLight)
    val mapState = rememberMapState(baseStyle = BaseStyle.Uri(styleUrl)) {
        // 1. Live route trail line
        if (routeCoordinates.size >= 2) {
            val lineGeoJson = lineStringJson(routeCoordinates)
            val lineSource = rememberGeoJsonSource(GeoJsonData.JsonString(lineGeoJson))
            LineLayer(
                id = "live-route-trail",
                source = lineSource,
                color = const(palette.primaryAccent),
                width = const(4.dp)
            )
        }

        // 2. Rider current location puck
        if (latitude != null && longitude != null) {
            val puckGeoJson = pointJson(latitude, longitude)
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
    }

    // Auto-center camera onto rider when following
    LaunchedEffect(latitude, longitude, followRider) {
        if (followRider && latitude != null && longitude != null) {
            mapState.animateCameraPosition(
                CameraPosition(
                    bearing = 0.0,
                    target = Position(longitude, latitude),
                    tilt = 0.0,
                    zoom = 16.0
                ),
                CameraAnimation.Ease(duration = 500.milliseconds)
            )
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
                    // If user manually drags/pans the map, pause auto-follow
                }
                else -> {}
            }
        }
    }

    Box(modifier = modifier.background(RetroBackground)) {
        // Base MapLibre map
        MaplibreMap(
            state = mapState,
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

        // Floating Retro Mini-Cockpit HUD Card at Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Attribution link
            MapAttributionBadge(
                attribution = mapStyleProvider.getAttribution(),
                mapUnavailable = mapUnavailable
            )

            // Mini Cockpit Cluster
            Box(
                modifier = Modifier
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

