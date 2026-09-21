package com.abrar.motolog.ui.live.retro

import android.graphics.Paint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.JewelRed
import com.abrar.motolog.ui.theme.RetroAmber
import com.abrar.motolog.ui.theme.RetroBrass
import com.abrar.motolog.ui.theme.RetroChrome
import com.abrar.motolog.ui.theme.RetroDialFace
import com.abrar.motolog.ui.theme.RetroIvory
import com.abrar.motolog.ui.theme.RetroNeedle
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vintage analog motorcycle speedometer gauge.
 * Features brushed chrome/brass bezels, engraved calibration tick marks,
 * animated mechanical inertia needle, and embedded digital readout.
 */
@Composable
fun RetroSpeedometerDial(
    currentSpeed: Double,
    isMetric: Boolean,
    isSpeedAlert: Boolean,
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    val maxGaugeSpeed = if (isMetric) 200f else 120f
    val redlineStart = if (isMetric) 140f else 85f
    val unitLabel = if (isMetric) "KM/H" else "MPH"

    // Sweep arc angles (in degrees): from 135° to 405° (sweep of 270°)
    val startAngle = 135f
    val sweepAngle = 270f

    val clampedSpeed = currentSpeed.toFloat().coerceIn(0f, maxGaugeSpeed)
    val targetNeedleAngle = startAngle + (clampedSpeed / maxGaugeSpeed) * sweepAngle

    // Physics spring simulation mimicking real speedometer mechanical spring inertia
    val animatedNeedleAngle by animateFloatAsState(
        targetValue = targetNeedleAngle,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessLow
        ),
        label = "speedo_needle"
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f
            if (outerRadius <= 0f) return@Canvas

            // 1. Outer Cast Iron & Chrome Multi-tier Bezel
            drawOuterBezel(center, outerRadius, palette)

            val dialRadius = outerRadius * 0.84f

            // 2. Dial Face Background & Texture
            drawDialFace(center, dialRadius, isSpeedAlert, palette)

            // 3. Calibration Ticks and Vintage Numerals
            drawSpeedometerCalibration(
                center = center,
                dialRadius = dialRadius,
                maxSpeed = maxGaugeSpeed,
                redlineStart = redlineStart,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                palette = palette
            )

            // 4. Digital Speed Window at bottom center of dial
            drawDigitalWindow(
                center = center,
                dialRadius = dialRadius,
                speed = currentSpeed,
                unitLabel = unitLabel,
                isSpeedAlert = isSpeedAlert,
                palette = palette
            )

            // 5. Sweeping Needle
            drawSweepingNeedle(
                center = center,
                dialRadius = dialRadius,
                angle = animatedNeedleAngle,
                palette = palette
            )

            // 6. Central Brass & Chrome Hub Cap
            drawCenterHub(center, dialRadius * 0.16f, palette)

            // 7. Glass Lens Glint Reflection
            drawLensHighlight(center, dialRadius)
        }
    }
}

private fun DrawScope.drawOuterBezel(center: Offset, radius: Float, palette: CockpitThemePalette) {
    // Outer casing rim
    drawCircle(
        color = if (palette.isLight) Color(0xFFD0D7DE) else Color(0xFF141210),
        radius = radius,
        center = center
    )

    // Brushed Metallic Bezel Ring
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                palette.bezelOuter,
                palette.bezelOuter.copy(alpha = 0.6f),
                palette.bezelOuter,
                Color.White.copy(alpha = if (palette.isLight) 0.5f else 0.85f),
                palette.bezelOuter,
                palette.bezelOuter.copy(alpha = 0.5f),
                palette.bezelOuter
            ),
            center = center
        ),
        radius = radius * 0.98f,
        center = center
    )

    // Shadow groove between outer and inner bezel
    drawCircle(
        color = if (palette.isLight) Color(0xFFB0BEC5) else Color(0xFF0A0908),
        radius = radius * 0.90f,
        center = center
    )

    // Inner Accent Ring
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.bezelInner.copy(alpha = 0.95f),
                palette.bezelInner,
                palette.bezelInner.copy(alpha = 0.55f)
            ),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius * 0.90f
        ),
        radius = radius * 0.88f,
        center = center
    )
    // Inner bevel shadow
    drawCircle(
        color = if (palette.isLight) Color(0xFFCFD8DC) else Color(0xFF1B1714),
        radius = radius * 0.85f,
        center = center
    )
}

private fun DrawScope.drawDialFace(center: Offset, dialRadius: Float, isSpeedAlert: Boolean, palette: CockpitThemePalette) {
    // Dial face base with slight warm vignette
    drawCircle(
        brush = Brush.radialGradient(
            colors = if (isSpeedAlert) {
                listOf(Color(0xFF4A1010), Color(0xFF260A0A), palette.dialFace)
            } else {
                listOf(
                    palette.dialFace.copy(alpha = 0.92f),
                    palette.dialFace,
                    palette.dialFace
                )
            },
            center = center,
            radius = dialRadius
        ),
        radius = dialRadius,
        center = center
    )

    // Machined concentric grooves
    drawCircle(
        color = palette.surfaceBorder.copy(alpha = if (palette.isLight) 0.18f else 0.35f),
        radius = dialRadius * 0.72f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        color = palette.surfaceBorder.copy(alpha = if (palette.isLight) 0.12f else 0.25f),
        radius = dialRadius * 0.52f,
        center = center,
        style = Stroke(width = 0.8.dp.toPx())
    )
}

private fun DrawScope.drawSpeedometerCalibration(
    center: Offset,
    dialRadius: Float,
    maxSpeed: Float,
    redlineStart: Float,
    startAngle: Float,
    sweepAngle: Float,
    palette: CockpitThemePalette
) {
    val step = if (maxSpeed > 150f) 10f else 5f
    val majorStep = if (maxSpeed > 150f) 20f else 10f
    val totalSteps = (maxSpeed / step).toInt()

    val textPaint = Paint().apply {
        isAntiAlias = true
        color = palette.dialText.toArgb()
        textSize = dialRadius * 0.12f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD
        )
    }

    val redlineTextPaint = Paint().apply {
        isAntiAlias = true
        color = JewelRed.toArgb()
        textSize = dialRadius * 0.12f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD
        )
    }

    val tickRadiusOuter = dialRadius * 0.94f
    val tickRadiusMajorInner = dialRadius * 0.81f
    val tickRadiusMinorInner = dialRadius * 0.87f
    val textRadius = dialRadius * 0.67f

    for (i in 0..totalSteps) {
        val speedValue = i * step
        val angleDeg = startAngle + (speedValue / maxSpeed) * sweepAngle
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val isMajor = (speedValue % majorStep == 0f)
        val isRedline = speedValue >= redlineStart

        val tickColor = if (isRedline) JewelRed else if (isMajor) palette.tickMajor else palette.tickMinor
        val innerR = if (isMajor) tickRadiusMajorInner else tickRadiusMinorInner
        val strokeW = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx()

        val startX = center.x + innerR * cos(angleRad)
        val startY = center.y + innerR * sin(angleRad)
        val endX = center.x + tickRadiusOuter * cos(angleRad)
        val endY = center.y + tickRadiusOuter * sin(angleRad)

        drawLine(
            color = tickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeW
        )

        // Draw numbers for major ticks
        if (isMajor) {
            val numX = center.x + textRadius * cos(angleRad)
            val numY = center.y + textRadius * sin(angleRad) + (textPaint.textSize / 3f)

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    speedValue.toInt().toString(),
                    numX,
                    numY,
                    if (isRedline) redlineTextPaint else textPaint
                )
            }
        }
    }
}

private fun DrawScope.drawDigitalWindow(
    center: Offset,
    dialRadius: Float,
    speed: Double,
    unitLabel: String,
    isSpeedAlert: Boolean,
    palette: CockpitThemePalette
) {
    val windowW = dialRadius * 0.72f
    val windowH = dialRadius * 0.34f
    val windowTop = center.y + dialRadius * 0.20f
    val windowLeft = center.x - (windowW / 2f)

    // Beveled frame
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.surfaceBorder, palette.surface),
            startY = windowTop - 2.dp.toPx(),
            endY = windowTop + windowH + 2.dp.toPx()
        ),
        topLeft = Offset(windowLeft - 2.dp.toPx(), windowTop - 2.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(windowW + 4.dp.toPx(), windowH + 4.dp.toPx())
    )

    // Inset LCD background
    drawRect(
        color = if (isSpeedAlert) Color(0xFF2B0A0A) else if (palette.isLight) Color(0xFFE8ECEF) else Color(0xFF0F0E0C),
        topLeft = Offset(windowLeft, windowTop),
        size = androidx.compose.ui.geometry.Size(windowW, windowH)
    )

    // Text for speed & unit
    val speedInt = speed.toInt().coerceAtLeast(0)
    val textPaint = Paint().apply {
        isAntiAlias = true
        color = if (isSpeedAlert) JewelRed.toArgb() else palette.primaryAccent.toArgb()
        textSize = windowH * 0.65f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD
        )
    }

    val unitPaint = Paint().apply {
        isAntiAlias = true
        color = palette.secondaryAccent.toArgb()
        textSize = windowH * 0.24f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD
        )
    }

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            speedInt.toString(),
            center.x,
            windowTop + windowH * 0.62f,
            textPaint
        )
        canvas.nativeCanvas.drawText(
            unitLabel,
            center.x,
            windowTop + windowH * 0.90f,
            unitPaint
        )
    }
}

private fun DrawScope.drawSweepingNeedle(
    center: Offset,
    dialRadius: Float,
    angle: Float,
    palette: CockpitThemePalette
) {
    rotate(degrees = angle, pivot = center) {
        val needleLength = dialRadius * 0.88f
        val tailLength = dialRadius * 0.24f
        val needleWidth = dialRadius * 0.045f

        val needlePath = Path().apply {
            // Needle tip
            moveTo(center.x + needleLength, center.y)
            // Top shoulder
            lineTo(center.x, center.y - needleWidth / 2f)
            // Counterweight tail top
            lineTo(center.x - tailLength, center.y - needleWidth * 0.8f)
            // Counterweight end rounded
            lineTo(center.x - tailLength * 1.05f, center.y)
            // Counterweight tail bottom
            lineTo(center.x - tailLength, center.y + needleWidth * 0.8f)
            // Bottom shoulder
            lineTo(center.x, center.y + needleWidth / 2f)
            close()
        }

        // Drop shadow for 3D realism
        drawPath(
            path = needlePath,
            color = Color.Black.copy(alpha = 0.5f)
        )

        // Tapered needle gradient
        drawPath(
            path = needlePath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    palette.needleGradientStart,
                    palette.needle,
                    palette.needleGradientEnd,
                    palette.dialText
                ),
                startX = center.x - tailLength,
                endX = center.x + needleLength
            )
        )

        // Center spine highlight
        drawLine(
            color = palette.dialText.copy(alpha = 0.8f),
            start = Offset(center.x + dialRadius * 0.08f, center.y),
            end = Offset(center.x + needleLength * 0.92f, center.y),
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

private fun DrawScope.drawCenterHub(center: Offset, hubRadius: Float, palette: CockpitThemePalette) {
    // Outer shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.6f),
        radius = hubRadius * 1.15f,
        center = center
    )

    // Hub Base
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.hubColor.copy(alpha = 0.95f), palette.hubColor, palette.hubColor.copy(alpha = 0.5f)),
            center = Offset(center.x - hubRadius * 0.3f, center.y - hubRadius * 0.3f),
            radius = hubRadius
        ),
        radius = hubRadius,
        center = center
    )

    // Center Hub Screw Accent
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color.White, palette.hubCenter, Color(0xFF222222)),
            start = Offset(center.x - hubRadius * 0.4f, center.y - hubRadius * 0.4f),
            end = Offset(center.x + hubRadius * 0.4f, center.y + hubRadius * 0.4f)
        ),
        radius = hubRadius * 0.45f,
        center = center
    )
}

private fun DrawScope.drawLensHighlight(center: Offset, dialRadius: Float) {
    // Subtle arched gloss reflection across upper arc of dial glass
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent
            ),
            center = Offset(center.x - dialRadius * 0.25f, center.y - dialRadius * 0.4f),
            radius = dialRadius * 0.8f
        ),
        radius = dialRadius * 0.85f,
        center = center
    )
}
