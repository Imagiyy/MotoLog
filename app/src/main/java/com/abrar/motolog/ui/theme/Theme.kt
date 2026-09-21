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
 * Theme mode options for MotoLog.
 * AMOLED mode uses pure black backgrounds for OLED battery savings.
 */
enum class ThemeMode {
    RETRO,
    CAFE_RACER,
    TRACK_DAY,
    NEON_CYBER,
    DESERT_RALLY,
    DARK,
    AMOLED,
    LIGHT,
    SYSTEM
}

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val AmoledColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundAmoled,
    onBackground = OnBackgroundDark,
    surface = SurfaceAmoled,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantAmoled,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val RetroColorScheme = darkColorScheme(
    primary = RetroPrimary,
    onPrimary = RetroOnPrimary,
    primaryContainer = RetroPrimaryContainer,
    onPrimaryContainer = RetroOnPrimaryContainer,
    secondary = RetroSecondary,
    onSecondary = RetroOnSecondary,
    secondaryContainer = RetroSecondaryContainer,
    onSecondaryContainer = RetroOnSecondaryContainer,
    tertiary = RetroSecondary,
    onTertiary = RetroOnSecondary,
    tertiaryContainer = RetroSecondaryContainer,
    onTertiaryContainer = RetroOnSecondaryContainer,
    error = JewelRed,
    onError = Color.White,
    errorContainer = Color(0xFF5C0000),
    onErrorContainer = Color(0xFFFFCDD2),
    background = RetroBackground,
    onBackground = RetroOnBackground,
    surface = RetroSurface,
    onSurface = RetroOnSurface,
    surfaceVariant = RetroSurfaceVariant,
    onSurfaceVariant = RetroOnSurfaceVariant,
    outline = RetroOutline,
    outlineVariant = RetroOutlineVariant
)

private val CafeRacerColorScheme = darkColorScheme(
    primary = CafeRacerPrimary,
    onPrimary = CafeRacerOnPrimary,
    primaryContainer = CafeRacerPrimaryContainer,
    onPrimaryContainer = CafeRacerOnPrimaryContainer,
    secondary = CafeRacerSecondary,
    onSecondary = CafeRacerOnSecondary,
    secondaryContainer = CafeRacerSecondaryContainer,
    onSecondaryContainer = CafeRacerOnSecondaryContainer,
    tertiary = CafeRacerSecondary,
    onTertiary = CafeRacerOnSecondary,
    error = JewelRed,
    onError = Color.White,
    background = CafeRacerBackground,
    onBackground = CafeRacerOnBackground,
    surface = CafeRacerSurface,
    onSurface = CafeRacerOnSurface,
    surfaceVariant = CafeRacerSurfaceVariant,
    onSurfaceVariant = CafeRacerOnSurfaceVariant,
    outline = CafeRacerOutline,
    outlineVariant = Color(0xFF264A3B)
)

private val TrackDayColorScheme = darkColorScheme(
    primary = TrackDayPrimary,
    onPrimary = TrackDayOnPrimary,
    primaryContainer = TrackDayPrimaryContainer,
    onPrimaryContainer = TrackDayOnPrimaryContainer,
    secondary = TrackDaySecondary,
    onSecondary = TrackDayOnSecondary,
    secondaryContainer = TrackDaySecondaryContainer,
    onSecondaryContainer = TrackDayOnSecondaryContainer,
    tertiary = TrackDaySecondary,
    onTertiary = TrackDayOnSecondary,
    error = TrackDayPrimary,
    onError = Color.White,
    background = TrackDayBackground,
    onBackground = TrackDayOnBackground,
    surface = TrackDaySurface,
    onSurface = TrackDayOnSurface,
    surfaceVariant = TrackDaySurfaceVariant,
    onSurfaceVariant = TrackDayOnSurfaceVariant,
    outline = TrackDayOutline,
    outlineVariant = Color(0xFF424242)
)

private val NeonCyberColorScheme = darkColorScheme(
    primary = NeonCyberPrimary,
    onPrimary = NeonCyberOnPrimary,
    primaryContainer = NeonCyberPrimaryContainer,
    onPrimaryContainer = NeonCyberOnPrimaryContainer,
    secondary = NeonCyberSecondary,
    onSecondary = NeonCyberOnSecondary,
    secondaryContainer = NeonCyberSecondaryContainer,
    onSecondaryContainer = NeonCyberOnSecondaryContainer,
    tertiary = NeonCyberSecondary,
    onTertiary = NeonCyberOnSecondary,
    error = JewelRed,
    onError = Color.White,
    background = NeonCyberBackground,
    onBackground = NeonCyberOnBackground,
    surface = NeonCyberSurface,
    onSurface = NeonCyberOnSurface,
    surfaceVariant = NeonCyberSurfaceVariant,
    onSurfaceVariant = NeonCyberOnSurfaceVariant,
    outline = NeonCyberOutline,
    outlineVariant = Color(0xFF37275E)
)

private val DesertRallyColorScheme = darkColorScheme(
    primary = DesertRallyPrimary,
    onPrimary = DesertRallyOnPrimary,
    primaryContainer = DesertRallyPrimaryContainer,
    onPrimaryContainer = DesertRallyOnPrimaryContainer,
    secondary = DesertRallySecondary,
    onSecondary = DesertRallyOnSecondary,
    secondaryContainer = DesertRallySecondaryContainer,
    onSecondaryContainer = DesertRallyOnSecondaryContainer,
    tertiary = DesertRallySecondary,
    onTertiary = DesertRallyOnSecondary,
    error = JewelRed,
    onError = Color.White,
    background = DesertRallyBackground,
    onBackground = DesertRallyOnBackground,
    surface = DesertRallySurface,
    onSurface = DesertRallyOnSurface,
    surfaceVariant = DesertRallySurfaceVariant,
    onSurfaceVariant = DesertRallyOnSurfaceVariant,
    outline = DesertRallyOutline,
    outlineVariant = Color(0xFF453D34)
)

/**
 * Visual styling palette for the cockpit speedometer, gauges, and cards.
 * Automatically adapts the instrument cluster to the rider's chosen theme.
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
    return when (themeMode) {
        ThemeMode.RETRO -> CockpitThemePalette(
            background = RetroBackground,
            surface = RetroSurface,
            surfaceBorder = RetroBrass,
            bezelOuter = RetroChrome,
            bezelInner = RetroBrass,
            dialFace = RetroDialFace,
            dialText = RetroIvory,
            tickMajor = RetroBrass,
            tickMinor = RetroIvory.copy(alpha = 0.65f),
            needle = RetroNeedle,
            needleGradientStart = Color(0xFF7A1E0B),
            needleGradientEnd = Color(0xFFFF7A47),
            primaryAccent = RetroAmber,
            secondaryAccent = RetroBrass,
            hubColor = RetroBrass,
            hubCenter = RetroChrome
        )

        ThemeMode.CAFE_RACER -> CockpitThemePalette(
            background = CafeRacerBackground,
            surface = CafeRacerSurface,
            surfaceBorder = CafeRacerSecondary,
            bezelOuter = Color(0xFFECEFF1),
            bezelInner = CafeRacerPrimary,
            dialFace = Color(0xFF0F261E),
            dialText = CafeRacerIvory,
            tickMajor = Color(0xFFECEFF1),
            tickMinor = Color(0xFF81C784),
            needle = CafeRacerNeedle,
            needleGradientStart = Color(0xFFBF360C),
            needleGradientEnd = Color(0xFFFFB74D),
            primaryAccent = CafeRacerPrimary,
            secondaryAccent = CafeRacerSecondary,
            hubColor = Color(0xFFCFD8DC),
            hubCenter = Color(0xFF2E7D5B)
        )

        ThemeMode.TRACK_DAY -> CockpitThemePalette(
            background = TrackDayBackground,
            surface = TrackDaySurface,
            surfaceBorder = TrackDayPrimary,
            bezelOuter = Color(0xFF333333),
            bezelInner = TrackDayPrimary,
            dialFace = Color(0xFF0C0C0C),
            dialText = TrackDayIvory,
            tickMajor = TrackDaySecondary,
            tickMinor = Color(0xFF757575),
            needle = TrackDayNeedle,
            needleGradientStart = Color(0xFFB71C1C),
            needleGradientEnd = Color(0xFFFF5252),
            primaryAccent = TrackDayPrimary,
            secondaryAccent = TrackDaySecondary,
            hubColor = Color(0xFF37474F),
            hubCenter = TrackDayPrimary
        )

        ThemeMode.NEON_CYBER -> CockpitThemePalette(
            background = NeonCyberBackground,
            surface = NeonCyberSurface,
            surfaceBorder = NeonCyberPrimary,
            bezelOuter = NeonCyberSecondary,
            bezelInner = NeonCyberPrimary,
            dialFace = Color(0xFF0E0B1A),
            dialText = NeonCyberIvory,
            tickMajor = NeonCyberPrimary,
            tickMinor = NeonCyberSecondary.copy(alpha = 0.7f),
            needle = NeonCyberNeedle,
            needleGradientStart = Color(0xFF880E4F),
            needleGradientEnd = Color(0xFFFF4081),
            primaryAccent = NeonCyberPrimary,
            secondaryAccent = NeonCyberSecondary,
            hubColor = NeonCyberSecondary,
            hubCenter = NeonCyberPrimary
        )

        ThemeMode.DESERT_RALLY -> CockpitThemePalette(
            background = DesertRallyBackground,
            surface = DesertRallySurface,
            surfaceBorder = DesertRallyPrimary,
            bezelOuter = Color(0xFF5A4D3B),
            bezelInner = DesertRallySecondary,
            dialFace = Color(0xFF1B1713),
            dialText = DesertRallyIvory,
            tickMajor = DesertRallyPrimary,
            tickMinor = DesertRallySecondary,
            needle = DesertRallyNeedle,
            needleGradientStart = Color(0xFFE65100),
            needleGradientEnd = Color(0xFFFFB74D),
            primaryAccent = DesertRallyPrimary,
            secondaryAccent = DesertRallySecondary,
            hubColor = Color(0xFF6D4C41),
            hubCenter = DesertRallyPrimary
        )

        ThemeMode.AMOLED -> CockpitThemePalette(
            background = BackgroundAmoled,
            surface = SurfaceAmoled,
            surfaceBorder = Color(0xFF333333),
            bezelOuter = Color(0xFF222222),
            bezelInner = PrimaryDark,
            dialFace = Color(0xFF000000),
            dialText = Color.White,
            tickMajor = PrimaryDark,
            tickMinor = Color(0xFF555555),
            needle = RetroNeedle,
            needleGradientStart = Color(0xFFBF360C),
            needleGradientEnd = Color(0xFFFF5722),
            primaryAccent = PrimaryDark,
            secondaryAccent = SecondaryDark,
            hubColor = Color(0xFF212121),
            hubCenter = Color.White
        )

        ThemeMode.LIGHT -> CockpitThemePalette(
            background = BackgroundLight,
            surface = SurfaceLight,
            surfaceBorder = PrimaryLight,
            bezelOuter = Color(0xFFB0BEC5),
            bezelInner = PrimaryLight,
            dialFace = Color(0xFFF5EFEB),
            dialText = Color(0xFF1A1A1A),
            tickMajor = PrimaryLight,
            tickMinor = Color(0xFF78909C),
            needle = Color(0xFFD32F2F),
            needleGradientStart = Color(0xFFB71C1C),
            needleGradientEnd = Color(0xFFFF5252),
            primaryAccent = PrimaryLight,
            secondaryAccent = SecondaryLight,
            hubColor = Color(0xFF90A4AE),
            hubCenter = Color.Black,
            isLight = true
        )

        ThemeMode.DARK, ThemeMode.SYSTEM, null -> {
            if (themeMode == ThemeMode.SYSTEM && !isSystemDark) {
                CockpitThemePalette(
                    background = BackgroundLight,
                    surface = SurfaceLight,
                    surfaceBorder = PrimaryLight,
                    bezelOuter = Color(0xFFB0BEC5),
                    bezelInner = PrimaryLight,
                    dialFace = Color(0xFFF5EFEB),
                    dialText = Color(0xFF1A1A1A),
                    tickMajor = PrimaryLight,
                    tickMinor = Color(0xFF78909C),
                    needle = Color(0xFFD32F2F),
                    needleGradientStart = Color(0xFFB71C1C),
                    needleGradientEnd = Color(0xFFFF5252),
                    primaryAccent = PrimaryLight,
                    secondaryAccent = SecondaryLight,
                    hubColor = Color(0xFF90A4AE),
                    hubCenter = Color.Black,
                    isLight = true
                )
            } else {
                CockpitThemePalette(
                    background = BackgroundDark,
                    surface = SurfaceDark,
                    surfaceBorder = PrimaryDark,
                    bezelOuter = Color(0xFF78909C),
                    bezelInner = PrimaryDark,
                    dialFace = Color(0xFF151417),
                    dialText = Color(0xFFECE0DB),
                    tickMajor = PrimaryDark,
                    tickMinor = Color(0xFF90A4AE),
                    needle = Color(0xFFFF7043),
                    needleGradientStart = Color(0xFFD84315),
                    needleGradientEnd = Color(0xFFFFAB91),
                    primaryAccent = PrimaryDark,
                    secondaryAccent = SecondaryDark,
                    hubColor = Color(0xFF455A64),
                    hubCenter = PrimaryDark
                )
            }
        }
    }
}

/**
 * MotoLog Material 3 Theme.
 *
 * Supports themes:
 * - RETRO (Vintage Biker Cockpit)
 * - CAFE_RACER (British Racing Green & Aluminum)
 * - TRACK_DAY (Corse Racing Scarlet & Carbon)
 * - NEON_CYBER (Cyberpunk Tokyo Cyan & Magenta)
 * - DESERT_RALLY (Dakar Sand & Tactical Khaki)
 * - DARK (Modern Sport Dark)
 * - AMOLED (Night Stealth Pure Black)
 * - LIGHT (High-Noon Sunlight High-Contrast)
 * - SYSTEM (Follows Android Device Setting)
 */
@Composable
fun MotoLogTheme(
    themeMode: ThemeMode? = null,
    content: @Composable () -> Unit
) {
    val isDarkSystem = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        ThemeMode.RETRO -> RetroColorScheme
        ThemeMode.CAFE_RACER -> CafeRacerColorScheme
        ThemeMode.TRACK_DAY -> TrackDayColorScheme
        ThemeMode.NEON_CYBER -> NeonCyberColorScheme
        ThemeMode.DESERT_RALLY -> DesertRallyColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.SYSTEM, null -> if (isDarkSystem) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                colorScheme == LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MotoLogTypography,
        content = content
    )
}
