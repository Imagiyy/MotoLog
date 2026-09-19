package com.abrar.motolog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity

/**
 * Room database for MotoLog.
 *
 * Schema is exported to the schemas/ directory for migration testing.
 * Version starts at 1 — increment and provide migrations for any
 * schema changes in later stages.
 */
@Database(
    entities = [
        BikeEntity::class,
        RideEntity::class,
        RidePointEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MotoLogDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
    abstract fun rideDao(): RideDao
    abstract fun ridePointDao(): RidePointDao
}
