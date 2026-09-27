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
 * 8 Dedicated Unique Motorcycle Cockpit Themes for MotoLog.
 * Each theme represents a distinct motorcycle discipline with its own custom
 * instrument dashboard layout, speed visualization architecture, and telemetry cards.
 * Zero generic recolors.
 */
enum class ThemeMode {
    TRACK_DAY,          // Superbike MotoGP / WorldSBK Panoramic TFT Display
    DESERT_RALLY,       // Dakar Rally Navigation Tower & Dual-Trip Master
    NEON_CYBER,         // Tokyo Synthwave Holographic HUD with Hex Dial
    RETRO_CLASSIC,      // 1960s Smiths Chronometer & Rolling Mechanical Drum
    STEALTH_HUD,        // Fighter Jet Collimator Reticle & Pure OLED Black
    ADVENTURE_TOURING,  // Globe Explorer GS Alpine Split Cockpit & Live Altimeter
    CUSTOM_BOBBER,      // American V-Twin 180° Machined Billet Arc Speedometer
    NIGHT_CRUISER       // Midnight Highway Low-Glare Ambient Horizon
}

// 1. Track Day (Corse Racing Scarlet & Carbon Fiber)
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

// 2. Desert Rally (Dakar Sand & Tactical Khaki)
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

// 3. Neon Cyber (Tokyo Night, Electric Cyan & Hot Magenta)
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

// 4. Retro Classic (British Racing Green, Polished Aluminium & Vintage Ivory)
private val RetroClassicColorScheme = darkColorScheme(
    primary = CafeRacerPrimary,
    onPrimary = CafeRacerOnPrimary,
    primaryContainer = CafeRacerPrimaryContainer,
    onPrimaryContainer = CafeRacerOnPrimaryContainer,
    secondary = CafeRacerSecondary,
    onSecondary = CafeRacerOnSecondary,
    secondaryContainer = CafeRacerSecondaryContainer,
    onSecondaryContainer = CafeRacerOnSecondaryContainer,
    tertiary = RetroBrass,
    onTertiary = RetroOnSecondary,
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

// 5. Stealth HUD (Fighter Jet Collimator & Pure OLED Black)
private val StealthHudColorScheme = darkColorScheme(
    primary = StealthHudPrimary,
    onPrimary = StealthHudOnPrimary,
    primaryContainer = StealthHudPrimaryContainer,
    onPrimaryContainer = StealthHudOnPrimaryContainer,
    secondary = StealthHudSecondary,
    onSecondary = StealthHudOnSecondary,
    secondaryContainer = StealthHudSecondaryContainer,
    onSecondaryContainer = StealthHudOnSecondaryContainer,
    tertiary = StealthHudNeedle,
    onTertiary = StealthHudOnPrimary,
    error = JewelRed,
    onError = Color.White,
    background = StealthHudBackground,
    onBackground = StealthHudOnBackground,
    surface = StealthHudSurface,
    onSurface = StealthHudOnSurface,
    surfaceVariant = StealthHudSurfaceVariant,
    onSurfaceVariant = StealthHudOnSurfaceVariant,
    outline = StealthHudOutline,
    outlineVariant = Color(0xFF14331C)
)

// 6. Adventure Touring (Globe Explorer GS Alpine & Cordoba Blue)
private val AdventureTouringColorScheme = darkColorScheme(
    primary = AdventureTouringPrimary,
    onPrimary = AdventureTouringOnPrimary,
    primaryContainer = AdventureTouringPrimaryContainer,
    onPrimaryContainer = AdventureTouringOnPrimaryContainer,
    secondary = AdventureTouringSecondary,
    onSecondary = AdventureTouringOnSecondary,
    secondaryContainer = AdventureTouringSecondaryContainer,
    onSecondaryContainer = AdventureTouringOnSecondaryContainer,
    tertiary = AdventureTouringGlacial,
    onTertiary = AdventureTouringOnPrimary,
    error = JewelRed,
    onError = Color.White,
    background = AdventureTouringBackground,
    onBackground = AdventureTouringOnBackground,
    surface = AdventureTouringSurface,
    onSurface = AdventureTouringOnSurface,
    surfaceVariant = AdventureTouringSurfaceVariant,
    onSurfaceVariant = AdventureTouringOnSurfaceVariant,
    outline = AdventureTouringOutline,
    outlineVariant = Color(0xFF253041)
)

// 7. Custom Bobber (American V-Twin Billet Bronze & Raw Steel)
private val CustomBobberColorScheme = darkColorScheme(
    primary = CustomBobberPrimary,
    onPrimary = CustomBobberOnPrimary,
    primaryContainer = CustomBobberPrimaryContainer,
    onPrimaryContainer = CustomBobberOnPrimaryContainer,
    secondary = CustomBobberSecondary,
    onSecondary = CustomBobberOnSecondary,
    secondaryContainer = CustomBobberSecondaryContainer,
    onSecondaryContainer = CustomBobberOnSecondaryContainer,
    tertiary = CustomBobberSteel,
    onTertiary = CustomBobberOnPrimary,
    error = JewelRed,
    onError = Color.White,
    background = CustomBobberBackground,
    onBackground = CustomBobberOnBackground,
    surface = CustomBobberSurface,
    onSurface = CustomBobberOnSurface,
    surfaceVariant = CustomBobberSurfaceVariant,
    onSurfaceVariant = CustomBobberOnSurfaceVariant,
    outline = CustomBobberOutline,
    outlineVariant = Color(0xFF423328)
)

// 8. Night Cruiser (Midnight Interstate Horizon & Starlight Blue)
private val NightCruiserColorScheme = darkColorScheme(
    primary = NightCruiserPrimary,
    onPrimary = NightCruiserOnPrimary,
    primaryContainer = NightCruiserPrimaryContainer,
    onPrimaryContainer = NightCruiserOnPrimaryContainer,
    secondary = NightCruiserSecondary,
    onSecondary = NightCruiserOnSecondary,
    secondaryContainer = NightCruiserSecondaryContainer,
    onSecondaryContainer = NightCruiserOnSecondaryContainer,
    tertiary = NightCruiserHorizon,
    onTertiary = NightCruiserOnPrimary,
    error = JewelRed,
    onError = Color.White,
    background = NightCruiserBackground,
    onBackground = NightCruiserOnBackground,
    surface = NightCruiserSurface,
    onSurface = NightCruiserOnSurface,
    surfaceVariant = NightCruiserSurfaceVariant,
    onSurfaceVariant = NightCruiserOnSurfaceVariant,
    outline = NightCruiserOutline,
    outlineVariant = Color(0xFF1E2D4F)
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
    return when (themeMode ?: ThemeMode.TRACK_DAY) {
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

        ThemeMode.RETRO_CLASSIC -> CockpitThemePalette(
            background = CafeRacerBackground,
            surface = CafeRacerSurface,
            surfaceBorder = RetroBrass,
            bezelOuter = Color(0xFFD2D9DE),
            bezelInner = CafeRacerPrimary,
            dialFace = Color(0xFF092015),
            dialText = CafeRacerIvory,
            tickMajor = Color(0xFFD2D9DE),
            tickMinor = Color(0xFF81C784),
            needle = CafeRacerNeedle,
            needleGradientStart = Color(0xFFBF360C),
            needleGradientEnd = Color(0xFFFFB74D),
            primaryAccent = CafeRacerPrimary,
            secondaryAccent = RetroBrass,
            hubColor = Color(0xFFCFD8DC),
            hubCenter = Color(0xFF2E7D5B)
        )

        ThemeMode.STEALTH_HUD -> CockpitThemePalette(
            background = StealthHudBackground,
            surface = StealthHudSurface,
            surfaceBorder = StealthHudPrimary,
            bezelOuter = Color(0xFF1B4D2B),
            bezelInner = StealthHudPrimary,
            dialFace = Color(0xFF000000),
            dialText = StealthHudPrimary,
            tickMajor = StealthHudPrimary,
            tickMinor = StealthHudSecondary.copy(alpha = 0.6f),
            needle = StealthHudNeedle,
            needleGradientStart = Color(0xFF009624),
            needleGradientEnd = Color(0xFF00FF66),
            primaryAccent = StealthHudPrimary,
            secondaryAccent = StealthHudSecondary,
            hubColor = Color(0xFF003314),
            hubCenter = StealthHudPrimary
        )

        ThemeMode.ADVENTURE_TOURING -> CockpitThemePalette(
            background = AdventureTouringBackground,
            surface = AdventureTouringSurface,
            surfaceBorder = AdventureTouringPrimary,
            bezelOuter = Color(0xFF30363D),
            bezelInner = AdventureTouringPrimary,
            dialFace = Color(0xFF161B22),
            dialText = Color(0xFFF0F6FC),
            tickMajor = AdventureTouringPrimary,
            tickMinor = AdventureTouringGlacial,
            needle = AdventureTouringNeedle,
            needleGradientStart = Color(0xFFE65100),
            needleGradientEnd = Color(0xFFFFB300),
            primaryAccent = AdventureTouringPrimary,
            secondaryAccent = AdventureTouringSecondary,
            hubColor = Color(0xFF21262D),
            hubCenter = AdventureTouringGlacial
        )

        ThemeMode.CUSTOM_BOBBER -> CockpitThemePalette(
            background = CustomBobberBackground,
            surface = CustomBobberSurface,
            surfaceBorder = CustomBobberPrimary,
            bezelOuter = Color(0xFF4A3423),
            bezelInner = CustomBobberPrimary,
            dialFace = Color(0xFF1A1512),
            dialText = Color(0xFFF5EBE1),
            tickMajor = CustomBobberPrimary,
            tickMinor = CustomBobberSteel,
            needle = CustomBobberNeedle,
            needleGradientStart = Color(0xFFD84315),
            needleGradientEnd = Color(0xFFFF851B),
            primaryAccent = CustomBobberPrimary,
            secondaryAccent = CustomBobberSecondary,
            hubColor = Color(0xFF33251B),
            hubCenter = CustomBobberPrimary
        )

        ThemeMode.NIGHT_CRUISER -> CockpitThemePalette(
            background = NightCruiserBackground,
            surface = NightCruiserSurface,
            surfaceBorder = NightCruiserPrimary,
            bezelOuter = Color(0xFF1E2D4F),
            bezelInner = NightCruiserPrimary,
            dialFace = Color(0xFF0E172E),
            dialText = Color(0xFFEDF2F7),
            tickMajor = NightCruiserPrimary,
            tickMinor = NightCruiserSecondary,
            needle = NightCruiserNeedle,
            needleGradientStart = Color(0xFF0284C7),
            needleGradientEnd = Color(0xFF38BDF8),
            primaryAccent = NightCruiserPrimary,
            secondaryAccent = NightCruiserSecondary,
            hubColor = Color(0xFF182442),
            hubCenter = NightCruiserPrimary
        )
    }
}

/**
 * MotoLog Material 3 Theme.
 *
 * Supports the 8 unique motorcycle themes:
 * - TRACK_DAY (Corse Racing Scarlet & Carbon)
 * - DESERT_RALLY (Dakar Sand & Tactical Khaki)
 * - NEON_CYBER (Tokyo Night & Electric Cyan)
 * - RETRO_CLASSIC (Smiths Chrono & British Racing Green)
 * - STEALTH_HUD (Pure OLED Black & Phosphor Green)
 * - ADVENTURE_TOURING (GS Alpine Basalt & Cordoba Blue)
 * - CUSTOM_BOBBER (American V-Twin Billet Bronze & Raw Steel)
 * - NIGHT_CRUISER (Midnight Interstate Horizon & Starlight Blue)
 */
@Composable
fun MotoLogTheme(
    themeMode: ThemeMode? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode ?: ThemeMode.TRACK_DAY) {
        ThemeMode.TRACK_DAY -> TrackDayColorScheme
        ThemeMode.DESERT_RALLY -> DesertRallyColorScheme
        ThemeMode.NEON_CYBER -> NeonCyberColorScheme
        ThemeMode.RETRO_CLASSIC -> RetroClassicColorScheme
        ThemeMode.STEALTH_HUD -> StealthHudColorScheme
        ThemeMode.ADVENTURE_TOURING -> AdventureTouringColorScheme
        ThemeMode.CUSTOM_BOBBER -> CustomBobberColorScheme
        ThemeMode.NIGHT_CRUISER -> NightCruiserColorScheme
    }

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
