package com.abrar.motolog.ui.live

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.shared.domain.model.RideStats
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt

// ============================================================
// Signature Color Constants for Drive/Ride Summary
// ============================================================
private val SummaryElectricLime = Color(0xFFD4FF00)      // Signature neon yellow-lime accent
private val SummaryDarkBg = Color(0xFF0C0E12)            // Deep AMOLED background
private val SummaryCardBg = Color(0xFF14171E)            // Dark route preview surface
private val SummaryCardBorder = Color(0xFF262C3A)        // Subtle card border
private val SummaryGridLine = Color(0xFF222633)          // Stylized map road/grid lines
private val SummaryMutedLabel = Color(0xFFA0A5B5)        // Muted gray labels
private val SummaryHaloGlow = Color(0x55D4FF00)          // Soft glowing aura for end marker

/**
 * High-contrast, glanceable ride summary screen presented when a ride is completed.
 * Features a stylized dark mini-route preview card with neon dashed path, start & finish
 * location tags, start-to-finish route title, and 3 glanceable telemetry metrics.
 */
@Composable
fun RideSummaryView(
    state: LiveUiState.Stopped,
    useMetricUnits: Boolean,
    onStartNewRide: () -> Unit,
    modifier: Modifier = Modifier,
    onViewDetails: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141720),
                        SummaryDarkBg,
                        Color(0xFF07080A)
                    )
                )
            )
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            LandscapeRideSummaryContent(
                state = state,
                useMetricUnits = useMetricUnits,
                onStartNewRide = onStartNewRide,
                onViewDetails = onViewDetails
            )
        } else {
            PortraitRideSummaryContent(
                state = state,
                useMetricUnits = useMetricUnits,
                onStartNewRide = onStartNewRide,
                onViewDetails = onViewDetails
            )
        }
    }
}

// ============================================================
// Portrait Layout Content
// ============================================================
@Composable
private fun PortraitRideSummaryContent(
    state: LiveUiState.Stopped,
    useMetricUnits: Boolean,
    onStartNewRide: () -> Unit,
    onViewDetails: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top App Logo & Branding Header
        SummaryHeader()

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Stylized Route Card
        RideSummaryRouteCard(
            coordinates = state.routeCoordinates,
            startLabel = state.startLocationName ?: "Start",
            endLabel = state.endLocationName ?: "Finish",
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clickable(enabled = state.rideId > 0) { onViewDetails() }
        )

        Spacer(modifier = Modifier.height(22.dp))

        // 3. Start -> End Route Title
        SummaryRouteTitle(
            startLocation = state.startLocationName,
            endLocation = state.endLocationName,
            bikeName = state.bikeName
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 4. Primary 3-Column Metrics Row (Distance, Drive Time, Avg Speed)
        SummaryMetricsRow(
            stats = state.stats,
            useMetricUnits = useMetricUnits,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(36.dp))

        // 5. Glove-Friendly Action Buttons
        SummaryActionButtons(
            rideId = state.rideId,
            onStartNewRide = onStartNewRide,
            onViewDetails = onViewDetails,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ============================================================
// Landscape Layout Content
// ============================================================
@Composable
private fun LandscapeRideSummaryContent(
    state: LiveUiState.Stopped,
    useMetricUnits: Boolean,
    onStartNewRide: () -> Unit,
    onViewDetails: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Route Card
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            RideSummaryRouteCard(
                coordinates = state.routeCoordinates,
                startLabel = state.startLocationName ?: "Start",
                endLabel = state.endLocationName ?: "Finish",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clickable(enabled = state.rideId > 0) { onViewDetails() }
            )
        }

        // Right Column: Header, Title, Metrics, Buttons
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SummaryHeader()

            Spacer(modifier = Modifier.height(10.dp))

            SummaryRouteTitle(
                startLocation = state.startLocationName,
                endLocation = state.endLocationName,
                bikeName = state.bikeName
            )

            Spacer(modifier = Modifier.height(14.dp))

            SummaryMetricsRow(
                stats = state.stats,
                useMetricUnits = useMetricUnits,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            SummaryActionButtons(
                rideId = state.rideId,
                onStartNewRide = onStartNewRide,
                onViewDetails = onViewDetails,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================
// Top Header: Logo + App Name + Subtitle Tag
// ============================================================
@Composable
private fun SummaryHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Stylized Road Ribbon Emblem matching Trip360 aesthetic
            Canvas(modifier = Modifier.size(36.dp)) {
                val strokeW = 3.dp.toPx()
                // Outer circle border
                drawCircle(
                    color = Color.White,
                    radius = size.minDimension / 2f - strokeW / 2f,
                    style = Stroke(width = strokeW)
                )
                // Curved winding road ribbon inside
                val ribbonPath = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.72f)
                    cubicTo(
                        size.width * 0.35f, size.height * 0.30f,
                        size.width * 0.65f, size.height * 0.75f,
                        size.width * 0.78f, size.height * 0.28f
                    )
                }
                drawPath(
                    path = ribbonPath,
                    color = SummaryElectricLime,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "MotoLog",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "RIDE SUMMARY",
            color = SummaryElectricLime,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.5.sp
        )
    }
}

// ============================================================
// Stylized Route Map Card
// ============================================================
@Composable
private fun RideSummaryRouteCard(
    coordinates: List<Pair<Double, Double>>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SummaryCardBg)
            .border(1.dp, SummaryCardBorder, RoundedCornerShape(20.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw subtle abstract intersecting street lines in the background
            drawAbstractGridLines(width, height)

            // 2. Compute route points & start/end positions
            val padH = 46.dp.toPx()
            val padV = 44.dp.toPx()
            val availableW = width - (padH * 2)
            val availableH = height - (padV * 2)

            val routePath = Path()
            val startPt: Offset
            val endPt: Offset

            if (coordinates.size >= 2) {
                var minLat = Double.MAX_VALUE
                var maxLat = -Double.MAX_VALUE
                var minLon = Double.MAX_VALUE
                var maxLon = -Double.MAX_VALUE

                coordinates.forEach { (lat, lon) ->
                    if (lat < minLat) minLat = lat
                    if (lat > maxLat) maxLat = lat
                    if (lon < minLon) minLon = lon
                    if (lon > maxLon) maxLon = lon
                }

                val deltaLat = (maxLat - minLat).coerceAtLeast(0.0001)
                val deltaLon = (maxLon - minLon).coerceAtLeast(0.0001)
                val midLat = (minLat + maxLat) / 2.0
                val latFactor = cos(Math.toRadians(midLat)).coerceAtLeast(0.1)
                val effectiveDeltaLon = deltaLon * latFactor

                val scale = minOf(availableW / effectiveDeltaLon, availableH / deltaLat)
                val routeW = (effectiveDeltaLon * scale).toFloat()
                val routeH = (deltaLat * scale).toFloat()

                val offsetX = padH + (availableW - routeW) / 2f
                val offsetY = padV + (availableH - routeH) / 2f

                fun mapCoord(lat: Double, lon: Double): Offset {
                    val px = offsetX + (((lon - minLon) * latFactor) * scale).toFloat()
                    val py = offsetY + ((maxLat - lat) * scale).toFloat()
                    return Offset(px, py)
                }

                val first = mapCoord(coordinates.first().first, coordinates.first().second)
                val last = mapCoord(coordinates.last().first, coordinates.last().second)
                startPt = first
                endPt = last

                routePath.moveTo(first.x, first.y)
                var prev = first
                for (i in 1 until coordinates.size) {
                    val current = mapCoord(coordinates[i].first, coordinates[i].second)
                    val midX = (prev.x + current.x) / 2f
                    val midY = (prev.y + current.y) / 2f
                    routePath.quadraticTo(prev.x, prev.y, midX, midY)
                    prev = current
                }
                routePath.lineTo(last.x, last.y)
            } else {
                // Graceful fallback: signature motorcycle ribbon wave curve from screenshot
                val sx = padH + 10f
                val sy = height * 0.68f
                val ex = width - padH - 10f
                val ey = height * 0.28f

                startPt = Offset(sx, sy)
                endPt = Offset(ex, ey)

                routePath.moveTo(sx, sy)
                routePath.cubicTo(
                    width * 0.32f, height * 0.72f,
                    width * 0.38f, height * 0.42f,
                    width * 0.50f, height * 0.58f
                )
                routePath.cubicTo(
                    width * 0.62f, height * 0.70f,
                    width * 0.74f, height * 0.26f,
                    ex, ey
                )
            }

            // 3. Draw Route Ambient Neon Glow
            drawPath(
                path = routePath,
                color = Color(0x35D4FF00),
                style = Stroke(
                    width = 8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 4. Draw Neon Yellow-Lime Dashed Route Line
            drawPath(
                path = routePath,
                color = SummaryElectricLime,
                style = Stroke(
                    width = 3.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
            )

            // 5. Draw Start Marker (White Hollow Ring)
            val ringRadius = 7.5.dp.toPx()
            drawCircle(
                color = Color.White,
                radius = ringRadius,
                center = startPt,
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawCircle(
                color = SummaryCardBg,
                radius = ringRadius - 1.5.dp.toPx(),
                center = startPt
            )

            // 6. Draw End Marker (Neon Lime Glowing Dot)
            drawCircle(
                color = SummaryHaloGlow,
                radius = 16.dp.toPx(),
                center = endPt
            )
            drawCircle(
                color = SummaryElectricLime,
                radius = 6.5.dp.toPx(),
                center = endPt
            )

            // 7. Draw Start City Label
            val labelStyle = TextStyle(
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            val measuredStart = textMeasurer.measure(startLabel, style = labelStyle)
            val startTextX = (startPt.x - measuredStart.size.width / 2f)
                .coerceIn(12f, width - measuredStart.size.width - 12f)
            val startTextY = if (startPt.y + 14.dp.toPx() + measuredStart.size.height <= height) {
                startPt.y + 14.dp.toPx()
            } else {
                startPt.y - measuredStart.size.height - 12.dp.toPx()
            }

            drawText(
                textMeasurer = textMeasurer,
                text = startLabel,
                topLeft = Offset(startTextX, startTextY),
                style = labelStyle
            )

            // 8. Draw End City Label
            val measuredEnd = textMeasurer.measure(endLabel, style = labelStyle)
            val endTextX = (endPt.x - measuredEnd.size.width / 2f)
                .coerceIn(12f, width - measuredEnd.size.width - 12f)
            val endTextY = if (endPt.y + 16.dp.toPx() + measuredEnd.size.height <= height) {
                endPt.y + 16.dp.toPx()
            } else {
                endPt.y - measuredEnd.size.height - 12.dp.toPx()
            }

            drawText(
                textMeasurer = textMeasurer,
                text = endLabel,
                topLeft = Offset(endTextX, endTextY),
                style = labelStyle
            )
        }
    }
}

/**
 * Draws the subtle, angled road grid lines seen in the background of the screenshot map card.
 */
private fun DrawScope.drawAbstractGridLines(width: Float, height: Float) {
    val gridStroke = 1.6.dp.toPx()
    val lines = listOf(
        // Nearly horizontal street lines
        Offset(0f, height * 0.42f) to Offset(width, height * 0.40f),
        Offset(0f, height * 0.52f) to Offset(width, height * 0.50f),
        Offset(0f, height * 0.70f) to Offset(width, height * 0.68f),
        // Angled diagonal avenue lines
        Offset(width * 0.12f, height) to Offset(width * 0.88f, 0f),
        Offset(0f, height * 0.65f) to Offset(width * 0.75f, 0f),
        Offset(width * 0.35f, height) to Offset(width, height * 0.32f),
        Offset(0f, height * 0.85f) to Offset(width * 0.50f, 0f)
    )

    lines.forEach { (start, end) ->
        drawLine(
            color = SummaryGridLine,
            start = start,
            end = end,
            strokeWidth = gridStroke,
            cap = StrokeCap.Round
        )
    }
}

// ============================================================
// Start -> End Title
// ============================================================
@Composable
private fun SummaryRouteTitle(
    startLocation: String?,
    endLocation: String?,
    bikeName: String?,
    modifier: Modifier = Modifier
) {
    val titleText = when {
        !startLocation.isNullOrBlank() && !endLocation.isNullOrBlank() && startLocation != endLocation -> {
            "$startLocation → $endLocation"
        }
        !startLocation.isNullOrBlank() && !endLocation.isNullOrBlank() -> {
            "$startLocation Loop"
        }
        !startLocation.isNullOrBlank() -> {
            "$startLocation Ride"
        }
        !endLocation.isNullOrBlank() -> {
            "Ride to $endLocation"
        }
        !bikeName.isNullOrBlank() -> {
            "$bikeName Ride"
        }
        else -> "Ride Complete"
    }

    Text(
        text = titleText,
        color = Color.White,
        fontSize = 25.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth()
    )
}

// ============================================================
// 3-Column Metrics Row: Distance | Drive time | Avg speed
// ============================================================
@Composable
private fun SummaryMetricsRow(
    stats: RideStats,
    useMetricUnits: Boolean,
    modifier: Modifier = Modifier
) {
    val totalKm = stats.totalDistanceMeters / 1000.0
    val displayDist = if (useMetricUnits) totalKm else totalKm * 0.621371
    val distUnit = if (useMetricUnits) "km" else "mi"

    val avgSpeed = if (useMetricUnits) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
    val speedUnit = if (useMetricUnits) "km/h" else "mph"

    // Format distance: comma separated for large distances (e.g. 1,500 km) like screenshot
    val formattedDist = if (displayDist >= 100.0) {
        String.format(Locale.US, "%,d", displayDist.roundToInt())
    } else {
        String.format(Locale.US, "%.1f", displayDist)
    }

    // Format moving drive/ride time (e.g. 5h 12m)
    val totalSec = (stats.movingTimeMs / 1000).coerceAtLeast(0)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // 1. Distance Column
        MetricColumn(
            label = "Distance",
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedDist,
                    color = Color.White,
                    fontSize = 35.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = distUnit,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }

        // 2. Drive Time Column
        MetricColumn(
            label = "Drive time",
            modifier = Modifier.weight(1.1f)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                if (hours > 0) {
                    Text(
                        text = "$hours",
                        color = Color.White,
                        fontSize = 35.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "h ",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    Text(
                        text = "$minutes",
                        color = Color.White,
                        fontSize = 35.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "m",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                } else {
                    Text(
                        text = "$minutes",
                        color = Color.White,
                        fontSize = 35.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "m ",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    Text(
                        text = "$seconds",
                        color = Color.White,
                        fontSize = 35.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "s",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }

        // 3. Avg Speed Column
        val avgSpeedFormatted = if (avgSpeed >= 10.0) {
            "${avgSpeed.roundToInt()}"
        } else {
            String.format(Locale.US, "%.1f", avgSpeed)
        }

        MetricColumn(
            label = "Avg speed",
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = avgSpeedFormatted,
                    color = Color.White,
                    fontSize = 35.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = speedUnit,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = SummaryMutedLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

// ============================================================
// Action Buttons: Start New Ride & View Details
// ============================================================
@Composable
private fun SummaryActionButtons(
    rideId: Long,
    onStartNewRide: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Primary Neon Lime Button
        Button(
            onClick = onStartNewRide,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SummaryElectricLime,
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "START NEW RIDE",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        // Secondary View Details Button (if saved ride exists)
        if (rideId > 0) {
            OutlinedButton(
                onClick = onViewDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF141720),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262C3A))
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = SummaryMutedLabel,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VIEW FULL DETAILS & EXPORT",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

