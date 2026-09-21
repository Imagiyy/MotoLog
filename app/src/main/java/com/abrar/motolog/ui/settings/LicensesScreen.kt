package com.abrar.motolog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val url: String,
    val description: String
)

val OPEN_SOURCE_LIBRARIES = listOf(
    OpenSourceLibrary(
        name = "AndroidX & Jetpack Compose",
        author = "The Android Open Source Project",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack",
        description = "Modern UI toolkit, Lifecycle, Navigation, and Core libraries."
    ),
    OpenSourceLibrary(
        name = "Kotlin & Coroutines",
        author = "JetBrains s.r.o. and Google LLC",
        license = "Apache License 2.0",
        url = "https://kotlinlang.org",
        description = "Kotlin programming language, standard library, and kotlinx coroutines/serialization."
    ),
    OpenSourceLibrary(
        name = "Google Dagger & Hilt",
        author = "Google LLC",
        license = "Apache License 2.0",
        url = "https://dagger.dev/hilt/",
        description = "Dependency injection framework for Android."
    ),
    OpenSourceLibrary(
        name = "AndroidX Room",
        author = "The Android Open Source Project",
        license = "Apache License 2.0",
        url = "https://developer.android.com/training/data-storage/room",
        description = "SQLite object mapping library providing robust on-device persistence."
    ),
    OpenSourceLibrary(
        name = "MapLibre Native & Compose",
        author = "MapLibre Contributors",
        license = "BSD 3-Clause / Apache License 2.0",
        url = "https://maplibre.org",
        description = "Open-source map rendering engine and Jetpack Compose bindings for vector tiles."
    ),
    OpenSourceLibrary(
        name = "OpenFreeMap & OpenMapTiles",
        author = "OpenFreeMap & OpenMapTiles Contributors",
        license = "Open Database License (ODbL) / CC-BY 4.0",
        url = "https://openfreemap.org",
        description = "Free vector map tiles and styles derived from OpenStreetMap data."
    ),
    OpenSourceLibrary(
        name = "OpenStreetMap",
        author = "OpenStreetMap Contributors",
        license = "Open Database License (ODbL)",
        url = "https://www.openstreetmap.org/copyright",
        description = "Open map data provided to the public under ODbL."
    ),
    OpenSourceLibrary(
        name = "Google Play Services Location",
        author = "Google LLC",
        license = "Android Software Development Kit License",
        url = "https://developers.google.com/android/guides/setup",
        description = "Fused location provider client for high-accuracy GPS readings."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licenses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MotoLog is built with gratitude to the following open-source projects:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(OPEN_SOURCE_LIBRARIES) { lib ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lib.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lib.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Author: ${lib.author}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "License: ${lib.license}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
