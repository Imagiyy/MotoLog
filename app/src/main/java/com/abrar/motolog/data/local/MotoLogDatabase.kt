package com.abrar.motolog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.FuelLogDao
import com.abrar.motolog.data.local.dao.MaintenanceDao
import com.abrar.motolog.data.local.dao.MileageCycleDao
import com.abrar.motolog.data.local.dao.NavigationSessionDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.data.local.dao.RidePointDao
import com.abrar.motolog.data.local.dao.SavedDestinationDao
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.data.local.entity.MileageCycleEntity
import com.abrar.motolog.data.local.entity.NavigationSessionEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.SavedDestinationEntity

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
        FuelLogEntity::class,
        MileageCycleEntity::class,
        SavedDestinationEntity::class,
        NavigationSessionEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MotoLogDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
    abstract fun rideDao(): RideDao
    abstract fun ridePointDao(): RidePointDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun mileageCycleDao(): MileageCycleDao
    abstract fun savedDestinationDao(): SavedDestinationDao
    abstract fun navigationSessionDao(): NavigationSessionDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add import tracking and odometer attribution columns to rides
                db.execSQL("ALTER TABLE `rides` ADD COLUMN `isImported` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `rides` ADD COLUMN `countsTowardOdometer` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add composite index for fast ride point retrieval and export
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ride_points_rideId_timestamp` ON `ride_points` (`rideId`, `timestamp`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add vehicle registration number column to bikes
                db.execSQL("ALTER TABLE `bikes` ADD COLUMN `registrationNumber` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add mileage reset tracking columns to bikes
                db.execSQL("ALTER TABLE `bikes` ADD COLUMN `mileageResetOdometerKm` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `bikes` ADD COLUMN `mileageResetEpochMs` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mileage_cycles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bikeId` INTEGER NOT NULL,
                        `resetTimestampEpochMs` INTEGER NOT NULL,
                        `startOdometerKm` REAL NOT NULL,
                        `endOdometerKm` REAL NOT NULL,
                        `distanceKm` REAL NOT NULL,
                        `totalLitres` REAL NOT NULL,
                        `totalCost` REAL NOT NULL,
                        `averageMileageKml` REAL,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`bikeId`) REFERENCES `bikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mileage_cycles_bikeId` ON `mileage_cycles` (`bikeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mileage_cycles_resetTimestampEpochMs` ON `mileage_cycles` (`resetTimestampEpochMs`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_destinations` (
                        `id` TEXT PRIMARY KEY NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `providerPlaceId` TEXT,
                        `sourceType` TEXT NOT NULL,
                        `createdAtTimestamp` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `navigation_sessions` (
                        `sessionId` TEXT PRIMARY KEY NOT NULL,
                        `plannedRouteId` TEXT NOT NULL,
                        `rideId` INTEGER,
                        `status` TEXT NOT NULL,
                        `startedAtTimestamp` INTEGER NOT NULL,
                        `endedAtTimestamp` INTEGER,
                        `lastKnownProgressJson` TEXT,
                        `routeSnapshotJson` TEXT,
                        `rerouteCount` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_navigation_sessions_rideId` ON `navigation_sessions` (`rideId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_navigation_sessions_startedAtTimestamp` ON `navigation_sessions` (`startedAtTimestamp`)")
            }
        }
    }
}

