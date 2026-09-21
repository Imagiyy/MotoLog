package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.nativeCanvas
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

// Cafe Racer Ace Cafe Palette Constants
private val CafeDarkBg = Color(0xFF0C130E)
private val CafeSurface = Color(0xFF142018)
private val CafeBilletOuter = Color(0xFFD2D9DE)
private val CafeBilletInner = Color(0xFF80909A)
private val CafeBritishGreen = Color(0xFF092015)
private val CafeIvory = Color(0xFFFFF9E8)
private val CafeBrass = Color(0xFFC5A059)
private val CafeCrimson = Color(0xFF9E1B1B)
private val CafeTextMuted = Color(0xFFA5B8A8)

/**
 * Ace Cafe London Smiths-Style Chronometer Cockpit Dashboard.
 * Inspired by 1960s British cafe racers (Triumph Bonneville, Norton Commando).
 * Features a polished aluminum billet clamp, Smiths chronometer dial face with aged ivory numerals,
 * mechanical roller drum odometer, and Lucas-style pilot jewel lamps.
 */
@Composable
fun CafeRacerCockpitDashboard(
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
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.CAFE_RACER)
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
                    colors = listOf(CafeDarkBg, Color(0xFF070E09), CafeDarkBg)
                )
            )
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = screenWidth > screenHeight

        if (isLandscape) {
            // Landscape Cafe Racer Cockpit
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Smiths Analog Chronometer Gauge + Roller Drum
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val dialSize = (screenHeight * 0.72f).coerceAtMost(screenWidth * 0.45f)
                    CafeSmithsChronometerDial(
                        currentSpeed = displaySpeed,
                        isMetric = isMetric,
                        isSpeedAlert = isSpeedAlert,
                        modifier = Modifier.size(dialSize)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    RetroOdometerDrum(
                        distanceValue = displayDistance,
                        unitLabel = distanceUnit,
                        palette = palette
                    )
                }

                // Right Column: Ace Cafe Instrument Cluster + Controls
                Column(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Plaque & Map Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CafePlaque(bikeName = bikeName ?: "TON-UP SPECIAL")

                        Surface(
                            onClick = onSwitchToMap,
                            shape = RoundedCornerShape(8.dp),
                            color = CafeSurface,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, CafeBrass),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Pocket Map",
                                    tint = CafeBrass,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ROAD MAP",
                                    color = CafeIvory,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }

                    // Pilot Warning Lamps Bar
                    CafePilotLampBar(
                        isGpsLost = isGpsLost,
                        pauseState = pauseState,
                        isSpeedAlert = isSpeedAlert,
                        accuracyMeters = accuracyMeters,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2x2 Instrument Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetroInstrumentCard(
                            label = if (showOverallStats) "Elapsed Time" else "Moving Time",
                            value = timeDisplay,
                            subtitle = if (showOverallStats) "Overall (tap)" else "Moving (tap)",
                            palette = palette,
                            modifier = Modifier.weight(1f),
                            onClick = { showOverallStats = !showOverallStats }
                        )

                        RetroInstrumentCard(
                            label = if (showOverallStats) "Overall Avg" else "Moving Avg",
                            value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                            unit = speedUnit,
                            subtitle = if (showOverallStats) "Overall (tap)" else "Moving (tap)",
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
                            label = "Peak Speed",
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            subtitle = "Ace Top Mark",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )

                        RetroInstrumentCard(
                            label = "GPS Fix",
                            value = if (isGpsLost) "--" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                            subtitle = if (isGpsLost) "Signal Lost" else "Satellite Fix",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Cafe Racer Controls Row (56dp min)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (pauseState.isPaused) onResumeClick() else onPauseClick()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (pauseState.isPaused) CafeBrass.copy(alpha = 0.2f) else CafeSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (pauseState.isPaused) CafeBrass else CafeBilletInner
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
                                    tint = if (pauseState.isPaused) CafeBrass else CafeIvory,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pauseState.isPaused) "RESUME" else "IDLE PAUSE",
                                    color = if (pauseState.isPaused) CafeBrass else CafeIvory,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }

                        ThemedHoldToStopButton(
                            onStopConfirmed = onStopConfirmed,
                            label = "MAGNETO CUT",
                            progressLabel = "CUTTING",
                            borderColor = CafeBrass,
                            gradientColors = listOf(Color(0xFF4A1010), Color(0xFF200505)),
                            progressFillColor = CafeCrimson.copy(alpha = 0.6f),
                            textColor = CafeIvory,
                            cornerRadius = 8.dp,
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        } else {
            // Portrait Cafe Racer Cockpit Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Plaque & Map Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CafePlaque(bikeName = bikeName ?: "TON-UP SPECIAL")

                    Surface(
                        onClick = onSwitchToMap,
                        shape = RoundedCornerShape(8.dp),
                        color = CafeSurface,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CafeBrass),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Pocket Map",
                                tint = CafeBrass,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ROAD MAP",
                                color = CafeIvory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }
                }

                // Smiths Analog Chronometer Gauge
                val dialSize = (screenWidth * 0.75f).coerceAtMost(300.dp)
                CafeSmithsChronometerDial(
                    currentSpeed = displaySpeed,
                    isMetric = isMetric,
                    isSpeedAlert = isSpeedAlert,
                    modifier = Modifier.size(dialSize)
                )

                // Mechanical Roller Drum Odometer
                RetroOdometerDrum(
                    distanceValue = displayDistance,
                    unitLabel = distanceUnit,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                // Pilot Warning Lamps
                CafePilotLampBar(
                    isGpsLost = isGpsLost,
                    pauseState = pauseState,
                    isSpeedAlert = isSpeedAlert,
                    accuracyMeters = accuracyMeters,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2x2 Telemetry Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RetroInstrumentCard(
                        label = if (showOverallStats) "Elapsed Time" else "Moving Time",
                        value = timeDisplay,
                        subtitle = if (showOverallStats) "Overall (tap)" else "Moving (tap)",
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { showOverallStats = !showOverallStats }
                    )

                    RetroInstrumentCard(
                        label = if (showOverallStats) "Overall Avg" else "Moving Avg",
                        value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                        unit = speedUnit,
                        subtitle = if (showOverallStats) "Overall (tap)" else "Moving (tap)",
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
                        label = "Peak Speed",
                        value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                        unit = speedUnit,
                        subtitle = "Ace Top Mark",
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )

                    RetroInstrumentCard(
                        label = "GPS Fix",
                        value = if (isGpsLost) "--" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                        subtitle = if (isGpsLost) "Signal Lost" else "Satellite Fix",
                        palette = palette,
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
                        shape = RoundedCornerShape(8.dp),
                        color = if (pauseState.isPaused) CafeBrass.copy(alpha = 0.2f) else CafeSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (pauseState.isPaused) CafeBrass else CafeBilletInner
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
                                tint = if (pauseState.isPaused) CafeBrass else CafeIvory,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pauseState.isPaused) "RESUME" else "IDLE PAUSE",
                                color = if (pauseState.isPaused) CafeBrass else CafeIvory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }

                    ThemedHoldToStopButton(
                        onStopConfirmed = onStopConfirmed,
                        label = "MAGNETO CUT",
                        progressLabel = "CUTTING",
                        borderColor = CafeBrass,
                        gradientColors = listOf(Color(0xFF4A1010), Color(0xFF200505)),
                        progressFillColor = CafeCrimson.copy(alpha = 0.6f),
                        textColor = CafeIvory,
                        cornerRadius = 8.dp,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

/**
 * 1960s Smiths Chronometer Speedometer Dial with polished aluminum rim,
 * British Racing Green face, and aged ivory numerals.
 */
@Composable
private fun CafeSmithsChronometerDial(
    currentSpeed: Double,
    isMetric: Boolean,
    isSpeedAlert: Boolean,
    modifier: Modifier = Modifier
) {
    val maxDialSpeed = if (isMetric) 180.0 else 120.0
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.toFloat().coerceIn(0f, maxDialSpeed.toFloat()),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "cafeNeedle"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension / 2f
        val bezelThickness = outerRadius * 0.11f
        val innerRadius = outerRadius - bezelThickness

        // 1. Polished Billet Aluminum Clamp Outer Rim
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    CafeBilletOuter,
                    CafeBilletInner,
                    CafeBilletOuter,
                    Color(0xFF6A7780),
                    CafeBilletOuter
                )
            ),
            radius = outerRadius,
            center = center
        )

        // 2. Knurled Bezel Edge Groove
        drawCircle(
            color = Color(0xFF20262B),
            radius = outerRadius - 2.dp.toPx(),
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 3. Inner Brass Bezel Accent Ring
        drawCircle(
            color = CafeBrass,
            radius = innerRadius + 1.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // 4. British Racing Green Dial Face with subtle radial depth
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0F3020),
                    CafeBritishGreen,
                    Color(0xFF040F08)
                ),
                center = center,
                radius = innerRadius
            ),
            radius = innerRadius,
            center = center
        )

        // 5. Smiths Chronometer Scale (140° to 400° -> 260° sweep)
        val startAngle = 140f
        val sweepAngle = 260f
        val step = if (isMetric) 20 else 10
        val numTicks = (maxDialSpeed / step).toInt()

        for (i in 0..numTicks) {
            val tickFraction = i.toFloat() / numTicks.toFloat()
            val tickAngle = startAngle + (tickFraction * sweepAngle)
            val tickAngleRad = Math.toRadians(tickAngle.toDouble())

            val tickOuter = innerRadius - 6.dp.toPx()
            val tickInner = innerRadius - 16.dp.toPx()

            val startX = center.x + tickInner * cos(tickAngleRad).toFloat()
            val startY = center.y + tickInner * sin(tickAngleRad).toFloat()
            val endX = center.x + tickOuter * cos(tickAngleRad).toFloat()
            val endY = center.y + tickOuter * sin(tickAngleRad).toFloat()

            // Major Tick Mark
            drawLine(
                color = CafeIvory,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.5.dp.toPx()
            )

            // Minor subdivision ticks between major ticks
            if (i < numTicks) {
                for (sub in 1..4) {
                    val subFraction = (i + sub / 5f) / numTicks.toFloat()
                    val subAngle = startAngle + (subFraction * sweepAngle)
                    val subRad = Math.toRadians(subAngle.toDouble())

                    val subOuter = innerRadius - 6.dp.toPx()
                    val subInner = innerRadius - 11.dp.toPx()

                    drawLine(
                        color = CafeIvory.copy(alpha = 0.6f),
                        start = Offset(
                            center.x + subInner * cos(subRad).toFloat(),
                            center.y + subInner * sin(subRad).toFloat()
                        ),
                        end = Offset(
                            center.x + subOuter * cos(subRad).toFloat(),
                            center.y + subOuter * sin(subRad).toFloat()
                        ),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }

            // Dial Numerals
            val labelRadius = innerRadius - 26.dp.toPx()
            val lx = center.x + labelRadius * cos(tickAngleRad).toFloat()
            val ly = center.y + labelRadius * sin(tickAngleRad).toFloat()
            val labelText = (i * step).toString()

            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(255, 249, 232)
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.SERIF
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                lx,
                ly + (textPaint.textSize / 3f),
                textPaint
            )
        }

        // 6. Smiths Script & Unit Branding
        val scriptPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(197, 160, 89)
            textSize = 11.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            "SMITHS",
            center.x,
            center.y - (innerRadius * 0.35f),
            scriptPaint
        )

        val unitPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(165, 184, 168)
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            if (isMetric) "KM/H" else "M.P.H.",
            center.x,
            center.y - (innerRadius * 0.22f),
            unitPaint
        )

        // 7. Large Digital Readout in lower sector
        val digitalSpeed = String.format(Locale.US, "%.0f", currentSpeed)
        val digitalPaint = android.graphics.Paint().apply {
            color = if (isSpeedAlert) android.graphics.Color.RED else android.graphics.Color.rgb(255, 249, 232)
            textSize = 28.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            digitalSpeed,
            center.x,
            center.y + (innerRadius * 0.48f),
            digitalPaint
        )

        // 8. Smiths Pointer Needle with Pear-Drop Tail
        val speedFraction = (animatedSpeed / maxDialSpeed.toFloat()).coerceIn(0f, 1f)
        val needleAngle = startAngle + (speedFraction * sweepAngle)
        val needleAngleRad = Math.toRadians(needleAngle.toDouble())

        val needleTipRadius = innerRadius - 10.dp.toPx()
        val tipX = center.x + needleTipRadius * cos(needleAngleRad).toFloat()
        val tipY = center.y + needleTipRadius * sin(needleAngleRad).toFloat()

        val tailRadius = innerRadius * 0.22f
        val tailAngleRad = Math.toRadians(needleAngle + 180.0)
        val tailX = center.x + tailRadius * cos(tailAngleRad).toFloat()
        val tailY = center.y + tailRadius * sin(tailAngleRad).toFloat()

        // Needle Line
        drawLine(
            color = if (isSpeedAlert) Color.Red else CafeIvory,
            start = Offset(tailX, tailY),
            end = Offset(tipX, tipY),
            strokeWidth = 2.8.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 9. Central Brass Knurled Dome Nut
        drawCircle(
            color = CafeBrass,
            radius = 10.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color(0xFF5A441A),
            radius = 5.dp.toPx(),
            center = center
        )
    }
}

/**
 * Lucas-style Pilot Jewel Lamps row.
 */
@Composable
private fun CafePilotLampBar(
    isGpsLost: Boolean,
    pauseState: PauseState,
    isSpeedAlert: Boolean,
    accuracyMeters: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CafeSurface)
            .border(1.dp, CafeBrass.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pilot Jewels
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Emerald Jewel (GPS)
            RetroJewelLamp(
                label = "IGN",
                isActive = !isGpsLost,
                color = JewelColor.GREEN
            )

            // Amber Jewel (Pause)
            RetroJewelLamp(
                label = "IDL",
                isActive = pauseState.isPaused,
                color = JewelColor.AMBER
            )

            // Ruby Jewel (Speed Alert)
            RetroJewelLamp(
                label = "ALT",
                isActive = isSpeedAlert,
                color = JewelColor.RED,
                shouldBlink = isSpeedAlert
            )
        }

        Text(
            text = if (isGpsLost) "NO GPS FIX" else "CHRONO SYNC ±${accuracyMeters.toInt()}M",
            color = CafeTextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Serif
        )
    }
}

@Composable
private fun CafePlaque(bikeName: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1B2A1E))
            .border(1.dp, CafeBrass, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(CafeBrass)
        )
        Text(
            text = bikeName.uppercase(Locale.US),
            color = CafeIvory,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            letterSpacing = 1.sp
        )
    }
}
