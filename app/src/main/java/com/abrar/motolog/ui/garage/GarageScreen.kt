package com.abrar.motolog.ui.garage

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.domain.engine.UnitConverter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    onNavigateToBikeDetail: (Long) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: GarageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Garage", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddBikeDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Motorcycle")
            }
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
                uiState.bikes.isEmpty() -> {
                    EmptyGarageView(
                        onAddBike = { viewModel.openAddBikeDialog() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.bikes, key = { it.bike.id }) { item ->
                            BikeCard(
                                item = item,
                                useMetricUnits = uiState.useMetricUnits,
                                onClick = { onNavigateToBikeDetail(item.bike.id) },
                                onSelectAsActive = { viewModel.selectCurrentBike(item.bike.id) },
                                onDeleteOrArchive = { viewModel.requestArchiveOrDelete(item.bike) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddBikeDialogOpen) {
        AddBikeDialog(
            useMetricUnits = uiState.useMetricUnits,
            onDismiss = { viewModel.dismissAddBikeDialog() },
            onConfirm = { name, makeModel, initialOdo ->
                viewModel.addBike(name, makeModel, initialOdo)
            }
        )
    }

    uiState.bikeToArchiveOrDelete?.let { bike ->
        ArchiveOrDeleteDialog(
            bike = bike,
            hasRides = uiState.bikeHasRides,
            onDismiss = { viewModel.dismissArchiveOrDeleteDialog() },
            onConfirm = { viewModel.confirmArchiveOrDelete() }
        )
    }
}

@Composable
private fun BikeCard(
    item: BikeListItem,
    useMetricUnits: Boolean,
    onClick: () -> Unit,
    onSelectAsActive: () -> Unit,
    onDeleteOrArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCurrentBike) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        tint = if (item.isCurrentBike) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.bike.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.bike.makeModel.isNotBlank()) {
                            Text(
                                text = item.bike.makeModel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (!item.isCurrentBike) {
                            DropdownMenuItem(
                                text = { Text("Set as Active Bike") },
                                onClick = {
                                    menuExpanded = false
                                    onSelectAsActive()
                                },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Archive / Delete") },
                            onClick = {
                                menuExpanded = false
                                onDeleteOrArchive()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "ODOMETER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val userOdo = UnitConverter.kmToUserDistance(item.currentOdometerKm, useMetricUnits)
                    val unitStr = UnitConverter.distanceUnit(useMetricUnits)
                    Text(
                        text = String.format(Locale.US, "%,.1f %s", userOdo, unitStr),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (item.isCurrentBike) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ACTIVE BIKE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Maintenance badge summary
            MaintenanceSummaryBadge(item = item)
        }
    }
}

@Composable
private fun MaintenanceSummaryBadge(item: BikeListItem) {
    val overdue = item.overdueCount
    val dueSoon = item.dueSoonCount

    val (bg, fg, label, icon) = when {
        overdue > 0 -> Quadruple(
            Color(0x33EF5350),
            Color(0xFFEF5350),
            "$overdue Service Overdue",
            Icons.Default.Warning
        )
        dueSoon > 0 -> Quadruple(
            Color(0x33FFA726),
            Color(0xFFFFA726),
            "$dueSoon Service Due Soon",
            Icons.Default.Build
        )
        item.maintenanceEvaluations.isNotEmpty() -> Quadruple(
            Color(0x3366BB6A),
            Color(0xFF66BB6A),
            "All Services OK",
            Icons.Default.CheckCircle
        )
        else -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "No service schedule configured",
            Icons.Default.Build
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = fg,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun EmptyGarageView(
    onAddBike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.TwoWheeler,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your Garage is Empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your motorcycle to track odometer, service maintenance reminders, and fuel mileage.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onAddBike) {
            Text("Add Motorcycle")
        }
    }
}

@Composable
private fun AddBikeDialog(
    useMetricUnits: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, makeModel: String, initialOdometerKm: Double) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var makeModel by rememberSaveable { mutableStateOf("") }
    var initialOdoText by rememberSaveable { mutableStateOf("0") }
    val isNameValid = name.isNotBlank()
    val initialOdo = initialOdoText.toDoubleOrNull()
    val isOdoValid = initialOdo != null && initialOdo >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Motorcycle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bike Name (e.g. Daily Commuter)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = makeModel,
                    onValueChange = { makeModel = it },
                    label = { Text("Make & Model (e.g. Honda CB300R)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val unitLabel = UnitConverter.distanceUnit(useMetricUnits)
                OutlinedTextField(
                    value = initialOdoText,
                    onValueChange = { initialOdoText = it },
                    label = { Text("Current Odometer Reading ($unitLabel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isNameValid && isOdoValid) {
                        val odoKm = UnitConverter.userDistanceToKm(initialOdo!!, useMetricUnits)
                        onConfirm(name, makeModel, odoKm)
                    }
                },
                enabled = isNameValid && isOdoValid
            ) {
                Text("Add")
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
private fun ArchiveOrDeleteDialog(
    bike: BikeEntity,
    hasRides: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (hasRides) "Archive Motorcycle" else "Delete Motorcycle")
        },
        text = {
            Text(
                if (hasRides) {
                    "${bike.name} has recorded rides in MotoLog and cannot be permanently deleted. Archiving will hide it from the active garage and selection, while preserving all existing ride history."
                } else {
                    "Are you sure you want to delete ${bike.name}? This motorcycle has no recorded rides."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (hasRides) "Archive" else "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
