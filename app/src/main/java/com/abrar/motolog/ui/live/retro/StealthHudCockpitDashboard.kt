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
            // Landscape Tactical Collimator Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Airspeed Ladder Tape
                HudAirspeedLadder(
                    currentSpeed = displaySpeed,
                    isMetric = isMetric,
                    modifier = Modifier
                        .width(90.dp)
                        .fillMaxHeight()
                )

                // Center: Collimator Reticle
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    val reticleSize = (screenHeight * 0.85f).coerceAtMost(screenWidth * 0.45f)
                    HudCollimatorReticle(
                        speed = displaySpeed,
                        speedUnit = speedUnit,
                        isSpeedAlert = isSpeedAlert,
                        isGpsLost = isGpsLost,
                        modifier = Modifier.size(reticleSize)
                    )
                }

                // Right: Minimal Tactical Cards & Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Telemetry Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HudCard(
                                label = "DST",
                                value = String.format(Locale.US, "%.1f", displayDistance),
                                unit = distanceUnit,
                                modifier = Modifier.weight(1f)
                            )
                            HudCard(
                                label = "TIME",
                                value = movingTimeDisplay,
                                unit = "",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HudCard(
                                label = "AVG",
                                value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                                unit = speedUnit,
                                modifier = Modifier.weight(1f)
                            )
                            HudCard(
                                label = "PEAK",
                                value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                                unit = speedUnit,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Tactical Control Row
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
                                .weight(1f)
                                .height(44.dp)
                                .border(1.dp, HudPhosphorGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Map", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MAP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (!isIdle) {
                            Button(
                                onClick = if (pauseState.isPaused) onResumeClick else onPauseClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pauseState.isPaused) HudAmber.copy(alpha = 0.2f) else HudWireframe,
                                    contentColor = if (pauseState.isPaused) HudAmber else HudCyan
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .border(1.dp, (if (pauseState.isPaused) HudAmber else HudCyan).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            ) {
                                Icon(
                                    imageVector = if (pauseState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (pauseState.isPaused) "RESUME" else "PAUSE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            RetroHoldToStopButton(
                                onStopConfirmed = onStopConfirmed,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait Tactical Collimator Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Status Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HUD // ${bikeName?.uppercase(Locale.US) ?: "STEALTH"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudPhosphorGreen.copy(alpha = 0.8f),
                        letterSpacing = 2.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isGpsLost) "GPS LOST" else String.format(Locale.US, "FIX ±%.0fm", accuracyMeters),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (isGpsLost) HudRed else HudCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isGpsLost) HudRed else HudPhosphorGreen)
                        )
                    }
                }

                // Center Main Reticle & Side Ladder
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    HudAirspeedLadder(
                        currentSpeed = displaySpeed,
                        isMetric = isMetric,
                        modifier = Modifier
                            .width(64.dp)
                            .height(260.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val reticleSize = (screenWidth * 0.72f).coerceAtMost(screenHeight * 0.38f)
                    HudCollimatorReticle(
                        speed = displaySpeed,
                        speedUnit = speedUnit,
                        isSpeedAlert = isSpeedAlert,
                        isGpsLost = isGpsLost,
                        modifier = Modifier.size(reticleSize)
                    )
                }

                // Bottom Tactical Telemetry Cards Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HudCard(
                            label = "DISTANCE",
                            value = String.format(Locale.US, "%.1f", displayDistance),
                            unit = distanceUnit,
                            modifier = Modifier.weight(1f)
                        )
                        HudCard(
                            label = "MOVING TIME",
                            value = movingTimeDisplay,
                            unit = "",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HudCard(
                            label = "AVG SPEED",
                            value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                        HudCard(
                            label = "MAX SPEED",
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Controls
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
                                .weight(1f)
                                .height(48.dp)
                                .border(1.dp, HudPhosphorGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Map", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MAP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (!isIdle) {
                            Button(
                                onClick = if (pauseState.isPaused) onResumeClick else onPauseClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pauseState.isPaused) HudAmber.copy(alpha = 0.2f) else HudWireframe,
                                    contentColor = if (pauseState.isPaused) HudAmber else HudCyan
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .border(1.dp, (if (pauseState.isPaused) HudAmber else HudCyan).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            ) {
                                Icon(
                                    imageVector = if (pauseState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pauseState.isPaused) "RESUME" else "PAUSE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            RetroHoldToStopButton(
                                onStopConfirmed = onStopConfirmed,
                                modifier = Modifier.weight(1.4f)
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
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                color = speedColor,
                letterSpacing = (-2).sp
            )
            Text(
                text = speedUnit,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = HudPhosphorGreen
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudCyan,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
