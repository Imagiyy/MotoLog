package com.abrar.motolog.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.MenuAnchorType
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
import com.abrar.motolog.domain.engine.UnitConverter
import com.abrar.motolog.domain.model.FuelUnit
import com.abrar.motolog.domain.model.SpeedAlertStyle
import com.abrar.motolog.domain.model.TrackingMode
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
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val voiceAnnouncementsEnabled by viewModel.voiceAnnouncementsEnabled.collectAsStateWithLifecycle()
    val thermalEcoMode by viewModel.thermalEcoMode.collectAsStateWithLifecycle()
    val activeBikes by viewModel.activeBikes.collectAsStateWithLifecycle()

    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val pendingRestore by viewModel.pendingRestoreManifest.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showLicensesScreen by remember { mutableStateOf(false) }

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
                }
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

            // Theme Mode Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Theme Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (mode) {
                                        ThemeMode.RETRO -> "Retro Biker (Vintage Cockpit)"
                                        ThemeMode.CAFE_RACER -> "Cafe Racer (British Racing Green)"
                                        ThemeMode.TRACK_DAY -> "Track Day (Corse Scarlet & Carbon)"
                                        ThemeMode.NEON_CYBER -> "Neon Cyberpunk (Tokyo Night & Cyan)"
                                        ThemeMode.DESERT_RALLY -> "Desert Rally (Dakar Sand & Khaki)"
                                        ThemeMode.DARK -> "Modern Dark (Recommended for riders)"
                                        ThemeMode.AMOLED -> "AMOLED Black (Pure OLED Black)"
                                        ThemeMode.LIGHT -> "High-Noon Light (Direct Sunlight)"
                                        ThemeMode.SYSTEM -> "System Default"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (themeMode == mode) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = when (mode) {
                                        ThemeMode.RETRO -> "Warm amber gold, brushed brass & cast iron"
                                        ThemeMode.CAFE_RACER -> "Deep spruce green, polished aluminium & ivory"
                                        ThemeMode.TRACK_DAY -> "Racing red, speed yellow & carbon titanium"
                                        ThemeMode.NEON_CYBER -> "Electric cyan, hot pink & synthwave violet"
                                        ThemeMode.DESERT_RALLY -> "Dakar gold, tactical khaki & weathered sandstone"
                                        ThemeMode.DARK -> "High-contrast dark graphite with amber highlights"
                                        ThemeMode.AMOLED -> "Pure 0% power black pixels for OLED night rides"
                                        ThemeMode.LIGHT -> "Ultra-high contrast parchment for blinding sunlight"
                                        ThemeMode.SYSTEM -> "Follows your Android system display setting"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
