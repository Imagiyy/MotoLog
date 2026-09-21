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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.domain.model.PauseState
import com.abrar.motolog.domain.model.RideStats
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Neon Cyber Palette Constants
private val CyberBg = Color(0xFF07040E)
private val CyberSurface = Color(0xFF100921)
private val CyberSurfaceAlt = Color(0xFF180F33)
private val CyberCyan = Color(0xFF00F5FF)
private val CyberMagenta = Color(0xFFFF007F)
private val CyberYellow = Color(0xFFFFE600)
private val CyberGreen = Color(0xFF00FF88)
private val CyberPurple = Color(0xFF6B11FF)
private val CyberTextMuted = Color(0xFF8B80AC)
private val CyberWhite = Color(0xFFF9F7FF)

/**
 * Neon Cyber / Tokyo Night Synthwave Holographic HUD Dashboard.
 * Features an angular hexagonal vector tachometer dial, glowing neon laser sweep,
 * equalizer bars, chamfered holographic panels, and high-visibility cyber telemetry.
 */
@Composable
fun NeonCyberCockpitDashboard(
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
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.NEON_CYBER)
) {
    var showOverallStats by remember { mutableStateOf(false) }

    val totalDistanceKm = stats.totalDistanceMeters / 1000.0
    val displaySpeed = if (isMetric) speedKmh else speedKmh * 0.621371
    val displayDistance = if (isMetric) totalDistanceKm else totalDistanceKm * 0.621371
    val speedUnit = if (isMetric) "KM/H" else "MPH"
    val distanceUnit = if (isMetric) "KM" else "MI"

    val avgSpeedDisplay = if (showOverallStats) {
        if (isMetric) stats.avgOverallSpeedKmh else stats.avgOverallSpeedKmh * 0.621371
    } else {
        if (isMetric) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
    }

    val maxSpeedDisplay = if (isMetric) stats.maxSpeedKmh else stats.maxSpeedKmh * 0.621371
    val timeDisplay = CockpitUtils.formatDurationMs(if (showOverallStats) stats.elapsedTimeMs else stats.movingTimeMs)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF15082E), CyberBg, Color(0xFF040208)),
                    radius = 1200f
                )
            )
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = screenWidth > screenHeight

        if (isLandscape) {
            // Landscape Cyber Cockpit
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Hexagonal Vector HUD Dial
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    CyberHeaderBar(
                        bikeName = bikeName,
                        isGpsLost = isGpsLost,
                        modifier = Modifier.fillMaxWidth()
                    )

                    CyberHexTachometerDial(
                        speed = displaySpeed,
                        unit = speedUnit,
                        distance = displayDistance,
                        distanceUnit = distanceUnit,
                        isSpeedAlert = isSpeedAlert,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    CyberEqualizerBar(
                        speed = displaySpeed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Right Column: Telemetry Panels + Controls
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: System ID + Map Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CyberHudTag(text = "HUD_SYS // 2077")

                        Surface(
                            onClick = onSwitchToMap,
                            shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                            color = CyberSurface,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Cyber Map",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "CYBER GRID",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 2x2 Holographic Telemetry Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberTelemetryCard(
                            label = if (showOverallStats) "CHRONO_TOTAL" else "CHRONO_WARP",
                            value = timeDisplay,
                            subtitle = if (showOverallStats) "ELAPSED" else "MOVING",
                            accentColor = CyberCyan,
                            modifier = Modifier.weight(1f),
                            onClick = { showOverallStats = !showOverallStats }
                        )

                        CyberTelemetryCard(
                            label = if (showOverallStats) "AVG_OVERALL" else "CYBER_PACE",
                            value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                            unit = speedUnit,
                            subtitle = if (showOverallStats) "ELAPSED" else "MOVING",
                            accentColor = CyberGreen,
                            modifier = Modifier.weight(1f),
                            onClick = { showOverallStats = !showOverallStats }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CyberTelemetryCard(
                            label = "PEAK_VELOCITY",
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            subtitle = "MAX RECORD",
                            accentColor = CyberMagenta,
                            modifier = Modifier.weight(1f)
                        )

                        CyberTelemetryCard(
                            label = "GPS_LOCK_ACC",
                            value = if (isGpsLost) "LOST" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                            subtitle = if (isGpsLost) "CRITICAL GAP" else "SATELLITE SYNC",
                            accentColor = if (isGpsLost) CyberMagenta else CyberYellow,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Cyber Controls Row (56dp min touch target)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pause / Matrix Button
                        Surface(
                            onClick = {
                                if (pauseState.isPaused) onResumeClick() else onPauseClick()
                            },
                            shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                            color = if (pauseState.isPaused) CyberYellow.copy(alpha = 0.2f) else CyberSurfaceAlt,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (pauseState.isPaused) CyberYellow else CyberCyan
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
                                    tint = if (pauseState.isPaused) CyberYellow else CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pauseState.isPaused) "RE-ENGAGE" else "PAUSE MATRIX",
                                    color = if (pauseState.isPaused) CyberYellow else CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // System Override Hold-to-Stop Button (56dp min)
                        ThemedHoldToStopButton(
                            onStopConfirmed = onStopConfirmed,
                            label = "OVERRIDE SYSTEM",
                            progressLabel = "DISENGAGING",
                            borderColor = CyberMagenta,
                            gradientColors = listOf(Color(0xFF4A0033), Color(0xFF1E0017)),
                            progressFillColor = CyberMagenta.copy(alpha = 0.6f),
                            textColor = CyberWhite,
                            cornerRadius = 8.dp,
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        } else {
            // Portrait Cyber Cockpit Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with System status and Map switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CyberHudTag(text = "NEO-HUD // ${bikeName?.uppercase(Locale.US) ?: "CYBER-01"}")

                    Surface(
                        onClick = onSwitchToMap,
                        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        color = CyberSurface,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Cyber Map",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CYBER GRID",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Hexagonal Vector Tachometer Dial
                CyberHexTachometerDial(
                    speed = displaySpeed,
                    unit = speedUnit,
                    distance = displayDistance,
                    distanceUnit = distanceUnit,
                    isSpeedAlert = isSpeedAlert,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                // Equalizer Bar
                CyberEqualizerBar(
                    speed = displaySpeed,
                    modifier = Modifier.fillMaxWidth()
                )

                // Cyber Header Status
                CyberHeaderBar(
                    bikeName = bikeName,
                    isGpsLost = isGpsLost,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2x2 Telemetry Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberTelemetryCard(
                        label = if (showOverallStats) "CHRONO_TOTAL" else "CHRONO_WARP",
                        value = timeDisplay,
                        subtitle = if (showOverallStats) "ELAPSED" else "MOVING",
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { showOverallStats = !showOverallStats }
                    )

                    CyberTelemetryCard(
                        label = if (showOverallStats) "AVG_OVERALL" else "CYBER_PACE",
                        value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                        unit = speedUnit,
                        subtitle = if (showOverallStats) "ELAPSED" else "MOVING",
                        accentColor = CyberGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { showOverallStats = !showOverallStats }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberTelemetryCard(
                        label = "PEAK_VELOCITY",
                        value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                        unit = speedUnit,
                        subtitle = "MAX RECORD",
                        accentColor = CyberMagenta,
                        modifier = Modifier.weight(1f)
                    )

                    CyberTelemetryCard(
                        label = "GPS_LOCK_ACC",
                        value = if (isGpsLost) "LOST" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                        subtitle = if (isGpsLost) "CRITICAL GAP" else "SATELLITE SYNC",
                        accentColor = if (isGpsLost) CyberMagenta else CyberYellow,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Controls Row (56dp min buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = {
                            if (pauseState.isPaused) onResumeClick() else onPauseClick()
                        },
                        shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                        color = if (pauseState.isPaused) CyberYellow.copy(alpha = 0.2f) else CyberSurfaceAlt,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (pauseState.isPaused) CyberYellow else CyberCyan
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
                                tint = if (pauseState.isPaused) CyberYellow else CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pauseState.isPaused) "RE-ENGAGE" else "PAUSE MATRIX",
                                color = if (pauseState.isPaused) CyberYellow else CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    ThemedHoldToStopButton(
                        onStopConfirmed = onStopConfirmed,
                        label = "OVERRIDE SYSTEM",
                        progressLabel = "DISENGAGING",
                        borderColor = CyberMagenta,
                        gradientColors = listOf(Color(0xFF4A0033), Color(0xFF1E0017)),
                        progressFillColor = CyberMagenta.copy(alpha = 0.6f),
                        textColor = CyberWhite,
                        cornerRadius = 8.dp,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

/**
 * Angular Hexagonal Vector Tachometer Dial.
 * Features vector-rendered cybernetic neon sweeps, glowing laser needle,
 * and central digital HUD readout.
 */
@Composable
private fun CyberHexTachometerDial(
    speed: Double,
    unit: String,
    distance: Double,
    distanceUnit: String,
    isSpeedAlert: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyberGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .clip(CutCornerShape(16.dp))
            .background(CyberSurface)
            .border(
                1.5.dp,
                if (isSpeedAlert) CyberMagenta else CyberCyan.copy(alpha = 0.7f),
                CutCornerShape(16.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Compose Canvas for Hexagonal Arc and Laser Pointer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 16.dp.toPx()

            // Outer hexagonal guide reticle
            val hexPoints = 6
            val hexPath = Path()
            for (i in 0 until hexPoints) {
                val angleRad = Math.toRadians((i * 60.0) - 30.0)
                val px = center.x + radius * cos(angleRad).toFloat()
                val py = center.y + radius * sin(angleRad).toFloat()
                if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
            }
            hexPath.close()

            drawPath(
                path = hexPath,
                color = CyberPurple.copy(alpha = 0.4f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Dynamic Tachometer Arc (from 140° to 400° -> 260° sweep)
            val startAngle = 140f
            val sweepTotal = 260f
            val maxSpeed = 160.0
            val speedRatio = (speed / maxSpeed).coerceIn(0.0, 1.0).toFloat()

            // Background Arc Track
            drawArc(
                color = Color(0xFF231448),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active Laser Sweep Arc
            val activeSweep = sweepTotal * speedRatio
            if (activeSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(CyberCyan, CyberMagenta, CyberYellow)
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Laser Needle Pointer
            val needleAngle = startAngle + activeSweep
            val needleRad = Math.toRadians(needleAngle.toDouble())
            val needleEnd = Offset(
                center.x + (radius - 4.dp.toPx()) * cos(needleRad).toFloat(),
                center.y + (radius - 4.dp.toPx()) * sin(needleRad).toFloat()
            )
            val needleStart = Offset(
                center.x + (radius * 0.45f) * cos(needleRad).toFloat(),
                center.y + (radius * 0.45f) * sin(needleRad).toFloat()
            )

            drawLine(
                color = if (isSpeedAlert) CyberMagenta else CyberCyan,
                start = needleStart,
                end = needleEnd,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Central Speed Numbers Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isSpeedAlert) "! SPEED WARN !" else "KINETIC VECTOR",
                color = if (isSpeedAlert) CyberMagenta else CyberCyan.copy(alpha = pulseAlpha),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Text(
                text = String.format(Locale.US, "%.0f", speed),
                color = if (isSpeedAlert) CyberMagenta else CyberWhite,
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp
            )

            Text(
                text = unit,
                color = CyberCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Sub-trip ticker
            Surface(
                color = CyberBg.copy(alpha = 0.8f),
                shape = CutCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberPurple.copy(alpha = 0.6f))
            ) {
                Text(
                    text = String.format(Locale.US, "WARP: %.2f %s", distance, distanceUnit),
                    color = CyberTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Dynamic Cyber Equalizer Acceleration Bar Graphic.
 */
@Composable
private fun CyberEqualizerBar(
    speed: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CutCornerShape(4.dp))
            .background(CyberSurfaceAlt)
            .border(1.dp, CyberPurple.copy(alpha = 0.4f), CutCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bars = 20
        val fillCount = ((speed / 140.0) * bars).toInt().coerceIn(0, bars)

        for (i in 0 until bars) {
            val isFilled = i < fillCount
            val barColor = when {
                i < 8 -> CyberCyan
                i < 15 -> CyberPurple
                else -> CyberMagenta
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (i % 3 == 0) 10.dp else 6.dp)
                    .padding(horizontal = 1.5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isFilled) barColor else Color(0xFF1E1436))
            )
        }
    }
}

/**
 * Cut-corner sci-fi holographic telemetry card.
 */
@Composable
private fun CyberTelemetryCard(
    label: String,
    value: String,
    unit: String = "",
    subtitle: String? = null,
    accentColor: Color = CyberCyan,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(CutCornerShape(8.dp))
            .background(CyberSurface)
            .border(1.dp, CyberPurple.copy(alpha = 0.6f), CutCornerShape(8.dp))
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
                color = CyberTextMuted,
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
                        color = CyberTextMuted,
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
                    color = Color(0xFF635685),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Top Status Header Bar for Neon Cyber Cockpit.
 */
@Composable
private fun CyberHeaderBar(
    bikeName: String?,
    isGpsLost: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CutCornerShape(4.dp))
            .background(CyberSurface)
            .border(1.dp, CyberPurple.copy(alpha = 0.5f), CutCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = if (isGpsLost) CyberMagenta else CyberCyan,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = if (isGpsLost) "QUANTUM LINK SEVERED" else "HOLO-TELEMETRY ONLINE",
                color = if (isGpsLost) CyberMagenta else CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = bikeName?.uppercase(Locale.US) ?: "CYBER-01",
            color = CyberTextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun CyberHudTag(text: String) {
    Row(
        modifier = Modifier
            .clip(CutCornerShape(4.dp))
            .background(CyberSurfaceAlt)
            .border(1.dp, CyberCyan.copy(alpha = 0.4f), CutCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = CyberCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
