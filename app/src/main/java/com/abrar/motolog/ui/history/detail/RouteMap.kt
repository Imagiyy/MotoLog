package com.abrar.motolog.ui.history.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.abrar.motolog.data.map.MapSupport
import com.abrar.motolog.shared.domain.map.MapStyleProvider
import com.abrar.motolog.shared.domain.model.RouteMapData
import kotlinx.coroutines.CancellationException
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RouteMap(
    route: RouteMapData,
    mapStyleProvider: MapStyleProvider,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isNativeSupported = remember { MapSupport.isNativeSupported }
    if (!isNativeSupported) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    text = "Map rendering unavailable on this architecture",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RouteMapOverlay(
                minSpeedKmh = route.minSpeedKmh,
                maxSpeedKmh = route.maxSpeedKmh,
                attribution = mapStyleProvider.getAttribution(),
                mapUnavailable = true,
                isDarkMap = isDarkTheme,
                onToggleMapTheme = {},
                onZoomIn = {},
                onZoomOut = {},
                onFitRoute = {}
            )
        }
        return
    }

    var isDarkMap by rememberSaveable(isDarkTheme) { mutableStateOf(isDarkTheme) }
    var mapUnavailable by remember { mutableStateOf(false) }
    val styleUrl = mapStyleProvider.getStyleUrl(isDarkMap)
    val coroutineScope = rememberCoroutineScope()

    val firstPoint = route.start ?: route.segments.firstOrNull()?.points?.firstOrNull()
    val initialCameraPosition = remember(firstPoint) {
        if (firstPoint != null) {
            CameraPosition(
                bearing = 0.0,
                target = Position(firstPoint.longitude, firstPoint.latitude),
                tilt = 0.0,
                zoom = 13.0
            )
        } else {
            CameraPosition()
        }
    }

    val bucketGeoJsons = remember(route) {
        (0..4).map { b ->
            val segs = route.segments.filter { it.speedBucket == b }.map { it.points }
            bucketMultiLineJson(segs)
        }
    }
    val startGeoJson = remember(route.start) {
        route.start?.let { pointJson(it.latitude, it.longitude) } ?: EMPTY_GEOJSON
    }
    val endGeoJson = remember(route.end) {
        route.end?.let { pointJson(it.latitude, it.longitude) } ?: EMPTY_GEOJSON
    }

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = initialCameraPosition
    ) {
        val s0 = rememberGeoJsonSource(GeoJsonData.JsonString(bucketGeoJsons[0]))
        LineLayer(id = "route-b0", source = s0, color = const(bucketColors[0]), width = const(4.dp))

        val s1 = rememberGeoJsonSource(GeoJsonData.JsonString(bucketGeoJsons[1]))
        LineLayer(id = "route-b1", source = s1, color = const(bucketColors[1]), width = const(4.dp))

        val s2 = rememberGeoJsonSource(GeoJsonData.JsonString(bucketGeoJsons[2]))
        LineLayer(id = "route-b2", source = s2, color = const(bucketColors[2]), width = const(4.dp))

        val s3 = rememberGeoJsonSource(GeoJsonData.JsonString(bucketGeoJsons[3]))
        LineLayer(id = "route-b3", source = s3, color = const(bucketColors[3]), width = const(4.dp))

        val s4 = rememberGeoJsonSource(GeoJsonData.JsonString(bucketGeoJsons[4]))
        LineLayer(id = "route-b4", source = s4, color = const(bucketColors[4]), width = const(4.dp))

        val sStart = rememberGeoJsonSource(GeoJsonData.JsonString(startGeoJson))
        CircleLayer(id = "route-start", source = sStart, color = const(Color(0xFF39D98A)), radius = const(7.dp))

        val sEnd = rememberGeoJsonSource(GeoJsonData.JsonString(endGeoJson))
        CircleLayer(id = "route-end", source = sEnd, color = const(Color(0xFFFF6B6B)), radius = const(7.dp))
    }

    LaunchedEffect(route) {
        val points = route.segments.flatMap { it.points }
        if (points.isNotEmpty()) {
            try {
                val minLon = points.minOf { it.longitude }
                val minLat = points.minOf { it.latitude }
                val maxLon = points.maxOf { it.longitude }
                val maxLat = points.maxOf { it.latitude }
                val delta = 0.002
                val adjMinLon = if (minLon == maxLon) minLon - delta else minLon
                val adjMaxLon = if (minLon == maxLon) maxLon + delta else maxLon
                val adjMinLat = if (minLat == maxLat) minLat - delta else minLat
                val adjMaxLat = if (minLat == maxLat) maxLat + delta else maxLat

                mapState.animateCameraToBounds(
                    boundingBox = BoundingBox(
                        west = adjMinLon,
                        south = adjMinLat,
                        east = adjMaxLon,
                        north = adjMaxLat
                    ),
                    padding = PaddingValues(48.dp),
                    animation = CameraAnimation.Ease(duration = 700.milliseconds)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Non-fatal exception if viewport is still measuring
            }
        }
    }

    LaunchedEffect(mapState) {
        mapState.events.collect { event ->
            mapUnavailable = when (event) {
                is MapEvent.StyleLoadFailed, is MapEvent.SourceDataFailed -> true
                is MapEvent.StyleLoaded -> false
                else -> mapUnavailable
            }
        }
    }

    val uiOptions = remember {
        MapUiOptions {
            renderMode = AndroidRenderMode.Texture
        }
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        MaplibreMap(
            state = mapState,
            uiOptions = uiOptions,
            modifier = Modifier.fillMaxSize()
        )
        RouteMapOverlay(
            minSpeedKmh = route.minSpeedKmh,
            maxSpeedKmh = route.maxSpeedKmh,
            attribution = mapStyleProvider.getAttribution(),
            mapUnavailable = mapUnavailable,
            isDarkMap = isDarkMap,
            onToggleMapTheme = { isDarkMap = !isDarkMap },
            onZoomIn = {
                coroutineScope.launch {
                    try {
                        val currentZoom = mapState.cameraPosition.zoom
                        val nextZoom = (currentZoom + 1.0).coerceAtMost(20.0)
                        mapState.animateCameraPosition(
                            CameraPosition(
                                bearing = mapState.cameraPosition.bearing,
                                target = mapState.cameraPosition.target,
                                tilt = mapState.cameraPosition.tilt,
                                zoom = nextZoom
                            ),
                            CameraAnimation.Ease(duration = 250.milliseconds)
                        )
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
            },
            onZoomOut = {
                coroutineScope.launch {
                    try {
                        val currentZoom = mapState.cameraPosition.zoom
                        val nextZoom = (currentZoom - 1.0).coerceAtLeast(1.0)
                        mapState.animateCameraPosition(
                            CameraPosition(
                                bearing = mapState.cameraPosition.bearing,
                                target = mapState.cameraPosition.target,
                                tilt = mapState.cameraPosition.tilt,
                                zoom = nextZoom
                            ),
                            CameraAnimation.Ease(duration = 250.milliseconds)
                        )
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
            },
            onFitRoute = {
                coroutineScope.launch {
                    try {
                        val points = route.segments.flatMap { it.points }
                        if (points.isNotEmpty()) {
                            val minLon = points.minOf { it.longitude }
                            val minLat = points.minOf { it.latitude }
                            val maxLon = points.maxOf { it.longitude }
                            val maxLat = points.maxOf { it.latitude }
                            val delta = 0.002
                            mapState.animateCameraToBounds(
                                boundingBox = BoundingBox(
                                    west = if (minLon == maxLon) minLon - delta else minLon,
                                    south = if (minLat == maxLat) minLat - delta else minLat,
                                    east = if (minLon == maxLon) maxLon + delta else maxLon,
                                    north = if (minLat == maxLat) maxLat + delta else maxLat
                                ),
                                padding = PaddingValues(48.dp),
                                animation = CameraAnimation.Ease(duration = 500.milliseconds)
                            )
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
            }
        )
    }
}

@Composable
private fun RouteMapOverlay(
    minSpeedKmh: Double,
    maxSpeedKmh: Double,
    attribution: String,
    mapUnavailable: Boolean,
    isDarkMap: Boolean,
    onToggleMapTheme: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitRoute: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Floating Controls Column (Theme toggle, Zoom In, Zoom Out, Fit Route)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 3.dp
            ) {
                IconButton(
                    onClick = onToggleMapTheme,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkMap) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkMap) "Switch to Light Map" else "Switch to Dark Map",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 3.dp
            ) {
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 3.dp
            ) {
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 3.dp
            ) {
                IconButton(
                    onClick = onFitRoute,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Fit Entire Route",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("SLOW", color = bucketColors.first(), style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    bucketColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 6.dp)
                                .background(color)
                        )
                    }
                }
                Text(
                    "${minSpeedKmh.toInt()} - ${maxSpeedKmh.toInt()} km/h",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Surface(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 2.dp
        ) {
            Text(
                text = attribution,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .width(180.dp)
                    .clickable { uriHandler.openUri("https://www.openstreetmap.org/copyright") },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                softWrap = true
            )
        }
        if (mapUnavailable) {
            Text(
                text = "Map background unavailable",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val bucketColors = listOf(
    Color(0xFF39D98A),
    Color(0xFFB8D947),
    Color(0xFFFFC857),
    Color(0xFFFF8A4C),
    Color(0xFFFF5C5C)
)

private const val EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

private fun bucketMultiLineJson(segments: List<List<com.abrar.motolog.shared.domain.model.RouteMapPoint>>): String {
    if (segments.isEmpty()) return EMPTY_GEOJSON
    val validSegments = segments.filter { it.size >= 2 }
    if (validSegments.isEmpty()) return EMPTY_GEOJSON
    val coords = validSegments.joinToString(",") { pts ->
        "[" + pts.joinToString(",") { "[${it.longitude},${it.latitude}]" } + "]"
    }
    return "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"MultiLineString\",\"coordinates\":[$coords]}}"
}

private fun pointJson(latitude: Double, longitude: Double): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[${longitude},${latitude}]}}"
