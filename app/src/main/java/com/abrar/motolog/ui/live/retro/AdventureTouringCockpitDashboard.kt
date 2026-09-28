package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Adventure Touring GS Palette Constants
private val GsDarkBasalt = Color(0xFF0C1017)
private val GsPanelBg = Color(0xFF141923)
private val GsCordobaBlue = Color(0xFF1976D2)
private val GsGlacialCyan = Color(0xFF4FC3F7)
private val GsExpeditionGold = Color(0xFFFFB300)
private val GsAlpineWhite = Color(0xFFF0F6FC)
private val GsTextMuted = Color(0xFF8B949E)
private val GsBorder = Color(0xFF232D3F)
private val GsDanger = Color(0xFFFF4D4D)

/**
 * Globe Explorer BMW GS / Africa Twin Panoramic Overland Cockpit Dashboard.
 * Features a split overland instrument cluster with live Altimeter elevation profile,
 * dual expedition tripmeters, compass navigation ring, and high-altitude touring telemetry.
 */
@Composable
fun AdventureTouringCockpitDashboard(
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
    elevationGainMeters: Double = 0.0,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.ADVENTURE_TOURING)
) {
    val totalDistanceKm = stats.totalDistanceMeters / 1000.0
    val displaySpeed = if (isMetric) speedKmh else speedKmh * 0.621371
    val displayDistance = if (isMetric) totalDistanceKm else totalDistanceKm * 0.621371
    val speedUnit = if (isMetric) "KM/H" else "MPH"
    val distanceUnit = if (isMetric) "KM" else "MI"

    val movingTimeDisplay = CockpitUtils.formatDurationMs(stats.movingTimeMs)
    val totalTimeDisplay = CockpitUtils.formatDurationMs(stats.elapsedTimeMs)
    val avgMovingSpeedDisplay = if (isMetric) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
    val maxSpeedDisplay = if (isMetric) stats.maxSpeedKmh else stats.maxSpeedKmh * 0.621371

    val animatedSpeed by animateFloatAsState(
        targetValue = displaySpeed.toFloat(),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "gsSpeed"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(GsDarkBasalt, Color(0xFF090D13), GsDarkBasalt))
            )
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = screenWidth > screenHeight

        if (isLandscape) {
            // Landscape GS Split Panoramic Cockpit
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left: Altimeter & Expedition Profile Module
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    GsAltimeterCard(
                        elevationGain = elevationGainMeters,
                        isMetric = isMetric,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GsMetricCard(
                            title = "EXPEDITION TRIP",
                            value = String.format(Locale.US, "%.1f", displayDistance),
                            unit = distanceUnit,
                            accentColor = GsExpeditionGold,
                            modifier = Modifier.weight(1f)
                        )
                        GsMetricCard(
                            title = "MOVING TIME",
                            value = movingTimeDisplay,
                            unit = "",
                            accentColor = GsGlacialCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Tactical Compass Ribbon
                    GsCompassRibbon(modifier = Modifier.fillMaxWidth())
                }

                // Right: Speed Cluster & Action Controls
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Panoramic High-Contrast Speedometer Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.5.dp, GsCordobaBlue.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GsPanelBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "OVERLAND SPEED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GsGlacialCyan,
                                letterSpacing = 2.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.0f", animatedSpeed),
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSpeedAlert) GsDanger else GsAlpineWhite,
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = (-2).sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = speedUnit,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GsGlacialCyan,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Controls Row
                    if (isIdle) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onStartClick,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GsExpeditionGold),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "START EXPEDITION",
                                    color = Color(0xFF1A1300),
                                    fontSize = 13.sp,
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
                                badgeText = "EXPEDITION",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onSwitchToMap,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GsPanelBg,
                                    contentColor = GsGlacialCyan
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(46.dp)
                                    .border(1.dp, GsBorder, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MAP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = if (pauseState.isPaused) onResumeClick else onPauseClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GsPanelBg,
                                    contentColor = if (pauseState.isPaused) GsExpeditionGold else GsCordobaBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .border(1.dp, (if (pauseState.isPaused) GsExpeditionGold else GsCordobaBlue).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = if (pauseState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (pauseState.isPaused) "RESUME" else "PAUSE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            ThemedHoldToStopButton(
                                onStopConfirmed = onStopConfirmed,
                                label = "END",
                                progressLabel = "OFF",
                                borderColor = GsDanger,
                                gradientColors = listOf(Color(0xFF5A1010), Color(0xFF250505)),
                                progressFillColor = GsDanger.copy(alpha = 0.6f),
                                textColor = GsAlpineWhite,
                                cornerRadius = 8.dp,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait GS Overland Cockpit
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (ThemedIdleTopBar in Idle, or Expedition Banner + Map Button when tracking)
                if (isIdle) {
                    ThemedIdleTopBar(
                        keepScreenOn = keepScreenOn,
                        onToggleKeepScreenOn = onToggleKeepScreenOn,
                        onNavigateToSettings = onNavigateToSettings,
                        palette = palette,
                        badgeText = "EXPEDITION STANDBY"
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isGpsLost) GsDanger else GsCordobaBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GS EXPEDITION // ${bikeName?.uppercase(Locale.US) ?: "ADVENTURE"}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GsGlacialCyan,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGpsLost) GsDanger else GsTextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            onClick = onSwitchToMap,
                            shape = RoundedCornerShape(8.dp),
                            color = GsPanelBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GsCordobaBlue),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Expedition Map",
                                    tint = GsGlacialCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "MAP",
                                    color = GsGlacialCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Main Speedometer Hero Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, GsCordobaBlue.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GsPanelBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CRUISE SPEED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GsGlacialCyan,
                            letterSpacing = 2.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.0f", animatedSpeed),
                                fontSize = 76.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSpeedAlert) GsDanger else GsAlpineWhite,
                                letterSpacing = (-2).sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = speedUnit,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GsGlacialCyan,
                                modifier = Modifier.padding(bottom = 12.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Altimeter & Climb Profile Card
                GsAltimeterCard(
                    elevationGain = elevationGainMeters,
                    isMetric = isMetric,
                    modifier = Modifier.fillMaxWidth()
                )

                // Expedition Telemetry Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GsMetricCard(
                        title = "TOTAL EXPEDITION",
                        value = String.format(Locale.US, "%.1f", displayDistance),
                        unit = distanceUnit,
                        accentColor = GsExpeditionGold,
                        modifier = Modifier.weight(1f)
                    )
                    GsMetricCard(
                        title = "EXPEDITION TIME",
                        value = movingTimeDisplay,
                        unit = "",
                        accentColor = GsGlacialCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GsMetricCard(
                        title = "AVG CRUISE",
                        value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                        unit = speedUnit,
                        accentColor = GsAlpineWhite,
                        modifier = Modifier.weight(1f)
                    )
                    GsMetricCard(
                        title = "PEAK RECORD",
                        value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                        unit = speedUnit,
                        accentColor = GsAlpineWhite,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Action Controls
                if (isIdle) {
                    Button(
                        onClick = onStartClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GsExpeditionGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Text(
                            text = "START EXPEDITION",
                            color = Color(0xFF1A1300),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { if (pauseState.isPaused) onResumeClick() else onPauseClick() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (pauseState.isPaused) GsExpeditionGold.copy(alpha = 0.2f) else GsPanelBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (pauseState.isPaused) GsExpeditionGold else GsCordobaBlue
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (pauseState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = if (pauseState.isPaused) GsExpeditionGold else GsGlacialCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pauseState.isPaused) "RESUME" else "BASE CAMP",
                                    color = if (pauseState.isPaused) GsExpeditionGold else GsAlpineWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        ThemedHoldToStopButton(
                            onStopConfirmed = onStopConfirmed,
                            label = "END EXPEDITION",
                            progressLabel = "CONCLUDING",
                            borderColor = GsDanger,
                            gradientColors = listOf(Color(0xFF5A1010), Color(0xFF250505)),
                            progressFillColor = GsDanger.copy(alpha = 0.6f),
                            textColor = GsAlpineWhite,
                            cornerRadius = 8.dp,
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Altimeter card with live elevation gain and terrain profile graphic.
 */
@Composable
private fun GsAltimeterCard(
    elevationGain: Double,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val displayGain = if (isMetric) elevationGain else elevationGain * 3.28084
    val gainUnit = if (isMetric) "m" else "ft"

    Card(
        modifier = modifier
            .border(1.dp, GsBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = GsPanelBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Landscape,
                        contentDescription = "Altimeter",
                        tint = GsGlacialCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ALTIMETER // CLIMB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GsTextMuted,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "+%.0f", displayGain),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = GsAlpineWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = gainUnit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GsGlacialCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            // Mountain Range Terrain Profile Graphic
            Canvas(modifier = Modifier.size(width = 80.dp, height = 30.dp)) {
                val path = Path().apply {
                    moveTo(0f, size.height * 0.85f)
                    lineTo(size.width * 0.25f, size.height * 0.45f)
                    lineTo(size.width * 0.45f, size.height * 0.65f)
                    lineTo(size.width * 0.70f, size.height * 0.20f)
                    lineTo(size.width, size.height * 0.75f)
                }
                drawPath(
                    path = path,
                    color = GsCordobaBlue,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Compact Tactical Compass Ribbon.
 */
@Composable
private fun GsCompassRibbon(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .border(1.dp, GsBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = GsPanelBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Explore, contentDescription = "Compass", tint = GsExpeditionGold, modifier = Modifier.size(16.dp))
            Text("N", fontSize = 11.sp, fontWeight = FontWeight.Black, color = GsDanger, fontFamily = FontFamily.Monospace)
            Text("030", fontSize = 10.sp, color = GsTextMuted, fontFamily = FontFamily.Monospace)
            Text("060", fontSize = 10.sp, color = GsTextMuted, fontFamily = FontFamily.Monospace)
            Text("E", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GsAlpineWhite, fontFamily = FontFamily.Monospace)
            Text("120", fontSize = 10.sp, color = GsTextMuted, fontFamily = FontFamily.Monospace)
            Text("150", fontSize = 10.sp, color = GsTextMuted, fontFamily = FontFamily.Monospace)
            Text("S", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GsAlpineWhite, fontFamily = FontFamily.Monospace)
        }
    }
}

/**
 * Clean Overland Metric Card.
 */
@Composable
private fun GsMetricCard(
    title: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, GsBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = GsPanelBg)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = GsTextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GsTextMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
