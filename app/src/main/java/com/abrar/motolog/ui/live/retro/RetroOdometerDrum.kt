package com.abrar.motolog.ui.live.retro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.RetroBrass
import com.abrar.motolog.ui.theme.RetroIvory
import com.abrar.motolog.ui.theme.RetroNeedle
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import java.util.Locale

/**
 * Mechanical rolling drum odometer widget.
 * Features 5 whole-unit cylinders and 1 decimal cylinder, styled dynamically with the cockpit palette.
 */
@Composable
fun RetroOdometerDrum(
    distanceValue: Double,
    unitLabel: String,
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    // Format distance as 6 characters: 5 integer digits + 1 decimal digit
    val clamped = distanceValue.coerceAtLeast(0.0)
    val totalTenths = (clamped * 10).toLong()
    val integerPart = (totalTenths / 10).coerceAtMost(99999L)
    val decimalPart = (totalTenths % 10).toInt()

    val integerDigits = String.format(Locale.US, "%05d", integerPart).map { it.toString() }
    val decimalDigit = decimalPart.toString()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Outer metallic bezel frame
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (palette.isLight) {
                            listOf(Color(0xFFCFD8DC), Color(0xFFECEFF1), Color(0xFFB0BEC5))
                        } else {
                            listOf(Color(0xFF2C2825), Color(0xFF141210), Color(0xFF38332E))
                        }
                    )
                )
                .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                .padding(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 5 Integer digits
                integerDigits.forEach { digit ->
                    OdometerDrumWheel(
                        digit = digit,
                        isDecimal = false,
                        palette = palette
                    )
                }

                // Decimal wheel (tenth)
                OdometerDrumWheel(
                    digit = decimalDigit,
                    isDecimal = true,
                    palette = palette
                )
            }
        }

        // Label underneath
        Text(
            text = unitLabel.uppercase(Locale.US),
            color = palette.secondaryAccent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun OdometerDrumWheel(
    digit: String,
    isDecimal: Boolean,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    val bgColor = if (isDecimal) palette.needle else if (palette.isLight) Color(0xFF263238) else Color(0xFF111111)
    val textColor = if (isDecimal) Color.Black else palette.dialText

    Box(
        modifier = Modifier
            .width(14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(bgColor)
            // Drum cylindrical curvature shadow
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.65f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.70f)
                    )
                )
            )
            .border(0.5.dp, if (palette.isLight) Color(0xFF455A64) else Color(0xFF2A2825), RoundedCornerShape(2.dp))
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}
