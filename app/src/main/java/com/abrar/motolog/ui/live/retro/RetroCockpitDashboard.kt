package com.abrar.motolog.ui.live.retro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.JewelGreen
import com.abrar.motolog.ui.theme.RetroAmber
import com.abrar.motolog.ui.theme.RetroBackground
import com.abrar.motolog.ui.theme.RetroBrass
import com.abrar.motolog.ui.theme.RetroChrome
import com.abrar.motolog.ui.theme.RetroIvory
import com.abrar.motolog.ui.theme.RetroSurface
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import java.util.Locale

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Responsive Retro Biker Cockpit Dashboard.
 * Automatically adapts layout to screen size and orientation (Portrait and Landscape handlebar mounts).
 * Displays all ride metrics in classic motorcycle instrumentation themed to the selected visual style.
 */
@Composable
fun RetroCockpitDashboard(
    stats: RideStats,
    speedKmh: Double,
    accuracyMeters: Float,
    isMetric: Boolean,
    pauseState: PauseState,
    isGpsLost: Boolean,
    isSpeedAlert: Boolean,
    bikeName: String?,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopConfirmed: () -> Unit,
    onSwitchToMap: () -> Unit,
    modifier: Modifier = Modifier,
    isIdle: Boolean = false,
    onStartClick: () -> Unit = {},
    keepScreenOn: Boolean = false,
    onToggleKeepScreenOn: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    val totalDistanceKm = stats.totalDistanceMeters / 1000.0
    val displaySpeed = if (isMetric) speedKmh else speedKmh * 0.621371
    val displayDistance = if (isMetric) totalDistanceKm else totalDistanceKm * 0.621371
    val speedUnit = if (isMetric) "km/h" else "mph"
    val distanceUnit = if (isMetric) "km" else "mi"

    val movingTimeDisplay = formatDurationMs(stats.movingTimeMs)
    val totalTimeDisplay = formatDurationMs(stats.elapsedTimeMs)
    val avgMovingSpeedDisplay = if (isMetric) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
    val avgOverallSpeedDisplay = if (isMetric) stats.avgOverallSpeedKmh else stats.avgOverallSpeedKmh * 0.621371
    val maxSpeedDisplay = if (isMetric) stats.maxSpeedKmh else stats.maxSpeedKmh * 0.621371

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (palette.isLight) {
                        listOf(palette.background, Color(0xFFDDE2E5), palette.background)
                    } else {
                        listOf(palette.background, Color(0xFF0A0908), palette.background)
                    }
                )
            )
    ) {
        val screenMaxWidth = maxWidth
        val screenMaxHeight = maxHeight
        val isLandscape = screenMaxWidth > screenMaxHeight

        if (isLandscape) {
            // Fullscreen Landscape Speedometer with Slide-Down Numerical Telemetry Drawer
            var showLandscapeTelemetrySheet by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 20) {
                                showLandscapeTelemetrySheet = true
                            } else if (dragAmount < -20) {
                                showLandscapeTelemetrySheet = false
                            }
                        }
                    }
                    .padding(8.dp)
            ) {
                // Centered Hero Speedometer Dial & Mechanical Odometer
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val dialSize = (screenMaxHeight * 0.78f).coerceAtMost(screenMaxWidth * 0.55f)
                    RetroSpeedometerDial(
                        currentSpeed = displaySpeed,
                        isMetric = isMetric,
                        isSpeedAlert = isSpeedAlert,
                        palette = palette,
                        modifier = Modifier.size(dialSize)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    RetroOdometerDrum(
                        distanceValue = displayDistance,
                        unitLabel = distanceUnit,
                        palette = palette
                    )
                }

                // Subtle Top Pull Tab Indicator
                LandscapeTelemetryPullTab(
                    visible = !showLandscapeTelemetrySheet,
                    onClick = { showLandscapeTelemetrySheet = true },
                    accentColor = palette.primaryAccent,
                    backgroundColor = palette.surface,
                    borderColor = RetroBrass,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Slide-down drawer with numerical values and controls
                LandscapeTelemetryDrawer(
                    visible = showLandscapeTelemetrySheet,
                    onDismiss = { showLandscapeTelemetrySheet = false },
                    backgroundColor = palette.surface.copy(alpha = 0.96f),
                    borderColor = RetroBrass,
                    accentColor = palette.primaryAccent,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    // Numerical Telemetry Row - Displays all specifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RetroInstrumentCard(
                            label = "Moving Time",
                            value = movingTimeDisplay,
                            subtitle = "Moving Clock",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )

                        RetroInstrumentCard(
                            label = "Total Time",
                            value = totalTimeDisplay,
                            subtitle = "Total on Ride",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )

                        RetroInstrumentCard(
                            label = "Moving Avg",
                            value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                            unit = speedUnit,
                            subtitle = "Active Pace",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )

                        RetroInstrumentCard(
                            label = "Overall Avg",
                            value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
                            unit = speedUnit,
                            subtitle = "Trip Pace",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )

                        RetroInstrumentCard(
                            label = "Max Speed",
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            subtitle = "Session Peak",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )

                        val accText = if (isMetric) "±${accuracyMeters.toInt()}m" else "±${(accuracyMeters * 3.28084).toInt()}ft"
                        RetroInstrumentCard(
                            label = "GPS Fix",
                            value = if (isGpsLost) "--" else accText,
                            subtitle = if (isGpsLost) "Signal Lost" else "Target ≤25m",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Controls Row inside Drawer
                    if (isIdle) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    showLandscapeTelemetrySheet = false
                                    onStartClick()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RetroBrass),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "START RIDE",
                                    color = palette.background,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }

                            ThemedIdleTopBar(
                                keepScreenOn = keepScreenOn,
                                onToggleKeepScreenOn = onToggleKeepScreenOn,
                                onNavigateToSettings = onNavigateToSettings,
                                palette = palette,
                                badgeText = "RETRO CLASSIC",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onSwitchToMap,
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RetroBrass),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(52.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = "Map",
                                        tint = palette.primaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "MAP",
                                        color = palette.dialText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            ThemedCockpitControlsRow(
                                isPaused = pauseState.isPaused,
                                onPauseClick = onPauseClick,
                                onResumeClick = onResumeClick,
                                onStopConfirmed = onStopConfirmed,
                                palette = palette
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Bar: Bike Plaque + Jewels + Map Switcher, or ThemedIdleTopBar
                if (isIdle) {
                    ThemedIdleTopBar(
                        keepScreenOn = keepScreenOn,
                        onToggleKeepScreenOn = onToggleKeepScreenOn,
                        onNavigateToSettings = onNavigateToSettings,
                        palette = palette,
                        badgeText = "RETRO CLASSIC // STANDBY"
                    )
                } else {
                    DashboardTopBar(
                        bikeName = bikeName,
                        pauseState = pauseState,
                        isGpsLost = isGpsLost,
                        onSwitchToMap = onSwitchToMap,
                        palette = palette
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Hero Center Speedometer Dial
                val dialSize = (screenMaxHeight * 0.38f).coerceAtMost(screenMaxWidth * 0.88f)
                RetroSpeedometerDial(
                    currentSpeed = displaySpeed,
                    isMetric = isMetric,
                    isSpeedAlert = isSpeedAlert,
                    palette = palette,
                    modifier = Modifier.size(dialSize)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Mechanical Odometer Drum
                RetroOdometerDrum(
                    distanceValue = displayDistance,
                    unitLabel = distanceUnit,
                    palette = palette
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Telemetry Cards displaying ALL specifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RetroInstrumentCard(
                        label = "Moving Time",
                        value = movingTimeDisplay,
                        subtitle = "Moving Clock",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )

                    RetroInstrumentCard(
                        label = "Total Time",
                        value = totalTimeDisplay,
                        subtitle = "Total on Ride",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RetroInstrumentCard(
                        label = "Moving Avg",
                        value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                        unit = speedUnit,
                        subtitle = "Active Pace",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )

                    RetroInstrumentCard(
                        label = "Overall Avg",
                        value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
                        unit = speedUnit,
                        subtitle = "Trip Pace",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RetroInstrumentCard(
                        label = "Max Speed",
                        value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                        unit = speedUnit,
                        subtitle = "Session Peak",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )

                    val accText = if (isMetric) "±${accuracyMeters.toInt()}m" else "±${(accuracyMeters * 3.28084).toInt()}ft"
                    RetroInstrumentCard(
                        label = "GPS Fix",
                        value = if (isGpsLost) "--" else accText,
                        subtitle = if (isGpsLost) "Signal Lost" else "Target ≤25m",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Bottom Controls: Start button if idle, or Pause/Resume/Stop if riding
                if (isIdle) {
                    Button(
                        onClick = onStartClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RetroBrass),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Text(
                            text = "START RIDE",
                            color = palette.background,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                    }
                } else {
                    ThemedCockpitControlsRow(
                        isPaused = pauseState.isPaused,
                        onPauseClick = onPauseClick,
                        onResumeClick = onResumeClick,
                        onStopConfirmed = onStopConfirmed,
                        palette = palette
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun DashboardTopBar(
    bikeName: String?,
    pauseState: PauseState,
    isGpsLost: Boolean,
    onSwitchToMap: () -> Unit,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vintage Engraved Bike Plaque
        VintageBikePlaque(bikeName = bikeName ?: "MOTO LOG", palette = palette)

        // Jewel Indicator Lights
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RetroJewelLamp(
                label = "REC",
                isActive = !pauseState.isPaused && !isGpsLost,
                color = JewelColor.GREEN,
                size = 24.dp
            )
            RetroJewelLamp(
                label = "PAUSE",
                isActive = pauseState.isPaused,
                color = JewelColor.AMBER,
                size = 24.dp,
                shouldBlink = true
            )
            RetroJewelLamp(
                label = "GPS",
                isActive = isGpsLost,
                color = JewelColor.RED,
                size = 24.dp,
                shouldBlink = true
            )
        }

        // Live Map Switcher Button
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                .clickable { onSwitchToMap() },
            color = palette.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Open Map",
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "MAP",
                    color = palette.dialText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun VintageBikePlaque(
    bikeName: String,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.linearGradient(
                    colors = if (palette.isLight) {
                        listOf(palette.surface, Color(0xFFECEFF1), palette.surface)
                    } else {
                        listOf(palette.surface, Color(0xFF1E1812), palette.surface)
                    },
                    start = Offset.Zero,
                    end = Offset(150f, 150f)
                )
            )
            .border(1.2.dp, palette.surfaceBorder.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = bikeName.uppercase(Locale.US),
            color = palette.secondaryAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
            maxLines = 1
        )
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = ms / 1000
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

