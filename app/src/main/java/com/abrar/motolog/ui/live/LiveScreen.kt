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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    // Keep screen on during active tracking if setting is enabled
    val isTrackingActive = uiState is LiveUiState.WaitingForGps || uiState is LiveUiState.Tracking
    val view = LocalView.current
    DisposableEffect(keepScreenOn, isTrackingActive) {
        view.keepScreenOn = keepScreenOn && isTrackingActive
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
                viewModel.startTracking()
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
                viewModel.startTracking()
            }
        } else {
            showPermissionRationaleDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar: Status and Keep Screen On Toggle & Settings
        LiveTopBar(
            uiState = uiState,
            keepScreenOn = keepScreenOn,
            onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!keepScreenOn) },
            onNavigateToSettings = onNavigateToSettings
        )

        // Middle Section: Live Speedometer & Metrics
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            LiveSpeedometer(uiState = uiState)

            Spacer(modifier = Modifier.height(24.dp))

            // 3 Glanceable Metric Displays for Distance, Moving Time, and Avg Speed
            MetricsRow(uiState = uiState)
        }

        // Bottom Section: Glove-friendly action controls
        LiveBottomControls(
            uiState = uiState,
            onStartClick = { initiateStart() },
            onPauseClick = { viewModel.pauseTracking() },
            onResumeClick = { viewModel.resumeTracking() },
            onStopConfirmed = { viewModel.stopTracking() },
            onResetClick = { viewModel.resetToIdle() }
        )
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
                    "Tracking only runs while you ride and stops immediately when you tap Stop."
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

@Composable
private fun LiveTopBar(
    uiState: LiveUiState,
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Badge
        val (statusText, statusColor) = when (uiState) {
            is LiveUiState.Idle, is LiveUiState.RecoveryPrompt -> "READY" to MaterialTheme.colorScheme.onSurfaceVariant
            is LiveUiState.WaitingForGps -> "WAITING FOR GPS" to GpsWaiting
            is LiveUiState.Tracking -> {
                when {
                    uiState.isGpsLost -> "GPS SIGNAL LOST" to MaterialTheme.colorScheme.error
                    uiState.pauseState == PauseState.AUTO_PAUSED -> "AUTO-PAUSED" to Color(0xFFFFB300)
                    uiState.isPaused -> "PAUSED" to Color(0xFFFF9800)
                    else -> "RECORDING" to SpeedGreen
                }
            }
            is LiveUiState.Stopped -> "STOPPED" to MaterialTheme.colorScheme.error
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = statusColor.copy(alpha = 0.15f),
            modifier = Modifier.padding(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Screen Keep-On Indicator / Toggle
            IconButton(
                onClick = onToggleKeepScreenOn,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (keepScreenOn) Icons.Default.ScreenLockPortrait else Icons.Default.ScreenRotation,
                    contentDescription = if (keepScreenOn) "Screen stay awake enabled" else "Screen stay awake disabled",
                    tint = if (keepScreenOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Settings Navigation Button
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsIcon,
                    contentDescription = "Open Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LiveSpeedometer(
    uiState: LiveUiState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (uiState) {
            is LiveUiState.WaitingForGps -> {
                Icon(
                    imageVector = Icons.Default.GpsNotFixed,
                    contentDescription = "Waiting for GPS",
                    tint = GpsWaiting,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ACQUIRING GPS FIX",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GpsWaiting
                )
                Spacer(modifier = Modifier.height(6.dp))
                val accuracyText = if (uiState.currentAccuracyMeters != null) {
                    "Current accuracy: ±${uiState.currentAccuracyMeters.roundToInt()} m (Target: ≤ 25 m)"
                } else {
                    "Searching for satellites..."
                }
                Text(
                    text = accuracyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is LiveUiState.Tracking -> {
                val speedColor = when {
                    uiState.isGpsLost -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                    uiState.pauseState == PauseState.AUTO_PAUSED -> Color(0xFFFFB300)
                    uiState.isPaused -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }
                val speedSubtitle = when {
                    uiState.pauseState == PauseState.AUTO_PAUSED -> "KM/H (AUTO-PAUSED)"
                    uiState.isPaused -> "KM/H (PAUSED)"
                    else -> "KM/H"
                }

                Text(
                    text = "${uiState.speedKmh.roundToInt()}",
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 110.sp,
                    color = speedColor,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = speedSubtitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isGpsLost) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsNotFixed,
                                contentDescription = "GPS Lost",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GPS signal lost • Searching for satellites...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "GPS locked",
                            tint = SpeedGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "±${uiState.accuracyMeters.roundToInt()}m accuracy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is LiveUiState.Stopped -> {
                Text(
                    text = "0",
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 110.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "KM/H",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            is LiveUiState.Idle, is LiveUiState.RecoveryPrompt -> {
                Text(
                    text = "0",
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 110.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "KM/H",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricsRow(uiState: LiveUiState) {
    val stats = when (uiState) {
        is LiveUiState.Tracking -> uiState.stats
        is LiveUiState.Stopped -> uiState.stats
        else -> null
    }

    var showOverallMetrics by remember { mutableStateOf(false) }

    val distanceText = if (stats != null) {
        String.format(java.util.Locale.US, "%.1f", stats.totalDistanceMeters / 1000.0)
    } else {
        "0.0"
    }

    val displayTimeMs = if (showOverallMetrics) {
        stats?.elapsedTimeMs ?: 0L
    } else {
        stats?.movingTimeMs ?: 0L
    }

    val timeText = run {
        val totalSec = displayTimeMs / 1000
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        if (hrs > 0) {
            String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
        }
    }

    val avgSpeedValue = if (showOverallMetrics) {
        stats?.avgOverallSpeedKmh ?: 0.0
    } else {
        stats?.avgMovingSpeedKmh ?: 0.0
    }

    val avgSpeedText = String.format(java.util.Locale.US, "%.1f", avgSpeedValue)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            label = "DISTANCE",
            value = distanceText,
            unit = "km",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = if (showOverallMetrics) "ELAPSED TIME" else "MOVING TIME",
            badge = if (showOverallMetrics) "OVERALL" else "MOVING",
            value = timeText,
            unit = if (displayTimeMs >= 3_600_000L) "hrs" else "min",
            onClick = { showOverallMetrics = !showOverallMetrics },
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "AVG SPEED",
            badge = if (showOverallMetrics) "OVERALL" else "MOVING",
            value = avgSpeedText,
            unit = "km/h",
            onClick = { showOverallMetrics = !showOverallMetrics },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    )
    val cardShape = RoundedCornerShape(12.dp)

    val content = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = cardColors,
            shape = cardShape
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            colors = cardColors,
            shape = cardShape
        ) {
            content()
        }
    }
}

@Composable
private fun LiveBottomControls(
    uiState: LiveUiState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopConfirmed: () -> Unit,
    onResetClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is LiveUiState.Idle, is LiveUiState.RecoveryPrompt -> {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Ride",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START RIDE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            is LiveUiState.WaitingForGps -> {
                HoldToStopButton(
                    onStopConfirmed = onStopConfirmed
                )
            }

            is LiveUiState.Tracking -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause/Resume Glove-friendly button (at least 56dp height)
                    if (uiState.isPaused) {
                        Button(
                            onClick = onResumeClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpeedGreen
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume Ride",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RESUME",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPauseClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Ride"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PAUSE",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2-Second Hold-To-Stop Button
                    HoldToStopButton(
                        onStopConfirmed = onStopConfirmed,
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }

            is LiveUiState.Stopped -> {
                Button(
                    onClick = onResetClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New Ride",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESET / NEW RIDE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 2-Second Hold-to-Stop button with animated circular progress indicator.
 * Protects riders from accidental touches caused by glove bumps or road vibrations.
 */
@Composable
private fun HoldToStopButton(
    onStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isHolding) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val animJob = coroutineScope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 2000,
                                    easing = LinearEasing
                                )
                            )
                        }

                        val released = try {
                            tryAwaitRelease()
                            true
                        } catch (e: Exception) {
                            false
                        }

                        if (released) {
                            if (progress.value >= 0.99f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStopConfirmed()
                            }
                            animJob.cancel()
                            progress.snapTo(0f)
                            isHolding = false
                        } else {
                            animJob.cancel()
                            progress.snapTo(0f)
                            isHolding = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Progress Fill Background
        val progressFill = progress.value
        val errorColor = MaterialTheme.colorScheme.error
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (progressFill > 0f) {
                drawRect(
                    color = errorColor.copy(alpha = 0.35f),
                    size = size.copy(width = size.width * progressFill)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp)
            ) {
                if (isHolding) {
                    CircularProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 3.dp,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Ride",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isHolding) "HOLD TO STOP (2s)..." else "HOLD TO STOP",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
