package com.abrar.motolog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import kotlinx.serialization.Serializable

/**
 * Ride status indicating the lifecycle state of a ride.
 */
@Serializable
enum class RideStatus {
    /** Currently recording */
    ACTIVE,
    /** Finished normally via Stop */
    COMPLETED,
    /** Recovered after an app crash or force-kill */
    RECOVERED
}

/**
 * Represents a single ride session.
 */
@Serializable
@Entity(
    tableName = "rides",
    foreignKeys = [
        ForeignKey(
            entity = BikeEntity::class,
            parentColumns = ["id"],
            childColumns = ["bikeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("bikeId")]
)
data class RideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Optional association with a bike. Null if no bike selected. */
    val bikeId: Long? = null,

    /** User-assigned ride name (e.g., "Morning Commute").
     *  Defaults to a timestamp-based name. */
    val name: String = "",

    /** Epoch millis when the ride started */
    val startTime: Long = 0L,

    /** Epoch millis when the ride ended. 0 if still active. */
    val endTime: Long = 0L,

    /** Total distance traveled in meters */
    val distanceMeters: Double = 0.0,

    /** Total elapsed time in milliseconds (start to stop) */
    val elapsedTimeMs: Long = 0L,

    /** Time spent moving in milliseconds (excludes paused/stopped time) */
    val movingTimeMs: Long = 0L,

    /** Average speed while moving in m/s (distance / moving time) */
    val avgMovingSpeedMs: Double = 0.0,

    /** Overall average speed in m/s (distance / elapsed time) */
    val overallAvgSpeedMs: Double = 0.0,

    /** Maximum speed recorded during the ride in m/s */
    val maxSpeedMs: Double = 0.0,

    /** Total elevation gain in meters (when available) */
    val elevationGainMeters: Double = 0.0,

    /** Total elevation loss in meters (when available) */
    val elevationLossMeters: Double = 0.0,

    /** Source of elevation data: "barometer", "gps", or "" (none) */
    val elevationSource: String = "",

    /** Current ride lifecycle status */
    val status: RideStatus = RideStatus.ACTIVE,

    /** Whether this ride was imported from an external GPX file */
    val isImported: Boolean = false,

    /** Whether an imported ride contributes to the assigned bike's cumulative odometer */
    val countsTowardOdometer: Boolean = false
)
