package com.abrar.motolog.ui.garage.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.domain.model.MaintenanceEvaluation
import com.abrar.motolog.domain.model.MaintenancePreset
import com.abrar.motolog.domain.model.MaintenanceStatus
import com.abrar.motolog.ui.util.FormatUtils
import com.abrar.motolog.domain.engine.UnitConverter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: BikeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bike = uiState.bike

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = bike?.name ?: "Motorcycle",
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
                actions = {
                    if (bike != null) {
                        IconButton(onClick = { viewModel.openEditBikeDialog() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Motorcycle")
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                bike == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Motorcycle not found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Odometer & Overview card
                        OdometerHeaderCard(
                            currentOdometerKm = uiState.currentOdometerKm,
                            makeModel = bike.makeModel,
                            ridesCount = uiState.recordedRidesCount,
                            ridesDistanceKm = uiState.recordedRidesDistanceKm,
                            useMetricUnits = uiState.useMetricUnits,
                            onAdjustOdometer = { viewModel.openSetOdometerDialog() },
                            modifier = Modifier.padding(16.dp)
                        )

                        // Tabs: Maintenance vs Fuel
                        TabRow(selectedTabIndex = uiState.selectedTab) {
                            Tab(
                                selected = uiState.selectedTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                text = { Text("Maintenance (${uiState.maintenanceEvaluations.size})") }
                            )
                            Tab(
                                selected = uiState.selectedTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                text = { Text("Fuel Log (${uiState.fuelLogs.size})") }
                            )
                        }

                        when (uiState.selectedTab) {
                            0 -> MaintenanceTabContent(
                                evaluations = uiState.maintenanceEvaluations,
                                useMetricUnits = uiState.useMetricUnits,
                                onAddItem = { viewModel.openAddMaintenanceDialog() },
                                onMarkDone = { viewModel.requestMarkDone(it.item) },
                                onEdit = { viewModel.openEditMaintenanceDialog(it.item) },
                                onDelete = { viewModel.requestDeleteMaintenanceItem(it.item) }
                            )
                            1 -> FuelTabContent(
                                uiState = uiState,
                                onAddLog = { viewModel.openAddFuelDialog() },
                                onEditLog = { viewModel.openEditFuelDialog(it) },
                                onDeleteLog = { viewModel.requestDeleteFuelLog(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.isSetOdometerDialogOpen && bike != null) {
        SetOdometerDialog(
            currentOdoKm = uiState.currentOdometerKm,
            useMetricUnits = uiState.useMetricUnits,
            onDismiss = { viewModel.dismissSetOdometerDialog() },
            onConfirm = { viewModel.setOdometer(it) }
        )
    }

    if (uiState.isEditBikeDialogOpen && bike != null) {
        EditBikeDialog(
            initialName = bike.name,
            initialMakeModel = bike.makeModel,
            onDismiss = { viewModel.dismissEditBikeDialog() },
            onConfirm = { name, makeModel -> viewModel.updateBike(name, makeModel) }
        )
    }

    if (uiState.isAddMaintenanceDialogOpen) {
        AddEditMaintenanceDialog(
            item = null,
            useMetricUnits = uiState.useMetricUnits,
            onDismiss = { viewModel.dismissAddMaintenanceDialog() },
            onConfirm = { name, intervalKm, intervalDays ->
                viewModel.addMaintenanceItem(name, intervalKm, intervalDays)
            }
        )
    }

    uiState.itemToEdit?.let { item ->
        AddEditMaintenanceDialog(
            item = item,
            useMetricUnits = uiState.useMetricUnits,
            onDismiss = { viewModel.dismissEditMaintenanceDialog() },
            onConfirm = { name, intervalKm, intervalDays ->
                viewModel.updateMaintenanceItem(item.copy(name = name, intervalKm = intervalKm, intervalDays = intervalDays))
            }
        )
    }

    uiState.itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteMaintenanceDialog() },
            title = { Text("Delete Service Schedule") },
            text = { Text("Are you sure you want to delete ${item.name}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteMaintenanceItem() }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteMaintenanceDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    uiState.itemToMarkDone?.let { item ->
        MarkDoneDialog(
            item = item,
            currentOdometerKm = uiState.currentOdometerKm,
            useMetricUnits = uiState.useMetricUnits,
            onDismiss = { viewModel.dismissMarkDoneDialog() },
            onConfirm = { odo -> viewModel.confirmMarkDone(item.id, odo) }
        )
    }

    if (uiState.isAddFuelDialogOpen) {
        AddEditFuelLogDialog(
            log = null,
            defaultOdometerKm = uiState.currentOdometerKm,
            useMetricUnits = uiState.useMetricUnits,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { viewModel.dismissAddFuelDialog() },
            onConfirm = { odo, litres, cost, full, notes, timestamp ->
                viewModel.addFuelLog(odo, litres, cost, full, notes, timestamp)
            }
        )
    }

    uiState.fuelLogToEdit?.let { log ->
        AddEditFuelLogDialog(
            log = log,
            defaultOdometerKm = log.odometerKm,
            useMetricUnits = uiState.useMetricUnits,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { viewModel.dismissEditFuelDialog() },
            onConfirm = { odo, litres, cost, full, notes, timestamp ->
                viewModel.updateFuelLog(
                    log.copy(
                        odometerKm = odo,
                        litres = litres,
                        totalCost = cost,
                        isFullTank = full,
                        notes = notes,
                        timestampEpochMs = timestamp
                    )
                )
            }
        )
    }

    uiState.fuelLogToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteFuelDialog() },
            title = { Text("Delete Fuel Entry") },
            text = { Text("Are you sure you want to delete this fuel fill-up of ${log.litres} L?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteFuelLog() }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteFuelDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OdometerHeaderCard(
    currentOdometerKm: Double,
    makeModel: String,
    ridesCount: Int,
    ridesDistanceKm: Double,
    useMetricUnits: Boolean,
    onAdjustOdometer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT ODOMETER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val userOdo = UnitConverter.kmToUserDistance(currentOdometerKm, useMetricUnits)
                    val unitStr = UnitConverter.distanceUnit(useMetricUnits)
                    Text(
                        text = String.format(Locale.US, "%,.1f %s", userOdo, unitStr),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(onClick = onAdjustOdometer) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Calibrate")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (makeModel.isNotBlank()) makeModel else "MotoLog Registered",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val userRidesDist = UnitConverter.kmToUserDistance(ridesDistanceKm, useMetricUnits)
                val unitStr = UnitConverter.distanceUnit(useMetricUnits)
                Text(
                    text = "$ridesCount rides (${String.format(Locale.US, "%.1f", userRidesDist)} $unitStr in app)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ============================================================
// Maintenance Tab
// ============================================================

@Composable
private fun MaintenanceTabContent(
    evaluations: List<MaintenanceEvaluation>,
    useMetricUnits: Boolean,
    onAddItem: () -> Unit,
    onMarkDone: (MaintenanceEvaluation) -> Unit,
    onEdit: (MaintenanceEvaluation) -> Unit,
    onDelete: (MaintenanceEvaluation) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled Services",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onAddItem) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Service")
                }
            }
        }

        if (evaluations.isEmpty()) {
            item {
                EmptySectionView(
                    icon = Icons.Default.Build,
                    title = "No maintenance schedules",
                    subtitle = "Add oil changes, chain maintenance, or tyre checks to get timely alerts."
                )
            }
        } else {
            items(evaluations, key = { it.item.id }) { eval ->
                MaintenanceItemCard(
                    eval = eval,
                    useMetricUnits = useMetricUnits,
                    onMarkDone = { onMarkDone(eval) },
                    onEdit = { onEdit(eval) },
                    onDelete = { onDelete(eval) }
                )
            }
        }
    }
}

@Composable
private fun MaintenanceItemCard(
    eval: MaintenanceEvaluation,
    useMetricUnits: Boolean,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = eval.status
    val (statusBg, statusFg, statusLabel) = when (status) {
        MaintenanceStatus.OVERDUE -> Triple(Color(0x33EF5350), Color(0xFFEF5350), "OVERDUE")
        MaintenanceStatus.DUE_SOON -> Triple(Color(0x33FFA726), Color(0xFFFFA726), "DUE SOON")
        MaintenanceStatus.OK -> Triple(Color(0x3366BB6A), Color(0xFF66BB6A), "OK")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = eval.item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatInterval(eval.item.intervalKm, eval.item.intervalDays, useMetricUnits),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusFg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = eval.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (status == MaintenanceStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            val lastDoneUserOdo = UnitConverter.kmToUserDistance(eval.item.lastDoneOdometerKm, useMetricUnits)
            val distUnit = UnitConverter.distanceUnit(useMetricUnits)
            Text(
                text = "Last done: ${String.format(Locale.US, "%,.1f %s", lastDoneUserOdo, distUnit)} (${FormatUtils.formatDate(eval.item.lastDoneDateEpochMs)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onMarkDone,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Done")
                }
            }
        }
    }
}

private fun formatInterval(intervalKm: Double?, intervalDays: Int?, useMetricUnits: Boolean): String {
    val intervalText = intervalKm?.let { UnitConverter.formatInterval(it, useMetricUnits) }
    return when {
        intervalText != null && intervalDays != null -> "Every $intervalText or $intervalDays days"
        intervalText != null -> "Every $intervalText"
        intervalDays != null -> "Every $intervalDays days"
        else -> "Custom schedule"
    }
}

// ============================================================
// Fuel Tab
// ============================================================

@Composable
private fun FuelTabContent(
    uiState: BikeDetailUiState,
    onAddLog: () -> Unit,
    onEditLog: (FuelLogEntity) -> Unit,
    onDeleteLog: (FuelLogEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FuelSummaryCard(
                stats = uiState.fuelStats,
                fuelUnit = uiState.fuelUnit
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fill-Up History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onAddLog) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Fill-Up")
                }
            }
        }

        if (uiState.fuelLogs.isEmpty()) {
            item {
                EmptySectionView(
                    icon = Icons.Default.LocalGasStation,
                    title = "No fuel records",
                    subtitle = "Log consecutive full-tank fills to compute accurate fuel mileage (${uiState.fuelUnit.unitLabel})."
                )
            }
        } else {
            // Map interval efficiencies to logs
            val intervalMap = uiState.fuelStats.intervalResults.associateBy { it.endLogId }

            items(uiState.fuelLogs, key = { it.id }) { log ->
                val intervalResult = intervalMap[log.id]
                FuelLogCard(
                    log = log,
                    intervalKml = intervalResult?.mileageKml,
                    useMetricUnits = uiState.useMetricUnits,
                    fuelUnit = uiState.fuelUnit,
                    currencySymbol = uiState.currencySymbol,
                    onEdit = { onEditLog(log) },
                    onDelete = { onDeleteLog(log) }
                )
            }
        }
    }
}

@Composable
private fun FuelSummaryCard(
    stats: com.abrar.motolog.domain.model.FuelMileageStats,
    fuelUnit: com.abrar.motolog.domain.model.FuelUnit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "FUEL MILEAGE (FULL-TANK METHOD)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("AVERAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val avg = stats.averageMileageKml
                    Text(
                        text = if (avg != null) UnitConverter.formatFuelEconomy(avg, fuelUnit) else "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column {
                    Text("LATEST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val latest = stats.latestMileageKml
                    Text(
                        text = if (latest != null) UnitConverter.formatFuelEconomy(latest, fuelUnit) else "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column {
                    Text("TOTAL LITRES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.US, "%.1f L", stats.totalLitres),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (stats.measuredIntervalsCount == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Requires at least 2 full-tank fill-ups to compute consumption (${fuelUnit.unitLabel}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FuelLogCard(
    log: FuelLogEntity,
    intervalKml: Double?,
    useMetricUnits: Boolean,
    fuelUnit: com.abrar.motolog.domain.model.FuelUnit,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val userOdo = UnitConverter.kmToUserDistance(log.odometerKm, useMetricUnits)
                    val distUnit = UnitConverter.distanceUnit(useMetricUnits)
                    Text(
                        text = String.format(Locale.US, "%,.1f %s", userOdo, distUnit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = FormatUtils.formatDate(log.timestampEpochMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (intervalKml != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = UnitConverter.formatFuelEconomy(intervalKml, fuelUnit),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Surface(
                        color = if (log.isFullTank) Color(0x3366BB6A) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (log.isFullTank) "FULL" else "PARTIAL",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (log.isFullTank) Color(0xFF66BB6A) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.2f", log.litres)} L  •  Cost: $currencySymbol${String.format(Locale.US, "%.2f", log.totalCost)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (log.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptySectionView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ============================================================
// Dialogs
// ============================================================

@Composable
private fun SetOdometerDialog(
    currentOdoKm: Double,
    useMetricUnits: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val displayOdo = UnitConverter.kmToUserDistance(currentOdoKm, useMetricUnits)
    var textValue by rememberSaveable { mutableStateOf(String.format(Locale.US, "%.1f", displayOdo)) }
    val newOdo = textValue.toDoubleOrNull()
    val isValid = newOdo != null && newOdo >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calibrate Odometer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter the current physical dashboard odometer reading of your motorcycle. MotoLog will adjust its calibration offset accordingly.",
                    style = MaterialTheme.typography.bodySmall
                )
                val distUnit = UnitConverter.distanceUnit(useMetricUnits)
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Target Odometer ($distUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        val odoKm = UnitConverter.userDistanceToKm(newOdo!!, useMetricUnits)
                        onConfirm(odoKm)
                    }
                },
                enabled = isValid
            ) {
                Text("Set Odometer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditBikeDialog(
    initialName: String,
    initialMakeModel: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, makeModel: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var makeModel by rememberSaveable { mutableStateOf(initialMakeModel) }
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Motorcycle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = makeModel,
                    onValueChange = { makeModel = it },
                    label = { Text("Make & Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (isValid) onConfirm(name, makeModel) }, enabled = isValid) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddEditMaintenanceDialog(
    item: MaintenanceItemEntity?,
    useMetricUnits: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, intervalKm: Double?, intervalDays: Int?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(item?.name.orEmpty()) }
    var distText by rememberSaveable {
        mutableStateOf(item?.intervalKm?.let {
            val disp = UnitConverter.getIntervalForDisplay(it, useMetricUnits)
            if (kotlin.math.abs(disp - kotlin.math.round(disp)) < 0.01) {
                disp.toLong().toString()
            } else {
                String.format(Locale.US, "%.1f", disp)
            }
        }.orEmpty())
    }
    var daysText by rememberSaveable { mutableStateOf(item?.intervalDays?.toString().orEmpty()) }

    val isValid = name.isNotBlank() && (distText.toDoubleOrNull() != null || daysText.toIntOrNull() != null)
    val distUnit = UnitConverter.distanceUnit(useMetricUnits)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add Service Schedule" else "Edit Service Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item == null) {
                    Text("Quick Presets", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MaintenancePreset.DEFAULT_PRESETS) { preset ->
                            FilterChip(
                                selected = name == preset.name,
                                onClick = {
                                    name = preset.name
                                    distText = preset.defaultIntervalKm?.let {
                                        val disp = UnitConverter.getIntervalForDisplay(it, useMetricUnits)
                                        if (kotlin.math.abs(disp - kotlin.math.round(disp)) < 0.01) {
                                            disp.toLong().toString()
                                        } else {
                                            String.format(Locale.US, "%.1f", disp)
                                        }
                                    }.orEmpty()
                                    daysText = preset.defaultIntervalDays?.toString().orEmpty()
                                },
                                label = { Text(preset.name, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name (e.g. Engine Oil)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = distText,
                    onValueChange = { distText = it },
                    label = { Text("Interval ($distUnit, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("Interval (Days, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        val intervalKm = distText.toDoubleOrNull()?.let { UnitConverter.userDistanceToKm(it, useMetricUnits) }
                        onConfirm(name, intervalKm, daysText.toIntOrNull())
                    }
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MarkDoneDialog(
    item: MaintenanceItemEntity,
    currentOdometerKm: Double,
    useMetricUnits: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val displayOdo = UnitConverter.kmToUserDistance(currentOdometerKm, useMetricUnits)
    var odoText by rememberSaveable { mutableStateOf(String.format(Locale.US, "%.1f", displayOdo)) }
    val odo = odoText.toDoubleOrNull()
    val isValid = odo != null && odo >= 0.0
    val distUnit = UnitConverter.distanceUnit(useMetricUnits)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Service as Done") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Record service for ${item.name} performed today.")
                OutlinedTextField(
                    value = odoText,
                    onValueChange = { odoText = it },
                    label = { Text("Odometer Reading ($distUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        val odoKm = UnitConverter.userDistanceToKm(odo!!, useMetricUnits)
                        onConfirm(odoKm)
                    }
                },
                enabled = isValid
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddEditFuelLogDialog(
    log: FuelLogEntity?,
    defaultOdometerKm: Double,
    useMetricUnits: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (odometerKm: Double, litres: Double, totalCost: Double, isFullTank: Boolean, notes: String, timestamp: Long) -> Unit
) {
    val initialOdo = log?.odometerKm ?: defaultOdometerKm
    val displayOdo = UnitConverter.kmToUserDistance(initialOdo, useMetricUnits)
    var odoText by rememberSaveable { mutableStateOf(String.format(Locale.US, "%.1f", displayOdo)) }
    var litresText by rememberSaveable { mutableStateOf(log?.litres?.toString().orEmpty()) }
    var costText by rememberSaveable { mutableStateOf(log?.totalCost?.toString().orEmpty()) }
    var isFullTank by rememberSaveable { mutableStateOf(log?.isFullTank ?: true) }
    var notes by rememberSaveable { mutableStateOf(log?.notes.orEmpty()) }
    val distUnit = UnitConverter.distanceUnit(useMetricUnits)

    val odo = odoText.toDoubleOrNull()
    val litres = litresText.toDoubleOrNull()
    val cost = costText.toDoubleOrNull()
    val isValid = odo != null && odo >= 0.0 && litres != null && litres > 0.0 && cost != null && cost >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (log == null) "Add Fuel Fill-Up" else "Edit Fuel Fill-Up") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = odoText,
                    onValueChange = { odoText = it },
                    label = { Text("Odometer ($distUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = litresText,
                    onValueChange = { litresText = it },
                    label = { Text("Volume (Litres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Total Cost ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isFullTank = !isFullTank }
                ) {
                    Checkbox(checked = isFullTank, onCheckedChange = { isFullTank = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filled tank to full", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional, e.g. 95 Octane)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        val odoKm = UnitConverter.userDistanceToKm(odo!!, useMetricUnits)
                        onConfirm(
                            odoKm,
                            litres!!,
                            cost!!,
                            isFullTank,
                            notes,
                            log?.timestampEpochMs ?: System.currentTimeMillis()
                        )
                    }
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
