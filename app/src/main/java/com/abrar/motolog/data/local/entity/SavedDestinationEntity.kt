package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for saved, bookmarked, or recent destinations.
 */
@Entity(tableName = "saved_destinations")
data class SavedDestinationEntity(
    @PrimaryKey
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val providerPlaceId: String? = null,
    val sourceType: String, // DestinationSourceType.name
    val createdAtTimestamp: Long,
    val isFavorite: Boolean = false
)
