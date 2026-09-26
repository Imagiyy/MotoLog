package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import kotlinx.serialization.Serializable

/**
 * Represents a motorcycle/bike in the garage.
 * Each ride can optionally be associated with a bike.
 */
@Serializable
@Entity(tableName = "bikes")
data class BikeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** User-assigned display name (e.g., "My Honda CB300R") */
    val name: String,

    /** Make and model (e.g., "Honda CB300R") */
    val makeModel: String = "",

    /** Initial odometer reading when bike was registered */
    val initialOdometerKm: Double = 0.0,

    /** Odometer offset in kilometers — used to sync with the
     *  bike's actual odometer when calibrated. */
    val odometerOffsetKm: Double = 0.0,

    /** Timestamp when this bike was added */
    val createdAt: Long = System.currentTimeMillis(),

    /** True if the bike is archived (hidden from active selection, preserved for history) */
    val isArchived: Boolean = false,

    /** Vehicle registration / license plate number (e.g., "KA-01-AB-1234") */
    val registrationNumber: String = ""
)
