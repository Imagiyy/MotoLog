package com.abrar.motolog.data.local

import androidx.room.TypeConverter
import com.abrar.motolog.data.local.entity.RideStatus

/**
 * Room type converters for enum types.
 */
class Converters {

    @TypeConverter
    fun fromRideStatus(status: RideStatus): String = status.name

    @TypeConverter
    fun toRideStatus(value: String): RideStatus = RideStatus.valueOf(value)
}
