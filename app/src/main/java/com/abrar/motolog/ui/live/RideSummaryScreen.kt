package com.abrar.motolog.ui.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.abrar.motolog.data.map.MapSupport
import com.abrar.motolog.data.map.OpenFreeMapStyleProvider
import com.abrar.motolog.shared.domain.map.MapStyleProvider
import com.abrar.motolog.shared.domain.model.RideStats
import kotlinx.coroutines.CancellationException
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.AndroidRenderMode
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.map.renderMode
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

// ============================================================
// Signature Color Constants for Drive/Ride Summary
// ============================================================
private val SummaryElectricLime = Color(0xFFD4FF00)      // Signature neon yellow-lime accent
private val SummaryDarkBg = Color(0xFF0C0E12)            // Deep AMOLED background
private val SummaryCardBg = Color(0xFF14171E)            // Dark route preview surface
private val SummaryCardBorder = Color(0xFF262C3A)        // Subtle card border
private val SummaryMutedLabel = Color(0xFFA0A5B5)        // Muted gray labels
private val SummaryHaloGlow = Color(0x55D4FF00)          // Soft glowing aura for end marker

private const val EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

/**
 * High-contrast, glanceable ride summary screen presented when a ride is completed.
 * Features a real dark vector map route preview card with neon route path, start & finish
 * location tags, start-to-finish route title, and 3 glanceable telemetry metrics.
 */
@Composable
fun RideSummaryView(
    state: LiveUiState.Stopped,
    useMetricUnits: Boolean,
    onStartNewRide: () -> Unit,
    modifier: Modifier = Modifier,
    mapStyleProvider: MapStyleProvider = remember { OpenFreeMapStyleProvider() },
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
                mapStyleProvider = mapStyleProvider,
                onStartNewRide = onStartNewRide,
                onViewDetails = onViewDetails
            )
        } else {
            PortraitRideSummaryContent(
                state = state,
                useMetricUnits = useMetricUnits,
                mapStyleProvider = mapStyleProvider,
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
    mapStyleProvider: MapStyleProvider,
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

        // 2. Real Vector Map Route Card
        RideSummaryRouteCard(
            coordinates = state.routeCoordinates,
            startLabel = state.startLocationName ?: "Start",
            endLabel = state.endLocationName ?: "Finish",
            mapStyleProvider = mapStyleProvider,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
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
    mapStyleProvider: MapStyleProvider,
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
        // Left Column: Real Vector Map Route Card
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
                mapStyleProvider = mapStyleProvider,
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
// Real Vector Map Route Card
// ============================================================
@Composable
private fun RideSummaryRouteCard(
    coordinates: List<Pair<Double, Double>>,
    startLabel: String,
    endLabel: String,
    mapStyleProvider: MapStyleProvider,
    modifier: Modifier = Modifier
) {
    val isNativeSupported = remember { MapSupport.isNativeSupported }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SummaryCardBg)
            .border(1.dp, SummaryCardBorder, RoundedCornerShape(20.dp))
    ) {
        if (isNativeSupported && coordinates.size >= 2) {
            MapLibreRouteSummaryCard(
                coordinates = coordinates,
                startLabel = startLabel,
                endLabel = endLabel,
                mapStyleProvider = mapStyleProvider
            )
        } else {
            FallbackRouteCardCanvas(
                coordinates = coordinates,
                startLabel = startLabel,
                endLabel = endLabel
            )
        }
    }
}

// ============================================================
// MapLibre Vector Map Card Component
// ============================================================
@Composable
private fun MapLibreRouteSummaryCard(
    coordinates: List<Pair<Double, Double>>,
    startLabel: String,
    endLabel: String,
    mapStyleProvider: MapStyleProvider
) {
    val styleUrl = remember(mapStyleProvider) {
        mapStyleProvider.getStyleUrl(isDarkTheme = true)
    }

    val sanitizedCoords = remember(coordinates) {
        sanitizeRouteCoordinates(coordinates)
    }

    val isLoop = remember(sanitizedCoords, startLabel, endLabel) {
        if (sanitizedCoords.size < 2) true
        else {
            val first = sanitizedCoords.first()
            val last = sanitizedCoords.last()
            val dLat = (first.first - last.first) * 111_000.0
            val dLon = (first.second - last.second) * 111_000.0 * cos(Math.toRadians(first.first))
            (dLat * dLat + dLon * dLon) < 45.0 * 45.0 || startLabel.trim().equals(endLabel.trim(), ignoreCase = true)
        }
    }

    val firstPoint = sanitizedCoords.firstOrNull()
    val initialCameraPosition = remember(firstPoint) {
        if (firstPoint != null) {
            CameraPosition(
                bearing = 0.0,
                target = Position(firstPoint.second, firstPoint.first),
                tilt = 0.0,
                zoom = 13.0
            )
        } else {
            CameraPosition()
        }
    }

    val routeGeoJson = remember(sanitizedCoords) {
        if (sanitizedCoords.size >= 2) lineStringJson(sanitizedCoords) else EMPTY_GEOJSON
    }

    val startGeoJson = remember(sanitizedCoords, isLoop) {
        if (!isLoop && sanitizedCoords.isNotEmpty()) {
            pointJson(sanitizedCoords.first().first, sanitizedCoords.first().second)
        } else {
            EMPTY_GEOJSON
        }
    }

    val endGeoJson = remember(sanitizedCoords) {
        sanitizedCoords.lastOrNull()?.let { pointJson(it.first, it.second) } ?: EMPTY_GEOJSON
    }

    var styleLoaded by remember { mutableStateOf(false) }

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = initialCameraPosition
    ) {
        // 1. Neon glowing aura for route
        if (sanitizedCoords.size >= 2) {
            val routeSource = rememberGeoJsonSource(GeoJsonData.JsonString(routeGeoJson))
            LineLayer(
                id = "summary-route-glow",
                source = routeSource,
                color = const(Color(0x66D4FF00)),
                width = const(9.dp)
            )
            // 2. High-contrast solid electric lime core line
            LineLayer(
                id = "summary-route-core",
                source = routeSource,
                color = const(SummaryElectricLime),
                width = const(4.5.dp)
            )
        }

        // 3. Start Point Ring (if not a loop)
        if (!isLoop && sanitizedCoords.isNotEmpty()) {
            val startSource = rememberGeoJsonSource(GeoJsonData.JsonString(startGeoJson))
            CircleLayer(
                id = "summary-start-ring",
                source = startSource,
                color = const(Color.White),
                radius = const(6.5.dp)
            )
            CircleLayer(
                id = "summary-start-inner",
                source = startSource,
                color = const(SummaryCardBg),
                radius = const(3.dp)
            )
        }

        // 4. End Point (or unified loop point) with Glowing Aura
        if (sanitizedCoords.isNotEmpty()) {
            val endSource = rememberGeoJsonSource(GeoJsonData.JsonString(endGeoJson))
            CircleLayer(
                id = "summary-end-halo",
                source = endSource,
                color = const(Color(0x66D4FF00)),
                radius = const(14.dp)
            )
            CircleLayer(
                id = "summary-end-core",
                source = endSource,
                color = const(SummaryElectricLime),
                radius = const(6.dp)
            )
        }
    }

    // Monitor style loading
    LaunchedEffect(mapState) {
        mapState.events.collect { event ->
            if (event is MapEvent.StyleLoaded) {
                styleLoaded = true
            }
        }
    }

    // Camera auto-framing on route points
    LaunchedEffect(sanitizedCoords, styleLoaded) {
        if (sanitizedCoords.isNotEmpty()) {
            try {
                val minLat = sanitizedCoords.minOf { it.first }
                val maxLat = sanitizedCoords.maxOf { it.first }
                val minLon = sanitizedCoords.minOf { it.second }
                val maxLon = sanitizedCoords.maxOf { it.second }

                val centerLat = (minLat + maxLat) / 2.0
                val centerLon = (minLon + maxLon) / 2.0
                val latSpan = (maxLat - minLat).coerceAtLeast(0.005)
                val lonSpan = (maxLon - minLon).coerceAtLeast(0.005)

                mapState.animateCameraToBounds(
                    boundingBox = BoundingBox(
                        west = centerLon - lonSpan / 2.0,
                        south = centerLat - latSpan / 2.0,
                        east = centerLon + lonSpan / 2.0,
                        north = centerLat + latSpan / 2.0
                    ),
                    padding = PaddingValues(36.dp),
                    animation = CameraAnimation.Ease(duration = 600.milliseconds)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    val uiOptions = remember {
        MapUiOptions(MapUiOptions.None) {
            renderMode = AndroidRenderMode.Texture
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            state = mapState,
            uiOptions = uiOptions,
            interactions = MapInteractions.None,
            overlay = {},
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Badge: Top Right "Inspect" pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC14171E),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CropFree,
                    contentDescription = null,
                    tint = SummaryElectricLime,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Tap to inspect",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Overlay Badge: Bottom Location Chip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xDD14171E),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isLoop) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SummaryElectricLime, CircleShape)
                    )
                    Text(
                        text = startLabel.ifBlank { "Round Trip" },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                    Text(
                        text = startLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = SummaryMutedLabel,
                        modifier = Modifier.size(11.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(SummaryElectricLime, CircleShape)
                    )
                    Text(
                        text = endLabel,
                        color = SummaryElectricLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================================
// Fallback Canvas Route Card (JVM unit tests & single point rides)
// ============================================================
@Composable
private fun FallbackRouteCardCanvas(
    coordinates: List<Pair<Double, Double>>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawAbstractGridLines(width, height)

        val padH = 48.dp.toPx()
        val padV = 44.dp.toPx()
        val availableW = width - (padH * 2)
        val availableH = height - (padV * 2)

        val routePath = Path()
        val startPt: Offset
        val endPt: Offset

        if (coordinates.size >= 2) {
            val midLatApprox = (coordinates.first().first + coordinates.last().first) / 2.0
            val latFactor = cos(Math.toRadians(midLatApprox)).coerceAtLeast(0.1)

            val filtered = ArrayList<Pair<Double, Double>>(coordinates.size)
            for (pt in coordinates) {
                if (filtered.isEmpty()) {
                    filtered.add(pt)
                } else {
                    val prev = filtered.last()
                    val dLatM = (pt.first - prev.first) * 111320.0
                    val dLonM = (pt.second - prev.second) * 111320.0 * latFactor
                    if (dLatM * dLatM + dLonM * dLonM >= 12.25) {
                        filtered.add(pt)
                    }
                }
            }
            if (filtered.size < 2 && coordinates.size >= 2) {
                filtered.clear()
                filtered.add(coordinates.first())
                filtered.add(coordinates.last())
            }

            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE
            var maxLon = -Double.MAX_VALUE

            filtered.forEach { (lat, lon) ->
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
                if (lon < minLon) minLon = lon
                if (lon > maxLon) maxLon = lon
            }

            val centerLat = (minLat + maxLat) / 2.0
            val effectiveLatFactor = cos(Math.toRadians(centerLat)).coerceAtLeast(0.1)

            val minSpanLatDeg = 350.0 / 111320.0
            val minSpanLonDeg = 350.0 / (111320.0 * effectiveLatFactor)

            val rawSpanLat = maxLat - minLat
            val rawSpanLon = (maxLon - minLon) * effectiveLatFactor

            val effectiveSpanLat = maxOf(rawSpanLat, minSpanLatDeg)
            val effectiveSpanLon = maxOf(rawSpanLon, minSpanLonDeg)

            val scale = minOf(availableW / effectiveSpanLon, availableH / effectiveSpanLat)

            val routePixelW = (rawSpanLon * scale).toFloat()
            val routePixelH = (rawSpanLat * scale).toFloat()

            val offsetX = padH + (availableW - routePixelW) / 2f
            val offsetY = padV + (availableH - routePixelH) / 2f

            fun mapCoord(lat: Double, lon: Double): Offset {
                val px = offsetX + (((lon - minLon) * effectiveLatFactor) * scale).toFloat()
                val py = offsetY + ((maxLat - lat) * scale).toFloat()
                return Offset(px, py)
            }

            val rawPixelPoints = filtered.map { mapCoord(it.first, it.second) }
            val smoothedPoints = smoothPointsChaikin(rawPixelPoints, iterations = 2)

            startPt = rawPixelPoints.first()
            endPt = rawPixelPoints.last()

            if (smoothedPoints.isNotEmpty()) {
                routePath.moveTo(smoothedPoints.first().x, smoothedPoints.first().y)
                for (i in 1 until smoothedPoints.size) {
                    routePath.lineTo(smoothedPoints[i].x, smoothedPoints[i].y)
                }
            }
        } else {
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

        // Draw Ambient Glow
        drawPath(
            path = routePath,
            color = Color(0x35D4FF00),
            style = Stroke(
                width = 8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw Neon Yellow-Lime Dashed Route Line
        drawPath(
            path = routePath,
            color = SummaryElectricLime,
            style = Stroke(
                width = 3.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 14f), 0f)
            )
        )

        val dx = startPt.x - endPt.x
        val dy = startPt.y - endPt.y
        val endpointsDistanceSq = dx * dx + dy * dy
        val minSeparationPx = 42.dp.toPx()
        val isLoopOrSameLocation = endpointsDistanceSq < (minSeparationPx * minSeparationPx) ||
                startLabel.trim().equals(endLabel.trim(), ignoreCase = true)

        val labelStyle = TextStyle(
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )

        if (isLoopOrSameLocation) {
            val pinCenter = Offset((startPt.x + endPt.x) / 2f, (startPt.y + endPt.y) / 2f)

            drawCircle(
                color = SummaryHaloGlow,
                radius = 16.dp.toPx(),
                center = pinCenter
            )
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = pinCenter,
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawCircle(
                color = SummaryElectricLime,
                radius = 4.5.dp.toPx(),
                center = pinCenter
            )

            val cleanLabel = startLabel.ifBlank { endLabel }.ifBlank { "Start & Finish" }
            val measured = textMeasurer.measure(cleanLabel, style = labelStyle)
            val textX = (pinCenter.x - measured.size.width / 2f)
                .coerceIn(12f, width - measured.size.width - 12f)
            val textY = if (pinCenter.y + 16.dp.toPx() + measured.size.height <= height) {
                pinCenter.y + 14.dp.toPx()
            } else {
                pinCenter.y - measured.size.height - 12.dp.toPx()
            }

            drawText(
                textMeasurer = textMeasurer,
                text = cleanLabel,
                topLeft = Offset(textX, textY),
                style = labelStyle
            )
        } else {
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

            val measuredStart = textMeasurer.measure(startLabel, style = labelStyle)
            val measuredEnd = textMeasurer.measure(endLabel, style = labelStyle)

            val startTextX = (startPt.x - measuredStart.size.width / 2f)
                .coerceIn(12f, width - measuredStart.size.width - 12f)
            val endTextX = (endPt.x - measuredEnd.size.width / 2f)
                .coerceIn(12f, width - measuredEnd.size.width - 12f)

            val isNearbyY = kotlin.math.abs(startPt.y - endPt.y) < 36.dp.toPx()
            val isNearbyX = kotlin.math.abs(startPt.x - endPt.x) < 80.dp.toPx()

            val (startTextY, endTextY) = if (isNearbyX && isNearbyY) {
                val sy = (startPt.y - measuredStart.size.height - 12.dp.toPx()).coerceAtLeast(8f)
                val ey = (endPt.y + 14.dp.toPx()).coerceAtMost(height - measuredEnd.size.height - 8f)
                sy to ey
            } else {
                val sy = if (startPt.y + 14.dp.toPx() + measuredStart.size.height <= height) {
                    startPt.y + 14.dp.toPx()
                } else {
                    startPt.y - measuredStart.size.height - 12.dp.toPx()
                }
                val ey = if (endPt.y + 16.dp.toPx() + measuredEnd.size.height <= height) {
                    endPt.y + 16.dp.toPx()
                } else {
                    endPt.y - measuredEnd.size.height - 12.dp.toPx()
                }
                sy to ey
            }

            drawText(
                textMeasurer = textMeasurer,
                text = startLabel,
                topLeft = Offset(startTextX, startTextY),
                style = labelStyle
            )

            drawText(
                textMeasurer = textMeasurer,
                text = endLabel,
                topLeft = Offset(endTextX, endTextY),
                style = labelStyle
            )
        }
    }
}

private fun sanitizeRouteCoordinates(raw: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
    if (raw.size <= 2) return raw
    val result = ArrayList<Pair<Double, Double>>(raw.size)
    result.add(raw.first())
    for (i in 1 until raw.size - 1) {
        val prev = result.last()
        val curr = raw[i]
        val dLat = (curr.first - prev.first) * 111_000.0
        val dLon = (curr.second - prev.second) * 111_000.0 * cos(Math.toRadians(prev.first))
        if (dLat * dLat + dLon * dLon >= 4.0) { // at least 2m apart
            result.add(curr)
        }
    }
    result.add(raw.last())
    return result
}

private fun lineStringJson(points: List<Pair<Double, Double>>): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[${points.joinToString { "[${it.second},${it.first}]" }}]}}"

private fun pointJson(latitude: Double, longitude: Double): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[${longitude},${latitude}]}}"

private fun smoothPointsChaikin(points: List<Offset>, iterations: Int = 2): List<Offset> {
    if (points.size <= 2) return points
    var current = points
    repeat(iterations) {
        val next = ArrayList<Offset>(current.size * 2)
        next.add(current.first())
        for (i in 0 until current.lastIndex) {
            val p0 = current[i]
            val p1 = current[i + 1]
            val q = Offset(0.75f * p0.x + 0.25f * p1.x, 0.75f * p0.y + 0.25f * p1.y)
            val r = Offset(0.25f * p0.x + 0.75f * p1.x, 0.25f * p0.y + 0.75f * p1.y)
            next.add(q)
            next.add(r)
        }
        next.add(current.last())
        current = next
    }
    return current
}

private fun DrawScope.drawAbstractGridLines(width: Float, height: Float) {
    val gridColor = Color(0x18FFFFFF)
    val gridStroke = 1.2.dp.toPx()
    val lines = listOf(
        Offset(0f, height * 0.32f) to Offset(width, height * 0.30f),
        Offset(0f, height * 0.62f) to Offset(width, height * 0.60f),
        Offset(0f, height * 0.82f) to Offset(width, height * 0.84f),
        Offset(width * 0.28f, 0f) to Offset(width * 0.26f, height),
        Offset(width * 0.72f, 0f) to Offset(width * 0.74f, height),
        Offset(0f, height * 0.75f) to Offset(width * 0.85f, 0f),
        Offset(width * 0.20f, height) to Offset(width, height * 0.28f)
    )

    lines.forEach { (start, end) ->
        drawLine(
            color = gridColor,
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

    val formattedDist = if (displayDist >= 100.0) {
        String.format(Locale.US, "%,d", displayDist.roundToInt())
    } else {
        String.format(Locale.US, "%.1f", displayDist)
    }

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
                border = BorderStroke(1.dp, Color(0xFF262C3A))
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
