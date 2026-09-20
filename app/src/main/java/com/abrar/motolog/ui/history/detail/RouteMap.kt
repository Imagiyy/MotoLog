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
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox
import com.abrar.motolog.domain.map.MapStyleProvider
import com.abrar.motolog.domain.model.RouteMapData
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RouteMap(
    route: RouteMapData,
    mapStyleProvider: MapStyleProvider,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var mapUnavailable by remember { mutableStateOf(false) }
    val styleUrl = mapStyleProvider.getStyleUrl(isDarkTheme)
    val mapState = rememberMapState(baseStyle = BaseStyle.Uri(styleUrl)) {
        route.segments.forEachIndexed { index, segment ->
            val source = rememberGeoJsonSource(GeoJsonData.JsonString(segmentJson(segment.points)))
            LineLayer(
                id = "route-$index",
                source = source,
                color = const(bucketColors[index.coerceIn(0, 4)]),
                width = const(4.dp)
            )
        }
        route.start?.let {
            val source = rememberGeoJsonSource(GeoJsonData.JsonString(pointJson(it.latitude, it.longitude)))
            CircleLayer(id = "route-start", source = source, color = const(Color(0xFF39D98A)), radius = const(7.dp))
        }
        route.end?.let {
            val source = rememberGeoJsonSource(GeoJsonData.JsonString(pointJson(it.latitude, it.longitude)))
            CircleLayer(id = "route-end", source = source, color = const(Color(0xFFFF6B6B)), radius = const(7.dp))
        }
    }

    LaunchedEffect(route) {
        val points = route.segments.flatMap { it.points }
        if (points.isNotEmpty()) {
            mapState.animateCameraToBounds(
                boundingBox = BoundingBox(
                    points.minOf { it.longitude },
                    points.minOf { it.latitude },
                    points.maxOf { it.longitude },
                    points.maxOf { it.latitude }
                ),
                padding = PaddingValues(48.dp),
                animation = CameraAnimation.Ease(duration = 700.milliseconds)
            )
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

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        MaplibreMap(state = mapState, modifier = Modifier.fillMaxSize())
        RouteMapOverlay(
            minSpeedKmh = route.minSpeedKmh,
            maxSpeedKmh = route.maxSpeedKmh,
            attribution = mapStyleProvider.getAttribution(),
            mapUnavailable = mapUnavailable
        )
    }
}

@Composable
private fun RouteMapOverlay(
    minSpeedKmh: Double,
    maxSpeedKmh: Double,
    attribution: String,
    mapUnavailable: Boolean
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.End
    ) {
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

private fun segmentJson(points: List<com.abrar.motolog.domain.model.RouteMapPoint>): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[${points.joinToString { "[${it.longitude},${it.latitude}]" }}]}}"

private fun pointJson(latitude: Double, longitude: Double): String =
    "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[${longitude},${latitude}]}}"
