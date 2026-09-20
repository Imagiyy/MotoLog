package com.abrar.motolog.ui.live

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
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
    viewModel: LiveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDisclaimerAccepted by viewModel.isDisclaimerAccepted.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

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
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showApproximatePermissionDialog by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        when {
            fineGranted -> {
                viewModel.startTracking()
            }
            coarseGranted -> {
                // Coarse only: user selected "approximate location"
                showApproximatePermissionDialog = true
            }
            else -> {
                // Denied
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

        if (fineGranted) {
            viewModel.startTracking()
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
        // Top Bar: Status and Keep Screen On Toggle
        LiveTopBar(
            uiState = uiState,
            keepScreenOn = keepScreenOn,
            onToggleKeepScreenOn = { viewModel.setKeepScreenOn(!keepScreenOn) }
        )

        // Middle Section: Live Speedometer & Metrics
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            LiveSpeedometer(uiState = uiState)

            Spacer(modifier = Modifier.height(24.dp))

            // 3 Glanceable Metric Placeholders for Stage 1
            MetricsRow()
        }

        // Bottom Section: Glove-friendly action controls
        LiveBottomControls(
            uiState = uiState,
            onStartClick = { initiateStart() },
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

    // Location Permission Rationale Dialog
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = { Text("Location Permission Required") },
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
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
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

    // Approximate Location Warning Dialog
    if (showApproximatePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showApproximatePermissionDialog = false },
            title = { Text("Precise Location Needed") },
            text = {
                Text(
                    "You granted Approximate location only. Motorcycle speed calculation and distance " +
                    "tracking require Precise location.\n\n" +
                    "Please enable 'Precise location' in your device settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApproximatePermissionDialog = false
                        openAppSettings(context)
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproximatePermissionDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Permanently Denied Permission Dialog
    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentlyDeniedDialog = false },
            title = { Text("Permission Denied Permanently") },
            text = {
                Text(
                    "Location permission has been permanently denied. To track your rides, " +
                    "please grant Location permission manually in App Settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermanentlyDeniedDialog = false
                        openAppSettings(context)
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Open App Settings")
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
    onToggleKeepScreenOn: () -> Unit
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
            is LiveUiState.Idle -> "READY" to MaterialTheme.colorScheme.onSurfaceVariant
            is LiveUiState.WaitingForGps -> "WAITING FOR GPS" to GpsWaiting
            is LiveUiState.Tracking -> "RECORDING" to SpeedGreen
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
                Text(
                    text = "${uiState.speedKmh.roundToInt()}",
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 110.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "KM/H",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
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

            is LiveUiState.Idle -> {
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
private fun MetricsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            label = "DISTANCE",
            value = "0.0",
            unit = "km",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "MOVING TIME",
            value = "00:00",
            unit = "min",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "AVG SPEED",
            value = "0.0",
            unit = "km/h",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
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
}

@Composable
private fun LiveBottomControls(
    uiState: LiveUiState,
    onStartClick: () -> Unit,
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
            is LiveUiState.Idle -> {
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

            is LiveUiState.WaitingForGps, is LiveUiState.Tracking -> {
                HoldToStopButton(
                    onStopConfirmed = onStopConfirmed
                )
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
    val progressColor = MaterialTheme.colorScheme.error

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val job = coroutineScope.launch {
                                progress.snapTo(0f)
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 2000,
                                        easing = LinearEasing
                                    )
                                )
                                // Reached 100% hold
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStopConfirmed()
                            }
                            tryAwaitRelease()
                            job.cancel()
                            progress.snapTo(0f)
                        }
                    )
                }
        ) {
            // Background Canvas for Circular Progress Ring
            Canvas(modifier = Modifier.size(96.dp)) {
                // Background Track
                drawArc(
                    color = progressColor.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                // Active Progress Arc
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Center Stop Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Hold to stop",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "HOLD 2S TO STOP",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            letterSpacing = 1.sp
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
