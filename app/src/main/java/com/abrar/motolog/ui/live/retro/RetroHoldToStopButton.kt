package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.ui.theme.JewelRed
import com.abrar.motolog.ui.theme.RetroBrass
import com.abrar.motolog.ui.theme.RetroIvory
import kotlinx.coroutines.launch

/**
 * 2-Second Hold-to-Stop button with customizable motorcycle styling
 * and visual progress fill animation. Complies with the 56dp glove-friendly touch target rule.
 */
@Composable
fun ThemedHoldToStopButton(
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "HOLD TO STOP",
    progressLabel: String = "HOLD",
    borderColor: Color = RetroBrass,
    gradientColors: List<Color> = listOf(
        Color(0xFF5E1717),
        Color(0xFF2C0B0B)
    ),
    progressFillColor: Color = JewelRed.copy(alpha = 0.5f),
    textColor: Color = RetroIvory,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp
) {
    val coroutineScope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(colors = gradientColors))
            .border(1.5.dp, borderColor, RoundedCornerShape(cornerRadius))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val animationJob = coroutineScope.launch {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                        )
                        onStopConfirmed()
                    }

                    // Wait until pointer is released or cancelled
                    var isUp = false
                    while (!isUp) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { it.id != down.id || !it.pressed }) {
                            isUp = true
                        }
                    }

                    animationJob.cancel()
                    coroutineScope.launch {
                        progress.snapTo(0f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Progress Fill Bar
        if (progress.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = progressFillColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width * progress.value, size.height)
                )
            }
        }

        Text(
            text = if (progress.value > 0f) {
                "$progressLabel (${(2.0 * (1f - progress.value) + 0.1).toInt()}s)"
            } else {
                label
            },
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * Vintage 2-Second Hold-to-Stop button with classic motorcycle ignition kill-switch styling.
 */
@Composable
fun RetroHoldToStopButton(
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    ThemedHoldToStopButton(
        onStopConfirmed = onStopConfirmed,
        modifier = modifier
    )
}
