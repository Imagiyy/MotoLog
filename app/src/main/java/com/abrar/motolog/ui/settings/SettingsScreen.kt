package com.abrar.motolog.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontStyle
import com.abrar.motolog.shared.domain.model.MapThemePreference
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.shared.domain.engine.UnitConverter
import com.abrar.motolog.shared.domain.model.FuelUnit
import com.abrar.motolog.shared.domain.model.SpeedAlertStyle
import com.abrar.motolog.shared.domain.model.TrackingMode
import com.abrar.motolog.ui.theme.ThemeMode
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val PRIVACY_POLICY_URL = "https://github.com/abrar/motolog/blob/main/docs/play-store/PRIVACY_POLICY.md"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val useMetricUnits by viewModel.useMetricUnits.collectAsStateWithLifecycle()
    val fuelUnit by viewModel.fuelUnit.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val trackingMode by viewModel.trackingMode.collectAsStateWithLifecycle()
    val speedAlertEnabled by viewModel.speedAlertEnabled.collectAsStateWithLifecycle()
    val speedAlertThresholdKmh by viewModel.speedAlertThresholdKmh.collectAsStateWithLifecycle()
    val speedAlertStyle by viewModel.speedAlertStyle.collectAsStateWithLifecycle()
    val autoPauseEnabled by viewModel.autoPauseEnabled.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val dashboardTextColor by viewModel.dashboardTextColor.collectAsStateWithLifecycle()
    val dashboardBgColor by viewModel.dashboardBgColor.collectAsStateWithLifecycle()
    val dashboardAccentColor by viewModel.dashboardAccentColor.collectAsStateWithLifecycle()
    val dashboardComponentColor by viewModel.dashboardComponentColor.collectAsStateWithLifecycle()
    val voiceAnnouncementsEnabled by viewModel.voiceAnnouncementsEnabled.collectAsStateWithLifecycle()
    val thermalEcoMode by viewModel.thermalEcoMode.collectAsStateWithLifecycle()
    val activeBikes by viewModel.activeBikes.collectAsStateWithLifecycle()

    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val pendingRestore by viewModel.pendingRestoreManifest.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showLicensesScreen by remember { mutableStateOf(false) }
    var hexDialogTarget by remember { mutableStateOf<String?>(null) }

    if (showLicensesScreen) {
        LicensesScreen(onNavigateBack = { showLicensesScreen = false })
        return
    }

    LaunchedEffect(Unit) {
        viewModel.messageFlow.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // SAF Launchers
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { viewModel.createBackup(it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.prepareRestore(it) }
    }

    val gpxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportAllRidesGpx(it) }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportRidesSummaryCsv(it) }
    }

    val gpxImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.prepareGpxImport(it) }
    }

    // Dialog States
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showSpeedAlertDialog by remember { mutableStateOf(false) }
    var showBackupWarningDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ============================================================
            // 1. UNITS & REGIONAL
            // ============================================================
            SettingsSectionHeader(title = "UNITS & REGIONAL")

            // Distance & Speed Units
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Distance & Speed Units",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectablePill(
                            selected = useMetricUnits,
                            text = "Metric (km, km/h)",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setUseMetricUnits(true) }
                        )
                        SelectablePill(
                            selected = !useMetricUnits,
                            text = "Imperial (mi, mph)",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setUseMetricUnits(false) }
                        )
                    }
                }
            }

            // Fuel Economy Unit
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Fuel Economy Unit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Separate unit for fuel consumption calculations in garage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FuelUnitDropdown(
                        selectedUnit = fuelUnit,
                        onUnitSelected = { viewModel.setFuelUnit(it) }
                    )
                }
            }

            // Currency Symbol
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCurrencyDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Currency Symbol",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Used for fuel cost display (no exchange/network)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ============================================================
            // 2. TRACKING MODE & GPS
            // ============================================================
            SettingsSectionHeader(title = "TRACKING & GPS")

            // Tracking Mode (Accuracy vs Battery)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tracking Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    TrackingMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setTrackingMode(mode) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = trackingMode == mode,
                                onClick = { viewModel.setTrackingMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Auto-pause Setting Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PauseCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Auto-Pause",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pause tracking when stopped under 3 km/h for 8s; resume when speed exceeds 5 km/h.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = autoPauseEnabled,
                        onCheckedChange = { viewModel.setAutoPauseEnabled(it) }
                    )
                }
            }

            // ============================================================
            // 3. SPEED ALERT
            // ============================================================
            SettingsSectionHeader(title = "SPEED ALERT")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Speed Alert",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (speedAlertEnabled) "Active above ${UnitConverter.formatSpeedWithUnit(speedAlertThresholdKmh, useMetricUnits)}" else "Off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (speedAlertEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = speedAlertEnabled,
                            onCheckedChange = { viewModel.setSpeedAlertEnabled(it) }
                        )
                    }

                    if (speedAlertEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Threshold button
                        OutlinedButton(
                            onClick = { showSpeedAlertDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Set Threshold: ${UnitConverter.formatSpeedWithUnit(speedAlertThresholdKmh, useMetricUnits)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Alert Feedback Style:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        SpeedAlertStyle.entries.forEach { style ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setSpeedAlertStyle(style) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = speedAlertStyle == style,
                                    onClick = { viewModel.setSpeedAlertStyle(style) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = style.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GPS speed is satellite-derived and is not a legal speedometer. This threshold is rider-configured for situational awareness.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Helmet Voice Announcements Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Helmet Voice Announcements",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Speaks speed warnings, auto-pause/resume, and ride milestones over Bluetooth intercoms (Sena, Cardo).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = voiceAnnouncementsEnabled,
                        onCheckedChange = { viewModel.setVoiceAnnouncementsEnabled(it) }
                    )
                }
            }

            // ============================================================
            // 4. DISPLAY & THEME
            // ============================================================
            SettingsSectionHeader(title = "DISPLAY & THEME")

            // Keep Screen On Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenLockPortrait,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Keep Screen On",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Keep display awake during active rides for glove-friendly viewing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = { viewModel.setKeepScreenOn(it) }
                    )
                }
            }

            // Thermal & Battery Saver Mode Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Thermal & Battery Saver Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Minimizes GPU redraws and optimizes battery cooling for phones mounted in direct handlebar sunlight.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = thermalEcoMode,
                        onCheckedChange = { viewModel.setThermalEcoMode(it) }
                    )
                }
            }

            // Dashboard Color Customization Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "App Theme & Colors",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Customize text, background, accent, and surface colors across the entire app",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Live Mini Preview of Theme Colors
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(dashboardBgColor))
                            .border(1.5.dp, Color(dashboardAccentColor), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Sample speedometer readout
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "0",
                                    color = Color(dashboardTextColor),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 28.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "KM/H",
                                    color = Color(dashboardTextColor).copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            // Sample component container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(dashboardComponentColor))
                                    .border(0.8.dp, Color(dashboardAccentColor).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "0.0 KM",
                                        color = Color(dashboardTextColor),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "DISTANCE",
                                        color = Color(dashboardTextColor).copy(alpha = 0.65f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Text(text = "TOP 0.0", color = Color(dashboardTextColor), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text(text = "TIME 00:00", color = Color(dashboardTextColor), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text(text = "AVG 0.0", color = Color(dashboardTextColor), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 1. Text Color Selector
                    ColorSelectorRow(
                        title = "Text Color",
                        description = "Applies to speed digits, text labels, telemetry stats, and headings across the app",
                        selectedColor = dashboardTextColor,
                        palette = listOf(
                            0xFFFFFFFFL, // Pure White
                            0xFF00E5FFL, // Electric Cyan
                            0xFF00E676L, // Neon Lime
                            0xFFFFB300L, // Sunlight Amber
                            0xFFFFEA00L, // Bright Yellow
                            0xFFFF9100L, // Blaze Orange
                            0xFFFF1744L, // Hot Red
                            0xFF40C4FFL  // Sky Blue
                        ),
                        onColorSelected = { viewModel.setDashboardTextColor(it) },
                        onCustomClick = { hexDialogTarget = "text" }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 2. Background Color Selector
                    ColorSelectorRow(
                        title = "Background Color",
                        description = "Background color for the dashboard, navigation, and screens",
                        selectedColor = dashboardBgColor,
                        palette = listOf(
                            0xFF000000L, // AMOLED Pure Black
                            0xFF030508L, // Deep Racing TFT
                            0xFF080C14L, // Midnight Navy
                            0xFF121316L, // Carbon Charcoal
                            0xFF1A1E24L, // Dark Slate
                            0xFF0A140FL  // Racing Deep Green
                        ),
                        onColorSelected = { viewModel.setDashboardBgColor(it) },
                        onCustomClick = { hexDialogTarget = "bg" }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 3. Gauge & Accent Color Selector
                    ColorSelectorRow(
                        title = "Accent & Highlight Color",
                        description = "Speed arc sweep, active tabs, buttons, borders, and highlights across the app",
                        selectedColor = dashboardAccentColor,
                        palette = listOf(
                            0xFF00D4E5L, // Electric Cyan
                            0xFF00E676L, // Racing Green
                            0xFFFF9100L, // Moto Orange
                            0xFFFF1744L, // Sport Red
                            0xFFFFFF00L, // Superbike Yellow
                            0xFFD500F9L, // Neon Violet
                            0xFFFFFFFFL  // Monochrome White
                        ),
                        onColorSelected = { viewModel.setDashboardAccentColor(it) },
                        onCustomClick = { hexDialogTarget = "accent" }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 4. Component Surface Color Selector
                    ColorSelectorRow(
                        title = "Component Surface Color",
                        description = "Cards, telemetry boxes, top bars, and containers across the app",
                        selectedColor = dashboardComponentColor,
                        palette = listOf(
                            0xFF000000L, // Deep AMOLED
                            0xFF0D0F14L, // Bezel Charcoal
                            0xFF141720L, // Surface Slate
                            0xFF1C2028L, // Carbon Gray
                            0xFF242933L  // Dark Metallic
                        ),
                        onColorSelected = { viewModel.setDashboardComponentColor(it) },
                        onCustomClick = { hexDialogTarget = "component" }
                    )

                    // Reset button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.resetDashboardColors() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset to Defaults")
                        }
                    }
                }
            }

            // ============================================================
            // 5. DATA OWNERSHIP & BACKUP
            // ============================================================
            SettingsSectionHeader(title = "DATA OWNERSHIP & BACKUP")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Export & Share Rides",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Export your ride history without cloud accounts. All data is owned by you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                gpxExportLauncher.launch("motolog_rides_$timestamp.gpx")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GPX File")
                        }

                        OutlinedButton(
                            onClick = { viewModel.shareAllRidesGpx() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share GPX")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            csvExportLauncher.launch("motolog_summary_$timestamp.csv")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Rides Summary (CSV)")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    Text(
                        text = "Import GPX Track",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { gpxImportLauncher.launch(arrayOf("application/gpx+xml", "text/xml", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import GPX Track (SAF)")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    Text(
                        text = "Full Database Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Creates a portable ZIP containing all rides, GPS points, bikes, maintenance records, and settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showBackupWarningDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup")
                        }

                        OutlinedButton(
                            onClick = { restoreLauncher.launch(arrayOf("application/zip", "*/*")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }

            // ============================================================
            // 6. ABOUT & COMPLIANCE
            // ============================================================
            SettingsSectionHeader(title = "ABOUT & COMPLIANCE")

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "MotoLog",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version 1.0.0 (Build 1)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    // Safety disclaimer
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Speed readings are GPS-derived and do not replace a vehicle speedometer. Never operate or adjust settings while riding.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Map Attribution
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Maps powered by OpenFreeMap © OpenMapTiles © OpenStreetMap contributors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    // Privacy Policy Button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Privacy Policy")
                    }

                    // Open Source Licenses Button
                    OutlinedButton(
                        onClick = { showLicensesScreen = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Source Licenses")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ============================================================
    // DIALOGS
    // ============================================================

    // Custom HEX Color Dialog
    hexDialogTarget?.let { target ->
        val targetName = when (target) {
            "text" -> "Text Color"
            "bg" -> "Background Color"
            "accent" -> "Gauge & Accent Color"
            else -> "Component Surface Color"
        }
        val currentTargetColor = when (target) {
            "text" -> dashboardTextColor
            "bg" -> dashboardBgColor
            "accent" -> dashboardAccentColor
            else -> dashboardComponentColor
        }
        val currentHex = String.format("%06X", currentTargetColor and 0xFFFFFFL)
        var inputHex by remember(target) { mutableStateOf(currentHex) }
        val parsedColor: Long? = remember(inputHex) {
            val clean = inputHex.removePrefix("#").trim()
            if (clean.length == 6) {
                clean.toLongOrNull(16)?.let { 0xFF000000L or it }
            } else null
        }

        AlertDialog(
            onDismissRequest = { hexDialogTarget = null },
            title = { Text("Custom $targetName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter a 6-digit hex code (e.g. 00E5FF or #FFFFFF):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = inputHex,
                        onValueChange = { inputHex = it.uppercase() },
                        singleLine = true,
                        label = { Text("HEX Color") },
                        prefix = { Text("#") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(parsedColor ?: currentTargetColor))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = if (parsedColor != null) "Preview ready" else "Enter 6 valid hex characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (parsedColor != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        parsedColor?.let { color ->
                            when (target) {
                                "text" -> viewModel.setDashboardTextColor(color)
                                "bg" -> viewModel.setDashboardBgColor(color)
                                "accent" -> viewModel.setDashboardAccentColor(color)
                                "component" -> viewModel.setDashboardComponentColor(color)
                            }
                        }
                        hexDialogTarget = null
                    },
                    enabled = parsedColor != null
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { hexDialogTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Currency Dialog
    if (showCurrencyDialog) {
        var tempSymbol by remember { mutableStateOf(currencySymbol) }
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Currency Symbol") },
            text = {
                Column {
                    Text(
                        text = "Enter currency symbol or code to display with fuel logs:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempSymbol,
                        onValueChange = { tempSymbol = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("$", "€", "£", "₹", "¥", "C$").forEach { chip ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { tempSymbol = chip }
                            ) {
                                Text(chip, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setCurrencySymbol(tempSymbol)
                    showCurrencyDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Speed Alert Threshold Dialog
    if (showSpeedAlertDialog) {
        val userSpeed = UnitConverter.kmhToUserSpeed(speedAlertThresholdKmh, useMetricUnits)
        var tempSpeedStr by remember { mutableStateOf(userSpeed.toInt().toString()) }
        val speedUnitStr = UnitConverter.speedUnit(useMetricUnits)

        AlertDialog(
            onDismissRequest = { showSpeedAlertDialog = false },
            title = { Text("Speed Alert Threshold") },
            text = {
                Column {
                    Text(
                        text = "Enter alert speed threshold ($speedUnitStr):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempSpeedStr,
                        onValueChange = { tempSpeedStr = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = { Text(speedUnitStr, modifier = Modifier.padding(end = 12.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = tempSpeedStr.toDoubleOrNull() ?: 100.0
                    viewModel.setSpeedAlertThresholdUser(parsed, useMetricUnits)
                    showSpeedAlertDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpeedAlertDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backup Location Warning Dialog
    if (showBackupWarningDialog) {
        AlertDialog(
            onDismissRequest = { showBackupWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Location History Warning") },
            text = {
                Text(
                    text = "The backup file contains complete, unencrypted GPS ride tracks and location history. Store this file only in a location or cloud storage you trust.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = {
                    showBackupWarningDialog = false
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    backupLauncher.launch("motolog_backup_$timestamp.zip")
                }) {
                    Text("Proceed to Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // GPX Import Options Dialog
    pendingImport?.let { pending ->
        var selectedBikeId by remember { mutableStateOf<Long?>(activeBikes.firstOrNull()?.id) }
        var countsTowardOdo by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissImportDialog() },
            title = { Text("Import GPX Track") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Found ${pending.tracks.size} track(s) with ${pending.tracks.sumOf { it.points.size }} points.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (pending.isDuplicateDetected) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "A ride with similar start time and distance was already found in your history. Import anyway?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Bike assignment
                    Text("Assign to Bike (Optional):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    BikeAssignmentDropdown(
                        bikes = activeBikes,
                        selectedBikeId = selectedBikeId,
                        onBikeSelected = { selectedBikeId = it }
                    )

                    // Odometer counting checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = countsTowardOdo,
                            onCheckedChange = { countsTowardOdo = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add distance to bike odometer (Defaults to No)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.executeImport(selectedBikeId, countsTowardOdo) }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    pendingRestore?.let { (_, manifest) ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreDialog() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Restore Database Backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Restoring will REPLACE all current data in the app with the backup contents:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Rides: ${manifest.rideCount}", fontWeight = FontWeight.SemiBold)
                    Text("• GPS Points: ${manifest.pointCount}", fontWeight = FontWeight.SemiBold)
                    Text("• Bikes: ${manifest.bikeCount}", fontWeight = FontWeight.SemiBold)
                    Text("• Maintenance Items: ${manifest.maintenanceCount}", fontWeight = FontWeight.SemiBold)
                    Text("• Fuel Logs: ${manifest.fuelLogCount}", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This operation cannot be undone. Are you sure you want to proceed?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRestore() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Replace & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestoreDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SelectablePill(
    selected: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelUnitDropdown(
    selectedUnit: FuelUnit,
    onUnitSelected: (FuelUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = "${selectedUnit.displayName} (${selectedUnit.unitLabel})",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FuelUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text("${unit.displayName} (${unit.unitLabel})") },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BikeAssignmentDropdown(
    bikes: List<BikeEntity>,
    selectedBikeId: Long?,
    onBikeSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentBikeName = bikes.firstOrNull { it.id == selectedBikeId }?.name ?: "No Bike Assigned"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentBikeName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("No Bike Assigned") },
                onClick = {
                    onBikeSelected(null)
                    expanded = false
                }
            )
            bikes.forEach { bike ->
                DropdownMenuItem(
                    text = { Text(bike.name) },
                    onClick = {
                        onBikeSelected(bike.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorSelectorRow(
    title: String,
    description: String,
    selectedColor: Long,
    palette: List<Long>,
    onColorSelected: (Long) -> Unit,
    onCustomClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            palette.forEach { colorValue ->
                val isSelected = (selectedColor and 0xFFFFFFL) == (colorValue and 0xFFFFFFL)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(colorValue))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorValue) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (Color(colorValue).luminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onCustomClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "+ HEX",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
