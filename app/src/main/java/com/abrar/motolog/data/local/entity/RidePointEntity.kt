package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single GPS location point recorded during a ride.
 * Points are written continuously to the database in batches
 * during an active ride.
 */
@Entity(
    tableName = "ride_points",
    foreignKeys = [
        ForeignKey(
            entity = RideEntity::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rideId")]
)
data class RidePointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The ride this point belongs to */
    val rideId: Long,

    /** Epoch millis when this point was recorded */
    val timestamp: Long,

    /** Latitude in degrees */
    val latitude: Double,

    /** Longitude in degrees */
    val longitude: Double,

    /** Speed at this point in m/s (from GPS or calculated) */
    val speedMs: Double = 0.0,

    /** GPS accuracy in meters */
    val accuracyMeters: Float = 0f,

    /** Altitude in meters (if available, 0 otherwise) */
    val altitudeMeters: Double = 0.0,

    /** True if the ride was paused (manually or auto) at this point */
    val isPaused: Boolean = false,

    /** True if this point marks a GPS signal gap */
    val isGap: Boolean = false
)
