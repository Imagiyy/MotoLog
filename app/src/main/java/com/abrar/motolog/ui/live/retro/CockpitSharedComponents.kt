package com.abrar.motolog.ui.live.retro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import com.abrar.motolog.ui.theme.JewelAmber
import com.abrar.motolog.ui.theme.JewelBlue
import com.abrar.motolog.ui.theme.RetroChrome
import com.abrar.motolog.ui.theme.RetroIvory
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Brush
import com.abrar.motolog.ui.theme.JewelGreen
import com.abrar.motolog.ui.theme.JewelRed
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.abrar.motolog.ui.theme.CockpitThemePalette

enum class JewelColor(val activeColor: Color, val inactiveColor: Color) {
    GREEN(JewelGreen, Color(0xFF1B3820)),
    AMBER(JewelAmber, Color(0xFF3E2D10)),
    RED(JewelRed, Color(0xFF3D1616)),
    BLUE(JewelBlue, Color(0xFF132B45))
}

@Composable
fun RetroJewelLamp(
    label: String,
    isActive: Boolean,
    color: JewelColor,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
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
        verticalArrangement = Arrangement.spacedBy(1.5.dp)
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

            val baseColor = if (isActive) color.activeColor else color.inactiveColor
            val effectiveAlpha = if (isActive) blinkAlpha else 0.6f

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

            drawCircle(
                color = Color.White.copy(alpha = if (isActive) 0.85f * effectiveAlpha else 0.3f),
                radius = lensRadius * 0.28f,
                center = Offset(center.x - lensRadius * 0.35f, center.y - lensRadius * 0.35f)
            )
        }

        Text(
            text = label,
            color = if (isActive) RetroIvory else Color(0xFF6B665F),
            fontSize = 8.5.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Shared animated slide-down drawer for landscape mode in motorcycle cockpit dashboards.
 * When the rider slides down, this drawer slides smoothly from the top of the screen,
 * displaying numerical telemetry cards (Distance, Time, Avg Speed, Top Speed) and
 * active/idle control buttons.
 */
@Composable
fun LandscapeTelemetryDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    backgroundColor: Color,
    borderColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(20f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(backgroundColor)
                .border(
                    1.5.dp,
                    borderColor,
                    RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()

            Spacer(modifier = Modifier.height(8.dp))

            // Dismiss handle / indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(borderColor.copy(alpha = 0.35f))
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Hide Telemetry",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SWIPE UP TO CLOSE",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Subtle pull-tab indicator at the top of the screen in landscape mode.
 * Informs the rider that sliding down reveals numerical telemetry values.
 */
@Composable
fun LandscapeTelemetryPullTab(
    visible: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    label: String = "TELEMETRY"
) {
    if (visible) {
        Box(
            modifier = modifier
                .zIndex(10f)
                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(backgroundColor.copy(alpha = 0.85f))
                .border(
                    1.dp,
                    borderColor.copy(alpha = 0.6f),
                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Show Telemetry",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    color = accentColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Top control bar for Idle / Standby dashboard view across all cockpit themes.
 */
@Composable
fun ThemedIdleTopBar(
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onNavigateToSettings: () -> Unit,
    palette: CockpitThemePalette,
    badgeText: String = "READY TO RIDE",
    modifier: Modifier = Modifier,
    onSwitchToMap: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(palette.surface)
                .border(1.2.dp, palette.surfaceBorder.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = badgeText,
                color = palette.secondaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onToggleKeepScreenOn,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (keepScreenOn) Icons.Default.ScreenLockPortrait else Icons.Default.ScreenRotation,
                    contentDescription = if (keepScreenOn) "Screen stay awake enabled" else "Screen stay awake disabled",
                    tint = if (keepScreenOn) palette.primaryAccent else palette.dialText.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Settings",
                    tint = palette.secondaryAccent
                )
            }
            if (onSwitchToMap != null) {
                Surface(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.2.dp, palette.primaryAccent.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                        .clickable { onSwitchToMap() },
                    color = palette.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Open Map",
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "MAP",
                            color = palette.dialText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated top-right Map button for landscape and active cockpit headers.
 */
@Composable
fun ThemedTopRightMapButton(
    onClick: () -> Unit,
    palette: CockpitThemePalette,
    modifier: Modifier = Modifier,
    label: String = "MAP"
) {
    Surface(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.2.dp, palette.primaryAccent.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        color = palette.surface.copy(alpha = 0.92f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "Open Map",
                tint = palette.primaryAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = palette.dialText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Unified active controls row for motorcycle dashboards.
 * Features a glove-friendly Pause/Resume toggle button and an instant Stop button.
 */
@Composable
fun ThemedCockpitControlsRow(
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopConfirmed: () -> Unit,
    palette: CockpitThemePalette,
    modifier: Modifier = Modifier,
    pauseLabel: String = "PAUSE",
    resumeLabel: String = "RESUME",
    stopLabel: String = "STOP",
    cornerRadius: Dp = 8.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pause / Resume Button (56dp min height)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    if (isPaused) {
                        Brush.verticalGradient(listOf(JewelGreen, Color(0xFF184E25)))
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                palette.secondaryAccent.copy(alpha = 0.7f),
                                palette.surface
                            )
                        )
                    }
                )
                .border(1.5.dp, palette.surfaceBorder, RoundedCornerShape(cornerRadius))
                .clickable {
                    if (isPaused) onResumeClick() else onPauseClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPaused) resumeLabel else pauseLabel,
                color = palette.dialText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Stop Button (56dp min height, instant tap)
        ThemedHoldToStopButton(
            onStopConfirmed = onStopConfirmed,
            label = stopLabel,
            borderColor = palette.primaryAccent.copy(alpha = 0.8f),
            cornerRadius = cornerRadius,
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        )
    }
}
