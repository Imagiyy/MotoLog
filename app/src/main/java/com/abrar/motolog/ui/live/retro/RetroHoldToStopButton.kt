package com.abrar.motolog.ui.live.retro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.ui.theme.JewelRed
import com.abrar.motolog.ui.theme.RetroBrass
import com.abrar.motolog.ui.theme.RetroIvory

/**
 * Stop button with customizable motorcycle styling and instant press interaction.
 * Complies with the 56dp glove-friendly touch target rule.
 */
@Composable
fun ThemedHoldToStopButton(
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "STOP",
    progressLabel: String = "STOP",
    borderColor: Color = RetroBrass,
    gradientColors: List<Color> = listOf(
        Color(0xFF5E1717),
        Color(0xFF2C0B0B)
    ),
    progressFillColor: Color = JewelRed.copy(alpha = 0.5f),
    textColor: Color = RetroIvory,
    cornerRadius: Dp = 8.dp
) {
    val currentOnStopConfirmed by rememberUpdatedState(onStopConfirmed)
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(colors = gradientColors))
            .border(1.5.dp, borderColor, RoundedCornerShape(cornerRadius))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnStopConfirmed()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
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
 * Vintage Stop button with classic motorcycle ignition kill-switch styling.
 * Triggers stop immediately on press.
 */
@Composable
fun RetroHoldToStopButton(
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "STOP"
) {
    ThemedHoldToStopButton(
        onStopConfirmed = onStopConfirmed,
        modifier = modifier,
        label = label
    )
}

