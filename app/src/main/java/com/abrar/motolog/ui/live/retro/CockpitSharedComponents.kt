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
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.zIndex
import com.abrar.motolog.ui.theme.CockpitThemePalette

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
    modifier: Modifier = Modifier
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

        Row(verticalAlignment = Alignment.CenterVertically) {
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
        }
    }
}
