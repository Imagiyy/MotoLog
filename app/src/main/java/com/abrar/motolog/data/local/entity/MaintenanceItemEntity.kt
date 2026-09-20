package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a maintenance or service schedule item for a specific motorcycle.
 */
@Entity(
    tableName = "maintenance_items",
    foreignKeys = [
        ForeignKey(
            entity = BikeEntity::class,
            parentColumns = ["id"],
            childColumns = ["bikeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bikeId")]
)
data class MaintenanceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The bike this maintenance task belongs to */
    val bikeId: Long,

    /** Name of the maintenance task (e.g., "Engine Oil & Filter") */
    val name: String,

    /** Distance interval in kilometers between services (null if time-only) */
    val intervalKm: Double? = null,

    /** Time interval in days between services (null if distance-only) */
    val intervalDays: Int? = null,

    /** Bike odometer reading in kilometers when this service was last performed */
    val lastDoneOdometerKm: Double = 0.0,

    /** Epoch millis timestamp when this service was last performed */
    val lastDoneDateEpochMs: Long = System.currentTimeMillis(),

    /** Epoch millis when a due notification was last sent for this item (avoids spam) */
    val lastNotifiedDueEpochMs: Long? = null
)
