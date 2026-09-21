package com.abrar.motolog.ui.live.retro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.abrar.motolog.ui.theme.RetroAmber
import com.abrar.motolog.ui.theme.RetroBrass
import com.abrar.motolog.ui.theme.RetroChrome
import com.abrar.motolog.ui.theme.RetroIvory
import com.abrar.motolog.ui.theme.RetroSurface
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette

/**
 * Vintage dashboard instrument gauge card with metallic rivets, brass trim,
 * and high-contrast glowing readout adapted to the current theme palette.
 */
@Composable
fun RetroInstrumentCard(
    label: String,
    value: String,
    unit: String = "",
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO),
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = if (palette.isLight) {
                        listOf(palette.surface, Color(0xFFECEFF1), palette.surface)
                    } else {
                        listOf(palette.surface, Color(0xFF14110E), Color(0xFF221C18))
                    },
                    start = Offset.Zero,
                    end = Offset(400f, 400f)
                )
            )
            .border(1.5.dp, palette.surfaceBorder.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(8.dp)
    ) {
        // 4 Corner Screws / Rivets
        RivetDot(modifier = Modifier.align(Alignment.TopStart), palette = palette)
        RivetDot(modifier = Modifier.align(Alignment.TopEnd), palette = palette)
        RivetDot(modifier = Modifier.align(Alignment.BottomStart), palette = palette)
        RivetDot(modifier = Modifier.align(Alignment.BottomEnd), palette = palette)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Label
            Text(
                text = label.uppercase(),
                color = palette.secondaryAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            // Primary Readout with theme glowing color
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = value,
                    color = palette.primaryAccent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.size(3.dp))
                    Text(
                        text = unit.uppercase(),
                        color = palette.dialText.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            // Subtitle / toggle hint
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = if (palette.isLight) Color(0xFF546E7A) else Color(0xFF8A8279),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RivetDot(
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White, palette.bezelOuter, Color(0xFF1E1E1E)),
                    radius = 8f
                )
            )
            .border(0.5.dp, if (palette.isLight) Color(0xFF90A4AE) else Color.Black, CircleShape)
    )
}
