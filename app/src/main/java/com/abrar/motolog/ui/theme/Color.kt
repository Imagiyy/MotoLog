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
