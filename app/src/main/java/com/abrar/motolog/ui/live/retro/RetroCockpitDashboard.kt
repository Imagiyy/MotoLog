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
import com.abrar.motolog.domain.model.PauseState
import com.abrar.motolog.domain.model.RideStats
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
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    var showOverallStats by remember { mutableStateOf(false) }

    val totalDistanceKm = stats.totalDistanceMeters / 1000.0
    val displaySpeed = if (isMetric) speedKmh else speedKmh * 0.621371
    val displayDistance = if (isMetric) totalDistanceKm else totalDistanceKm * 0.621371
    val speedUnit = if (isMetric) "km/h" else "mph"
    val distanceUnit = if (isMetric) "km" else "mi"

    val avgSpeedDisplay = if (showOverallStats) {
        if (isMetric) stats.avgOverallSpeedKmh else stats.avgOverallSpeedKmh * 0.621371
    } else {
        if (isMetric) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
    }

    val maxSpeedDisplay = if (isMetric) stats.maxSpeedKmh else stats.maxSpeedKmh * 0.621371
    val timeDisplay = formatDurationMs(if (showOverallStats) stats.elapsedTimeMs else stats.movingTimeMs)

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
            // Landscape Handlebar Mount Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Big Analog Speedometer Dial + Odometer Drum
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val dialSize = (screenMaxHeight * 0.72f).coerceAtMost(screenMaxWidth * 0.45f)
                    RetroSpeedometerDial(
                        currentSpeed = displaySpeed,
                        isMetric = isMetric,
                        isSpeedAlert = isSpeedAlert,
                        palette = palette,
                        modifier = Modifier.size(dialSize)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    RetroOdometerDrum(
                        distanceValue = displayDistance,
                        unitLabel = distanceUnit,
                        palette = palette
                    )
                }

                // Right Column: Instrument Cluster + Controls
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: Bike Plaque + Jewels + Map Switcher
                    DashboardTopBar(
                        bikeName = bikeName,
                        pauseState = pauseState,
                        isGpsLost = isGpsLost,
                        onSwitchToMap = onSwitchToMap,
                        palette = palette
                    )

                    // 2x2 Grid of Gauges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetroInstrumentCard(
                            label = if (showOverallStats) "Elapsed Time" else "Moving Time",
                            value = timeDisplay,
                            subtitle = if (showOverallStats) "Overall (tap to swap)" else "Moving (tap to swap)",
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            onClick = { showOverallStats = !showOverallStats }
                        )

                        RetroInstrumentCard(
                            label = if (showOverallStats) "Overall Avg" else "Moving Avg",
                            value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                            unit = speedUnit,
                            subtitle = if (showOverallStats) "Overall (tap to swap)" else "Moving (tap to swap)",
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            onClick = { showOverallStats = !showOverallStats }
                        )
                    }

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
                            value = accText,
                            subtitle = if (isGpsLost) "Signal Lost" else "Target ≤25m",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Glove-friendly Bottom Control Row (56dp min)
                    CockpitControlsRow(
                        isPaused = pauseState.isPaused,
                        onPauseClick = onPauseClick,
                        onResumeClick = onResumeClick,
                        onStopConfirmed = onStopConfirmed,
                        palette = palette
                    )
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
                // 1. Top Bar: Bike Plaque + Jewels + Map Switcher
                DashboardTopBar(
                    bikeName = bikeName,
                    pauseState = pauseState,
                    isGpsLost = isGpsLost,
                    onSwitchToMap = onSwitchToMap,
                    palette = palette
                )

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

                // 4. 2x2 Secondary Instrument Gauges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RetroInstrumentCard(
                        label = if (showOverallStats) "Elapsed Time" else "Moving Time",
                        value = timeDisplay,
                        subtitle = if (showOverallStats) "Overall (tap to swap)" else "Moving (tap to swap)",
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { showOverallStats = !showOverallStats }
                    )

                    RetroInstrumentCard(
                        label = if (showOverallStats) "Overall Avg" else "Moving Avg",
                        value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                        unit = speedUnit,
                        subtitle = if (showOverallStats) "Overall (tap to swap)" else "Moving (tap to swap)",
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { showOverallStats = !showOverallStats }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        value = accText,
                        subtitle = if (isGpsLost) "Signal Lost" else "Target ≤25m",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Glove-friendly 56dp+ Bottom Controls
                CockpitControlsRow(
                    isPaused = pauseState.isPaused,
                    onPauseClick = onPauseClick,
                    onResumeClick = onResumeClick,
                    onStopConfirmed = onStopConfirmed,
                    palette = palette
                )

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

@Composable
private fun CockpitControlsRow(
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopConfirmed: () -> Unit,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pause / Resume Button (56dp min height)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isPaused) {
                        Brush.verticalGradient(listOf(JewelGreen, Color(0xFF184E25)))
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

        // Hold-to-Stop Button (56dp min height, 2-second hold gesture)
        RetroHoldToStopButton(
            onStopConfirmed = onStopConfirmed,
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
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

