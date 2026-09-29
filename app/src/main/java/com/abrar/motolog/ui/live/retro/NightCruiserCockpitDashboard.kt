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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

// Night Cruiser Midnight Horizon Palette Constants
private val NightNavyBg = Color(0xFF060B19)
private val NightCardBg = Color(0xFF0E172E)
private val NightStarlightBlue = Color(0xFF4D96FF)
private val NightHorizonMint = Color(0xFF6BCB77)
private val NightSkyIce = Color(0xFF38BDF8)
private val NightPearl = Color(0xFFEDF2F7)
private val NightMuted = Color(0xFF7085A6)
private val NightBorder = Color(0xFF1E2D4F)
private val NightWarning = Color(0xFFFF5252)

/**
 * Midnight Highway Low-Glare Ambient Horizon Cockpit Dashboard.
 * Designed for fatigue-free cross-country night highway touring.
 * Features an ambient illuminated horizon bar, floating soft-diffuse speed numerals,
 * and high-contrast night-optimized starlight cruise telemetry.
 */
@Composable
fun NightCruiserCockpitDashboard(
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
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.NIGHT_CRUISER)
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

    val animatedSpeed by animateFloatAsState(
        targetValue = displaySpeed.toFloat(),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "nightSpeed"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NightNavyBg, Color(0xFF03060E), NightNavyBg))
            )
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = screenWidth > screenHeight

        if (isLandscape) {
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
                // Fullscreen Dominant Midnight Horizon Speedometer
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NightHorizonBeam(speed = displaySpeed, modifier = Modifier.fillMaxWidth(0.7f).height(24.dp))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f", animatedSpeed),
                            fontSize = 82.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSpeedAlert) NightWarning else NightPearl,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = speedUnit,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NightSkyIce,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TRIP ${String.format(Locale.US, "%.1f", displayDistance)} $distanceUnit",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NightStarlightBlue,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "MIDNIGHT CRUISE - LOW GLARE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NightMuted,
                        letterSpacing = 2.sp
                    )
                }

                // Subtle Top Pull Tab indicator
                LandscapeTelemetryPullTab(
                    visible = !showLandscapeTelemetrySheet,
                    onClick = { showLandscapeTelemetrySheet = true },
                    accentColor = NightStarlightBlue,
                    backgroundColor = NightCardBg,
                    borderColor = NightBorder,
                    label = "CRUISE TELEMETRY",
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Top Right Map Button
                ThemedTopRightMapButton(
                    onClick = onSwitchToMap,
                    palette = palette,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    label = "MAP"
                )

                // Slide-down drawer with numerical values and controls
                LandscapeTelemetryDrawer(
                    visible = showLandscapeTelemetrySheet,
                    onDismiss = { showLandscapeTelemetrySheet = false },
                    backgroundColor = NightNavyBg.copy(alpha = 0.96f),
                    borderColor = NightBorder,
                    accentColor = NightStarlightBlue,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    // 6 Night Metric Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NightMetricCard(
                            label = "MOVING TIME",
                            value = movingTimeDisplay,
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                        NightMetricCard(
                            label = "TOTAL TIME",
                            value = totalTimeDisplay,
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                        NightMetricCard(
                            label = "CRUISE AVG",
                            value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        NightMetricCard(
                            label = "OVERALL AVG",
                            value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        NightMetricCard(
                            label = "MAX VELOCITY",
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        NightMetricCard(
                            label = "GPS FIX",
                            value = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Controls Row inside Drawer
                    if (isIdle) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    showLandscapeTelemetrySheet = false
                                    onStartClick()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NightStarlightBlue),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "START CRUISE",
                                    color = Color(0xFF030712),
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
                                onSwitchToMap = onSwitchToMap,
                                palette = palette,
                                badgeText = "NIGHT CRUISE",
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
                                    containerColor = NightCardBg,
                                    contentColor = NightStarlightBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(44.dp)
                                    .border(1.dp, NightBorder, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MAP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = if (pauseState.isPaused) onResumeClick else onPauseClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NightCardBg,
                                    contentColor = if (pauseState.isPaused) NightHorizonMint else NightSkyIce
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .border(1.dp, (if (pauseState.isPaused) NightHorizonMint else NightSkyIce).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = if (pauseState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (pauseState.isPaused) "RESUME" else "PAUSE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            ThemedHoldToStopButton(
                                onStopConfirmed = onStopConfirmed,
                                label = "END",
                                progressLabel = "OFF",
                                borderColor = NightWarning,
                                gradientColors = listOf(Color(0xFF501018), Color(0xFF1E050A)),
                                progressFillColor = NightWarning.copy(alpha = 0.6f),
                                textColor = NightPearl,
                                cornerRadius = 6.dp,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }        } else {
            // Portrait Night Cruiser Layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = screenHeight - 24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Section: Header + Horizon Light Beam + BIG Floating Speedometer & Odometer Card
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header (ThemedIdleTopBar in Idle, or Night Header + Map Button when tracking)
                        if (isIdle) {
                            ThemedIdleTopBar(
                                keepScreenOn = keepScreenOn,
                                onToggleKeepScreenOn = onToggleKeepScreenOn,
                                onNavigateToSettings = onNavigateToSettings,
                                onSwitchToMap = onSwitchToMap,
                                palette = palette,
                                badgeText = "MIDNIGHT STANDBY"
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NightlightRound,
                                        contentDescription = "Night Cruiser",
                                        tint = NightStarlightBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NIGHT CRUISER - ${bikeName?.uppercase(Locale.US) ?: "HIGHWAY"}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NightStarlightBlue,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGpsLost) NightWarning else NightMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Surface(
                                    onClick = onSwitchToMap,
                                    shape = RoundedCornerShape(8.dp),
                                    color = NightCardBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NightStarlightBlue),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = "Night Map",
                                            tint = NightStarlightBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "MAP",
                                            color = NightStarlightBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Ambient Horizon Light Beam
                        NightHorizonBeam(speed = displaySpeed, modifier = Modifier.fillMaxWidth().height(26.dp))

                        // Floating Speedometer Hero Card with integrated Cruise Trip Odometer (BIGGER)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NightBorder, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = NightCardBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "HIGHWAY SPEED",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NightSkyIce,
                                    letterSpacing = 2.sp
                                )

                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.0f", animatedSpeed),
                                        fontSize = 92.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSpeedAlert) NightWarning else NightPearl,
                                        fontFamily = FontFamily.SansSerif,
                                        letterSpacing = (-2).sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = speedUnit,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NightSkyIce,
                                        modifier = Modifier.padding(bottom = 16.dp),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Prominent Cruise Trip Odometer Bar
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF040A17),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NightBorder.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "CRUISE ODOMETER",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NightMuted,
                                                letterSpacing = 1.sp
                                            )
                                            Row(verticalAlignment = Alignment.Bottom) {
                                                Text(
                                                    text = String.format(Locale.US, "%.1f", displayDistance),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = NightStarlightBlue,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = distanceUnit,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NightSkyIce,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "MOVING CLOCK",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NightMuted,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = movingTimeDisplay,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NightSkyIce,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Middle Section: Low-Glare Night Telemetry Cards - 6 Standard Metrics
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NightMetricCard(
                                label = "MOVING TIME",
                                value = movingTimeDisplay,
                                unit = "",
                                modifier = Modifier.weight(1f)
                            )
                            NightMetricCard(
                                label = "TOTAL TIME",
                                value = totalTimeDisplay,
                                unit = "",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NightMetricCard(
                                label = "CRUISE AVG",
                                value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                                unit = speedUnit,
                                modifier = Modifier.weight(1f)
                            )
                            NightMetricCard(
                                label = "OVERALL AVG",
                                value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
                                unit = speedUnit,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NightMetricCard(
                                label = "MAX VELOCITY",
                                value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                                unit = speedUnit,
                                modifier = Modifier.weight(1f)
                            )
                            NightMetricCard(
                                label = "GPS FIX",
                                value = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                                unit = "",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Bottom Section: Action Controls
                    if (isIdle) {
                        Button(
                            onClick = onStartClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NightStarlightBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text(
                                text = "START NIGHT CRUISE",
                                color = Color(0xFF030712),
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
                                color = if (pauseState.isPaused) NightHorizonMint.copy(alpha = 0.2f) else NightCardBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (pauseState.isPaused) NightHorizonMint else NightBorder
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
                                        tint = if (pauseState.isPaused) NightHorizonMint else NightSkyIce,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (pauseState.isPaused) "RESUME" else "REST STOP",
                                        color = if (pauseState.isPaused) NightHorizonMint else NightPearl,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            ThemedHoldToStopButton(
                                onStopConfirmed = onStopConfirmed,
                                label = "END CRUISE",
                                progressLabel = "CONCLUDING",
                                borderColor = NightWarning,
                                gradientColors = listOf(Color(0xFF501018), Color(0xFF1E050A)),
                                progressFillColor = NightWarning.copy(alpha = 0.6f),
                                textColor = NightPearl,
                                cornerRadius = 8.dp,
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ambient illuminated horizon light beam spanning across the dash.
 */
@Composable
private fun NightHorizonBeam(
    speed: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Ambient Horizon Glow Bar
        val speedFraction = (speed / 160.0).coerceIn(0.0, 1.0).toFloat()
        val beamWidth = w * (0.4f + speedFraction * 0.6f)
        val startX = (w - beamWidth) / 2f

        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    NightStarlightBlue.copy(alpha = 0.8f),
                    NightSkyIce,
                    NightHorizonMint,
                    Color.Transparent
                ),
                startX = startX,
                endX = startX + beamWidth
            ),
            start = Offset(startX, midY),
            end = Offset(startX + beamWidth, midY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center horizon marker tick
        drawLine(
            color = NightSkyIce,
            start = Offset(w / 2f, midY - 6.dp.toPx()),
            end = Offset(w / 2f, midY + 6.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * Soft-Diffuse Midnight Telemetry Card.
 */
@Composable
private fun NightMetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, NightBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = NightCardBg)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = NightMuted,
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
                    color = NightPearl
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NightSkyIce,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
