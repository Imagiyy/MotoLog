package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single fuel fill-up record for a motorcycle.
 */
@Entity(
    tableName = "fuel_logs",
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
data class FuelLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The bike this fuel fill belongs to */
    val bikeId: Long,

    /** Epoch millis timestamp when fuel was purchased */
    val timestampEpochMs: Long = System.currentTimeMillis(),

    /** Bike odometer reading in kilometers at the time of fill */
    val odometerKm: Double,

    /** Volume of fuel added in litres */
    val litres: Double,

    /** Total monetary cost of this fill (currency agnostic plain number) */
    val totalCost: Double,

    /** True if the tank was filled completely to full */
    val isFullTank: Boolean = true,

    /** Optional notes (e.g. brand, octane rating, station) */
    val notes: String = ""
)
