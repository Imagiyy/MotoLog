package com.abrar.motolog.ui.garage.detail

import com.abrar.motolog.domain.engine.toEntity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.PrimaryTabRow
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.shared.domain.model.MaintenanceEvaluation
import com.abrar.motolog.shared.domain.model.MaintenancePreset
import com.abrar.motolog.shared.domain.model.MaintenanceStatus
import com.abrar.motolog.ui.garage.MotorcycleCatalogSelector
import com.abrar.motolog.ui.garage.RegistrationPlateBadge
import com.abrar.motolog.ui.util.FormatUtils
import com.abrar.motolog.shared.domain.engine.UnitConverter
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
                        IconButton(onClick = { viewModel.toggleActiveBike() }) {
                            Icon(
                                imageVector = if (uiState.isCurrentBike) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (uiState.isCurrentBike) "Remove as active bike" else "Set as active bike",
                                tint = if (uiState.isCurrentBike) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                            registrationNumber = bike.registrationNumber,
                            ridesCount = uiState.recordedRidesCount,
                            ridesDistanceKm = uiState.recordedRidesDistanceKm,
                            unloggedDistanceKm = bike.odometerOffsetKm,
                            useMetricUnits = uiState.useMetricUnits,
                            isCurrentBike = uiState.isCurrentBike,
                            onToggleActive = { viewModel.toggleActiveBike() },
                            onAdjustOdometer = { viewModel.openSetOdometerDialog() },
                            modifier = Modifier.padding(16.dp)
                        )

                        // Tabs: Fuel Log vs Maintenance (Fuel Log first)
                        PrimaryTabRow(selectedTabIndex = uiState.selectedTab) {
                            Tab(
                                selected = uiState.selectedTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                text = { Text("Fuel Log (${uiState.fuelLogs.size})") }
                            )
                            Tab(
                                selected = uiState.selectedTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                text = { Text("Maintenance (${uiState.maintenanceEvaluations.size})") }
                            )
                        }

                        when (uiState.selectedTab) {
                            0 -> FuelTabContent(
                                uiState = uiState,
                                onAddLog = { viewModel.openAddFuelDialog() },
                                onEditLog = { viewModel.openEditFuelDialog(it) },
                                onDeleteLog = { viewModel.requestDeleteFuelLog(it) },
                                onResetMileage = { viewModel.openResetMileageDialog() },
                                onClearReset = { viewModel.clearMileageReset() },
                                onDeleteMileageCycle = { viewModel.deleteMileageCycle(it) }
                            )
                            1 -> MaintenanceTabContent(
                                evaluations = uiState.maintenanceEvaluations,
                                useMetricUnits = uiState.useMetricUnits,
                                onAddItem = { viewModel.openAddMaintenanceDialog() },
                                onMarkDone = { viewModel.requestMarkDone(it.item.toEntity()) },
                                onEdit = { viewModel.openEditMaintenanceDialog(it.item.toEntity()) },
                                onDelete = { viewModel.requestDeleteMaintenanceItem(it.item.toEntity()) }
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
            initialRegistrationNumber = bike.registrationNumber,
            onDismiss = { viewModel.dismissEditBikeDialog() },
            onConfirm = { name, makeModel, regNumber ->
                viewModel.updateBike(name, makeModel, regNumber)
            }
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
        val resetEpoch = uiState.bike?.mileageResetEpochMs
        val activeLogs = if (resetEpoch != null) {
            uiState.fuelLogs.filter { it.timestampEpochMs >= resetEpoch }
        } else {
            uiState.fuelLogs
        }
        val lastLogOdo = activeLogs.firstOrNull()?.odometerKm
        AddEditFuelLogDialog(
            log = null,
            defaultOdometerKm = uiState.currentOdometerKm,
            currentBikeOdometerKm = uiState.currentOdometerKm,
            lastLogOdometerKm = lastLogOdo,
            useMetricUnits = uiState.useMetricUnits,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { viewModel.dismissAddFuelDialog() },
            onConfirm = { odo, litres, cost, full, notes, timestamp, syncOdo ->
                viewModel.addFuelLog(odo, litres, cost, full, notes, timestamp, syncOdo)
            }
        )
    }

    uiState.fuelLogToEdit?.let { log ->
        val logIndex = uiState.fuelLogs.indexOfFirst { it.id == log.id }
        val lastLogOdo = if (logIndex in 0 until uiState.fuelLogs.size - 1) {
            uiState.fuelLogs[logIndex + 1].odometerKm
        } else null

        AddEditFuelLogDialog(
            log = log,
            defaultOdometerKm = log.odometerKm,
            currentBikeOdometerKm = uiState.currentOdometerKm,
            lastLogOdometerKm = lastLogOdo,
            useMetricUnits = uiState.useMetricUnits,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { viewModel.dismissEditFuelDialog() },
            onConfirm = { odo, litres, cost, full, notes, timestamp, syncOdo ->
                viewModel.updateFuelLog(
                    log.copy(
                        odometerKm = odo,
                        litres = litres,
                        totalCost = cost,
                        isFullTank = full,
                        notes = notes,
                        timestampEpochMs = timestamp
                    ),
                    syncOdometer = syncOdo
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

    if (uiState.isResetMileageDialogOpen && bike != null) {
        ResetMileageDialog(
            currentOdoKm = uiState.currentOdometerKm,
            useMetricUnits = uiState.useMetricUnits,
            onDismiss = { viewModel.dismissResetMileageDialog() },
            onConfirm = { clearLogs ->
                viewModel.confirmResetMileage(clearLogs)
            }
        )
    }
}

@Composable
private fun OdometerHeaderCard(
    currentOdometerKm: Double,
    makeModel: String,
    registrationNumber: String = "",
    ridesCount: Int,
    ridesDistanceKm: Double,
    unloggedDistanceKm: Double = 0.0,
    useMetricUnits: Boolean,
    isCurrentBike: Boolean = false,
    onToggleActive: () -> Unit = {},
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = if (makeModel.isNotBlank()) makeModel else "MotoLog Registered",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (registrationNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        RegistrationPlateBadge(registrationNumber = registrationNumber)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                val userRidesDist = UnitConverter.kmToUserDistance(ridesDistanceKm, useMetricUnits)
                val userUnloggedDist = UnitConverter.kmToUserDistance(unloggedDistanceKm, useMetricUnits)
                val unitStr = UnitConverter.distanceUnit(useMetricUnits)
                val ridesSummary = if (userUnloggedDist > 0.05) {
                    "$ridesCount rides (${String.format(Locale.US, "%.1f", userRidesDist)} $unitStr logged • +${String.format(Locale.US, "%.1f", userUnloggedDist)} $unitStr unlogged)"
                } else {
                    "$ridesCount rides (${String.format(Locale.US, "%.1f", userRidesDist)} $unitStr in app)"
                }
                Text(
                    text = ridesSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isCurrentBike) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable(onClick = onToggleActive)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE BIKE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove as active bike",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
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
    onResetMileage: () -> Unit = {},
    onClearReset: () -> Unit = {},
    onDeleteMileageCycle: (com.abrar.motolog.data.local.entity.MileageCycleEntity) -> Unit = {},
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
                fuelUnit = uiState.fuelUnit,
                currencySymbol = uiState.currencySymbol,
                useMetricUnits = uiState.useMetricUnits,
                currentOdometerKm = uiState.currentOdometerKm,
                latestFuelLog = uiState.bike?.mileageResetEpochMs?.let { resetEpoch ->
                    uiState.fuelLogs.firstOrNull { it.timestampEpochMs >= resetEpoch }
                } ?: uiState.fuelLogs.firstOrNull(),
                mileageResetOdometerKm = uiState.bike?.mileageResetOdometerKm,
                onResetMileage = onResetMileage,
                onClearReset = onClearReset
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
                    subtitle = "Log fuel fills with odometer readings to calculate distance/litre and distance/cost (${uiState.fuelUnit.unitLabel})."
                )
            }
        } else {
            items(uiState.fuelLogs, key = { it.id }) { log ->
                FuelLogCard(
                    log = log,
                    useMetricUnits = uiState.useMetricUnits,
                    currencySymbol = uiState.currencySymbol,
                    onEdit = { onEditLog(log) },
                    onDelete = { onDeleteLog(log) }
                )
            }
        }

        if (uiState.mileageCycles.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Previous Mileage Cycles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(uiState.mileageCycles, key = { "cycle_${it.id}" }) { cycle ->
                MileageCycleCard(
                    cycle = cycle,
                    fuelUnit = uiState.fuelUnit,
                    currencySymbol = uiState.currencySymbol,
                    useMetricUnits = uiState.useMetricUnits,
                    onDelete = { onDeleteMileageCycle(cycle) }
                )
            }
        }
    }
}

@Composable
private fun FuelSummaryCard(
    stats: com.abrar.motolog.shared.domain.model.FuelMileageStats,
    fuelUnit: com.abrar.motolog.shared.domain.model.FuelUnit,
    currencySymbol: String = "$",
    useMetricUnits: Boolean = true,
    currentOdometerKm: Double = 0.0,
    latestFuelLog: FuelLogEntity? = null,
    mileageResetOdometerKm: Double? = null,
    onResetMileage: () -> Unit = {},
    onClearReset: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FUEL MILEAGE & EFFICIENCY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mileageResetOdometerKm != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                val resetUser = UnitConverter.kmToUserDistance(mileageResetOdometerKm, useMetricUnits)
                                val unitStr = UnitConverter.distanceUnit(useMetricUnits)
                                Text(
                                    text = "Reset @ ${String.format(Locale.US, "%.0f", resetUser)} $unitStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = onClearReset,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Reset",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    OutlinedButton(
                        onClick = onResetMileage,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Mileage",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Fuel Summary Stats: AVG MILEAGE, TOTAL FUEL, TOTAL SPENT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AVG MILEAGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val avg = stats.averageMileageKml
                    Text(
                        text = if (avg != null && avg > 0.0) {
                            UnitConverter.formatFuelEconomy(avg, fuelUnit)
                        } else if (mileageResetOdometerKm != null) {
                            "0.0 ${fuelUnit.unitLabel}"
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (mileageResetOdometerKm != null && currentOdometerKm >= mileageResetOdometerKm) {
                        val trackedKm = currentOdometerKm - mileageResetOdometerKm
                        val trackedUser = UnitConverter.kmToUserDistance(trackedKm, useMetricUnits)
                        val distUnit = UnitConverter.distanceUnit(useMetricUnits)
                        Text(
                            text = "(${String.format(Locale.US, "%.1f", trackedUser)} $distUnit tracked)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column {
                    Text(
                        text = "TOTAL FUEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f L", stats.totalLitres),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "TOTAL SPENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (stats.totalCost > 0.0) {
                            "$currencySymbol${String.format(Locale.US, "%.1f", stats.totalCost)}"
                        } else {
                            "${currencySymbol}0.0"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val distUnit = UnitConverter.distanceUnit(useMetricUnits)
            if (stats.measuredIntervalsCount == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                if (latestFuelLog != null && currentOdometerKm > latestFuelLog.odometerKm) {
                    val distSoFarKm = currentOdometerKm - latestFuelLog.odometerKm
                    val distSoFarUser = UnitConverter.kmToUserDistance(distSoFarKm, useMetricUnits)
                    val lastOdoUser = UnitConverter.kmToUserDistance(latestFuelLog.odometerKm, useMetricUnits)
                    Text(
                        text = "1 fill-up logged at ${String.format(Locale.US, "%,.1f", lastOdoUser)} $distUnit • +${String.format(Locale.US, "%,.1f", distSoFarUser)} $distUnit ridden so far on this tank (includes unlogged/calibrated riding). Log your next fill to compute mileage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Log at least 2 fuel fills with odometer readings to compute mileage (${fuelUnit.unitLabel}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (latestFuelLog != null && currentOdometerKm > latestFuelLog.odometerKm) {
                val distSoFarKm = currentOdometerKm - latestFuelLog.odometerKm
                val distSoFarUser = UnitConverter.kmToUserDistance(distSoFarKm, useMetricUnits)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Current tank: +${String.format(Locale.US, "%,.1f", distSoFarUser)} $distUnit ridden since last fill (includes unlogged/calibrated distance).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FuelLogCard(
    log: FuelLogEntity,
    useMetricUnits: Boolean,
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", log.litres)} L  •  Cost: $currencySymbol${String.format(Locale.US, "%.2f", log.totalCost)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
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
private fun ResetMileageDialog(
    currentOdoKm: Double,
    useMetricUnits: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (clearLogs: Boolean) -> Unit
) {
    var clearLogs by rememberSaveable { mutableStateOf(false) }
    val userOdo = UnitConverter.kmToUserDistance(currentOdoKm, useMetricUnits)
    val distUnit = UnitConverter.distanceUnit(useMetricUnits)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
        title = { Text("Reset Mileage Period") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This will archive your current period stats into 'Previous Mileage Cycles' and reset active fuel tracking and odometer to zero (0.0 km/l, 0.0 L, 0.0 $distUnit).\n\nFuture rides and fuel fills will calculate fresh from 0.0 $distUnit.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { clearLogs = !clearLogs }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = clearLogs,
                        onCheckedChange = { clearLogs = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Also delete past fuel fills from history",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(clearLogs) }
            ) {
                Text("Reset")
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
private fun MileageCycleCard(
    cycle: com.abrar.motolog.data.local.entity.MileageCycleEntity,
    fuelUnit: com.abrar.motolog.shared.domain.model.FuelUnit,
    currencySymbol: String,
    useMetricUnits: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val startOdo = UnitConverter.kmToUserDistance(cycle.startOdometerKm, useMetricUnits)
                    val endOdo = UnitConverter.kmToUserDistance(cycle.endOdometerKm, useMetricUnits)
                    val distUnit = UnitConverter.distanceUnit(useMetricUnits)
                    val dist = UnitConverter.kmToUserDistance(cycle.distanceKm, useMetricUnits)
                    Text(
                        text = "Archived Cycle • ${FormatUtils.formatDate(cycle.resetTimestampEpochMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${String.format(Locale.US, "%,.1f", startOdo)} → ${String.format(Locale.US, "%,.1f", endOdo)} $distUnit (+${String.format(Locale.US, "%,.1f", dist)} $distUnit)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Cycle Record",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AVG MILEAGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (cycle.averageMileageKml != null && cycle.averageMileageKml > 0.0) {
                            UnitConverter.formatFuelEconomy(cycle.averageMileageKml, fuelUnit)
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "TOTAL FUEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f L", cycle.totalLitres),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "TOTAL SPENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (cycle.totalCost > 0.0) {
                            "$currencySymbol${String.format(Locale.US, "%.1f", cycle.totalCost)}"
                        } else {
                            "${currencySymbol}0.0"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                    text = "Enter the current physical dashboard odometer reading of your motorcycle. MotoLog will adjust its calibration offset accordingly, ensuring unlogged rides are included in your odometer and fuel calculations.",
                    style = MaterialTheme.typography.bodySmall
                )
                val distUnit = UnitConverter.distanceUnit(useMetricUnits)
                Text(
                    text = "Current MotoLog reading: ${String.format(Locale.US, "%,.1f", displayOdo)} $distUnit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
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
                        val odoKm = UnitConverter.userDistanceToKm(newOdo, useMetricUnits)
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
    initialRegistrationNumber: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, makeModel: String, registrationNumber: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var makeModel by rememberSaveable { mutableStateOf(initialMakeModel) }
    var registrationNumber by rememberSaveable { mutableStateOf(initialRegistrationNumber) }
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Motorcycle") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Catalog Search & Brand browser
                MotorcycleCatalogSelector(
                    onSelectMotorcycle = { _, model, fullName ->
                        makeModel = fullName
                        if (name.isBlank() || name == initialMakeModel) {
                            name = fullName
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "BIKE CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

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

                Column {
                    OutlinedTextField(
                        value = registrationNumber,
                        onValueChange = { registrationNumber = it.uppercase() },
                        label = { Text("Registration / Plate (optional)") },
                        placeholder = { Text("e.g. KA-01-AB-1234") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (registrationNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Plate preview: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            RegistrationPlateBadge(registrationNumber = registrationNumber)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(name, makeModel, registrationNumber) },
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
                        val odoKm = UnitConverter.userDistanceToKm(odo, useMetricUnits)
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

private enum class FuelInputMode {
    TRIP_DISTANCE,
    ODOMETER
}

@Composable
private fun AddEditFuelLogDialog(
    log: FuelLogEntity?,
    defaultOdometerKm: Double,
    currentBikeOdometerKm: Double,
    lastLogOdometerKm: Double?,
    useMetricUnits: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (odometerKm: Double, litres: Double, totalCost: Double, isFullTank: Boolean, notes: String, timestamp: Long, syncOdometer: Boolean) -> Unit
) {
    val distUnit = UnitConverter.distanceUnit(useMetricUnits)
    val prevLogOdo = lastLogOdometerKm

    // Determine initial values
    val initialOdoKm = log?.odometerKm ?: defaultOdometerKm
    val initialTripKm = if (prevLogOdo != null) {
        (initialOdoKm - prevLogOdo).coerceAtLeast(0.0)
    } else 0.0

    val initialDisplayOdo = UnitConverter.kmToUserDistance(initialOdoKm, useMetricUnits)
    val initialDisplayTrip = UnitConverter.kmToUserDistance(initialTripKm, useMetricUnits)

    var inputMode by rememberSaveable {
        mutableStateOf(
            if (log == null && prevLogOdo != null) FuelInputMode.TRIP_DISTANCE else FuelInputMode.ODOMETER
        )
    }

    var odoText by rememberSaveable { mutableStateOf(String.format(Locale.US, "%.1f", initialDisplayOdo)) }
    var tripText by rememberSaveable { mutableStateOf(String.format(Locale.US, "%.1f", initialDisplayTrip)) }
    var litresText by rememberSaveable { mutableStateOf(log?.litres?.toString().orEmpty()) }
    var costText by rememberSaveable { mutableStateOf(log?.totalCost?.toString().orEmpty()) }
    var isFullTank by rememberSaveable { mutableStateOf(log?.isFullTank ?: false) }
    var notes by rememberSaveable { mutableStateOf(log?.notes.orEmpty()) }

    // Parse values
    val enteredOdoUser = odoText.toDoubleOrNull()
    val enteredTripUser = tripText.toDoubleOrNull()
    val litres = litresText.toDoubleOrNull()
    val cost = costText.toDoubleOrNull()

    val effectiveOdoKm: Double? = if (prevLogOdo != null && inputMode == FuelInputMode.TRIP_DISTANCE) {
        if (enteredTripUser != null && enteredTripUser >= 0.0) {
            val tripKm = UnitConverter.userDistanceToKm(enteredTripUser, useMetricUnits)
            prevLogOdo + tripKm
        } else null
    } else {
        if (enteredOdoUser != null && enteredOdoUser >= 0.0) {
            UnitConverter.userDistanceToKm(enteredOdoUser, useMetricUnits)
        } else null
    }

    val isValid = effectiveOdoKm != null && effectiveOdoKm >= 0.0 && litres != null && litres > 0.0 && cost != null && cost >= 0.0

    val canSyncOdometer = effectiveOdoKm != null && effectiveOdoKm > currentBikeOdometerKm
    var syncOdometer by rememberSaveable(canSyncOdometer) { mutableStateOf(canSyncOdometer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (log == null) "Add Fuel Fill-Up" else "Edit Fuel Fill-Up") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (prevLogOdo != null) {
                    // Segmented toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (inputMode != FuelInputMode.TRIP_DISTANCE) {
                                    val currentOdo = odoText.toDoubleOrNull()
                                    if (currentOdo != null) {
                                        val odoKm = UnitConverter.userDistanceToKm(currentOdo, useMetricUnits)
                                        val deltaKm = (odoKm - lastLogOdometerKm).coerceAtLeast(0.0)
                                        tripText = String.format(Locale.US, "%.1f", UnitConverter.kmToUserDistance(deltaKm, useMetricUnits))
                                    }
                                    inputMode = FuelInputMode.TRIP_DISTANCE
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (inputMode == FuelInputMode.TRIP_DISTANCE) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Trip Meter",
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (inputMode == FuelInputMode.TRIP_DISTANCE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            onClick = {
                                if (inputMode != FuelInputMode.ODOMETER) {
                                    val trip = tripText.toDoubleOrNull()
                                    if (trip != null) {
                                        val tripKm = UnitConverter.userDistanceToKm(trip, useMetricUnits)
                                        val newOdoKm = lastLogOdometerKm + tripKm
                                        odoText = String.format(Locale.US, "%.1f", UnitConverter.kmToUserDistance(newOdoKm, useMetricUnits))
                                    }
                                    inputMode = FuelInputMode.ODOMETER
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (inputMode == FuelInputMode.ODOMETER) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Total Dash",
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (inputMode == FuelInputMode.ODOMETER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (inputMode == FuelInputMode.TRIP_DISTANCE) {
                        OutlinedTextField(
                            value = tripText,
                            onValueChange = { tripText = it },
                            label = { Text("Trip Distance ($distUnit)") },
                            placeholder = { Text("e.g. from bike's Trip A meter") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (effectiveOdoKm != null) {
                            val computedOdoUser = UnitConverter.kmToUserDistance(effectiveOdoKm, useMetricUnits)
                            val lastLogUser = UnitConverter.kmToUserDistance(lastLogOdometerKm, useMetricUnits)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Resulting Odometer: ${String.format(Locale.US, "%,.1f", computedOdoUser)} $distUnit",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "From last fill (${String.format(Locale.US, "%,.1f", lastLogUser)} $distUnit) + ${String.format(Locale.US, "%,.1f", enteredTripUser ?: 0.0)} $distUnit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = odoText,
                            onValueChange = { odoText = it },
                            label = { Text("Dashboard Odometer ($distUnit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (enteredOdoUser != null) {
                            val odoKm = UnitConverter.userDistanceToKm(enteredOdoUser, useMetricUnits)
                            val intervalKm = odoKm - lastLogOdometerKm
                            val intervalUser = UnitConverter.kmToUserDistance(intervalKm, useMetricUnits)
                            if (intervalKm > 0.0) {
                                Text(
                                    text = "Distance since last fill: +${String.format(Locale.US, "%.1f", intervalUser)} $distUnit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "Note: Reading is less than or equal to last fill (${String.format(Locale.US, "%,.1f", UnitConverter.kmToUserDistance(lastLogOdometerKm, useMetricUnits))} $distUnit)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = odoText,
                        onValueChange = { odoText = it },
                        label = { Text("Odometer ($distUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "First fill-up: Enter your motorcycle's dashboard odometer to begin tracking fuel economy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (canSyncOdometer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { syncOdometer = !syncOdometer }
                    ) {
                        Checkbox(checked = syncOdometer, onCheckedChange = { syncOdometer = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Sync motorcycle odometer",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val currentOdoUser = UnitConverter.kmToUserDistance(currentBikeOdometerKm, useMetricUnits)
                            val targetOdoUser = UnitConverter.kmToUserDistance(effectiveOdoKm, useMetricUnits)
                            Text(
                                text = "Updates dash from ${String.format(Locale.US, "%,.1f", currentOdoUser)} to ${String.format(Locale.US, "%,.1f", targetOdoUser)} $distUnit to account for unlogged riding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

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
                    if (effectiveOdoKm != null && effectiveOdoKm >= 0.0 && litres != null && litres > 0.0 && cost != null && cost >= 0.0) {
                        onConfirm(
                            effectiveOdoKm,
                            litres,
                            cost,
                            isFullTank,
                            notes,
                            log?.timestampEpochMs ?: System.currentTimeMillis(),
                            syncOdometer
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
