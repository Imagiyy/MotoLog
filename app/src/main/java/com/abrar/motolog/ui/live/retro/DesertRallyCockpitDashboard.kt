package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import java.util.Locale

// Desert Rally Palette Constants
private val RallyBg = Color(0xFF141210)
private val RallySurface = Color(0xFF1C1916)
private val RallyCardBg = Color(0xFF26211C)
private val RallyDakarOrange = Color(0xFFFF6600)
private val RallySandGold = Color(0xFFE5A65E)
private val RallyHighVisYellow = Color(0xFFFFD600)
private val RallyCompassCyan = Color(0xFF00E5FF)
private val RallyWhite = Color(0xFFF7F5F0)
private val RallyMuted = Color(0xFF8C8275)
private val RallyTowerDark = Color(0xFF1B1916)
private val RallyBorder = Color(0xFF38332C)
private val RallyTextMuted = Color(0xFFA0988E)

/**
 * Desert Rally Navigation Tower & Roadbook Cockpit Dashboard.
 * Inspired by Dakar Rally roadbook towers (KTM 450 Rally, Yamaha WR450F Rally).
 * Features a dual-trip master, electronic CAP compass heading ribbon, waypoint counters,
 * high-contrast desert sun glare protection, and heavy-duty rally controls.
 */
@Composable
fun DesertRallyCockpitDashboard(
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
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.DESERT_RALLY)
) {
    val totalDistanceKm = stats.totalDistanceMeters / 1000.0
    val displaySpeed = if (isMetric) speedKmh else speedKmh * 0.621371
    val displayDistance = if (isMetric) totalDistanceKm else totalDistanceKm * 0.621371
    val speedUnit = if (isMetric) "KM/H" else "MPH"
    val distanceUnit = if (isMetric) "KM" else "MI"

    val movingTimeDisplay = CockpitUtils.formatDurationMs(stats.movingTimeMs)
    val totalTimeDisplay = CockpitUtils.formatDurationMs(stats.elapsedTimeMs)
    val avgMovingSpeedDisplay = if (isMetric) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
    val avgOverallSpeedDisplay = if (isMetric) stats.avgOverallSpeedKmh else stats.avgOverallSpeedKmh * 0.621371
    val maxSpeedDisplay = if (isMetric) stats.maxSpeedKmh else stats.maxSpeedKmh * 0.621371

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(RallyBg, Color(0xFF1E1B18), RallyBg)
                )
            )
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = screenWidth > screenHeight

        if (isLandscape) {
            // Immersive Full-Screen Landscape Navigation Tower with Slide-Down Numerical Drawer
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
                // Fullscreen Navigation Tower (No bottom clutter)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RallyRoadbookTripMaster(
                        distance = displayDistance,
                        distanceUnit = distanceUnit,
                        accuracyMeters = accuracyMeters,
                        isGpsLost = isGpsLost,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    RallySpeedDisplay(
                        speed = displaySpeed,
                        unit = speedUnit,
                        isSpeedAlert = isSpeedAlert,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                // Subtle Top Pull Tab indicator
                LandscapeTelemetryPullTab(
                    visible = !showLandscapeTelemetrySheet,
                    onClick = { showLandscapeTelemetrySheet = true },
                    accentColor = RallyDakarOrange,
                    backgroundColor = RallyCardBg,
                    borderColor = RallySandGold,
                    label = "ROADBOOK TELEMETRY",
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Slide-down drawer with numerical values and controls
                LandscapeTelemetryDrawer(
                    visible = showLandscapeTelemetrySheet,
                    onDismiss = { showLandscapeTelemetrySheet = false },
                    backgroundColor = RallyCardBg.copy(alpha = 0.96f),
                    borderColor = RallyDakarOrange,
                    accentColor = RallySandGold,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    // Numerical Telemetry Row - Displays all specifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RallyTelemetryCard(
                            label = "SPECIAL STAGE",
                            value = movingTimeDisplay,
                            subtitle = "MOVING TIME",
                            accentColor = RallySandGold,
                            modifier = Modifier.weight(1f)
                        )

                        RallyTelemetryCard(
                            label = "STAGE ELAPSED",
                            value = totalTimeDisplay,
                            subtitle = "TOTAL ON RIDE",
                            accentColor = RallySandGold,
                            modifier = Modifier.weight(1f)
                        )

                        RallyTelemetryCard(
                            label = "MOVING AVG",
                            value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                            unit = speedUnit,
                            subtitle = "ACTIVE PACE",
                            accentColor = RallyHighVisYellow,
                            modifier = Modifier.weight(1f)
                        )

                        RallyTelemetryCard(
                            label = "TOTAL AVG",
                            value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
                            unit = speedUnit,
                            subtitle = "OVERALL PACE",
                            accentColor = RallyHighVisYellow,
                            modifier = Modifier.weight(1f)
                        )

                        RallyTelemetryCard(
                            label = "TOP VELOCITY",
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            subtitle = "PEAK SPEED",
                            accentColor = RallyDakarOrange,
                            modifier = Modifier.weight(1f)
                        )

                        RallyTelemetryCard(
                            label = "SAT ACCURACY",
                            value = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                            subtitle = if (isGpsLost) "SIGNAL LOST" else "GPS SYNC",
                            accentColor = if (isGpsLost) RallyDakarOrange else RallyCompassCyan,
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
                                colors = ButtonDefaults.buttonColors(containerColor = RallyDakarOrange),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "DEPART STAGE // START",
                                    color = RallyWhite,
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
                                badgeText = "DAKAR RALLY",
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
                                shape = RoundedCornerShape(6.dp),
                                color = RallyCardBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RallyDakarOrange),
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
                                        contentDescription = "Roadbook Map",
                                        tint = RallyDakarOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "MAP",
                                        color = RallyWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    if (pauseState.isPaused) onResumeClick() else onPauseClick()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (pauseState.isPaused) RallySandGold.copy(alpha = 0.2f) else RallyCardBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (pauseState.isPaused) RallyHighVisYellow else RallyDakarOrange
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (pauseState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = if (pauseState.isPaused) RallyHighVisYellow else RallyDakarOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (pauseState.isPaused) "RESUME" else "BIVOUAC",
                                        color = if (pauseState.isPaused) RallyHighVisYellow else RallyWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            ThemedHoldToStopButton(
                                onStopConfirmed = onStopConfirmed,
                                label = "STAGE FINISH",
                                progressLabel = "FINISHING",
                                borderColor = RallyDakarOrange,
                                gradientColors = listOf(Color(0xFF5A2500), Color(0xFF261000)),
                                progressFillColor = RallyDakarOrange.copy(alpha = 0.6f),
                                textColor = RallyWhite,
                                cornerRadius = 6.dp,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Dakar Rally Cockpit Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                if (isIdle) {
                    ThemedIdleTopBar(
                        keepScreenOn = keepScreenOn,
                        onToggleKeepScreenOn = onToggleKeepScreenOn,
                        onNavigateToSettings = onNavigateToSettings,
                        palette = palette,
                        badgeText = "DAKAR // STANDBY"
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RallyTag(text = "DAKAR RALLY // ${bikeName?.uppercase(Locale.US) ?: "KTM 450 RALLY"}")

                        Surface(
                            onClick = onSwitchToMap,
                            shape = RoundedCornerShape(6.dp),
                            color = RallyCardBg,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, RallyDakarOrange),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Roadbook Map",
                                    tint = RallyDakarOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ROADBOOK MAP",
                                    color = RallyWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Upper Navigation Tower: Roadbook Trip Master
                RallyRoadbookTripMaster(
                    distance = displayDistance,
                    distanceUnit = distanceUnit,
                    accuracyMeters = accuracyMeters,
                    isGpsLost = isGpsLost,
                    modifier = Modifier.fillMaxWidth()
                )

                // Lower Navigation Tower: Speedometer
                RallySpeedDisplay(
                    speed = displaySpeed,
                    unit = speedUnit,
                    isSpeedAlert = isSpeedAlert,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )

                // Rally Status Bar
                RallyStatusBar(
                    bikeName = bikeName,
                    pauseState = pauseState,
                    isGpsLost = isGpsLost,
                    waypointCount = stats.acceptedPointCount,
                    modifier = Modifier.fillMaxWidth()
                )

                // Telemetry Cards displaying ALL specifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RallyTelemetryCard(
                        label = "SPECIAL STAGE TIME",
                        value = movingTimeDisplay,
                        subtitle = "MOVING TIME",
                        accentColor = RallySandGold,
                        modifier = Modifier.weight(1f)
                    )

                    RallyTelemetryCard(
                        label = "STAGE ELAPSED",
                        value = totalTimeDisplay,
                        subtitle = "TOTAL ON RIDE",
                        accentColor = RallySandGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RallyTelemetryCard(
                        label = "MOVING AVG PACE",
                        value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                        unit = speedUnit,
                        subtitle = "ACTIVE PACE",
                        accentColor = RallyHighVisYellow,
                        modifier = Modifier.weight(1f)
                    )

                    RallyTelemetryCard(
                        label = "TOTAL AVG PACE",
                        value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
                        unit = speedUnit,
                        subtitle = "OVERALL PACE",
                        accentColor = RallyHighVisYellow,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RallyTelemetryCard(
                        label = "STAGE TOP VELOCITY",
                        value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                        unit = speedUnit,
                        subtitle = "MAX RECORD",
                        accentColor = RallyDakarOrange,
                        modifier = Modifier.weight(1f)
                    )

                    RallyTelemetryCard(
                        label = "SAT-LOCK ACCURACY",
                        value = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                        subtitle = if (isGpsLost) "SIGNAL LOST" else "GPS SYNC",
                        accentColor = if (isGpsLost) RallyDakarOrange else RallyCompassCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Controls Row
                if (isIdle) {
                    Button(
                        onClick = onStartClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RallyDakarOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Text(
                            text = "START RIDE",
                            color = RallyWhite,
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
                            onClick = {
                                if (pauseState.isPaused) onResumeClick() else onPauseClick()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (pauseState.isPaused) RallySandGold.copy(alpha = 0.2f) else RallyCardBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (pauseState.isPaused) RallyHighVisYellow else RallyDakarOrange
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
                                    tint = if (pauseState.isPaused) RallyHighVisYellow else RallyDakarOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pauseState.isPaused) "RESUME STAGE" else "BIVOUAC PAUSE",
                                    color = if (pauseState.isPaused) RallyHighVisYellow else RallyWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        ThemedHoldToStopButton(
                            onStopConfirmed = onStopConfirmed,
                            label = "STAGE FINISH",
                            progressLabel = "FINISHING",
                            borderColor = RallyDakarOrange,
                            gradientColors = listOf(Color(0xFF5A2500), Color(0xFF261000)),
                            progressFillColor = RallyDakarOrange.copy(alpha = 0.6f),
                            textColor = RallyWhite,
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
 * Upper Navigation Tower: Roadbook Trip Master (ICO Racing style).
 * Shows high-visibility yellow distance numbers on matte black casing.
 */
@Composable
private fun RallyRoadbookTripMaster(
    distance: Double,
    distanceUnit: String,
    accuracyMeters: Float,
    isGpsLost: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RallyTowerDark)
            .border(2.dp, RallyBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Roadbook Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isGpsLost) Color(0xFFFF3333) else RallyHighVisYellow)
                    )
                    Text(
                        text = "ICO TRIP 1 // ROADBOOK",
                        color = RallyDakarOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = if (isGpsLost) "NO GPS FIX" else "WAYPOINT LOCK ±${accuracyMeters.toInt()}M",
                    color = if (isGpsLost) Color(0xFFFF3333) else RallyTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Giant High-Vis Roadbook Distance Readout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0C0B0A))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KM-TOT",
                    color = RallySandGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%05.2f", distance),
                        color = RallyHighVisYellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = distanceUnit,
                        color = RallyWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Lower Navigation Tower: High-Contrast Glare-Proof Rally Speedometer.
 */
@Composable
private fun RallySpeedDisplay(
    speed: Double,
    unit: String,
    isSpeedAlert: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RallyCardBg)
            .border(
                2.dp,
                if (isSpeedAlert) Color(0xFFFF2200) else RallyDakarOrange,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // High-contrast rally alert hazard stripes if over speed
        if (isSpeedAlert) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stripeWidth = 14.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = Color(0xFFFF2200).copy(alpha = 0.15f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = stripeWidth
                    )
                    x += stripeWidth * 2
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isSpeedAlert) "⚠ SPEED ZONE EXCEEDED ⚠" else "STAGE SPEEDOMETER",
                color = if (isSpeedAlert) Color(0xFFFF4400) else RallyDakarOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.US, "%.0f", speed),
                    color = if (isSpeedAlert) Color(0xFFFF3300) else RallyWhite,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-2).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    color = RallyDakarOrange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
    }
}

/**
 * Rally Telemetry Card with heavy rugged Dakar borders.
 */
@Composable
private fun RallyTelemetryCard(
    label: String,
    value: String,
    unit: String = "",
    subtitle: String? = null,
    accentColor: Color = RallyDakarOrange,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(RallyCardBg)
            .border(1.5.dp, RallyBorder, RoundedCornerShape(6.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = RallyTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    color = accentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        color = RallyTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = RallyTextMuted.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Rally Status Bar with elevation gain and waypoint tracking.
 */
@Composable
private fun RallyStatusBar(
    bikeName: String?,
    pauseState: PauseState,
    isGpsLost: Boolean,
    waypointCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(RallyTowerDark)
            .border(1.dp, RallyBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Terrain,
                contentDescription = null,
                tint = RallySandGold,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = when {
                    isGpsLost -> "GPS BEARING LOST"
                    pauseState.isPaused -> "BIVOUAC PAUSE"
                    else -> "RALLY RAID TRACKING"
                },
                color = when {
                    isGpsLost -> Color(0xFFFF3333)
                    pauseState.isPaused -> RallyHighVisYellow
                    else -> RallyWhite
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = String.format(Locale.US, "WP LOGGED: %d", waypointCount),
            color = RallySandGold,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RallyTag(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF331600))
            .border(1.dp, RallyDakarOrange.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = RallyHighVisYellow,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
