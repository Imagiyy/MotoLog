package com.abrar.motolog.ui.history.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.OutlinedButton
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import com.abrar.motolog.ui.garage.RegistrationPlateBadge
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.abrar.motolog.domain.engine.UnitConverter
import com.abrar.motolog.domain.model.RideSplit
import com.abrar.motolog.domain.model.RouteMapData
import com.abrar.motolog.domain.util.RideNameGenerator
import com.abrar.motolog.ui.util.FormatUtils
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Detailed ride breakdown screen showing summary metrics, moving vs overall toggles,
 * and the per-kilometre splits table.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useMetricUnits by viewModel.useMetricUnits.collectAsStateWithLifecycle()
    val defaultMapTheme by viewModel.defaultMapTheme.collectAsStateWithLifecycle()
    val ride = uiState.ride
    val context = androidx.compose.ui.platform.LocalContext.current

    val gpxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportRideGpx(context, it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (ride != null) {
                        ride.name.ifBlank { RideNameGenerator.defaultNameForTimestamp(ride.startTime) }
                    } else {
                        "Ride Detail"
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (ride != null) {
                        IconButton(onClick = { viewModel.shareRideGpx(context) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share GPX"
                            )
                        }
                        IconButton(onClick = { gpxExportLauncher.launch("ride_${ride.id}.gpx") }) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export GPX"
                            )
                        }
                        IconButton(onClick = viewModel::openRenameDialog) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename ride"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                ride == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ride not found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    RideDetailContent(
                        ride = ride,
                        assignedBike = uiState.assignedBike,
                        splits = uiState.splits,
                        selectedSplitInterval = uiState.selectedSplitInterval,
                        onSelectSplitInterval = viewModel::setSplitInterval,
                        routeMap = uiState.routeMap,
                        isVisualsLoading = uiState.isVisualsLoading,
                        mapStyleProvider = viewModel.mapStyleProvider,
                        isSplitsLoading = uiState.isSplitsLoading,
                        showOverallStats = uiState.showOverallStats,
                        useMetricUnits = useMetricUnits,
                        defaultMapTheme = defaultMapTheme,
                        onToggleStatsMode = viewModel::toggleStatsMode,
                        onChangeBike = viewModel::openChangeBikeDialog
                    )
                }
            }

            if (uiState.isRenameDialogOpen && ride != null) {
                RenameRideDialog(
                    currentCandidateName = uiState.renameCandidateName,
                    defaultFallbackName = RideNameGenerator.defaultNameForTimestamp(ride.startTime),
                    onNameChange = viewModel::onRenameCandidateChanged,
                    onDismiss = viewModel::dismissRenameDialog,
                    onConfirm = viewModel::confirmRename
                )
            }

            if (uiState.isChangeBikeDialogOpen) {
                ChangeBikeDialog(
                    currentBikeId = ride?.bikeId,
                    availableBikes = uiState.availableBikes,
                    onDismiss = viewModel::dismissChangeBikeDialog,
                    onSelectBike = viewModel::selectBikeForRide
                )
            }
        }
    }
}

@Composable
private fun RideDetailContent(
    ride: RideEntity,
    assignedBike: BikeEntity?,
    splits: List<RideSplit>,
    selectedSplitInterval: SplitInterval = SplitInterval.SPLIT_1KM,
    onSelectSplitInterval: (SplitInterval) -> Unit = {},
    routeMap: RouteMapData?,
    isVisualsLoading: Boolean,
    mapStyleProvider: com.abrar.motolog.domain.map.MapStyleProvider,
    isSplitsLoading: Boolean,
    showOverallStats: Boolean,
    useMetricUnits: Boolean = true,
    defaultMapTheme: com.abrar.motolog.domain.model.MapThemePreference = com.abrar.motolog.domain.model.MapThemePreference.DARK,
    onToggleStatsMode: () -> Unit,
    onChangeBike: () -> Unit
) {
    val stoppedTimeMs = (ride.elapsedTimeMs - ride.movingTimeMs).coerceAtLeast(0L)
    val isMapDark = defaultMapTheme == com.abrar.motolog.domain.model.MapThemePreference.DARK
    var isMapTouching by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        userScrollEnabled = !isMapTouching,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isVisualsLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (routeMap != null && routeMap.segments.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pressed = event.changes.any { it.pressed }
                                    if (pressed != isMapTouching) {
                                        isMapTouching = pressed
                                    }
                                }
                            }
                        }
                ) {
                    RouteMap(
                        route = routeMap,
                        mapStyleProvider = mapStyleProvider,
                        isDarkTheme = isMapDark,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Date & Status Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = FormatUtils.formatDateTime(ride.startTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (ride.status == RideStatus.RECOVERED) {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        contentColor = Color(0xFFF59E0B),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "RECOVERED RIDE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Assigned Motorcycle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MOTORCYCLE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = assignedBike?.name ?: "No motorcycle assigned",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (assignedBike?.registrationNumber?.isNotBlank() == true) {
                                Spacer(modifier = Modifier.height(2.dp))
                                RegistrationPlateBadge(registrationNumber = assignedBike.registrationNumber)
                            }
                        }
                    }

                    OutlinedButton(onClick = onChangeBike) {
                        Text(if (assignedBike != null) "Change" else "Assign")
                    }
                }
            }
        }

        // Hero Card: Distance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL DISTANCE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = UnitConverter.formatDistance(ride.distanceMeters, useMetricUnits, decimals = 2),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (useMetricUnits) "KILOMETRES" else "MILES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Toggleable Speed & Time Section (Moving vs Overall)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RIDE METRICS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Moving vs Overall Pill Switch
                    MetricModeToggle(
                        showOverall = showOverallStats,
                        onToggle = onToggleStatsMode
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = if (showOverallStats) "OVERALL TIME" else "MOVING TIME",
                        value = FormatUtils.formatDuration(if (showOverallStats) ride.elapsedTimeMs else ride.movingTimeMs),
                        subtitle = if (showOverallStats) "Start to stop" else "Active riding",
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = if (showOverallStats) "OVERALL AVG" else "MOVING AVG",
                        value = UnitConverter.formatSpeedFromMsWithUnit(if (showOverallStats) ride.overallAvgSpeedMs else ride.avgMovingSpeedMs, useMetricUnits),
                        subtitle = if (showOverallStats) "Distance / Elapsed" else "Distance / Moving",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Secondary Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStatCard(
                    title = "MAX SPEED",
                    value = UnitConverter.formatSpeedFromMsWithUnit(ride.maxSpeedMs, useMetricUnits),
                    subtitle = "Peak recorded",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )

                MetricStatCard(
                    title = "STOPPED TIME",
                    value = FormatUtils.formatDuration(stoppedTimeMs),
                    subtitle = "Traffic & pauses",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Elevation Metrics (Gain, Loss, Sensor Source)
        if (ride.elevationGainMeters > 0.0 || ride.elevationLossMeters > 0.0 || ride.elevationSource.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ELEVATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (ride.elevationSource.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (ride.elevationSource.equals("barometer", ignoreCase = true)) "Barometer" else "GPS Altitude",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            title = "GAIN",
                            value = "+${UnitConverter.formatElevationWithUnit(ride.elevationGainMeters, useMetricUnits)}",
                            subtitle = "Cumulative climb",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "LOSS",
                            value = "-${UnitConverter.formatElevationWithUnit(ride.elevationLossMeters, useMetricUnits)}",
                            subtitle = "Cumulative descent",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Splits Header & Interval Selector
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DISTANCE SPLITS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (splits.isNotEmpty()) {
                    Text(
                        text = "${splits.size} ${if (splits.size == 1) "split" else "splits"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Selectable interval chips: 1 km, 10 km, 100 km (or 1 mi, 10 mi, 100 mi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SplitInterval.entries.forEach { interval ->
                    val label = if (useMetricUnits) interval.labelKm else interval.labelMi
                    val isSelected = interval == selectedSplitInterval
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSplitInterval(interval) },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        // Splits Table Content
        when {
            isSplitsLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            splits.isEmpty() -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "No distance splits recorded for this ride.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            }
            else -> {
                item {
                    SplitsTableHeader()
                }

                items(splits) { split ->
                    SplitRow(
                        split = split,
                        splitInterval = selectedSplitInterval,
                        useMetricUnits = useMetricUnits
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun MetricModeToggle(
    showOverall: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (!showOverall) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (!showOverall) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Moving",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = if (showOverall) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (showOverall) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Overall",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SplitsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SPLIT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "DISTANCE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "TIME",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "AVG SPEED",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.4f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SplitRow(
    split: RideSplit,
    splitInterval: SplitInterval = SplitInterval.SPLIT_1KM,
    useMetricUnits: Boolean = true
) {
    val unitSuffix = UnitConverter.distanceUnit(useMetricUnits)
    val multiplier = splitInterval.distanceMultiplier
    val splitTitle = if (split.isPartial) {
        "Split ${split.splitNumber} (final)"
    } else {
        if (multiplier == 1) {
            "${split.splitNumber} $unitSuffix"
        } else {
            val startDist = (split.splitNumber - 1) * multiplier
            val endDist = split.splitNumber * multiplier
            "$startDist-$endDist $unitSuffix"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = splitTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = UnitConverter.formatDistanceWithUnit(split.distanceMeters, useMetricUnits, decimals = 2),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center
        )
        Text(
            text = FormatUtils.formatDuration(split.durationMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center
        )
        Text(
            text = UnitConverter.formatSpeedWithUnit(split.avgSpeedKmh, useMetricUnits),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1.4f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun RenameRideDialog(
    currentCandidateName: String,
    defaultFallbackName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename Ride",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Give this ride a memorable name, or leave blank for default ($defaultFallbackName).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentCandidateName,
                    onValueChange = onNameChange,
                    label = { Text("Ride Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChangeBikeDialog(
    currentBikeId: Long?,
    availableBikes: List<BikeEntity>,
    onDismiss: () -> Unit,
    onSelectBike: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Motorcycle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select which motorcycle this ride belongs to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectBike(null) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentBikeId == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "None (Unassign)",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (currentBikeId == null) FontWeight.Bold else FontWeight.Normal
                    )
                }

                availableBikes.forEach { bike ->
                    val isSelected = bike.id == currentBikeId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectBike(bike.id) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = bike.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (bike.makeModel.isNotBlank()) {
                                Text(
                                    text = bike.makeModel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
