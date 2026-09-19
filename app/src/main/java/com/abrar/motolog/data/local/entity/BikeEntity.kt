package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a motorcycle/bike in the garage.
 * Each ride can optionally be associated with a bike.
 */
@Entity(tableName = "bikes")
data class BikeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** User-assigned display name (e.g., "My Honda CB300R") */
    val name: String,

    /** Make and model (e.g., "Honda CB300R") */
    val makeModel: String = "",

    /** Odometer offset in kilometers — used to sync with the
     *  bike's actual odometer when the app is installed mid-life. */
    val odometerOffsetKm: Double = 0.0,

    /** Timestamp when this bike was added */
    val createdAt: Long = System.currentTimeMillis()
)
