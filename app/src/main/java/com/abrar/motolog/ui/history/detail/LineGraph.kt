package com.abrar.motolog.ui.history.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.domain.model.GraphData
import com.abrar.motolog.domain.model.GraphMarker
import com.abrar.motolog.domain.model.GraphPoint
import kotlin.math.roundToInt

/**
 * Reusable Compose Canvas Line Graph for Speed-over-time and Elevation Profile.
 *
 * Supports:
 * - Broken lines across pauses and gaps (multiple segments)
 * - Gradient fill under line
 * - Horizontal dashed reference markers (e.g. Max / Avg speed)
 * - Touch-to-inspect crosshair & tooltip
 * - Formatted axis labels
 */
@Composable
fun LineGraph(
    data: GraphData,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color? = null,
    markers: List<GraphMarker> = emptyList(),
    heightDp: Int = 180
) {
    var touchX by remember { mutableStateOf<Float?>(null) }
    var touchPoint by remember { mutableStateOf<GraphPoint?>(null) }

    val xMin = data.xRange.start
    val xMax = data.xRange.endInclusive
    val yMin = data.yRange.start
    val yMax = data.yRange.endInclusive

    val xSpan = (xMax - xMin).coerceAtLeast(1f)
    val ySpan = (yMax - yMin).coerceAtLeast(1f)

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip header when inspecting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            touchPoint?.let { pt ->
                Text(
                    text = "Time: ${formatDuration(pt.x.toLong())}  •  ${data.yLabel}: ${"%.1f".format(pt.y)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } ?: run {
                Text(
                    text = data.yLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(data) {
                        detectTapGestures(
                            onPress = { offset ->
                                touchX = offset.x
                                touchPoint = findNearestPoint(offset.x, size.width.toFloat(), data, xMin, xSpan)
                            }
                        )
                    }
                    .pointerInput(data) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                touchX = change.position.x
                                touchPoint = findNearestPoint(change.position.x, size.width.toFloat(), data, xMin, xSpan)
                            },
                            onDragEnd = {
                                touchX = null
                                touchPoint = null
                            },
                            onDragCancel = {
                                touchX = null
                                touchPoint = null
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height

                // Draw background horizontal grid lines (3 lines: 0%, 50%, 100%)
                val gridLines = listOf(0.1f, 0.5f, 0.9f)
                for (ratio in gridLines) {
                    val y = height * (1f - ratio)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Function to map data point to canvas offset
                fun toOffset(pt: GraphPoint): Offset {
                    val nx = ((pt.x - xMin) / xSpan).coerceIn(0f, 1f)
                    val ny = ((pt.y - yMin) / ySpan).coerceIn(0f, 1f)
                    return Offset(
                        x = nx * width,
                        y = (1f - ny) * height
                    )
                }

                // Draw gradient fill if requested
                if (fillColor != null) {
                    val fillBrush = Brush.verticalGradient(
                        colors = listOf(
                            fillColor.copy(alpha = 0.35f),
                            fillColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = height
                    )

                    for (segment in data.segments) {
                        if (segment.points.size < 2) continue
                        val fillPath = Path().apply {
                            val firstOffset = toOffset(segment.points.first())
                            moveTo(firstOffset.x, height)
                            lineTo(firstOffset.x, firstOffset.y)
                            for (i in 1 until segment.points.size) {
                                val ptOffset = toOffset(segment.points[i])
                                lineTo(ptOffset.x, ptOffset.y)
                            }
                            val lastOffset = toOffset(segment.points.last())
                            lineTo(lastOffset.x, height)
                            close()
                        }
                        drawPath(path = fillPath, brush = fillBrush)
                    }
                }

                // Draw line segments
                for (segment in data.segments) {
                    if (segment.points.size < 2) continue
                    val linePath = Path().apply {
                        val first = toOffset(segment.points.first())
                        moveTo(first.x, first.y)
                        for (i in 1 until segment.points.size) {
                            val pt = toOffset(segment.points[i])
                            lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Draw reference markers
                for (marker in markers) {
                    val ny = ((marker.y - yMin) / ySpan).coerceIn(0f, 1f)
                    val markerY = (1f - ny) * height
                    val markerColor = Color(marker.colorArgb)

                    drawLine(
                        color = markerColor,
                        start = Offset(0f, markerY),
                        end = Offset(width, markerY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                }

                // Draw crosshair if touch is active
                touchX?.let { tx ->
                    val clampedTx = tx.coerceIn(0f, width)
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(clampedTx, 0f),
                        end = Offset(clampedTx, height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )

                    touchPoint?.let { pt ->
                        val ptOffset = toOffset(pt)
                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = ptOffset
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = ptOffset
                        )
                    }
                }
            }
        }

        // X-axis min / max time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(xMin.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatDuration(xMax.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

private fun findNearestPoint(
    touchX: Float,
    canvasWidth: Float,
    data: GraphData,
    xMin: Float,
    xSpan: Float
): GraphPoint? {
    if (canvasWidth <= 0f) return null
    val targetTime = xMin + (touchX / canvasWidth).coerceIn(0f, 1f) * xSpan

    var nearest: GraphPoint? = null
    var minDiff = Float.MAX_VALUE

    for (segment in data.segments) {
        for (point in segment.points) {
            val diff = kotlin.math.abs(point.x - targetTime)
            if (diff < minDiff) {
                minDiff = diff
                nearest = point
            }
        }
    }
    return nearest
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        "%d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}
