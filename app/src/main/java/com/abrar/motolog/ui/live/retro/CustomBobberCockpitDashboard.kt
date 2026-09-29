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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// Custom Bobber V-Twin Billet Palette Constants
private val BobberBlack = Color(0xFF0F0D0B)
private val BobberCastIron = Color(0xFF181412)
private val BobberBronze = Color(0xFFD48B47)
private val BobberDarkBronze = Color(0xFF5A381E)
private val BobberFlameOrange = Color(0xFFFF5722)
private val BobberRawSteel = Color(0xFFA4ADB8)
private val BobberParchment = Color(0xFFF5EBE1)
private val BobberMuted = Color(0xFF8C7A6D)
private val BobberBorder = Color(0xFF3B2E24)

/**
 * American V-Twin 180° Machined Billet Arc Speedometer Dashboard.
 * Inspired by custom bobbers and choppers (Indian Scout, Harley Fat Bob).
 * Features a sweeping 180° semi-circular billet arc dial, center stamped speed readout,
 * V-Twin engine rumble pulse bar, and dark bronze raw steel telemetry.
 */
@Composable
fun CustomBobberCockpitDashboard(
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
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.CUSTOM_BOBBER)
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
                Brush.verticalGradient(listOf(BobberBlack, Color(0xFF080706), BobberBlack))
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
                // Dominant Fullscreen 180° Billet Arc Speedometer
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val arcSize = (screenHeight * 0.82f).coerceAtMost(screenWidth * 0.65f)
                    BobberBilletArcGauge(
                        speed = displaySpeed,
                        speedUnit = speedUnit,
                        isSpeedAlert = isSpeedAlert,
                        modifier = Modifier.size(arcSize)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TRIP ${String.format(Locale.US, "%.1f", displayDistance)} $distanceUnit",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BobberBronze,
                        letterSpacing = 1.sp
                    )
                }

                // Subtle Top Pull Tab indicator
                LandscapeTelemetryPullTab(
                    visible = !showLandscapeTelemetrySheet,
                    onClick = { showLandscapeTelemetrySheet = true },
                    accentColor = BobberBronze,
                    backgroundColor = BobberCastIron,
                    borderColor = BobberBorder,
                    label = "BOBBER TELEMETRY",
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Top Right Map Button
                ThemedTopRightMapButton(
                    onClick = onSwitchToMap,
                    palette = palette,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    label = "ROAD MAP"
                )

                // Slide-down drawer with numerical values and controls
                LandscapeTelemetryDrawer(
                    visible = showLandscapeTelemetrySheet,
                    onDismiss = { showLandscapeTelemetrySheet = false },
                    backgroundColor = BobberBlack.copy(alpha = 0.96f),
                    borderColor = BobberBorder,
                    accentColor = BobberBronze,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    // Stamped Trip Odometer Strip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BobberCastIron,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BobberBronze.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRIP ODOMETER",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BobberMuted
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f %s", displayDistance, distanceUnit),
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = BobberParchment
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 6 Bobber Metric Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BobberMetricCard(
                            label = "RUMBLE TIME",
                            value = movingTimeDisplay,
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                        BobberMetricCard(
                            label = "TOTAL TIME",
                            value = totalTimeDisplay,
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                        BobberMetricCard(
                            label = "CRUISE AVG",
                            value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        BobberMetricCard(
                            label = "OVERALL AVG",
                            value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        BobberMetricCard(
                            label = "TOP MARK",
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        BobberMetricCard(
                            label = "SAT FIX",
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
                                colors = ButtonDefaults.buttonColors(containerColor = BobberFlameOrange),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "FIRE UP - START",
                                    color = BobberCastIron,
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
                                badgeText = "V-TWIN",
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
                                    containerColor = BobberCastIron,
                                    contentColor = BobberBronze
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(44.dp)
                                    .border(1.dp, BobberBorder, RoundedCornerShape(6.dp))
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ROAD MAP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = if (pauseState.isPaused) onResumeClick else onPauseClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BobberCastIron,
                                    contentColor = if (pauseState.isPaused) BobberFlameOrange else BobberBronze
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .border(1.dp, (if (pauseState.isPaused) BobberFlameOrange else BobberBronze).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
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
                                label = "KILL",
                                progressLabel = "OFF",
                                borderColor = BobberFlameOrange,
                                gradientColors = listOf(Color(0xFF4E1A05), Color(0xFF220A01)),
                                progressFillColor = BobberFlameOrange.copy(alpha = 0.6f),
                                textColor = BobberParchment,
                                cornerRadius = 6.dp,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Bobber Layout
            val gaugeHeight = (screenWidth * 0.70f).coerceIn(255.dp, 290.dp)

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
                    // Top Section: Header + BIG Billet Arc Gauge + Stamped Trip Odometer + Engine Rumble Bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header (ThemedIdleTopBar in Idle, or Custom Header + Map Button when tracking)
                        if (isIdle) {
                            ThemedIdleTopBar(
                                keepScreenOn = keepScreenOn,
                                onToggleKeepScreenOn = onToggleKeepScreenOn,
                                onNavigateToSettings = onNavigateToSettings,
                                onSwitchToMap = onSwitchToMap,
                                palette = palette,
                                badgeText = "V-TWIN STANDBY"
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "V-TWIN - ${bikeName?.uppercase(Locale.US) ?: "CUSTOM BOBBER"}",
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BobberBronze,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGpsLost) BobberFlameOrange else BobberRawSteel,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Surface(
                                    onClick = onSwitchToMap,
                                    shape = RoundedCornerShape(8.dp),
                                    color = BobberCastIron,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BobberBronze),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = "Cruiser Map",
                                            tint = BobberBronze,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "ROAD MAP",
                                            color = BobberBronze,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Center 180° Billet Arc Gauge (BIGGER)
                        BobberBilletArcGauge(
                            speed = displaySpeed,
                            speedUnit = speedUnit,
                            isSpeedAlert = isSpeedAlert,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gaugeHeight)
                        )

                        // Classic Stamped Trip Odometer Strip
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BobberCastIron,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BobberBronze.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TRIP ODOMETER",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BobberMuted,
                                    letterSpacing = 1.sp
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", displayDistance),
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BobberParchment
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = distanceUnit,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BobberBronze,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }

                        // V-Twin Engine Rumble Vibration Bar
                        BobberEngineRumbleBar(
                            speed = displaySpeed,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Middle Section: Leather-Stitched Telemetry Cards - 6 Standard Metrics
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BobberMetricCard(
                                label = "RUMBLE TIME",
                                value = movingTimeDisplay,
                                unit = "",
                                modifier = Modifier.weight(1f)
                            )
                            BobberMetricCard(
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
                            BobberMetricCard(
                                label = "CRUISE AVG",
                                value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                                unit = speedUnit,
                                modifier = Modifier.weight(1f)
                            )
                            BobberMetricCard(
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
                            BobberMetricCard(
                                label = "TOP MARK",
                                value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                                unit = speedUnit,
                                modifier = Modifier.weight(1f)
                            )
                            BobberMetricCard(
                                label = "SAT FIX",
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
                            colors = ButtonDefaults.buttonColors(containerColor = BobberFlameOrange),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text(
                                text = "FIRE UP - START",
                                color = BobberCastIron,
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
                                color = if (pauseState.isPaused) BobberFlameOrange.copy(alpha = 0.2f) else BobberCastIron,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (pauseState.isPaused) BobberFlameOrange else BobberBronze
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
                                        tint = if (pauseState.isPaused) BobberFlameOrange else BobberBronze,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (pauseState.isPaused) "RESUME" else "IDLE",
                                        color = if (pauseState.isPaused) BobberFlameOrange else BobberParchment,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            ThemedHoldToStopButton(
                                onStopConfirmed = onStopConfirmed,
                                label = "KILL MOTOR",
                                progressLabel = "SHUTTING DOWN",
                                borderColor = BobberFlameOrange,
                                gradientColors = listOf(Color(0xFF4E1A05), Color(0xFF220A01)),
                                progressFillColor = BobberFlameOrange.copy(alpha = 0.6f),
                                textColor = BobberParchment,
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
 * 180° Semi-Circular Machined Billet Arc Gauge with Center Stamped Speed Readout.
 */
@Composable
private fun BobberBilletArcGauge(
    speed: Double,
    speedUnit: String,
    isSpeedAlert: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "bobberSpeed"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.72f)
            val radius = (size.width * 0.44f).coerceAtMost(size.height * 0.65f)

            // Outer Billet Arc Track (180° sweep from 180° to 360°)
            drawArc(
                color = BobberBorder,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active Speed Flame Arc
            val maxScale = 160f
            val speedFraction = (animatedSpeed / maxScale).coerceIn(0f, 1f)
            val activeSweep = speedFraction * 180f

            drawArc(
                brush = Brush.horizontalGradient(
                    listOf(BobberDarkBronze, BobberBronze, if (isSpeedAlert) BobberFlameOrange else BobberFlameOrange)
                ),
                startAngle = 180f,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Billet Hash Notches
            for (step in 0..160 step 10) {
                val frac = step / 160f
                val angleDeg = 180f + frac * 180f
                val rad = Math.toRadians(angleDeg.toDouble())
                val isMajor = step % 20 == 0
                val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()

                val start = Offset(
                    x = center.x + (radius - tickLen - 6.dp.toPx()) * cos(rad).toFloat(),
                    y = center.y + (radius - tickLen - 6.dp.toPx()) * sin(rad).toFloat()
                )
                val end = Offset(
                    x = center.x + (radius - 6.dp.toPx()) * cos(rad).toFloat(),
                    y = center.y + (radius - 6.dp.toPx()) * sin(rad).toFloat()
                )

                drawLine(
                    color = if (isMajor) BobberBronze else BobberRawSteel.copy(alpha = 0.5f),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Machined Needle Indicator Pointing outward from center
            val needleRad = Math.toRadians((180f + activeSweep).toDouble())
            val needleLen = radius - 8.dp.toPx()
            val needleTip = Offset(
                x = center.x + needleLen * cos(needleRad).toFloat(),
                y = center.y + needleLen * sin(needleRad).toFloat()
            )
            drawLine(
                color = BobberFlameOrange,
                start = center,
                end = needleTip,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Billet Hub Center Cap
            drawCircle(
                color = BobberBronze,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = BobberCastIron,
                radius = 6.dp.toPx(),
                center = center
            )
        }

        // Center Stamped Speed Readout
        Column(
            modifier = Modifier.padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.US, "%.0f", animatedSpeed),
                fontSize = 82.sp,
                fontWeight = FontWeight.Black,
                color = if (isSpeedAlert) BobberFlameOrange else BobberParchment,
                fontFamily = FontFamily.Serif,
                letterSpacing = (-1).sp
            )
            Text(
                text = speedUnit,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BobberBronze,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }
    }
}

/**
 * V-Twin Engine Rumble Vibration Spectrum Bar.
 */
@Composable
private fun BobberEngineRumbleBar(
    speed: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .border(1.dp, BobberBorder, RoundedCornerShape(4.dp))
            .background(BobberCastIron, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val activeBars = ((speed / 140.0) * 12).toInt().coerceIn(1, 12)
        for (i in 0 until 12) {
            val isActive = i < activeBars
            val barColor = if (!isActive) BobberDarkBronze.copy(alpha = 0.3f)
                           else if (i > 9) BobberFlameOrange
                           else BobberBronze
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Handcrafted Leather / Billet Telemetry Card.
 */
@Composable
private fun BobberMetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, BobberBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BobberCastIron)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BobberMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = BobberParchment,
                    fontFamily = FontFamily.Serif
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BobberBronze,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
