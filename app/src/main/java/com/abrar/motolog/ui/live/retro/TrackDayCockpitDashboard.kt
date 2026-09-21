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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontStyle
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

// Track Day Race Palette Constants
private val RaceBg = Color(0xFF0C0D11)
private val RaceSurface = Color(0xFF141720)
private val RaceCarbonDark = Color(0xFF1A1D27)
private val RaceRed = Color(0xFFE10600)
private val RaceRedGlow = Color(0xFFFF1744)
private val RaceGreen = Color(0xFF00E676)
private val RaceAmber = Color(0xFFFF9100)
private val RaceCyan = Color(0xFF00E5FF)
private val RaceWhite = Color(0xFFF5F5F7)
private val RaceGray = Color(0xFF757D8A)

/**
 * Superbike Panoramic TFT Race Cockpit Dashboard.
 * Inspired by modern MotoGP and WorldSBK telemetry displays (Ducati Panigale, Yamaha R1).
 * Features a dynamic LED shift-light bar, giant bold racing typography, carbon fiber accents,
 * and high-visibility race telemetry cards.
 */
@Composable
fun TrackDayCockpitDashboard(
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
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.TRACK_DAY)
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
                Brush.verticalGradient(
                    colors = listOf(RaceBg, Color(0xFF10121A), RaceBg)
                )
            )
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = screenWidth > screenHeight

        if (isLandscape) {
            // Landscape Racing Cockpit Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Shift Lights + Big Racing Speedometer + Tachometer Arc
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Shift Light Array
                    TrackShiftLightBar(
                        currentSpeed = speedKmh,
                        isSpeedAlert = isSpeedAlert,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )

                    // Racing Speed Display with Tachometer
                    SuperbikeSpeedCluster(
                        speed = displaySpeed,
                        unit = speedUnit,
                        distance = displayDistance,
                        distanceUnit = distanceUnit,
                        isSpeedAlert = isSpeedAlert,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    // Track Status Banner
                    TrackStatusBar(
                        bikeName = bikeName,
                        pauseState = pauseState,
                        isGpsLost = isGpsLost,
                        accuracyMeters = accuracyMeters,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Right Column: Race Telemetry Cards + Pit Controls
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Map & Race Mode Switcher Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RaceBadge(text = "RACE TELEMETRY // TRACK MODE")

                        Surface(
                            onClick = onSwitchToMap,
                            shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
                            color = RaceCarbonDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RaceRed),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Track Map",
                                    tint = RaceRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "CIRCUIT MAP",
                                    color = RaceWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 2x2 Telemetry Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrackTelemetryCard(
                            label = if (showOverallStats) "ELAPSED TIME" else "SESSION TIME",
                            value = timeDisplay,
                            subtitle = if (showOverallStats) "OVERALL" else "MOVING",
                            accentColor = RaceCyan,
                            modifier = Modifier.weight(1f),
                            onClick = { showOverallStats = !showOverallStats }
                        )

                        TrackTelemetryCard(
                            label = if (showOverallStats) "OVERALL AVG" else "MOVING AVG",
                            value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                            unit = speedUnit,
                            subtitle = if (showOverallStats) "OVERALL" else "MOVING",
                            accentColor = RaceGreen,
                            modifier = Modifier.weight(1f),
                            onClick = { showOverallStats = !showOverallStats }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrackTelemetryCard(
                            label = "V-MAX (PEAK)",
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            subtitle = "TOP SPEED",
                            accentColor = RaceRed,
                            modifier = Modifier.weight(1f)
                        )

                        TrackTelemetryCard(
                            label = "GPS ACCURACY",
                            value = if (isGpsLost) "--" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                            subtitle = if (isGpsLost) "NO FIX" else "HIGH LOCK",
                            accentColor = if (isGpsLost) RaceRed else RaceAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Pit Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pit Pause Button (56dp min)
                        Surface(
                            onClick = {
                                if (pauseState.isPaused) onResumeClick() else onPauseClick()
                            },
                            shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                            color = if (pauseState.isPaused) RaceAmber.copy(alpha = 0.2f) else RaceCarbonDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (pauseState.isPaused) RaceAmber else RaceGray
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
                                    tint = if (pauseState.isPaused) RaceAmber else RaceWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pauseState.isPaused) "RESUME" else "PIT BOX",
                                    color = if (pauseState.isPaused) RaceAmber else RaceWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Hold to Stop / Kill Switch (56dp min)
                        ThemedHoldToStopButton(
                            onStopConfirmed = onStopConfirmed,
                            label = "KILL SWITCH",
                            progressLabel = "DISARM",
                            borderColor = RaceRed,
                            gradientColors = listOf(Color(0xFF660000), Color(0xFF2B0000)),
                            progressFillColor = RaceRedGlow.copy(alpha = 0.6f),
                            textColor = RaceWhite,
                            cornerRadius = 8.dp,
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        } else {
            // Portrait Racing Cockpit Layout (Scrollable for smaller screens)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Shift Light Strip
                TrackShiftLightBar(
                    currentSpeed = speedKmh,
                    isSpeedAlert = isSpeedAlert,
                    modifier = Modifier.fillMaxWidth()
                )

                // Header with Bike info and Map Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RaceBadge(text = "RACE TELEMETRY // ${bikeName?.uppercase(Locale.US) ?: "PANIGALE V4"}")

                    Surface(
                        onClick = onSwitchToMap,
                        shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
                        color = RaceCarbonDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaceRed),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Track Map",
                                tint = RaceRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CIRCUIT MAP",
                                color = RaceWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Superbike Speed Cluster
                SuperbikeSpeedCluster(
                    speed = displaySpeed,
                    unit = speedUnit,
                    distance = displayDistance,
                    distanceUnit = distanceUnit,
                    isSpeedAlert = isSpeedAlert,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )

                // Track Status Banner
                TrackStatusBar(
                    bikeName = bikeName,
                    pauseState = pauseState,
                    isGpsLost = isGpsLost,
                    accuracyMeters = accuracyMeters,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2x2 Telemetry Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrackTelemetryCard(
                        label = if (showOverallStats) "ELAPSED TIME" else "SESSION TIME",
                        value = timeDisplay,
                        subtitle = if (showOverallStats) "OVERALL" else "MOVING",
                        accentColor = RaceCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { showOverallStats = !showOverallStats }
                    )

                    TrackTelemetryCard(
                        label = if (showOverallStats) "OVERALL AVG" else "MOVING AVG",
                        value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                        unit = speedUnit,
                        subtitle = if (showOverallStats) "OVERALL" else "MOVING",
                        accentColor = RaceGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { showOverallStats = !showOverallStats }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrackTelemetryCard(
                        label = "V-MAX (PEAK)",
                        value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                        unit = speedUnit,
                        subtitle = "TOP SPEED",
                        accentColor = RaceRed,
                        modifier = Modifier.weight(1f)
                    )

                    TrackTelemetryCard(
                        label = "GPS ACCURACY",
                        value = if (isGpsLost) "--" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                        subtitle = if (isGpsLost) "NO FIX" else "HIGH LOCK",
                        accentColor = if (isGpsLost) RaceRed else RaceAmber,
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
                        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        color = if (pauseState.isPaused) RaceAmber.copy(alpha = 0.2f) else RaceCarbonDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (pauseState.isPaused) RaceAmber else RaceGray
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
                                tint = if (pauseState.isPaused) RaceAmber else RaceWhite,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pauseState.isPaused) "RESUME" else "PIT BOX",
                                color = if (pauseState.isPaused) RaceAmber else RaceWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    ThemedHoldToStopButton(
                        onStopConfirmed = onStopConfirmed,
                        label = "KILL SWITCH",
                        progressLabel = "DISARM",
                        borderColor = RaceRed,
                        gradientColors = listOf(Color(0xFF660000), Color(0xFF2B0000)),
                        progressFillColor = RaceRedGlow.copy(alpha = 0.6f),
                        textColor = RaceWhite,
                        cornerRadius = 8.dp,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

/**
 * High-intensity 12-Segment LED Shift Light Strip.
 * Segments 1-5: Green, 6-9: Amber, 10-12: Superbike Crimson Red.
 * Flashes vigorously at peak speed / speed alert.
 */
@Composable
private fun TrackShiftLightBar(
    currentSpeed: Double,
    isSpeedAlert: Boolean,
    modifier: Modifier = Modifier
) {
    val totalLeds = 12
    // Scales dynamically: 0 to 120 km/h fills the 12 LEDs
    val activeCount = ((currentSpeed / 120.0) * totalLeds).toInt().coerceIn(0, totalLeds)

    val infiniteTransition = rememberInfiniteTransition(label = "shiftFlash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashAlpha"
    )

    Row(
        modifier = modifier
            .clip(CutCornerShape(4.dp))
            .background(RaceCarbonDark)
            .border(1.dp, RaceGray.copy(alpha = 0.3f), CutCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalLeds) {
            val isActive = i < activeCount
            val baseColor = when {
                i < 5 -> RaceGreen
                i < 9 -> RaceAmber
                else -> RaceRedGlow
            }

            val isFlashing = (isSpeedAlert || activeCount >= 11) && i >= 9
            val color = if (isActive) {
                if (isFlashing) baseColor.copy(alpha = flashAlpha) else baseColor
            } else {
                Color(0xFF222633)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
                    .then(
                        if (isActive) Modifier.border(
                            0.5.dp,
                            baseColor.copy(alpha = 0.8f),
                            RoundedCornerShape(2.dp)
                        ) else Modifier
                    )
            )
        }
    }
}

/**
 * Superbike Digital Speedometer Cluster with horizontal tachometer bar and lap odometer.
 */
@Composable
private fun SuperbikeSpeedCluster(
    speed: Double,
    unit: String,
    distance: Double,
    distanceUnit: String,
    isSpeedAlert: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF161922), Color(0xFF0F1118), Color(0xFF181C26))
                )
            )
            .border(
                1.5.dp,
                if (isSpeedAlert) RaceRedGlow else RaceRed.copy(alpha = 0.6f),
                CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
            )
            .padding(12.dp)
    ) {
        // Subtle race grid lines canvas in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 20.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = Color(0xFF202534).copy(alpha = 0.35f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 0.8f
                )
                x += step
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Speedometer Center Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "V-SPEED",
                    color = RaceGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = if (isSpeedAlert) "⚠ SPEED LIMIT EXCEEDED" else "GEAR D // TRACTION ON",
                    color = if (isSpeedAlert) RaceRedGlow else RaceGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Giant Superbike Speed Digits
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.US, "%.0f", speed),
                    color = if (isSpeedAlert) RaceRedGlow else RaceWhite,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-2).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = unit,
                        color = RaceRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Horizontal Segmented Dynamic Tachometer Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                ) {
                    val segments = 24
                    val gap = 3.dp.toPx()
                    val segWidth = (size.width - (segments - 1) * gap) / segments
                    val speedRatio = (speed / 180.0).coerceIn(0.0, 1.0).toFloat()
                    val activeSegments = (speedRatio * segments).toInt()

                    for (s in 0 until segments) {
                        val left = s * (segWidth + gap)
                        val segColor = when {
                            s >= activeSegments -> Color(0xFF222838)
                            s < 14 -> RaceGreen
                            s < 20 -> RaceAmber
                            else -> RaceRedGlow
                        }
                        drawRect(
                            color = segColor,
                            topLeft = Offset(left, 0f),
                            size = Size(segWidth, size.height)
                        )
                    }
                }

                // Tachometer RPM/KMH Scale markings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0", color = RaceGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("40", color = RaceGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("80", color = RaceGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("120", color = RaceGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("160+", color = RaceRed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Lap / Trip Odometer Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(4.dp))
                    .background(Color(0xFF10131B))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRIP DISTANCE",
                    color = RaceGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = String.format(Locale.US, "%.2f %s", distance, distanceUnit),
                    color = RaceWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Race Telemetry HUD Card with chamfered corners and carbon-weave styling.
 */
@Composable
private fun TrackTelemetryCard(
    label: String,
    value: String,
    unit: String = "",
    subtitle: String? = null,
    accentColor: Color = RaceCyan,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(CutCornerShape(6.dp))
            .background(RaceSurface)
            .border(1.dp, Color(0xFF262C3D), CutCornerShape(6.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = RaceGray,
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
                        color = RaceGray,
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
                    color = Color(0xFF555E6D),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Track Status Bar showing bike name, pause state, and GPS fix state.
 */
@Composable
private fun TrackStatusBar(
    bikeName: String?,
    pauseState: PauseState,
    isGpsLost: Boolean,
    accuracyMeters: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CutCornerShape(4.dp))
            .background(Color(0xFF10131B))
            .border(1.dp, Color(0xFF222736), CutCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status State
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isGpsLost -> RaceRed
                            pauseState.isPaused -> RaceAmber
                            else -> RaceGreen
                        }
                    )
            )

            Text(
                text = when {
                    isGpsLost -> "GPS LOST // SEARCHING"
                    pauseState.isPaused -> "PIT PAUSED"
                    else -> "LIVE TELEMETRY ACTIVE"
                },
                color = when {
                    isGpsLost -> RaceRed
                    pauseState.isPaused -> RaceAmber
                    else -> RaceWhite
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Signal Lock
        Text(
            text = if (isGpsLost) "NO FIX" else "RTK FIX ±${accuracyMeters.toInt()}M",
            color = if (isGpsLost) RaceRed else RaceGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RaceBadge(text: String) {
    Row(
        modifier = Modifier
            .clip(CutCornerShape(4.dp))
            .background(Color(0xFF220808))
            .border(1.dp, RaceRed.copy(alpha = 0.5f), CutCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(RaceRed)
        )
        Text(
            text = text,
            color = RaceWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
