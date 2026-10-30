package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Persisted record of a completed fuel mileage period/cycle archived upon user reset.
 * Stores previous odometer span, total fuel, total cost, and computed average mileage.
 */
@Serializable
@Entity(
    tableName = "mileage_cycles",
    foreignKeys = [
        ForeignKey(
            entity = BikeEntity::class,
            parentColumns = ["id"],
            childColumns = ["bikeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bikeId"),
        Index("resetTimestampEpochMs")
    ]
)
data class MileageCycleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bikeId: Long,
    val resetTimestampEpochMs: Long,
    val startOdometerKm: Double,
    val endOdometerKm: Double,
    val distanceKm: Double,
    val totalLitres: Double,
    val totalCost: Double,
    val averageMileageKml: Double?,
    val notes: String = ""
)
