package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.ui.theme.JewelAmber
import com.abrar.motolog.ui.theme.JewelBlue
import com.abrar.motolog.ui.theme.JewelGreen
import com.abrar.motolog.ui.theme.JewelRed
import com.abrar.motolog.ui.theme.RetroChrome
import com.abrar.motolog.ui.theme.RetroIvory

enum class JewelColor(val activeColor: Color, val inactiveColor: Color) {
    GREEN(JewelGreen, Color(0xFF1B3820)),
    AMBER(JewelAmber, Color(0xFF3E2D10)),
    RED(JewelRed, Color(0xFF3D1616)),
    BLUE(JewelBlue, Color(0xFF132B45))
}

/**
 * Classic convex faceted jewel indicator light in a knurled chrome bezel ring.
 */
@Composable
fun RetroJewelLamp(
    label: String,
    isActive: Boolean,
    color: JewelColor,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    shouldBlink: Boolean = false
) {
    val blinkAlpha = if (shouldBlink && isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "jewel_blink")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jewel_alpha"
        )
        alpha
    } else {
        1f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f
            val bezelRadius = radius * 0.95f
            val lensRadius = radius * 0.75f

            // Outer chrome bezel
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF8C8A87), RetroChrome, Color(0xFF333333), Color(0xFFAAAAAA)),
                    start = Offset.Zero,
                    end = Offset(this.size.width, this.size.height)
                ),
                radius = bezelRadius,
                center = center
            )
            // Bezel inner groove
            drawCircle(
                color = Color(0xFF111111),
                radius = lensRadius + 1f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Jewel glass core
            val baseColor = if (isActive) color.activeColor else color.inactiveColor
            val effectiveAlpha = if (isActive) blinkAlpha else 0.6f

            // Aura glow when active
            if (isActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(baseColor.copy(alpha = 0.6f * effectiveAlpha), Color.Transparent),
                        center = center,
                        radius = radius * 1.05f
                    ),
                    radius = radius * 1.05f,
                    center = center
                )
            }

            // Lens convex dome gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = effectiveAlpha),
                        baseColor.copy(alpha = (effectiveAlpha * 0.8f).coerceAtLeast(0.1f)),
                        Color(0xFF0D0D0D)
                    ),
                    center = Offset(center.x - lensRadius * 0.25f, center.y - lensRadius * 0.25f),
                    radius = lensRadius
                ),
                radius = lensRadius,
                center = center
            )

            // Upper-left specular glint / facet reflection
            drawCircle(
                color = Color.White.copy(alpha = if (isActive) 0.85f * effectiveAlpha else 0.3f),
                radius = lensRadius * 0.28f,
                center = Offset(center.x - lensRadius * 0.35f, center.y - lensRadius * 0.35f)
            )
        }

        Text(
            text = label,
            color = if (isActive) RetroIvory else Color(0xFF6B665F),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
