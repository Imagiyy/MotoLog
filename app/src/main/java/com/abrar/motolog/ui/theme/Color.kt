package com.abrar.motolog.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// MotoLog Color Palette
// Designed for outdoor visibility, high contrast, AMOLED-friendly
// ============================================================

// Primary: Vibrant amber/orange — high visibility in sunlight
val PrimaryLight = Color(0xFFBF5700)       // Deep amber
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFFFDCC2)
val OnPrimaryContainerLight = Color(0xFF301400)

val PrimaryDark = Color(0xFFFFB77C)        // Warm amber
val OnPrimaryDark = Color(0xFF4E2600)
val PrimaryContainerDark = Color(0xFF6F3800)
val OnPrimaryContainerDark = Color(0xFFFFDCC2)

// Secondary: Cool slate blue — complementary contrast
val SecondaryLight = Color(0xFF735B4A)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFFFDCC2)
val OnSecondaryContainerLight = Color(0xFF2A180C)

val SecondaryDark = Color(0xFFE0C0A8)
val OnSecondaryDark = Color(0xFF412D1F)
val SecondaryContainerDark = Color(0xFF5A4334)
val OnSecondaryContainerDark = Color(0xFFFFDCC2)

// Tertiary: Teal/cyan — for accent elements
val TertiaryLight = Color(0xFF566419)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFD9EA91)
val OnTertiaryContainerLight = Color(0xFF182000)

val TertiaryDark = Color(0xFFBDCE78)
val OnTertiaryDark = Color(0xFF2D3500)
val TertiaryContainerDark = Color(0xFF434C03)
val OnTertiaryContainerDark = Color(0xFFD9EA91)

// Error
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Background & Surface — Light
val BackgroundLight = Color(0xFFFFF8F5)
val OnBackgroundLight = Color(0xFF211A15)
val SurfaceLight = Color(0xFFFFF8F5)
val OnSurfaceLight = Color(0xFF211A15)
val SurfaceVariantLight = Color(0xFFF4DED2)
val OnSurfaceVariantLight = Color(0xFF52443B)
val OutlineLight = Color(0xFF85746A)
val OutlineVariantLight = Color(0xFFD7C3B7)

// Background & Surface — Dark
val BackgroundDark = Color(0xFF1A120D)
val OnBackgroundDark = Color(0xFFF0DFD5)
val SurfaceDark = Color(0xFF1A120D)
val OnSurfaceDark = Color(0xFFF0DFD5)
val SurfaceVariantDark = Color(0xFF52443B)
val OnSurfaceVariantDark = Color(0xFFD7C3B7)
val OutlineDark = Color(0xFF9F8D83)
val OutlineVariantDark = Color(0xFF52443B)

// AMOLED Dark — pure black backgrounds for maximum battery savings
val BackgroundAmoled = Color(0xFF000000)
val SurfaceAmoled = Color(0xFF000000)
val SurfaceVariantAmoled = Color(0xFF1A1A1A)

// Speed display colors for live screen
val SpeedGreen = Color(0xFF4CAF50)         // Normal speed
val SpeedYellow = Color(0xFFFFC107)        // Approaching limit
val SpeedRed = Color(0xFFFF5252)           // Over limit
val GpsWaiting = Color(0xFFFF9800)         // GPS acquiring
val GpsLost = Color(0xFFE53935)            // GPS signal lost
val AutoPaused = Color(0xFF2196F3)         // Auto-paused state

// ============================================================
// Retro / Vintage Biker Cockpit Palette
// Inspired by vintage British & American motorcycle gauge clusters
// ============================================================
val RetroPrimary = Color(0xFFFFA000)          // Vintage tachometer amber-gold
val RetroAmber = RetroPrimary
val RetroOnPrimary = Color(0xFF14110F)
val RetroPrimaryContainer = Color(0xFF4E3100)
val RetroOnPrimaryContainer = Color(0xFFFFDDB8)

val RetroSecondary = Color(0xFFC5A059)        // Brushed vintage brass
val RetroBrass = RetroSecondary
val RetroOnSecondary = Color(0xFF1E1605)
val RetroSecondaryContainer = Color(0xFF3B2E15)
val RetroOnSecondaryContainer = Color(0xFFE6D2A8)

val RetroBackground = Color(0xFF12100E)       // Deep cast-iron / black leather
val RetroOnBackground = Color(0xFFEDE0D4)
val RetroSurface = Color(0xFF1B1714)          // Matte instrument gauge housing
val RetroOnSurface = Color(0xFFEDE0D4)
val RetroSurfaceVariant = Color(0xFF28221D)   // Recessed instrument cluster panel
val RetroOnSurfaceVariant = Color(0xFFD6C5B8)
val RetroOutline = Color(0xFF8D7A6E)
val RetroOutlineVariant = Color(0xFF4D4037)

val RetroChrome = Color(0xFFDDE3E6)           // Bezel chrome highlight
val RetroChromeDark = Color(0xFF78909C)       // Bezel chrome shadow
val RetroDialFace = Color(0xFF161311)         // Instrument dial matte black
val RetroIvory = Color(0xFFF7F2EB)            // Vintage dial numerals & markings
val RetroNeedle = Color(0xFFFF3D00)           // Mechanical needle orange-red

val JewelGreen = Color(0xFF00E676)            // REC / Neutral jewel lamp
val JewelAmber = Color(0xFFFFAB00)            // Auto-pause jewel lamp
val JewelRed = Color(0xFFFF1744)              // Alert / Warning jewel lamp
val JewelBlue = Color(0xFF2979FF)             // GPS Lock / High-beam jewel lamp

// ============================================================
// Cafe Racer Palette (British Racing Green & Polished Aluminium)
// ============================================================
val CafeRacerPrimary = Color(0xFF2E7D5B)          // Racing Green Emerald
val CafeRacerOnPrimary = Color(0xFFFFFFFF)
val CafeRacerPrimaryContainer = Color(0xFF0F3827)
val CafeRacerOnPrimaryContainer = Color(0xFFA5D6A7)
val CafeRacerSecondary = Color(0xFFB0BEC5)        // Polished Aluminium
val CafeRacerOnSecondary = Color(0xFF102027)
val CafeRacerSecondaryContainer = Color(0xFF263238)
val CafeRacerOnSecondaryContainer = Color(0xFFECEFF1)
val CafeRacerBackground = Color(0xFF081711)       // Deep British Racing Green / Dark Spruce
val CafeRacerOnBackground = Color(0xFFE8F5E9)
val CafeRacerSurface = Color(0xFF0F231B)          // Forest Cast Iron
val CafeRacerOnSurface = Color(0xFFE8F5E9)
val CafeRacerSurfaceVariant = Color(0xFF163327)
val CafeRacerOnSurfaceVariant = Color(0xFFC8E6C9)
val CafeRacerOutline = Color(0xFF4E7D63)
val CafeRacerIvory = Color(0xFFFAF6EE)            // Antique Ivory Numerals
val CafeRacerNeedle = Color(0xFFFF9100)           // Amber Racing Needle

// ============================================================
// Track Day Palette (Corse Racing Scarlet & Carbon Fiber)
// ============================================================
val TrackDayPrimary = Color(0xFFFF2A37)           // Corse Racing Scarlet
val TrackDayOnPrimary = Color(0xFFFFFFFF)
val TrackDayPrimaryContainer = Color(0xFF5A0007)
val TrackDayOnPrimaryContainer = Color(0xFFFFCDD2)
val TrackDaySecondary = Color(0xFFFFEA00)         // Speed Racing Yellow
val TrackDayOnSecondary = Color(0xFF212121)
val TrackDaySecondaryContainer = Color(0xFF4A4000)
val TrackDayOnSecondaryContainer = Color(0xFFFFF9C4)
val TrackDayBackground = Color(0xFF0D0D0D)        // Carbon Weave Black
val TrackDayOnBackground = Color(0xFFF5F5F5)
val TrackDaySurface = Color(0xFF1A1A1A)           // Matte Titanium Chassis
val TrackDayOnSurface = Color(0xFFF5F5F5)
val TrackDaySurfaceVariant = Color(0xFF262626)
val TrackDayOnSurfaceVariant = Color(0xFFE0E0E0)
val TrackDayOutline = Color(0xFFFF5252)
val TrackDayIvory = Color(0xFFFFFFFF)             // High-contrast Pure White
val TrackDayNeedle = Color(0xFFFF1744)            // Tachometer Red Needle

// ============================================================
// Neon Cyber Palette (Tokyo Night, Electric Cyan & Hot Pink)
// ============================================================
val NeonCyberPrimary = Color(0xFF00F0FF)          // Electric Cyan
val NeonCyberOnPrimary = Color(0xFF002229)
val NeonCyberPrimaryContainer = Color(0xFF004954)
val NeonCyberOnPrimaryContainer = Color(0xFF80F8FF)
val NeonCyberSecondary = Color(0xFFFF007F)        // Hot Neon Pink
val NeonCyberOnSecondary = Color(0xFF330018)
val NeonCyberSecondaryContainer = Color(0xFF5E002F)
val NeonCyberOnSecondaryContainer = Color(0xFFFF80BF)
val NeonCyberBackground = Color(0xFF090612)       // Midnight Tokyo Violet
val NeonCyberOnBackground = Color(0xFFEDE7F6)
val NeonCyberSurface = Color(0xFF130E24)          // Synthwave Dark Purple
val NeonCyberOnSurface = Color(0xFFEDE7F6)
val NeonCyberSurfaceVariant = Color(0xFF1F1738)
val NeonCyberOnSurfaceVariant = Color(0xFFD1C4E9)
val NeonCyberOutline = Color(0xFF00F0FF)
val NeonCyberIvory = Color(0xFFE0F7FA)            // Ice Cyan Numerals
val NeonCyberNeedle = Color(0xFFFF007F)           // Laser Pink Needle

// ============================================================
// Desert Rally Palette (Dakar Sand & Tactical Khaki)
// ============================================================
val DesertRallyPrimary = Color(0xFFE5A642)        // Sahara Sand Gold
val DesertRallyOnPrimary = Color(0xFF2E1C00)
val DesertRallyPrimaryContainer = Color(0xFF4F3507)
val DesertRallyOnPrimaryContainer = Color(0xFFFFE0B2)
val DesertRallySecondary = Color(0xFF8C9A79)      // Tactical Trail Khaki
val DesertRallyOnSecondary = Color(0xFF1A2114)
val DesertRallySecondaryContainer = Color(0xFF2F3B25)
val DesertRallyOnSecondaryContainer = Color(0xFFDCEDC8)
val DesertRallyBackground = Color(0xFF14110E)     // Dark Desert Basalt
val DesertRallyOnBackground = Color(0xFFF0EAE1)
val DesertRallySurface = Color(0xFF1E1A15)        // Weathered Sandstone
val DesertRallyOnSurface = Color(0xFFF0EAE1)
val DesertRallySurfaceVariant = Color(0xFF2C261F)
val DesertRallyOnSurfaceVariant = Color(0xFFD7CCC8)
val DesertRallyOutline = Color(0xFF8D7A65)
val DesertRallyIvory = Color(0xFFF7F1E5)          // Parchment Numerals
val DesertRallyNeedle = Color(0xFFFF6D00)         // Hazard Dune Orange


