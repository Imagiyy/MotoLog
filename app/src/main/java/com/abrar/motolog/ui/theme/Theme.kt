package com.abrar.motolog.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * MotoLog single unified Racing TFT theme.
 * Inspired by AiM MXS motorcycle data logger displays.
 */
enum class ThemeMode {
    RACING_TFT          // AiM MXS-style Racing TFT Display
}

// Racing TFT Color Scheme (Cyan + Dark AMOLED)
private val RacingTftColorScheme = darkColorScheme(
    primary = RacingTftCyan,
    onPrimary = Color(0xFF002229),
    primaryContainer = Color(0xFF004954),
    onPrimaryContainer = Color(0xFF80F8FF),
    secondary = RacingTftGreen,
    onSecondary = Color(0xFF002A0B),
    secondaryContainer = Color(0xFF004D1B),
    onSecondaryContainer = Color(0xFFB3F2BC),
    tertiary = RacingTftYellow,
    onTertiary = Color(0xFF212121),
    error = RacingTftRed,
    onError = Color.White,
    background = RacingTftBackground,
    onBackground = Color(0xFFF5F5F7),
    surface = RacingTftSurface,
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF141720),
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = RacingTftCyan.copy(alpha = 0.4f),
    outlineVariant = Color(0xFF1A1D27)
)

/**
 * Visual styling palette for the cockpit speedometer, gauges, and cards.
 * Single unified AiM TFT-style palette.
 */
data class CockpitThemePalette(
    val background: Color,
    val surface: Color,
    val surfaceBorder: Color,
    val bezelOuter: Color,
    val bezelInner: Color,
    val dialFace: Color,
    val dialText: Color,
    val tickMajor: Color,
    val tickMinor: Color,
    val needle: Color,
    val needleGradientStart: Color,
    val needleGradientEnd: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val hubColor: Color,
    val hubCenter: Color,
    val isLight: Boolean = false
)

fun getCockpitThemePalette(themeMode: ThemeMode?, isSystemDark: Boolean = true): CockpitThemePalette {
    return CockpitThemePalette(
        background = RacingTftBackground,
        surface = RacingTftSurface,
        surfaceBorder = RacingTftCyan,
        bezelOuter = Color(0xFF1A1D27),
        bezelInner = RacingTftCyan,
        dialFace = Color(0xFF0A0D15),
        dialText = Color(0xFFF5F5F7),
        tickMajor = RacingTftCyan,
        tickMinor = Color(0xFF6B7280),
        needle = RacingTftCyan,
        needleGradientStart = Color(0xFF004D54),
        needleGradientEnd = RacingTftCyan,
        primaryAccent = RacingTftCyan,
        secondaryAccent = RacingTftGreen,
        hubColor = Color(0xFF0E1118),
        hubCenter = RacingTftCyan
    )
}

/**
 * MotoLog Material 3 Theme.
 *
 * Single unified Racing TFT theme inspired by AiM MXS data logger displays.
 */
@Composable
fun MotoLogTheme(
    themeMode: ThemeMode? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = RacingTftColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MotoLogTypography,
        content = content
    )
}
