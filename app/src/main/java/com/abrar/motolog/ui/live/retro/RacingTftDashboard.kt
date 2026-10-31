package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

// ============================================================
// AiM MXS Racing TFT Color Constants
// ============================================================
private val TftBackground = Color(0xFF030508)
private val TftBezelBg = Color(0xFF0D0F14)
private val TftBezelBorder = Color(0xFF1E222D)
private val TftSurface = Color(0xFF090C12)
private val TftCyan = Color(0xFF00D4E5)
private val TftCyanDim = Color(0xFF005860)
private val TftGreen = Color(0xFF00E676)
private val TftYellow = Color(0xFFFFEA00)
private val TftOrange = Color(0xFFFF9100)
private val TftRed = Color(0xFFFF1744)
private val TftWhite = Color(0xFFF5F5F7)
private val TftGray = Color(0xFF6B7280)
private val TftDimGray = Color(0xFF232730)
private val TftBlue = Color(0xFF2979FF)

/**
 * Chamfered bezel shape matching physical AiM MXS / GS-Dash displays.
 */
val ChamferedBezelShape = GenericShape { size, _ ->
    val chamfer = min(size.width, size.height) * 0.08f
    moveTo(chamfer, 0f)
    lineTo(size.width - chamfer, 0f)
    lineTo(size.width, chamfer)
    lineTo(size.width, size.height - chamfer)
    lineTo(size.width - chamfer, size.height)
    lineTo(chamfer, size.height)
    lineTo(0f, size.height - chamfer)
    lineTo(0f, chamfer)
    close()
}

/**
 * AiM MXS Racing TFT Dashboard for MotoLog.
 *
 * Recreates the AiM MXS motorcycle racing data logger display:
 * - Chamfered matte black casing with physical button markings
 * - Top LED shift light array (10 segmented LEDs) flanked by green turn indicators
 * - Left status indicator stack: TC, High Beam, Battery, Oil, ABS
 * - Right warning lamps: High Beam, Engine MIL, Warning
 * - Circular cyan segmented live speed sweep arc
 * - Dominant central digital Speed readout (KM/H or MPH)
 * - Telemetry fields: ODOMETRO TOTALE KM, TRIP A KM, TRIP B KM
 * - Instant Fuel Consumption / Power segmented bar gauge (0..50 KM/L)
 * - Lap / Split indicator and Ambient air temperature readout
 * - Bottom strip: Fuel gauge with pump icon, Coolant temp, Intake temp,
 *   GPS health status, bike name / status info line, and live date/clock.
 */
/**
 * Dynamic custom colors for the Cockpit Dashboard.
 */
data class DashboardCustomColors(
    val textColor: Color = Color(0xFFFFFFFF),
    val backgroundColor: Color = Color(0xFF030508),
    val accentColor: Color = Color(0xFF00D4E5),
    val componentColor: Color = Color(0xFF0D0F14)
)

data class BatteryState(
    val level: Int = 100,
    val isCharging: Boolean = false
)

@Composable
fun rememberBatteryState(): BatteryState {
    val context = LocalContext.current
    var batteryState by remember {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        val initialLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val initialScale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val initialStatus = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = initialStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            initialStatus == BatteryManager.BATTERY_STATUS_FULL
        val pct = if (initialLevel >= 0 && initialScale > 0) {
            ((initialLevel * 100f) / initialScale).roundToInt()
        } else {
            100
        }
        mutableStateOf(BatteryState(pct, isCharging))
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    val pct = if (level >= 0 && scale > 0) {
                        ((level * 100f) / scale).roundToInt()
                    } else {
                        100
                    }
                    batteryState = BatteryState(pct, isCharging)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    return batteryState
}

data class CompassHeading(
    val degrees: Int = 0,
    val cardinal: String = "N"
)

private fun degreesToCardinal(degrees: Int): String {
    val normalized = (degrees % 360 + 360) % 360
    return when {
        normalized >= 338 || normalized < 23 -> "N"
        normalized < 68 -> "NE"
        normalized < 113 -> "E"
        normalized < 158 -> "SE"
        normalized < 203 -> "S"
        normalized < 248 -> "SW"
        normalized < 293 -> "W"
        else -> "NW"
    }
}

@Composable
fun rememberCompassHeading(
    bearingDegrees: Float?,
    speedKmh: Double
): CompassHeading {
    val context = LocalContext.current
    var sensorDegrees by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (sensorManager == null || rotationSensor == null) {
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val deg = ((azimuth + 360) % 360).roundToInt()
                    sensorDegrees = deg
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val effectiveDegrees = if (speedKmh > 3.0 && bearingDegrees != null) {
        ((bearingDegrees.roundToInt() % 360 + 360) % 360)
    } else {
        sensorDegrees
    }

    return CompassHeading(
        degrees = effectiveDegrees,
        cardinal = degreesToCardinal(effectiveDegrees)
    )
}

@Composable
fun BatteryIndicator(
    batteryState: BatteryState,
    textColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val icon = when {
        batteryState.isCharging -> Icons.Default.BatteryChargingFull
        batteryState.level >= 80 -> Icons.Default.BatteryFull
        batteryState.level >= 50 -> Icons.Default.Battery5Bar
        batteryState.level >= 20 -> Icons.Default.Battery4Bar
        else -> Icons.Default.Battery2Bar
    }
    val iconTint = when {
        batteryState.isCharging -> TftGreen
        batteryState.level <= 15 -> TftRed
        batteryState.level <= 30 -> TftYellow
        else -> accentColor
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Battery ${batteryState.level}%",
            tint = iconTint,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = "${batteryState.level}%",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * AiM MXS Racing TFT Dashboard for MotoLog.
 *
 * Full responsive compatibility for both Landscape and Portrait orientations.
 */
@Composable
fun RacingTftDashboard(
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
    altitudeMeters: Double? = null,
    bearingDegrees: Float? = null,
    onStartClick: () -> Unit = {},
    keepScreenOn: Boolean = false,
    onToggleKeepScreenOn: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDestination: () -> Unit = {},
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RACING_TFT),
    customColors: DashboardCustomColors = DashboardCustomColors()
) {
    // Live clock ticker
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy hh:mm a", Locale.US)
        while (true) {
            currentTimeString = dateFormat.format(Date()).uppercase()
            delay(1000)
        }
    }

    val batteryState = rememberBatteryState()
    val compass = rememberCompassHeading(bearingDegrees = bearingDegrees, speedKmh = speedKmh)
    val altitudeDisplay = remember(altitudeMeters, isMetric) {
        if (altitudeMeters != null) {
            if (isMetric) {
                "ALT ${altitudeMeters.roundToInt()} M"
            } else {
                val ft = (altitudeMeters * 3.28084).roundToInt()
                "ALT $ft FT"
            }
        } else {
            "ALT --"
        }
    }
    val headingDisplay = remember(compass) { "HDG ${compass.cardinal} ${compass.degrees}°" }

    val totalDistanceKm = stats.totalDistanceMeters / 1000.0
    val displaySpeed = if (isMetric) speedKmh else speedKmh * 0.621371
    val displayDistance = if (isMetric) totalDistanceKm else totalDistanceKm * 0.621371
    val speedUnit = if (isMetric) "KM/H" else "MPH"
    val distanceUnit = if (isMetric) "KM" else "MI"
    val maxScale = if (isMetric) 240.0 else 150.0

    val avgMovingSpeedDisplay = if (isMetric) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
    val maxSpeedDisplay = if (isMetric) stats.maxSpeedKmh else stats.maxSpeedKmh * 0.621371
    val totalElapsedTimeDisplay = remember(stats.elapsedTimeMs) {
        CockpitUtils.formatDurationMs(stats.elapsedTimeMs)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(customColors.backgroundColor)
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            TftLandscapeView(
                displaySpeed = displaySpeed,
                speedUnit = speedUnit,
                displayDistance = displayDistance,
                distanceUnit = distanceUnit,
                avgSpeedDisplay = avgMovingSpeedDisplay,
                maxSpeedDisplay = maxSpeedDisplay,
                timeDisplay = totalElapsedTimeDisplay,
                speedKmh = speedKmh,
                maxScale = maxScale,
                currentTimeString = currentTimeString,
                isSpeedAlert = isSpeedAlert,
                isGpsLost = isGpsLost,
                pauseState = pauseState,
                bikeName = bikeName,
                accuracyMeters = accuracyMeters,
                isMetric = isMetric,
                isIdle = isIdle,
                keepScreenOn = keepScreenOn,
                onToggleKeepScreenOn = onToggleKeepScreenOn,
                onNavigateToSettings = onNavigateToSettings,
                onStartClick = onStartClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onStopConfirmed = onStopConfirmed,
                onSwitchToMap = onSwitchToMap,
                onNavigateToDestination = onNavigateToDestination,
                palette = palette,
                customColors = customColors,
                batteryState = batteryState,
                altitudeDisplay = altitudeDisplay,
                headingDisplay = headingDisplay
            )
        } else {
            TftPortraitView(
                displaySpeed = displaySpeed,
                speedUnit = speedUnit,
                displayDistance = displayDistance,
                distanceUnit = distanceUnit,
                avgSpeedDisplay = avgMovingSpeedDisplay,
                maxSpeedDisplay = maxSpeedDisplay,
                timeDisplay = totalElapsedTimeDisplay,
                speedKmh = speedKmh,
                maxScale = maxScale,
                currentTimeString = currentTimeString,
                isSpeedAlert = isSpeedAlert,
                isGpsLost = isGpsLost,
                pauseState = pauseState,
                bikeName = bikeName,
                accuracyMeters = accuracyMeters,
                isMetric = isMetric,
                isIdle = isIdle,
                keepScreenOn = keepScreenOn,
                onToggleKeepScreenOn = onToggleKeepScreenOn,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToDestination = onNavigateToDestination,
                onStartClick = onStartClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onStopConfirmed = onStopConfirmed,
                onSwitchToMap = onSwitchToMap,
                palette = palette,
                customColors = customColors,
                batteryState = batteryState,
                altitudeDisplay = altitudeDisplay,
                headingDisplay = headingDisplay
            )
        }
    }
}

// ============================================================
// LANDSCAPE VIEW (AiM Casing + Large Speed Center Arc)
// ============================================================
@Composable
private fun TftLandscapeView(
    displaySpeed: Double,
    speedUnit: String,
    displayDistance: Double,
    distanceUnit: String,
    avgSpeedDisplay: Double,
    maxSpeedDisplay: Double,
    timeDisplay: String,
    speedKmh: Double,
    maxScale: Double,
    currentTimeString: String,
    isSpeedAlert: Boolean,
    isGpsLost: Boolean,
    pauseState: PauseState,
    bikeName: String?,
    accuracyMeters: Float,
    isMetric: Boolean,
    isIdle: Boolean,
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopConfirmed: () -> Unit,
    onSwitchToMap: () -> Unit,
    onNavigateToDestination: () -> Unit = {},
    palette: CockpitThemePalette,
    customColors: DashboardCustomColors,
    batteryState: BatteryState,
    altitudeDisplay: String,
    headingDisplay: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(customColors.backgroundColor)
            .border(2.dp, customColors.accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // MAIN INNER SCREEN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(customColors.componentColor)
                    .border(1.dp, customColors.accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT BEZEL COLUMN: Indicator Lamps Stack & Buttons
                TftLeftIndicatorColumn(
                    isGpsLost = isGpsLost,
                    pauseState = pauseState,
                    modifier = Modifier
                        .width(42.dp)
                        .fillMaxHeight(),
                    textColor = customColors.textColor.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // CENTER-LEFT: Speed Arc Gauge with Large Digital Speedometer at Center
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    val gaugeSize = minOf(maxWidth, maxHeight) * 0.98f

                    Box(
                        modifier = Modifier.size(gaugeSize),
                        contentAlignment = Alignment.Center
                    ) {
                        TftSpeedArcGauge(
                            currentSpeed = displaySpeed,
                            maxSpeed = maxScale,
                            isSpeedAlert = isSpeedAlert,
                            accentColor = customColors.accentColor,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Large Digital Speedometer Readout in Exact Center of Arc
                        Text(
                            text = displaySpeed.roundToInt().toString(),
                            color = if (isSpeedAlert) TftRed else customColors.textColor,
                            fontSize = (gaugeSize.value * 0.38f).sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                lineHeight = (gaugeSize.value * 0.38f).sp,
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            ),
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Speed Unit centered below the speedometer readout
                        Text(
                            text = speedUnit,
                            color = customColors.textColor.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (gaugeSize.value * 0.22f).dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // RIGHT: Telemetry Rows (Distance Prominent + Top Speed, Total Time, Moving Avg Row + Altitude/Heading)
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Prominent Distance
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", displayDistance),
                                color = customColors.textColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 28.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = distanceUnit,
                                color = customColors.textColor.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        Text(
                            text = "DISTANCE",
                            color = customColors.textColor.copy(alpha = 0.65f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                    }

                    HorizontalDivider(
                        color = customColors.accentColor.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )

                    // Row of values: Top Speed | Total Time | Moving Avg
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryStatItem(
                            value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                            unit = speedUnit,
                            label = "TOP SPEED",
                            textColor = customColors.textColor,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatItem(
                            value = timeDisplay,
                            unit = "",
                            label = "TOTAL TIME",
                            textColor = customColors.textColor,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatItem(
                            value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                            unit = speedUnit,
                            label = "MOVING AVG",
                            textColor = customColors.textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(
                        color = customColors.accentColor.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )

                    // Altitude & Compass Heading row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terrain,
                                contentDescription = "Altitude",
                                tint = customColors.accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = altitudeDisplay,
                                color = customColors.textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Heading",
                                tint = customColors.accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = headingDisplay,
                                color = customColors.textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // RIGHT BEZEL COLUMN: Warning Lamps & Button markings
                TftRightWarningColumn(
                    isSpeedAlert = isSpeedAlert,
                    modifier = Modifier
                        .width(42.dp)
                        .fillMaxHeight(),
                    textColor = customColors.textColor.copy(alpha = 0.65f)
                )
            }

            // BOTTOM BAR: Battery % + Altitude + Compass + Controls + GPS Info Line + AiM Logo
            TftLandscapeBottomBar(
                currentTimeString = currentTimeString,
                isGpsLost = isGpsLost,
                accuracyMeters = accuracyMeters,
                bikeName = bikeName,
                isIdle = isIdle,
                pauseState = pauseState,
                onStartClick = onStartClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onStopConfirmed = onStopConfirmed,
                onSwitchToMap = onSwitchToMap,
                onNavigateToDestination = onNavigateToDestination,
                palette = palette,
                customColors = customColors,
                batteryState = batteryState,
                altitudeDisplay = altitudeDisplay,
                headingDisplay = headingDisplay,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

// ============================================================
// PORTRAIT VIEW (AiM Casing with Large Speed Center Arc)
// ============================================================
@Composable
private fun TftPortraitView(
    displaySpeed: Double,
    speedUnit: String,
    displayDistance: Double,
    distanceUnit: String,
    avgSpeedDisplay: Double,
    maxSpeedDisplay: Double,
    timeDisplay: String,
    speedKmh: Double,
    maxScale: Double,
    currentTimeString: String,
    isSpeedAlert: Boolean,
    isGpsLost: Boolean,
    pauseState: PauseState,
    bikeName: String?,
    accuracyMeters: Float,
    isMetric: Boolean,
    isIdle: Boolean,
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopConfirmed: () -> Unit,
    onSwitchToMap: () -> Unit,
    onNavigateToDestination: () -> Unit = {},
    palette: CockpitThemePalette,
    customColors: DashboardCustomColors,
    batteryState: BatteryState,
    altitudeDisplay: String,
    headingDisplay: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(customColors.backgroundColor)
            .border(2.dp, customColors.accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // STATUS ROW: Live Phone Battery % + Keep Screen On + Settings + Map
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(customColors.componentColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Live Phone Battery % with dynamic icon
                BatteryIndicator(
                    batteryState = batteryState,
                    textColor = customColors.textColor,
                    accentColor = customColors.accentColor
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onToggleKeepScreenOn,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (keepScreenOn) Icons.Default.ScreenLockPortrait else Icons.Default.ScreenRotation,
                            contentDescription = "Keep Screen On",
                            tint = if (keepScreenOn) customColors.accentColor else customColors.textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = customColors.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, customColors.accentColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable { onSwitchToMap() },
                        color = customColors.componentColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Map",
                                tint = customColors.accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "MAP",
                                color = customColors.textColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, customColors.accentColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable { onNavigateToDestination() },
                        color = customColors.componentColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Navigation",
                                tint = customColors.accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "NAV",
                                color = customColors.textColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // CENTER: Speed Arc Gauge with Large Digital Speedometer at Center (Preserving size of 0)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f),
                contentAlignment = Alignment.Center
            ) {
                val gaugeSize = minOf(maxWidth, maxHeight) * 0.96f

                Box(
                    modifier = Modifier.size(gaugeSize),
                    contentAlignment = Alignment.Center
                ) {
                    TftSpeedArcGauge(
                        currentSpeed = displaySpeed,
                        maxSpeed = maxScale,
                        isSpeedAlert = isSpeedAlert,
                        accentColor = customColors.accentColor,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Large Digital Speedometer Readout in Exact Center of Arc
                    Text(
                        text = displaySpeed.roundToInt().toString(),
                        color = if (isSpeedAlert) TftRed else customColors.textColor,
                        fontSize = (gaugeSize.value * 0.38f).sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            lineHeight = (gaugeSize.value * 0.38f).sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Speed Unit centered below the speedometer readout
                    Text(
                        text = speedUnit,
                        color = customColors.textColor.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (gaugeSize.value * 0.22f).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // TELEMETRY PANEL: Distance (Prominent) + Row of (Top Speed, Total Time, Moving Avg)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(customColors.componentColor)
                    .border(1.dp, customColors.accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Distance prominently placed below the speedometer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", displayDistance),
                            color = customColors.textColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 32.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = distanceUnit,
                            color = customColors.textColor.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Text(
                        text = "DISTANCE",
                        color = customColors.textColor.copy(alpha = 0.65f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }

                HorizontalDivider(
                    color = customColors.accentColor.copy(alpha = 0.15f),
                    thickness = 1.dp
                )

                // Row of values below Distance: Top Speed | Total Time | Moving Avg
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryStatItem(
                        value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                        unit = speedUnit,
                        label = "TOP SPEED",
                        textColor = customColors.textColor,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatItem(
                        value = timeDisplay,
                        unit = "",
                        label = "TOTAL TIME",
                        textColor = customColors.textColor,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatItem(
                        value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                        unit = speedUnit,
                        label = "MOVING AVG",
                        textColor = customColors.textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // BOTTOM STATUS LINE (Altitude, Heading, GPS, Date)
            TftPortraitBottomInfo(
                currentTimeString = currentTimeString,
                isGpsLost = isGpsLost,
                accuracyMeters = accuracyMeters,
                bikeName = bikeName,
                altitudeDisplay = altitudeDisplay,
                headingDisplay = headingDisplay,
                customColors = customColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // CONTROLS (Glove-friendly Start or Pause/Resume + 2-sec Hold-to-Stop)
            if (isIdle) {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = customColors.accentColor)
                ) {
                    Text(
                        text = "START RIDE",
                        color = if (customColors.accentColor.luminance() > 0.5f) Color.Black else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }
            } else {
                ThemedCockpitControlsRow(
                    isPaused = pauseState.isPaused,
                    onPauseClick = onPauseClick,
                    onResumeClick = onResumeClick,
                    onStopConfirmed = onStopConfirmed,
                    palette = palette,
                    cornerRadius = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }


        }
    }
}

// ============================================================
// SPEED ARC GAUGE (AiM MXS Segmented Curved Arc)
// ============================================================
@Composable
private fun TftSpeedArcGauge(
    currentSpeed: Double,
    maxSpeed: Double,
    isSpeedAlert: Boolean,
    accentColor: Color = Color(0xFF00D4E5),
    modifier: Modifier = Modifier
) {
    val speedRatio = (currentSpeed / maxSpeed).coerceIn(0.0, 1.0).toFloat()
    val animatedSweep by animateFloatAsState(
        targetValue = speedRatio * 260f,
        animationSpec = tween(durationMillis = 200),
        label = "speedSweep"
    )

    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = canvasSize * 0.44f
        val arcStrokeWidth = canvasSize * 0.08f
        val innerRadius = outerRadius - arcStrokeWidth

        val startAngle = 140f
        val sweepAngle = 260f

        // Dim background track
        drawArc(
            color = Color(0xFF10141D),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Butt)
        )

        // Active cyan/accent segmented arc (turns red if speed alert)
        val arcColor = if (isSpeedAlert) TftRed else accentColor
        drawArc(
            color = arcColor,
            startAngle = startAngle,
            sweepAngle = animatedSweep,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Butt)
        )

        // Subtle glow effect
        if (animatedSweep > 10f) {
            drawArc(
                color = arcColor.copy(alpha = 0.2f),
                startAngle = startAngle,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = arcStrokeWidth * 1.5f, cap = StrokeCap.Butt)
            )
        }

        // Segment block dividers (black radial gaps between blocks)
        val totalSegments = 32
        for (i in 0..totalSegments) {
            val angleDeg = startAngle + (sweepAngle * i / totalSegments)
            val angleRad = angleDeg * PI.toFloat() / 180f

            val r1 = innerRadius - 2f
            val r2 = outerRadius + 2f

            val p1 = Offset(center.x + r1 * cos(angleRad), center.y + r1 * sin(angleRad))
            val p2 = Offset(center.x + r2 * cos(angleRad), center.y + r2 * sin(angleRad))

            drawLine(
                color = Color.Black,
                start = p1,
                end = p2,
                strokeWidth = 3f
            )
        }
    }
}


// ============================================================
// TURN INDICATOR ARROW (< and >)
// ============================================================
@Composable
private fun TurnIndicatorArrow(isLeft: Boolean, isActive: Boolean) {
    Canvas(modifier = Modifier.size(16.dp, 14.dp)) {
        val path = Path()
        if (isLeft) {
            path.moveTo(size.width, 0f)
            path.lineTo(0f, size.height / 2f)
            path.lineTo(size.width, size.height)
            path.close()
        } else {
            path.moveTo(0f, 0f)
            path.lineTo(size.width, size.height / 2f)
            path.lineTo(0f, size.height)
            path.close()
        }
        drawPath(
            path = path,
            color = if (isActive) TftGreen else TftDimGray
        )
    }
}

// ============================================================
// LEFT BEZEL COLUMN (TC, Battery, Oil, ABS & Button labels)
// ============================================================
@Composable
private fun TftLeftIndicatorColumn(
    isGpsLost: Boolean,
    pauseState: PauseState,
    textColor: Color = TftGray,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MENU\n<<",
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TC indicator
            Text(
                text = "TC",
                color = TftYellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            // High Beam (Dim blue)
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = "Beam",
                tint = TftBlue.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )

            // Battery (Red)
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = "Battery",
                tint = TftRed,
                modifier = Modifier.size(14.dp)
            )

            // ABS (Yellow)
            Text(
                text = "ABS",
                color = TftYellow,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = ">>",
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ============================================================
// RIGHT BEZEL COLUMN (High Beam, Warning & Button labels)
// ============================================================
@Composable
private fun TftRightWarningColumn(
    isSpeedAlert: Boolean,
    textColor: Color = TftGray,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MEM\nOK",
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High Beam (Bright blue)
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = "High Beam",
                tint = TftBlue,
                modifier = Modifier.size(15.dp)
            )

            // Engine MIL (Yellow)
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Engine Check",
                tint = TftYellow,
                modifier = Modifier.size(15.dp)
            )

            // Master Warning (Red)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isSpeedAlert) TftRed else TftRed.copy(alpha = 0.4f))
            )
        }

        Text(
            text = "VIEW\nQUIT",
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TelemetryStatItem(
    value: String,
    unit: String,
    label: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    color = textColor.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
        Text(
            text = label,
            color = textColor.copy(alpha = 0.6f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
    }
}

// ============================================================
// TELEMETRY ROW (AiM Style: Large White Number + Cyan Label)
// ============================================================
@Composable
private fun TftAiMTelemetryRow(
    value: String,
    label: String,
    valueColor: Color = TftWhite,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            lineHeight = 24.sp
        )
        Text(
            text = label,
            color = TftCyan,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun TftAiMTelemetryField(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = TftWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            color = TftCyan,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ============================================================
// INSTANT FUEL CONSUMPTION BAR (Horizontal Segmented 0..50 KM/L)
// ============================================================
@Composable
private fun InstantFuelConsumptionBar(
    speedKmh: Double,
    modifier: Modifier = Modifier
) {
    // Instant consumption ratio derived from live speed
    val consumptionRatio = (speedKmh / 120.0).coerceIn(0.1, 0.85).toFloat()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(TftYellow)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "QS OFF",
                    color = Color.Black,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = "INSTANT FUEL CONSUMPTION   KM/L",
                color = TftGray,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Horizontal Segmented Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            val totalBlocks = 20
            val gap = 2f
            val blockWidth = (size.width - (totalBlocks - 1) * gap) / totalBlocks

            for (i in 0 until totalBlocks) {
                val blockThreshold = (i + 1f) / totalBlocks
                val isLit = consumptionRatio >= blockThreshold

                val color = when {
                    i < 14 -> TftCyan
                    i < 18 -> TftYellow
                    else -> TftRed
                }

                val x = i * (blockWidth + gap)
                drawRect(
                    color = if (isLit) color else Color(0xFF131720),
                    topLeft = Offset(x, 0f),
                    size = Size(blockWidth, size.height)
                )
            }
        }
    }
}

// ============================================================
// LANDSCAPE BOTTOM BAR (Battery, Altitude, Compass, controls, live clock)
// ============================================================
@Composable
private fun TftLandscapeBottomBar(
    currentTimeString: String,
    isGpsLost: Boolean,
    accuracyMeters: Float,
    bikeName: String?,
    isIdle: Boolean,
    pauseState: PauseState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopConfirmed: () -> Unit,
    onSwitchToMap: () -> Unit,
    onNavigateToDestination: () -> Unit = {},
    palette: CockpitThemePalette,
    customColors: DashboardCustomColors = DashboardCustomColors(),
    batteryState: BatteryState,
    altitudeDisplay: String,
    headingDisplay: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Upper row: Battery %, Altitude, Compass, Map & Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Telemetry & Device Status: Battery % + Altitude + Compass Heading
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Phone Battery %
                BatteryIndicator(
                    batteryState = batteryState,
                    textColor = customColors.textColor,
                    accentColor = customColors.accentColor
                )

                // Current GPS Altitude
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = "Altitude",
                        tint = customColors.accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = altitudeDisplay,
                        color = customColors.textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Current Compass Heading
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Heading",
                        tint = customColors.accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = headingDisplay,
                        color = customColors.textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Interactive Controls Row (Glove friendly 56dp targets)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.2.dp, customColors.accentColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .clickable { onSwitchToMap() },
                    color = customColors.componentColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Map",
                            tint = customColors.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "MAP",
                            color = customColors.textColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.2.dp, customColors.accentColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .clickable { onNavigateToDestination() },
                    color = customColors.componentColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Navigate",
                            tint = customColors.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "NAV",
                            color = customColors.textColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (isIdle) {
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.accentColor)
                    ) {
                        Text(
                            text = "START",
                            color = if (customColors.accentColor.luminance() > 0.5f) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    ThemedCockpitControlsRow(
                        isPaused = pauseState.isPaused,
                        onPauseClick = onPauseClick,
                        onResumeClick = onResumeClick,
                        onStopConfirmed = onStopConfirmed,
                        palette = palette,
                        cornerRadius = 4.dp,
                        modifier = Modifier.width(220.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Lower strip: GPS Status + AiM Logo + Clock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: GPS status
            Text(
                text = if (isGpsLost) "GPS: LOST" else "GPS: GOOD (±${accuracyMeters.roundToInt()}M)",
                color = if (isGpsLost) TftRed else TftGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )



            // Right: Live Clock
            Text(
                text = currentTimeString.ifEmpty { "23.08.2026 11:35 PM" },
                color = customColors.textColor.copy(alpha = 0.65f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ============================================================
// PORTRAIT BOTTOM INFO (Altitude, Heading, GPS, Date)
// ============================================================
@Composable
private fun TftPortraitBottomInfo(
    currentTimeString: String,
    isGpsLost: Boolean,
    accuracyMeters: Float,
    bikeName: String?,
    altitudeDisplay: String,
    headingDisplay: String,
    customColors: DashboardCustomColors = DashboardCustomColors(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(customColors.componentColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current GPS Altitude
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terrain,
                    contentDescription = "Altitude",
                    tint = customColors.accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = altitudeDisplay,
                    color = customColors.textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Current Compass Heading
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Heading",
                    tint = customColors.accentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = headingDisplay,
                    color = customColors.textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isGpsLost) "GPS: LOST" else "GPS: GOOD (±${accuracyMeters.roundToInt()}M)",
                color = if (isGpsLost) TftRed else TftGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = currentTimeString.ifEmpty { "23.08.2026 11:35 PM" },
                color = customColors.textColor.copy(alpha = 0.6f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}


