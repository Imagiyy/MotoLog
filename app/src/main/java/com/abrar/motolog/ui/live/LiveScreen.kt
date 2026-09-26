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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.Stop
import com.abrar.motolog.domain.model.PauseState
import com.abrar.motolog.domain.model.RideStats
import com.abrar.motolog.ui.live.retro.JewelColor
import com.abrar.motolog.ui.live.retro.RetroCockpitDashboard
import com.abrar.motolog.ui.live.retro.RetroHoldToStopButton
import com.abrar.motolog.ui.live.retro.RetroInstrumentCard
import com.abrar.motolog.ui.live.retro.RetroJewelLamp
import com.abrar.motolog.ui.live.retro.RetroLiveMap
import com.abrar.motolog.ui.live.retro.RetroOdometerDrum
import com.abrar.motolog.ui.live.retro.RetroSpeedometerDial
import com.abrar.motolog.ui.live.retro.TrackDayCockpitDashboard
import com.abrar.motolog.ui.live.retro.NeonCyberCockpitDashboard
import com.abrar.motolog.ui.live.retro.DesertRallyCockpitDashboard
import com.abrar.motolog.ui.live.retro.CafeRacerCockpitDashboard
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
    val palette = remember(themeMode, isSystemDark) { getCockpitThemePalette(themeMode, isSystemDark) }

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
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        when {
            fineGranted && notifGranted -> {
                handleStart()
            }
            coarseGranted && fineGranted.not() -> {
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
                when (themeMode) {
                    ThemeMode.TRACK_DAY -> {
                        TrackDayCockpitDashboard(
                            stats = state.stats,
                            speedKmh = state.speedKmh,
                            accuracyMeters = state.accuracyMeters,
                            isMetric = useMetricUnits,
                            pauseState = state.pauseState,
                            isGpsLost = state.isGpsLost,
                            isSpeedAlert = state.isSpeedAlert,
                            bikeName = state.bikeName,
                            onPauseClick = handlePause,
                            onResumeClick = handleResume,
                            onStopConfirmed = { viewModel.stopTracking() },
                            onSwitchToMap = { currentViewMode = LiveViewMode.MAP },
                            palette = palette,
                            modifier = modifier.fillMaxSize()
                        )
                    }
                    ThemeMode.NEON_CYBER -> {
                        NeonCyberCockpitDashboard(
                            stats = state.stats,
                            speedKmh = state.speedKmh,
                            accuracyMeters = state.accuracyMeters,
                            isMetric = useMetricUnits,
                            pauseState = state.pauseState,
                            isGpsLost = state.isGpsLost,
                            isSpeedAlert = state.isSpeedAlert,
                            bikeName = state.bikeName,
                            onPauseClick = handlePause,
                            onResumeClick = handleResume,
                            onStopConfirmed = { viewModel.stopTracking() },
                            onSwitchToMap = { currentViewMode = LiveViewMode.MAP },
                            palette = palette,
                            modifier = modifier.fillMaxSize()
                        )
                    }
                    ThemeMode.DESERT_RALLY -> {
                        DesertRallyCockpitDashboard(
                            stats = state.stats,
                            speedKmh = state.speedKmh,
                            accuracyMeters = state.accuracyMeters,
                            isMetric = useMetricUnits,
                            pauseState = state.pauseState,
                            isGpsLost = state.isGpsLost,
                            isSpeedAlert = state.isSpeedAlert,
                            bikeName = state.bikeName,
                            onPauseClick = handlePause,
                            onResumeClick = handleResume,
                            onStopConfirmed = { viewModel.stopTracking() },
                            onSwitchToMap = { currentViewMode = LiveViewMode.MAP },
                            palette = palette,
                            modifier = modifier.fillMaxSize()
                        )
                    }
                    ThemeMode.CAFE_RACER -> {
                        CafeRacerCockpitDashboard(
                            stats = state.stats,
                            speedKmh = state.speedKmh,
                            accuracyMeters = state.accuracyMeters,
                            isMetric = useMetricUnits,
                            pauseState = state.pauseState,
                            isGpsLost = state.isGpsLost,
                            isSpeedAlert = state.isSpeedAlert,
                            bikeName = state.bikeName,
                            onPauseClick = handlePause,
                            onResumeClick = handleResume,
                            onStopConfirmed = { viewModel.stopTracking() },
                            onSwitchToMap = { currentViewMode = LiveViewMode.MAP },
                            palette = palette,
                            modifier = modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        RetroCockpitDashboard(
                            stats = state.stats,
                            speedKmh = state.speedKmh,
                            accuracyMeters = state.accuracyMeters,
                            isMetric = useMetricUnits,
                            pauseState = state.pauseState,
                            isGpsLost = state.isGpsLost,
                            isSpeedAlert = state.isSpeedAlert,
                            bikeName = state.bikeName,
                            onPauseClick = handlePause,
                            onResumeClick = handleResume,
                            onStopConfirmed = { viewModel.stopTracking() },
                            onSwitchToMap = { currentViewMode = LiveViewMode.MAP },
                            palette = palette,
                            modifier = modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        is LiveUiState.WaitingForGps -> {
            RetroWaitingForGpsView(
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
            when (themeMode) {
                ThemeMode.TRACK_DAY -> {
                    TrackDayCockpitDashboard(
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
                        onSwitchToMap = {},
                        isIdle = true,
                        onStartClick = { initiateStart() },
                        keepScreenOn = keepScreenOn,
                        onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!keepScreenOn) },
                        onNavigateToSettings = onNavigateToSettings,
                        palette = palette,
                        modifier = modifier.fillMaxSize()
                    )
                }
                ThemeMode.NEON_CYBER -> {
                    NeonCyberCockpitDashboard(
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
                        onSwitchToMap = {},
                        isIdle = true,
                        onStartClick = { initiateStart() },
                        keepScreenOn = keepScreenOn,
                        onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!keepScreenOn) },
                        onNavigateToSettings = onNavigateToSettings,
                        palette = palette,
                        modifier = modifier.fillMaxSize()
                    )
                }
                ThemeMode.DESERT_RALLY -> {
                    DesertRallyCockpitDashboard(
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
                        onSwitchToMap = {},
                        isIdle = true,
                        onStartClick = { initiateStart() },
                        keepScreenOn = keepScreenOn,
                        onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!keepScreenOn) },
                        onNavigateToSettings = onNavigateToSettings,
                        palette = palette,
                        modifier = modifier.fillMaxSize()
                    )
                }
                ThemeMode.CAFE_RACER -> {
                    CafeRacerCockpitDashboard(
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
                        onSwitchToMap = {},
                        isIdle = true,
                        onStartClick = { initiateStart() },
                        keepScreenOn = keepScreenOn,
                        onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!keepScreenOn) },
                        onNavigateToSettings = onNavigateToSettings,
                        palette = palette,
                        modifier = modifier.fillMaxSize()
                    )
                }
                else -> {
                    RetroCockpitDashboard(
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
                        onSwitchToMap = {},
                        isIdle = true,
                        onStartClick = { initiateStart() },
                        keepScreenOn = keepScreenOn,
                        onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!keepScreenOn) },
                        onNavigateToSettings = onNavigateToSettings,
                        palette = palette,
                        modifier = modifier.fillMaxSize()
                    )
                }
            }
        }

        is LiveUiState.Stopped -> {
            RetroStoppedCockpitView(
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

@Composable
private fun RetroWaitingForGpsView(
    currentAccuracyMeters: Float?,
    useMetricUnits: Boolean,
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    BoxWithConstraints(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = if (palette.isLight) {
                        listOf(palette.background, Color(0xFFDDE2E5), palette.background)
                    } else {
                        listOf(palette.background, Color(0xFF0A0908), palette.background)
                    }
                )
            )
            .padding(14.dp)
    ) {
        val screenMaxWidth = maxWidth
        val screenMaxHeight = maxHeight
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            IdleTopBar(
                keepScreenOn = keepScreenOn,
                onToggleKeepScreenOn = onToggleKeepScreenOn,
                onNavigateToSettings = onNavigateToSettings,
                palette = palette
            )
            val dialSize = (screenMaxHeight * 0.40f).coerceAtMost(screenMaxWidth * 0.85f)
            RetroSpeedometerDial(
                currentSpeed = 0.0,
                isMetric = useMetricUnits,
                isSpeedAlert = false,
                palette = palette,
                modifier = Modifier.size(dialSize)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RetroJewelLamp(
                    label = "GPS FIX",
                    isActive = true,
                    color = JewelColor.AMBER,
                    size = 36.dp,
                    shouldBlink = true
                )
                Text(
                    text = "ACQUIRING SATELLITE FIX",
                    color = palette.primaryAccent,
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
                    color = palette.dialText.copy(alpha = 0.75f),
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

@Composable
private fun RetroStoppedCockpitView(
    stats: com.abrar.motolog.domain.model.RideStats,
    useMetricUnits: Boolean,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    val totalDistKm = stats.totalDistanceMeters / 1000.0
    val displayDistance = if (useMetricUnits) totalDistKm else totalDistKm * 0.621371
    val distUnit = if (useMetricUnits) "km" else "mi"
    val spdUnit = if (useMetricUnits) "km/h" else "mph"
    val avgSpeedDisplay = if (useMetricUnits) stats.avgMovingSpeedKmh else stats.avgMovingSpeedKmh * 0.621371
    val maxSpeedDisplay = if (useMetricUnits) stats.maxSpeedKmh else stats.maxSpeedKmh * 0.621371

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = if (palette.isLight) {
                        listOf(palette.background, Color(0xFFDDE2E5), palette.background)
                    } else {
                        listOf(palette.background, Color(0xFF0A0908), palette.background)
                    }
                )
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.surface)
                    .border(1.5.dp, palette.surfaceBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "RIDE COMPLETED",
                    color = palette.primaryAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            }
            RetroOdometerDrum(
                distanceValue = displayDistance,
                unitLabel = "TRIP $distUnit",
                palette = palette
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalSec = stats.movingTimeMs / 1000
                val hrs = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                val timeStr = if (hrs > 0) {
                    String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
                } else {
                    String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
                }

                RetroInstrumentCard(
                    label = "Moving Time",
                    value = timeStr,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                RetroInstrumentCard(
                    label = "Moving Avg",
                    value = String.format(java.util.Locale.US, "%.1f", avgSpeedDisplay),
                    unit = spdUnit,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RetroInstrumentCard(
                    label = "Max Speed",
                    value = String.format(java.util.Locale.US, "%.1f", maxSpeedDisplay),
                    unit = spdUnit,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
                RetroInstrumentCard(
                    label = "Fix Points",
                    value = "${stats.acceptedPointCount}",
                    subtitle = "Recorded GPS Hits",
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = onResetClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent
                )
            ) {
                Text(
                    text = "START NEW RIDE",
                    color = if (palette.isLight) Color.White else Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun IdleTopBar(
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onNavigateToSettings: () -> Unit,
    palette: CockpitThemePalette = getCockpitThemePalette(ThemeMode.RETRO)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(palette.surface)
                .border(1.2.dp, palette.surfaceBorder.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "MOTO LOG",
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
                    imageVector = Icons.Default.SettingsIcon,
                    contentDescription = "Open Settings",
                    tint = palette.secondaryAccent
                )
            }
        }
    }
}
