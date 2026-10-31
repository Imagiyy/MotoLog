package com.abrar.motolog.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * MotoLog single unified Racing TFT theme.
 * Inspired by AiM MXS motorcycle data logger displays.
 */
enum class ThemeMode {
    RACING_TFT          // AiM MXS-style Racing TFT Display
}

/**
 * Generates an adaptive Material 3 ColorScheme dynamically reflecting the user's selected colors.
 */
fun createDynamicColorScheme(
    textColor: Color = Color(0xFFFFFFFF),
    backgroundColor: Color = RacingTftBackground,
    accentColor: Color = RacingTftCyan,
    componentColor: Color = RacingTftSurface
): ColorScheme {
    val isLight = backgroundColor.luminance() > 0.5f
    val onAccent = if (accentColor.luminance() > 0.5f) Color.Black else Color.White
    val onComponent = if (componentColor.luminance() > 0.5f) Color.Black else textColor

    return if (isLight) {
        lightColorScheme(
            primary = accentColor,
            onPrimary = onAccent,
            primaryContainer = componentColor,
            onPrimaryContainer = onComponent,
            secondary = accentColor,
            onSecondary = onAccent,
            secondaryContainer = componentColor,
            onSecondaryContainer = onComponent,
            tertiary = accentColor,
            onTertiary = onAccent,
            error = RacingTftRed,
            onError = Color.White,
            background = backgroundColor,
            onBackground = textColor,
            surface = componentColor,
            onSurface = textColor,
            surfaceVariant = componentColor,
            onSurfaceVariant = textColor.copy(alpha = 0.75f),
            outline = accentColor.copy(alpha = 0.5f),
            outlineVariant = accentColor.copy(alpha = 0.25f)
        )
    } else {
        darkColorScheme(
            primary = accentColor,
            onPrimary = onAccent,
            primaryContainer = componentColor,
            onPrimaryContainer = onComponent,
            secondary = accentColor,
            onSecondary = onAccent,
            secondaryContainer = componentColor,
            onSecondaryContainer = onComponent,
            tertiary = accentColor,
            onTertiary = onAccent,
            error = RacingTftRed,
            onError = Color.White,
            background = backgroundColor,
            onBackground = textColor,
            surface = componentColor,
            onSurface = textColor,
            surfaceVariant = componentColor,
            onSurfaceVariant = textColor.copy(alpha = 0.75f),
            outline = accentColor.copy(alpha = 0.5f),
            outlineVariant = accentColor.copy(alpha = 0.25f)
        )
    }
}

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

fun getCockpitThemePalette(
    themeMode: ThemeMode? = null,
    isSystemDark: Boolean = true,
    textColor: Color = Color(0xFFF5F5F7),
    backgroundColor: Color = RacingTftBackground,
    accentColor: Color = RacingTftCyan,
    componentColor: Color = RacingTftSurface
): CockpitThemePalette {
    return CockpitThemePalette(
        background = backgroundColor,
        surface = componentColor,
        surfaceBorder = accentColor,
        bezelOuter = Color(0xFF1A1D27),
        bezelInner = accentColor,
        dialFace = backgroundColor,
        dialText = textColor,
        tickMajor = accentColor,
        tickMinor = textColor.copy(alpha = 0.4f),
        needle = accentColor,
        needleGradientStart = accentColor.copy(alpha = 0.4f),
        needleGradientEnd = accentColor,
        primaryAccent = accentColor,
        secondaryAccent = accentColor,
        hubColor = componentColor,
        hubCenter = accentColor,
        isLight = backgroundColor.luminance() > 0.5f
    )
}

/**
 * MotoLog Material 3 Theme.
 *
 * Dynamically applies user-customized colors across the entire app.
 */
@Composable
fun MotoLogTheme(
    themeMode: ThemeMode? = null,
    textColor: Color = Color(0xFFFFFFFF),
    backgroundColor: Color = RacingTftBackground,
    accentColor: Color = RacingTftCyan,
    componentColor: Color = RacingTftSurface,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(textColor, backgroundColor, accentColor, componentColor) {
        createDynamicColorScheme(
            textColor = textColor,
            backgroundColor = backgroundColor,
            accentColor = accentColor,
            componentColor = componentColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            val isLightBg = backgroundColor.luminance() > 0.5f
            insetsController.isAppearanceLightStatusBars = isLightBg
            insetsController.isAppearanceLightNavigationBars = isLightBg
            window.statusBarColor = backgroundColor.toArgb()
            window.navigationBarColor = componentColor.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MotoLogTypography,
        content = content
    )
}
