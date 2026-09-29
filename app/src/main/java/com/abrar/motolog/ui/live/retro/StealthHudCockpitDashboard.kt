package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
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
import kotlin.math.cos
import kotlin.math.sin

// Stealth HUD Phosphor Palette Constants
private val HudBlack = Color(0xFF000000)
private val HudPhosphorGreen = Color(0xFF00FF66)
private val HudElectricLime = Color(0xFF39FF14)
private val HudCyan = Color(0xFF00E5FF)
private val HudAmber = Color(0xFFFFD600)
private val HudRed = Color(0xFFFF1744)
private val HudWireframe = Color(0xFF14331C)
private val HudTextMuted = Color(0xFF558B65)

/**
 * Fighter Jet Collimator & Hypernaked HUD Dashboard.
 * 100% Pure OLED Black with high-contrast phosphor green reticle, dynamic vertical
 * airspeed tape ladder, artificial horizon line, and military-grade telemetry cards.
 */
@Composable
fun StealthHudCockpitDashboard(
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
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.STEALTH_HUD)
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
            .background(HudBlack)
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
                // Fullscreen Dominant Tactical HUD
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudAirspeedLadder(
                        currentSpeed = displaySpeed,
                        isMetric = isMetric,
                        modifier = Modifier
                            .width(85.dp)
                            .fillMaxHeight()
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    val reticleSize = (screenHeight * 0.88f).coerceAtMost(screenWidth * 0.52f)
                    HudCollimatorReticle(
                        speed = displaySpeed,
                        speedUnit = speedUnit,
                        isSpeedAlert = isSpeedAlert,
                        isGpsLost = isGpsLost,
                        modifier = Modifier.size(reticleSize)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    HudAltitudeLadder(
                        elevationGain = elevationGainMeters,
                        isMetric = isMetric,
                        modifier = Modifier
                            .width(85.dp)
                            .fillMaxHeight()
                    )
                }

                // Subtle Top Pull Tab indicator
                LandscapeTelemetryPullTab(
                    visible = !showLandscapeTelemetrySheet,
                    onClick = { showLandscapeTelemetrySheet = true },
                    accentColor = HudPhosphorGreen,
                    backgroundColor = HudBlack,
                    borderColor = HudCyan,
                    label = "TACTICAL TELEMETRY",
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Top Right Map Button
                ThemedTopRightMapButton(
                    onClick = onSwitchToMap,
                    palette = palette,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    label = "TAC MAP"
                )

                // Slide-down drawer with numerical values and controls
                LandscapeTelemetryDrawer(
                    visible = showLandscapeTelemetrySheet,
                    onDismiss = { showLandscapeTelemetrySheet = false },
                    backgroundColor = HudBlack.copy(alpha = 0.96f),
                    borderColor = HudPhosphorGreen,
                    accentColor = HudCyan,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    // Tactical Distance Strip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HudWireframe,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HudPhosphorGreen.copy(alpha = 0.35f)),
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
                                text = "TAC TRIP DIST",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudPhosphorGreen.copy(alpha = 0.8f)
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f %s", displayDistance, distanceUnit),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = HudPhosphorGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 6 Telemetry Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HudCard(
                            label = "MISSION",
                            value = movingTimeDisplay,
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                        HudCard(
                            label = "TOTAL TIME",
                            value = totalTimeDisplay,
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                        HudCard(
                            label = "MOVING AVG",
                            value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        HudCard(
                            label = "OVERALL AVG",
                            value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        HudCard(
                            label = "PEAK SPEED",
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        HudCard(
                            label = "SAT FIX",
                            value = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tactical Control Row inside Drawer
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HudPhosphorGreen,
                                    contentColor = HudBlack
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "ENGAGE HUD",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                            }

                            ThemedIdleTopBar(
                                keepScreenOn = keepScreenOn,
                                onToggleKeepScreenOn = onToggleKeepScreenOn,
                                onNavigateToSettings = onNavigateToSettings,
                                palette = palette,
                                badgeText = "STEALTH",
                                modifier = Modifier.weight(1f),
                                onSwitchToMap = onSwitchToMap
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
                                    containerColor = HudWireframe,
                                    contentColor = HudPhosphorGreen
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .border(1.dp, HudPhosphorGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.Map, contentDescription = "Map", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MAP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = if (pauseState.isPaused) onResumeClick else onPauseClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pauseState.isPaused) HudAmber.copy(alpha = 0.2f) else HudWireframe,
                                    contentColor = if (pauseState.isPaused) HudAmber else HudCyan
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(1.dp, (if (pauseState.isPaused) HudAmber else HudCyan).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            ) {
                                Icon(
                                    imageVector = if (pauseState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (pauseState.isPaused) "RESUME" else "PAUSE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            ThemedHoldToStopButton(
                                onStopConfirmed = onStopConfirmed,
                                label = "DISARM",
                                progressLabel = "OFF",
                                borderColor = HudRed,
                                gradientColors = listOf(Color(0xFF5E0B0B), Color(0xFF260505)),
                                progressFillColor = HudRed.copy(alpha = 0.6f),
                                textColor = Color.White,
                                cornerRadius = 4.dp,
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(48.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Tactical Collimator Layout
            val reticleSize = (screenWidth * 0.70f).coerceIn(255.dp, 305.dp)
            val hudRowHeight = reticleSize + 14.dp

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
                    // Top Section: Header + BIG Collimator HUD + Mission Distance Strip
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header (ThemedIdleTopBar in Idle, or Tactical Header + Map Button when tracking)
                        if (isIdle) {
                            ThemedIdleTopBar(
                                keepScreenOn = keepScreenOn,
                                onToggleKeepScreenOn = onToggleKeepScreenOn,
                                onNavigateToSettings = onNavigateToSettings,
                                palette = palette,
                                badgeText = "STEALTH STANDBY",
                                onSwitchToMap = onSwitchToMap
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isGpsLost) HudRed else HudPhosphorGreen)
                                    )
                                    Text(
                                        text = "HUD - ${bikeName?.uppercase(Locale.US) ?: "STEALTH"}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HudPhosphorGreen.copy(alpha = 0.9f),
                                        letterSpacing = 2.sp
                                    )
                                    Text(
                                        text = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (isGpsLost) HudRed else HudCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    onClick = onSwitchToMap,
                                    shape = RoundedCornerShape(4.dp),
                                    color = HudWireframe,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, HudPhosphorGreen.copy(alpha = 0.7f)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = "Tactical Map",
                                            tint = HudPhosphorGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "TAC MAP",
                                            color = HudPhosphorGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Center Symmetrical HUD: Airspeed Tape on Left, BIG Collimator Reticle in Center, Altitude Tape on Right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hudRowHeight),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            HudAirspeedLadder(
                                currentSpeed = displaySpeed,
                                isMetric = isMetric,
                                modifier = Modifier
                                    .width(52.dp)
                                    .fillMaxHeight()
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            HudCollimatorReticle(
                                speed = displaySpeed,
                                speedUnit = speedUnit,
                                isSpeedAlert = isSpeedAlert,
                                isGpsLost = isGpsLost,
                                modifier = Modifier.size(reticleSize)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            HudAltitudeLadder(
                                elevationGain = elevationGainMeters,
                                isMetric = isMetric,
                                modifier = Modifier
                                    .width(52.dp)
                                    .fillMaxHeight()
                            )
                        }

                        // Tactical Mission Distance Strip
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HudWireframe,
                            border = androidx.compose.foundation.BorderStroke(1.dp, HudPhosphorGreen.copy(alpha = 0.35f)),
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
                                    text = "TAC MISSION DISTANCE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudPhosphorGreen.copy(alpha = 0.85f),
                                    letterSpacing = 1.sp
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", displayDistance),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = HudPhosphorGreen
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = distanceUnit,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HudCyan,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Middle Section: Tactical Telemetry Cards Grid - 6 Standard Metrics
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HudCard(
                                label = "MISSION TIME",
                                value = movingTimeDisplay,
                                unit = "",
                                modifier = Modifier.weight(1f)
                            )
                            HudCard(
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
                            HudCard(
                                label = "MOVING AVG",
                                value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                                unit = speedUnit,
                                modifier = Modifier.weight(1f)
                            )
                            HudCard(
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
                            HudCard(
                                label = "PEAK VELOCITY",
                                value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                                unit = speedUnit,
                                modifier = Modifier.weight(1f)
                            )
                            HudCard(
                                label = "GPS ACCURACY",
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
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HudPhosphorGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text(
                                text = "ENGAGE HUD - START",
                                color = HudBlack,
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
                            Button(
                                onClick = if (pauseState.isPaused) onResumeClick else onPauseClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pauseState.isPaused) HudAmber.copy(alpha = 0.2f) else HudWireframe,
                                    contentColor = if (pauseState.isPaused) HudAmber else HudCyan
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .border(1.dp, (if (pauseState.isPaused) HudAmber else HudCyan).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (pauseState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = if (pauseState.isPaused) HudAmber else HudCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (pauseState.isPaused) "RESUME" else "STAND DOWN",
                                        color = if (pauseState.isPaused) HudAmber else HudCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            ThemedHoldToStopButton(
                                onStopConfirmed = onStopConfirmed,
                                label = "DISARM HUD",
                                progressLabel = "DISARMING",
                                borderColor = HudRed,
                                gradientColors = listOf(Color(0xFF5E0B0B), Color(0xFF260505)),
                                progressFillColor = HudRed.copy(alpha = 0.6f),
                                textColor = Color.White,
                                cornerRadius = 4.dp,
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
 * Fighter Jet Collimator Reticle with Crosshairs & Center Laser Digits.
 */
@Composable
private fun HudCollimatorReticle(
    speed: Double,
    speedUnit: String,
    isSpeedAlert: Boolean,
    isGpsLost: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "hudSpeed"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 4.dp.toPx()

            val primaryColor = if (isSpeedAlert) HudRed else if (isGpsLost) HudAmber else HudPhosphorGreen

            // Outer Reticle Circle with Breaks
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Inner Collimator Ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = radius * 0.72f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Compass / Bearing tick notches around outer ring
            for (deg in 0 until 360 step 30) {
                val rad = Math.toRadians(deg.toDouble())
                val isMajor = deg % 90 == 0
                val tickLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                val start = Offset(
                    x = center.x + (radius - tickLen) * cos(rad).toFloat(),
                    y = center.y + (radius - tickLen) * sin(rad).toFloat()
                )
                val end = Offset(
                    x = center.x + radius * cos(rad).toFloat(),
                    y = center.y + radius * sin(rad).toFloat()
                )
                drawLine(
                    color = if (isMajor) primaryColor else primaryColor.copy(alpha = 0.4f),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Crosshair Pitch Horizon Ticks (Left & Right)
            val crosshairLen = radius * 0.28f
            // Left horizon bar
            drawLine(
                color = primaryColor.copy(alpha = 0.6f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x - radius + crosshairLen, center.y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square
            )
            // Left horizon vertical drop
            drawLine(
                color = primaryColor.copy(alpha = 0.6f),
                start = Offset(center.x - radius + crosshairLen, center.y),
                end = Offset(center.x - radius + crosshairLen, center.y + 6.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Right horizon bar
            drawLine(
                color = primaryColor.copy(alpha = 0.6f),
                start = Offset(center.x + radius - crosshairLen, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square
            )
            // Right horizon vertical drop
            drawLine(
                color = primaryColor.copy(alpha = 0.6f),
                start = Offset(center.x + radius - crosshairLen, center.y),
                end = Offset(center.x + radius - crosshairLen, center.y + 6.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Dynamic Arc indicator based on speed (0-160 scale)
            val speedSweep = (animatedSpeed / 160f).coerceIn(0f, 1f) * 240f
            drawArc(
                color = primaryColor,
                startAngle = 150f,
                sweepAngle = speedSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Center Laser Speed Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val speedColor = if (isSpeedAlert) HudRed else if (isGpsLost) HudAmber else HudElectricLime
            Text(
                text = String.format(Locale.US, "%.0f", animatedSpeed),
                fontFamily = FontFamily.Monospace,
                fontSize = 70.sp,
                fontWeight = FontWeight.Black,
                color = speedColor,
                letterSpacing = (-2).sp
            )
            Text(
                text = speedUnit,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HudCyan,
                letterSpacing = 2.sp
            )
        }
    }
}

/**
 * Vertical Airspeed Tape Ladder showing climbing velocity notches.
 */
@Composable
private fun HudAirspeedLadder(
    currentSpeed: Double,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(1.dp, HudWireframe, RoundedCornerShape(4.dp))
            .background(HudBlack.copy(alpha = 0.7f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val midY = h / 2f

            // Vertical axis line on right side of tape
            val axisX = w - 12.dp.toPx()
            drawLine(
                color = HudPhosphorGreen.copy(alpha = 0.4f),
                start = Offset(axisX, 0f),
                end = Offset(axisX, h),
                strokeWidth = 1.dp.toPx()
            )

            // Draw climbing hash marks relative to current speed
            val pixelsPerUnit = h / 60f // show window of 60 km/h or mph
            val roundedBase = (currentSpeed / 10).toInt() * 10

            for (spd in (roundedBase - 30)..(roundedBase + 30) step 5) {
                if (spd < 0) continue
                val diff = (spd - currentSpeed).toFloat()
                val y = midY - (diff * pixelsPerUnit)
                if (y in 0f..h) {
                    val isMajor = spd % 10 == 0
                    val tickLen = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                    drawLine(
                        color = if (isMajor) HudPhosphorGreen else HudPhosphorGreen.copy(alpha = 0.4f),
                        start = Offset(axisX - tickLen, y),
                        end = Offset(axisX, y),
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            // Current Airspeed Pointer Arrow: >
            val pointerPath = Path().apply {
                moveTo(axisX + 4.dp.toPx(), midY)
                lineTo(axisX + 11.dp.toPx(), midY - 6.dp.toPx())
                lineTo(axisX + 11.dp.toPx(), midY + 6.dp.toPx())
                close()
            }
            drawPath(pointerPath, color = HudCyan)
        }
    }
}

/**
 * Vertical Altitude Tape Ladder showing climbing elevation notches on the right.
 */
@Composable
private fun HudAltitudeLadder(
    elevationGain: Double,
    isMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val displayAlt = if (isMetric) elevationGain else elevationGain * 3.28084
    Box(
        modifier = modifier
            .border(1.dp, HudWireframe, RoundedCornerShape(4.dp))
            .background(HudBlack.copy(alpha = 0.7f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val midY = h / 2f
            val axisX = 12.dp.toPx()

            // Vertical axis line on left side of tape
            drawLine(
                color = HudCyan.copy(alpha = 0.4f),
                start = Offset(axisX, 0f),
                end = Offset(axisX, h),
                strokeWidth = 1.dp.toPx()
            )

            // Draw elevation tick notches
            val pixelsPerUnit = h / 60f
            val roundedBase = (displayAlt / 10).toInt() * 10

            for (alt in (roundedBase - 30)..(roundedBase + 30) step 5) {
                if (alt < 0) continue
                val diff = (alt - displayAlt).toFloat()
                val y = midY - (diff * pixelsPerUnit)
                if (y in 0f..h) {
                    val isMajor = alt % 10 == 0
                    val tickLen = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                    drawLine(
                        color = if (isMajor) HudCyan else HudCyan.copy(alpha = 0.4f),
                        start = Offset(axisX, y),
                        end = Offset(axisX + tickLen, y),
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }
            }

            // Current Altitude Pointer Arrow: <
            val pointerPath = Path().apply {
                moveTo(axisX - 4.dp.toPx(), midY)
                lineTo(axisX - 11.dp.toPx(), midY - 6.dp.toPx())
                lineTo(axisX - 11.dp.toPx(), midY + 6.dp.toPx())
                close()
            }
            drawPath(pointerPath, color = HudPhosphorGreen)
        }

        // Mini Label
        Text(
            text = "ALT",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = HudCyan.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )
    }
}

/**
 * Minimalist Tactical Wireframe Card.
 */
@Composable
private fun HudCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, HudPhosphorGreen.copy(alpha = 0.25f), RoundedCornerShape(4.dp)),
        color = HudBlack,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HudTextMuted,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = HudPhosphorGreen
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudCyan,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
