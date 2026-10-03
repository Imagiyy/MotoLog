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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val avgOverallSpeedDisplay = if (isMetric) stats.avgOverallSpeedKmh else stats.avgOverallSpeedKmh * 0.621371
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
                // Fullscreen Dominant GS Panoramic Speedometer
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "OVERLAND SPEED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GsGlacialCyan,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f", animatedSpeed),
                            fontSize = 82.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSpeedAlert) GsDanger else GsAlpineWhite,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = speedUnit,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GsGlacialCyan,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TRIP ${String.format(Locale.US, "%.1f", displayDistance)} $distanceUnit",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GsExpeditionGold,
                        letterSpacing = 1.sp
                    )
                }

                // Subtle Top Pull Tab indicator
                LandscapeTelemetryPullTab(
                    visible = !showLandscapeTelemetrySheet,
                    onClick = { showLandscapeTelemetrySheet = true },
                    accentColor = GsGlacialCyan,
                    backgroundColor = GsDarkBasalt,
                    borderColor = GsCordobaBlue,
                    label = "EXPEDITION DATA",
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
                    backgroundColor = GsDarkBasalt.copy(alpha = 0.96f),
                    borderColor = GsCordobaBlue,
                    accentColor = GsGlacialCyan,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    // Altimeter Banner
                    GsAltimeterCard(
                        elevationGain = elevationGainMeters,
                        isMetric = isMetric,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 6 Telemetry Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GsMetricCard(
                            title = "EXPEDITION",
                            value = movingTimeDisplay,
                            unit = "",
                            accentColor = GsGlacialCyan,
                            modifier = Modifier.weight(1f)
                        )
                        GsMetricCard(
                            title = "TOTAL TIME",
                            value = totalTimeDisplay,
                            unit = "",
                            accentColor = GsGlacialCyan,
                            modifier = Modifier.weight(1f)
                        )
                        GsMetricCard(
                            title = "MOVING AVG",
                            value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                            unit = speedUnit,
                            accentColor = GsAlpineWhite,
                            modifier = Modifier.weight(1f)
                        )
                        GsMetricCard(
                            title = "OVERALL AVG",
                            value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
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
                        GsMetricCard(
                            title = "SAT FIX",
                            value = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                            unit = "",
                            accentColor = if (isGpsLost) GsDanger else GsGlacialCyan,
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
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(46.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait GS Overland Cockpit with Proportional Sizing
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            val usableHeight = (screenHeight - 16.dp).coerceAtLeast(400.dp)
            val topBarHeight = 38.dp
            val actionButtonHeight = (usableHeight * 0.075f).coerceIn(56.dp, 64.dp)
            val availableForContent = (usableHeight - topBarHeight - actionButtonHeight).coerceAtLeast(300.dp)
            val itemSpacing = (availableForContent * 0.015f).coerceIn(5.dp, 9.dp)
            val netContentHeight = (availableForContent - itemSpacing * 6).coerceAtLeast(260.dp)

            val consoleHeight = (netContentHeight * 0.31f).coerceIn(160.dp, 240.dp)
            val odometerHeight = (netContentHeight * 0.085f).coerceIn(38.dp, 50.dp)
            val statusHeight = (netContentHeight * 0.085f).coerceIn(44.dp, 52.dp)
            val cardRowHeight = (netContentHeight * 0.173f).coerceIn(66.dp, 96.dp)

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (ThemedIdleTopBar in Idle, or Expedition Banner + Map Button when tracking)
                if (isIdle) {
                    ThemedIdleTopBar(
                        keepScreenOn = keepScreenOn,
                        onToggleKeepScreenOn = onToggleKeepScreenOn,
                        onNavigateToSettings = onNavigateToSettings,
                        palette = palette,
                        badgeText = "EXPEDITION STANDBY",
                        onSwitchToMap = onSwitchToMap
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
                                text = "GS EXPEDITION - ${bikeName?.uppercase(Locale.US) ?: "ADVENTURE"}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GsGlacialCyan,
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
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

                // GS Alpine TFT Cockpit Console (Speed + Tachometer Power Band)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(consoleHeight)
                        .border(1.5.dp, GsCordobaBlue.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GsPanelBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 10.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // GS Tachometer Power Arc / Progress Track
                        GsSpeedPowerBand(
                            speed = animatedSpeed,
                            maxScale = 160f,
                            isSpeedAlert = isSpeedAlert,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                        )

                        Text(
                            text = if (isSpeedAlert) "! SPEED WARNING !" else "ALPINE CRUISE SPEED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpeedAlert) GsDanger else GsGlacialCyan,
                            letterSpacing = 2.sp
                        )

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.0f", animatedSpeed),
                                fontSize = 58.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSpeedAlert) GsDanger else GsAlpineWhite,
                                letterSpacing = (-1.5).sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = speedUnit,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GsGlacialCyan,
                                modifier = Modifier.padding(bottom = 8.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = if (displaySpeed > 5.0) "HIGHWAY EXPEDITION CRUISE" else "ALPINE SADDLE STANDBY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            color = GsTextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Dual Expedition Console Bar: Expedition Odometer on Left, Altimeter Elevation on Right
                val displayGain = if (isMetric) elevationGainMeters else elevationGainMeters * 3.28084
                val gainUnit = if (isMetric) "m" else "ft"

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F1A24),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, GsCordobaBlue.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth().height(odometerHeight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EXPEDITION TRIP",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GsTextMuted,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", displayDistance),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GsExpeditionGold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = distanceUnit,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GsGlacialCyan,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Landscape,
                                    contentDescription = null,
                                    tint = GsGlacialCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ALTIMETER GAIN",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GsTextMuted,
                                    letterSpacing = 1.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "+%.0f", displayGain),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GsAlpineWhite,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(3.dp))
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
                    }
                }

                // Dedicated GS Satellite Status Bar (Matching Retro Pilot Lamps)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0A141E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GsCordobaBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(statusHeight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RetroJewelLamp(
                                label = "GNSS",
                                isActive = !isGpsLost,
                                color = JewelColor.BLUE,
                                size = 18.dp
                            )
                            RetroJewelLamp(
                                label = "IDL",
                                isActive = pauseState.isPaused,
                                color = JewelColor.AMBER,
                                size = 18.dp
                            )
                            RetroJewelLamp(
                                label = "ALT",
                                isActive = isSpeedAlert,
                                color = JewelColor.RED,
                                size = 18.dp,
                                shouldBlink = isSpeedAlert
                            )
                        }

                        Text(
                            text = if (isGpsLost) "GNSS SIGNAL LOST" else "ALPINE GNSS SYNC ±${accuracyMeters.toInt()}M",
                            color = GsGlacialCyan,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Expedition Telemetry - 6 Standard Metrics
                Row(
                    modifier = Modifier.fillMaxWidth().height(cardRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GsMetricCard(
                        title = "EXPEDITION TIME",
                        value = movingTimeDisplay,
                        unit = "",
                        accentColor = GsGlacialCyan,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    GsMetricCard(
                        title = "TOTAL ELAPSED",
                        value = totalTimeDisplay,
                        unit = "",
                        accentColor = GsGlacialCyan,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(cardRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GsMetricCard(
                        title = "MOVING AVG",
                        value = String.format(Locale.US, "%.1f", avgMovingSpeedDisplay),
                        unit = speedUnit,
                        accentColor = GsAlpineWhite,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    GsMetricCard(
                        title = "OVERALL AVG",
                        value = String.format(Locale.US, "%.1f", avgOverallSpeedDisplay),
                        unit = speedUnit,
                        accentColor = GsAlpineWhite,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(cardRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GsMetricCard(
                        title = "PEAK RECORD",
                        value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                        unit = speedUnit,
                        accentColor = GsAlpineWhite,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    GsMetricCard(
                        title = "SAT-LOCK FIX",
                        value = if (isGpsLost) "NO FIX" else String.format(Locale.US, "±%.0fm", accuracyMeters),
                        unit = "",
                        accentColor = if (isGpsLost) GsDanger else GsGlacialCyan,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                // Bottom Section: Action Controls
                if (isIdle) {
                    Button(
                        onClick = onStartClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GsExpeditionGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(actionButtonHeight)
                    ) {
                        Text(
                            text = "START EXPEDITION",
                            color = Color(0xFF1A1300),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(actionButtonHeight),
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
                                .fillMaxHeight()
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
                                    modifier = Modifier.size(22.dp)
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
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
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
                        text = "ALTIMETER - CLIMB",
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
                        fontSize = 22.sp,
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
 * BMW GS Style Dynamic Power / Speed Progress Ribbon with graduation ticks.
 */
@Composable
private fun GsSpeedPowerBand(
    speed: Float,
    maxScale: Float,
    isSpeedAlert: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val progress = (speed / maxScale).coerceIn(0f, 1f)

        // Background track
        drawRoundRect(
            color = GsBorder,
            size = Size(w, h),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Active speed bar
        if (progress > 0f) {
            val activeColor = if (isSpeedAlert) GsDanger else GsExpeditionGold
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(GsCordobaBlue, activeColor)
                ),
                size = Size(w * progress, h),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }

        // Graduation tick marks
        val tickCount = 16
        for (i in 1 until tickCount) {
            val x = w * (i.toFloat() / tickCount)
            drawLine(
                color = GsPanelBg,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1.5.dp.toPx()
            )
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GsTextMuted,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 19.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = unit,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GsTextMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
