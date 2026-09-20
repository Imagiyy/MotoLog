package com.abrar.motolog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.FuelLogDao
import com.abrar.motolog.data.local.dao.MaintenanceDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
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
        RidePointEntity::class,
        MaintenanceItemEntity::class,
        FuelLogEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MotoLogDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
    abstract fun rideDao(): RideDao
    abstract fun ridePointDao(): RidePointDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun fuelLogDao(): FuelLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to bikes
                db.execSQL("ALTER TABLE `bikes` ADD COLUMN `initialOdometerKm` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `bikes` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")

                // Create maintenance_items table and index
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `maintenance_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bikeId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `intervalKm` REAL,
                        `intervalDays` INTEGER,
                        `lastDoneOdometerKm` REAL NOT NULL,
                        `lastDoneDateEpochMs` INTEGER NOT NULL,
                        `lastNotifiedDueEpochMs` INTEGER,
                        FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_items_bikeId` ON `maintenance_items` (`bikeId`)")

                // Create fuel_logs table and index
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fuel_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bikeId` INTEGER NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        `odometerKm` REAL NOT NULL,
                        `litres` REAL NOT NULL,
                        `totalCost` REAL NOT NULL,
                        `isFullTank` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_logs_bikeId` ON `fuel_logs` (`bikeId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add elevation tracking columns to rides
                db.execSQL("ALTER TABLE `rides` ADD COLUMN `elevationLossMeters` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `rides` ADD COLUMN `elevationSource` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
