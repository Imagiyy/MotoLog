package com.abrar.motolog.ui.live

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Pause
import com.abrar.motolog.ui.live.retro.CockpitUtils
import java.util.Locale
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.Stop
import com.abrar.motolog.shared.domain.model.PauseState
import com.abrar.motolog.shared.domain.model.RideStats
import com.abrar.motolog.ui.live.retro.RacingTftDashboard
import com.abrar.motolog.ui.live.retro.DashboardCustomColors
import com.abrar.motolog.ui.live.retro.RetroHoldToStopButton
import com.abrar.motolog.ui.live.retro.RetroLiveMap
import com.abrar.motolog.ui.theme.JewelAmber
import com.abrar.motolog.ui.theme.JewelGreen
import com.abrar.motolog.ui.theme.JewelRed
import com.abrar.motolog.ui.theme.RetroAmber
import com.abrar.motolog.ui.theme.RetroBackground
import com.abrar.motolog.ui.theme.RetroBrass
import com.abrar.motolog.ui.theme.RetroIvory
import com.abrar.motolog.ui.theme.RetroSurface
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrar.motolog.ui.theme.ThemeMode
import com.abrar.motolog.ui.theme.CockpitThemePalette
import com.abrar.motolog.ui.theme.getCockpitThemePalette
import com.abrar.motolog.ui.theme.GpsWaiting
import com.abrar.motolog.ui.theme.SpeedGreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LiveScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    viewModel: LiveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDisclaimerAccepted by viewModel.isDisclaimerAccepted.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val batteryGuidanceSeen by viewModel.batteryGuidanceSeen.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val dashboardTextColor by viewModel.dashboardTextColor.collectAsStateWithLifecycle()
    val dashboardBgColor by viewModel.dashboardBgColor.collectAsStateWithLifecycle()
    val dashboardAccentColor by viewModel.dashboardAccentColor.collectAsStateWithLifecycle()
    val dashboardComponentColor by viewModel.dashboardComponentColor.collectAsStateWithLifecycle()

    val customColors = remember(dashboardTextColor, dashboardBgColor, dashboardAccentColor, dashboardComponentColor) {
        DashboardCustomColors(
            textColor = Color(dashboardTextColor),
            backgroundColor = Color(dashboardBgColor),
            accentColor = Color(dashboardAccentColor),
            componentColor = Color(dashboardComponentColor)
        )
    }
    val palette = remember(themeMode, isSystemDark, customColors) {
        getCockpitThemePalette(
            themeMode = themeMode,
            isSystemDark = isSystemDark,
            textColor = customColors.textColor,
            backgroundColor = customColors.backgroundColor,
            accentColor = customColors.accentColor,
            componentColor = customColors.componentColor
        )
    }

    val haptic = LocalHapticFeedback.current
    val handleStart: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.startTracking()
    }
    val handlePause: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.pauseTracking()
    }
    val handleResume: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.resumeTracking()
    }

    // Keep screen on while viewing cockpit or tracking if setting is enabled
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Permission and Disclaimer Dialog States
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showBatteryGuidanceDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showNotificationRationaleDialog by remember { mutableStateOf(false) }
    var showApproximatePermissionDialog by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Fix #34: Check actual permission state with ContextCompat so notification-only
        // requests don't misinterpret omitted location keys as location denial.
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)

        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION]
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS]
                ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        } else true

        when {
            fineGranted && notifGranted -> {
                handleStart()
            }
            coarseGranted && !fineGranted -> {
                showApproximatePermissionDialog = true
            }
            fineGranted && !notifGranted -> {
                showNotificationRationaleDialog = true
            }
            else -> {
                val shouldShowRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
                } ?: false

                if (!shouldShowRationale) {
                    showPermanentlyDeniedDialog = true
                }
            }
        }
    }

    fun initiateStart() {
        if (!isDisclaimerAccepted) {
            showDisclaimerDialog = true
            return
        }

        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (fineGranted && notifGranted) {
            if (!batteryGuidanceSeen) {
                showBatteryGuidanceDialog = true
            } else {
                handleStart()
            }
        } else {
            showPermissionRationaleDialog = true
        }
    }

    val useMetricUnits by viewModel.useMetricUnits.collectAsStateWithLifecycle()
    val defaultMapTheme by viewModel.defaultMapTheme.collectAsStateWithLifecycle()
    var currentViewMode by remember { mutableStateOf(LiveViewMode.COCKPIT) }

    when (val state = uiState) {
        is LiveUiState.Tracking -> {
            if (currentViewMode == LiveViewMode.MAP && viewModel.mapStyleProvider != null) {
                RetroLiveMap(
                    latitude = state.latitude,
                    longitude = state.longitude,
                    routeCoordinates = state.routeCoordinates,
                    stats = state.stats,
                    speedKmh = state.speedKmh,
                    isMetric = useMetricUnits,
                    pauseState = state.pauseState,
                    isGpsLost = state.isGpsLost,
                    isSpeedAlert = state.isSpeedAlert,
                    mapStyleProvider = viewModel.mapStyleProvider,
                    onPauseClick = handlePause,
                    onResumeClick = handleResume,
                    onStopProgressChange = { viewModel.stopTracking() },
                    onSwitchToCockpit = { currentViewMode = LiveViewMode.COCKPIT },
                    defaultMapTheme = defaultMapTheme,
                    palette = palette,
                    modifier = modifier.fillMaxSize()
                )
            } else {
                RacingTftDashboard(
                    stats = state.stats,
                    speedKmh = state.speedKmh,
                    accuracyMeters = state.accuracyMeters,
                    isMetric = useMetricUnits,
                    pauseState = state.pauseState,
                    isGpsLost = state.isGpsLost,
                    isSpeedAlert = state.isSpeedAlert,
                    bikeName = state.bikeName,
                    altitudeMeters = state.altitudeMeters,
                    bearingDegrees = state.bearingDegrees,
                    onPauseClick = handlePause,
                    onResumeClick = handleResume,
                    onStopConfirmed = { viewModel.stopTracking() },
                    onSwitchToMap = { currentViewMode = LiveViewMode.MAP },
                    palette = palette,
                    customColors = customColors,
                    modifier = modifier.fillMaxSize()
                )
            }
        }

        is LiveUiState.WaitingForGps -> {
            TftWaitingForGpsView(
                currentAccuracyMeters = state.currentAccuracyMeters,
                useMetricUnits = useMetricUnits,
                keepScreenOn = keepScreenOn,
                onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!keepScreenOn) },
                onNavigateToSettings = onNavigateToSettings,
                onStopConfirmed = { viewModel.stopTracking() },
                palette = palette,
                modifier = modifier.fillMaxSize()
            )
        }

        is LiveUiState.Idle, is LiveUiState.RecoveryPrompt -> {
            val emptyStats = RideStats()
            if (currentViewMode == LiveViewMode.MAP && viewModel.mapStyleProvider != null) {
                RetroLiveMap(
                    latitude = null,
                    longitude = null,
                    routeCoordinates = emptyList(),
                    stats = emptyStats,
                    speedKmh = 0.0,
                    isMetric = useMetricUnits,
                    pauseState = PauseState.RECORDING,
                    isGpsLost = false,
                    isSpeedAlert = false,
                    mapStyleProvider = viewModel.mapStyleProvider,
                    onPauseClick = {},
                    onResumeClick = {},
                    onStopProgressChange = {},
                    onSwitchToCockpit = { currentViewMode = LiveViewMode.COCKPIT },
                    defaultMapTheme = defaultMapTheme,
                    palette = palette,
                    isIdle = true,
                    onStartClick = { initiateStart() },
                    modifier = modifier.fillMaxSize()
                )
            } else {
                RacingTftDashboard(
                    stats = emptyStats,
                    speedKmh = 0.0,
                    accuracyMeters = 0f,
                    isMetric = useMetricUnits,
                    pauseState = PauseState.RECORDING,
                    isGpsLost = false,
                    isSpeedAlert = false,
                    bikeName = null,
                    onPauseClick = {},
                    onResumeClick = {},
                    onStopConfirmed = {},
                    onSwitchToMap = { currentViewMode = LiveViewMode.MAP },
                    isIdle = true,
                    onStartClick = { initiateStart() },
                    keepScreenOn = keepScreenOn,
                    onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!keepScreenOn) },
                    onNavigateToSettings = onNavigateToSettings,
                    palette = palette,
                    customColors = customColors,
                    modifier = modifier.fillMaxSize()
                )
            }
        }

        is LiveUiState.Stopped -> {
            TftStoppedView(
                stats = state.stats,
                useMetricUnits = useMetricUnits,
                onResetClick = { viewModel.resetToIdle() },
                palette = palette,
                modifier = modifier.fillMaxSize()
            )
        }
    }

    // Safety Disclaimer Dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = {
                Text(
                    text = "Rider Safety Disclaimer",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Speed is derived solely from GPS and is NOT a legal speedometer.\n\n" +
                    "• Always observe posted speed limits and road conditions.\n" +
                    "• Operate this app only when safely stopped.\n" +
                    "• Never look at or interact with the screen while riding."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisclaimerDialog = false
                        viewModel.acceptDisclaimer()
                        initiateStart()
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("I Understand & Accept")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Battery Optimization Guidance Dialog (shown once before first ride)
    if (showBatteryGuidanceDialog) {
        AlertDialog(
            onDismissRequest = {
                showBatteryGuidanceDialog = false
                viewModel.setBatteryGuidanceSeen(true)
                initiateStart()
            },
            title = {
                Text(
                    text = "Battery Optimization Guidance",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Many phone manufacturers (Samsung, Xiaomi, OnePlus, Oppo, Vivo) aggressively kill background tracking when the screen turns off.\n\n" +
                    "To guarantee your ride is recorded uninterrupted in your pocket or handlebar mount, we recommend setting MotoLog to 'Unrestricted' battery usage in system settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryGuidanceDialog = false
                        viewModel.setBatteryGuidanceSeen(true)
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Open Battery Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBatteryGuidanceDialog = false
                        viewModel.setBatteryGuidanceSeen(true)
                        initiateStart()
                    }
                ) {
                    Text("Continue")
                }
            }
        )
    }

    // Crash / Force-Kill Recovery Prompt Dialog
    if (uiState is LiveUiState.RecoveryPrompt) {
        val activeRide = (uiState as LiveUiState.RecoveryPrompt).activeRide
        AlertDialog(
            onDismissRequest = { /* Rider must choose recover or discard */ },
            title = {
                Text(
                    text = "Unfinished Ride Detected",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "An unfinished ride was detected from a previous session (interrupted by an app force-close or device crash).\n\n" +
                    "Would you like to reconstruct and save your ride stats up to the last recorded point, or discard it?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.recoverRide(activeRide) },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Recover Ride")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.discardRide(activeRide) },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Discard")
                }
            }
        )
    }

    // Location Permission Rationale Dialog
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = { Text("Location Permission Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "MotoLog needs Precise (Fine) Location access to track your motorcycle speed, " +
                    "distance, and route accurately.\n\n" +
                    "Tracking only runs between Start and Stop. Your location data is stored strictly " +
                    "on your device and is never uploaded to any server or shared."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        }
                        permissionLauncher.launch(permissions)
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    // Notification Permission Rationale Dialog (Android 13+)
    if (showNotificationRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationRationaleDialog = false },
            title = { Text("Notification Permission Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "MotoLog needs notification permission to show live speed, distance, and lock-screen controls " +
                    "while your screen is off.\n\n" +
                    "This guarantees lock-screen controls so you can pause or stop without navigating menus while wearing gloves."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationRationaleDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationRationaleDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    // Approximate Only Warning Dialog
    if (showApproximatePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showApproximatePermissionDialog = false },
            title = { Text("Precise Location Needed", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "MotoLog was granted 'Approximate' location. Approximate location is not accurate " +
                    "enough to derive a motorcycle speedometer or calculate travel distance.\n\n" +
                    "Please choose 'Precise' location in system settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApproximatePermissionDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Open App Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproximatePermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permanently Denied Dialog
    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentlyDeniedDialog = false },
            title = { Text("Location Access Disabled", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Location permission is permanently denied. MotoLog cannot track motorcycle rides without " +
                    "precise location access.\n\n" +
                    "Please tap below to enable location permission in Android settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermanentlyDeniedDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermanentlyDeniedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

enum class LiveViewMode {
    COCKPIT,
    MAP
}

// ============================================================
// TFT-style Waiting for GPS View
// ============================================================
@Composable
private fun TftWaitingForGpsView(
    currentAccuracyMeters: Float?,
    useMetricUnits: Boolean,
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RACING_TFT)
) {
    val tftCyan = Color(0xFF00D4E5)
    val tftBg = Color(0xFF080A10)
    val tftSurface = Color(0xFF0E1118)
    val tftYellow = Color(0xFFFFEA00)
    val tftGray = Color(0xFF6B7280)
    val tftWhite = Color(0xFFF5F5F7)

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(tftBg, Color(0xFF06080E), tftBg)
                )
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tftSurface)
                        .border(1.dp, tftCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "MOTO LOG",
                        color = tftCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
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
                            tint = if (keepScreenOn) tftCyan else tftGray
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsIcon,
                            contentDescription = "Open Settings",
                            tint = tftGray
                        )
                    }
                }
            }

            // Large GPS animation area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pulsing GPS icon
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "gpsPulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "gpsPulseAlpha"
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(tftYellow.copy(alpha = pulseAlpha * 0.1f))
                        .border(2.dp, tftYellow.copy(alpha = pulseAlpha * 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsNotFixed,
                        contentDescription = "Acquiring GPS",
                        tint = tftYellow.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "ACQUIRING SATELLITE FIX",
                    color = tftYellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                val accuracyText = if (currentAccuracyMeters != null) {
                    val acc = if (useMetricUnits) "±${currentAccuracyMeters.toInt()}m" else "±${(currentAccuracyMeters * 3.28084).toInt()}ft"
                    val target = if (useMetricUnits) "≤25m" else "≤82ft"
                    "Current: $acc (Target: $target)"
                } else {
                    "Searching satellites..."
                }
                Text(
                    text = accuracyText,
                    color = tftGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            RetroHoldToStopButton(
                onStopConfirmed = onStopConfirmed,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================
// TFT-style Stopped / Ride Summary View
// ============================================================
@Composable
private fun TftStoppedView(
    stats: RideStats,
    useMetricUnits: Boolean,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RACING_TFT)
) {
    val tftCyan = Color(0xFF00D4E5)
    val tftBg = Color(0xFF080A10)
    val tftSurface = Color(0xFF0E1118)
    val tftGreen = Color(0xFF00E676)
    val tftYellow = Color(0xFFFFEA00)
    val tftRed = Color(0xFFFF1744)
    val tftGray = Color(0xFF6B7280)
    val tftWhite = Color(0xFFF5F5F7)

    val totalDistKm = stats.totalDistanceMeters / 1000.0
    val displayDistance = if (useMetricUnits) totalDistKm else totalDistKm * 0.621371
    val distUnit = if (useMetricUnits) "km" else "mi"
    val spdUnit = if (useMetricUnits) "km/h" else "mph"
    val avgSpeedDisplay = if (useMetricUnits) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
    val maxSpeedDisplay = if (useMetricUnits) stats.maxSpeedKmh else stats.maxSpeedKmh * 0.621371
    val overallAvgSpeed = if (useMetricUnits) stats.avgOverallSpeedKmh else stats.avgOverallSpeedKmh * 0.621371

    val movingTimeStr = CockpitUtils.formatDurationMs(stats.movingTimeMs)
    val totalTimeStr = CockpitUtils.formatDurationMs(stats.elapsedTimeMs)

    BoxWithConstraints(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(tftBg, Color(0xFF06080E), tftBg)
                )
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val isLandscape = maxWidth > maxHeight
        val cardHeight = if (isLandscape) 64.dp else 76.dp
        val spacing = if (isLandscape) 8.dp else 12.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Header Status Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(tftSurface)
                    .border(1.5.dp, tftCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "SESSION COMPLETE",
                    color = tftCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            }

            // Trip Distance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(tftSurface)
                    .border(1.dp, tftCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL DISTANCE",
                        color = tftCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", displayDistance),
                            color = tftWhite,
                            fontSize = 34.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = distUnit.uppercase(),
                            color = tftGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }

            // Telemetry Cards Row 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TftSummaryCard(
                    label = "Moving Time",
                    value = movingTimeStr,
                    accentColor = tftGreen,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                TftSummaryCard(
                    label = "Moving Avg",
                    value = String.format(Locale.US, "%.1f", avgSpeedDisplay),
                    unit = spdUnit,
                    accentColor = tftYellow,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            // Telemetry Cards Row 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TftSummaryCard(
                    label = "Max Speed",
                    value = String.format(Locale.US, "%.1f", maxSpeedDisplay),
                    unit = spdUnit,
                    accentColor = tftRed,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                TftSummaryCard(
                    label = "Total Time",
                    value = totalTimeStr,
                    accentColor = tftCyan,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            // Telemetry Cards Row 3
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TftSummaryCard(
                    label = "Overall Avg",
                    value = String.format(Locale.US, "%.1f", overallAvgSpeed),
                    unit = spdUnit,
                    accentColor = Color(0xFFFF9100),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                TftSummaryCard(
                    label = "GPS Hits",
                    value = "${stats.acceptedPointCount}",
                    subtitle = "Recorded Fixes",
                    accentColor = tftGray,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Start New Ride Button
            Button(
                onClick = onResetClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tftCyan
                )
            ) {
                Text(
                    text = "START NEW RIDE",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Summary card for the stopped/completed ride screen, matching TFT aesthetic.
 */
@Composable
private fun TftSummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    subtitle: String? = null,
    accentColor: Color = Color(0xFF00D4E5)
) {
    val tftSurface = Color(0xFF0E1118)
    val tftWhite = Color(0xFFF5F5F7)
    val tftGray = Color(0xFF6B7280)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tftSurface)
            .border(0.8.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(),
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    color = tftWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        color = tftGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = tftGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
