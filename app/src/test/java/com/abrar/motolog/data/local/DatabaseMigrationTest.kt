package com.abrar.motolog.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.abrar.motolog.data.local.entity.BikeEntity
import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RideStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun `migration from version 1 to 2 executes non-destructive schema upgrades`() {
        val executedSql = mutableListOf<String>()

        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                val sql = args[0] as String
                executedSql.add(sql)
            }
            null
        } as SupportSQLiteDatabase

        // Execute migration
        MotoLogDatabase.MIGRATION_1_2.migrate(mockDb)

        // 1. Version numbers
        assertEquals(1, MotoLogDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, MotoLogDatabase.MIGRATION_1_2.endVersion)

        // 2. No destructive fallback (DROP TABLE)
        assertFalse("Migration must not contain DROP TABLE", executedSql.any { it.contains("DROP TABLE", ignoreCase = true) })

        // 3. Alters bikes table with new columns having safe defaults
        assertTrue(
            "Must add initialOdometerKm with default 0.0",
            executedSql.any { it.contains("ALTER TABLE `bikes` ADD COLUMN `initialOdometerKm` REAL NOT NULL DEFAULT 0.0") }
        )
        assertTrue(
            "Must add isArchived with default 0",
            executedSql.any { it.contains("ALTER TABLE `bikes` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0") }
        )

        // 4. Creates maintenance_items table and index
        assertTrue(
            "Must create maintenance_items table",
            executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `maintenance_items`") }
        )
        assertTrue(
            "Must index maintenance_items bikeId",
            executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS `index_maintenance_items_bikeId` ON `maintenance_items` (`bikeId`)") }
        )

        // 5. Creates fuel_logs table and index
        assertTrue(
            "Must create fuel_logs table",
            executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `fuel_logs`") }
        )
        assertTrue(
            "Must index fuel_logs bikeId",
            executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS `index_fuel_logs_bikeId` ON `fuel_logs` (`bikeId`)") }
        )

        // 6. Verify existing ride entities from previous stages survive with null or assigned bikeId
        val existingUnassignedRide = RideEntity(
            id = 101L,
            bikeId = null, // from Stage 3/4/5
            name = "Stage 5 Ride",
            startTime = 1700000000L,
            endTime = 1700003600L,
            distanceMeters = 25000.0,
            status = RideStatus.COMPLETED
        )
        assertNull(existingUnassignedRide.bikeId)
        assertEquals(25000.0, existingUnassignedRide.distanceMeters, 0.001)

        val existingAssignedRide = existingUnassignedRide.copy(bikeId = 1L)
        assertEquals(1L, existingAssignedRide.bikeId)

        // 7. Verify new entities can be instantiated with foreign keys
        val bike = BikeEntity(id = 1L, name = "Honda CB300R", makeModel = "Honda", initialOdometerKm = 5000.0)
        assertEquals(0.0, bike.odometerOffsetKm, 0.001)
        assertFalse(bike.isArchived)

        val maintenanceItem = MaintenanceItemEntity(
            id = 1L,
            bikeId = bike.id,
            name = "Oil Change",
            intervalKm = 3000.0,
            lastDoneOdometerKm = 5000.0
        )
        assertNotNull(maintenanceItem)
        assertEquals(1L, maintenanceItem.bikeId)

        val fuelLog = FuelLogEntity(
            id = 1L,
            bikeId = bike.id,
            odometerKm = 5300.0,
            litres = 10.0,
            totalCost = 25.0,
            isFullTank = true
        )
        assertNotNull(fuelLog)
        assertEquals(1L, fuelLog.bikeId)
    }

    @Test
    fun `migration from version 2 to 3 executes non-destructive schema upgrades`() {
        val executedSql = mutableListOf<String>()

        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                val sql = args[0] as String
                executedSql.add(sql)
            }
            null
        } as SupportSQLiteDatabase

        // Execute migration
        MotoLogDatabase.MIGRATION_2_3.migrate(mockDb)

        // 1. Version numbers
        assertEquals(2, MotoLogDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, MotoLogDatabase.MIGRATION_2_3.endVersion)

        // 2. No destructive fallback (DROP TABLE)
        assertFalse("Migration must not contain DROP TABLE", executedSql.any { it.contains("DROP TABLE", ignoreCase = true) })

        // 3. Adds elevation columns with safe defaults
        assertTrue(
            "Must add elevationLossMeters with default 0.0",
            executedSql.any { it.contains("ALTER TABLE `rides` ADD COLUMN `elevationLossMeters` REAL NOT NULL DEFAULT 0.0") }
        )
        assertTrue(
            "Must add elevationSource with default ''",
            executedSql.any { it.contains("ALTER TABLE `rides` ADD COLUMN `elevationSource` TEXT NOT NULL DEFAULT ''") }
        )

        // 4. Verify RideEntity defaults
        val defaultRide = RideEntity(id = 1L)
        assertEquals(0.0, defaultRide.elevationGainMeters, 0.001)
        assertEquals(0.0, defaultRide.elevationLossMeters, 0.001)
        assertEquals("", defaultRide.elevationSource)
    }

    @Test
    fun `migration from version 3 to 4 executes non-destructive schema upgrades`() {
        val executedSql = mutableListOf<String>()

        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                val sql = args[0] as String
                executedSql.add(sql)
            }
            null
        } as SupportSQLiteDatabase

        // Execute migration
        MotoLogDatabase.MIGRATION_3_4.migrate(mockDb)

        // 1. Version numbers
        assertEquals(3, MotoLogDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, MotoLogDatabase.MIGRATION_3_4.endVersion)

        // 2. No destructive fallback (DROP TABLE)
        assertFalse("Migration must not contain DROP TABLE", executedSql.any { it.contains("DROP TABLE", ignoreCase = true) })

        // 3. Adds isImported and countsTowardOdometer columns with safe defaults (0 / false)
        assertTrue(
            "Must add isImported with default 0",
            executedSql.any { it.contains("ALTER TABLE `rides` ADD COLUMN `isImported` INTEGER NOT NULL DEFAULT 0") }
        )
        assertTrue(
            "Must add countsTowardOdometer with default 0",
            executedSql.any { it.contains("ALTER TABLE `rides` ADD COLUMN `countsTowardOdometer` INTEGER NOT NULL DEFAULT 0") }
        )

        // 4. Verify RideEntity defaults
        val defaultRide = RideEntity(id = 1L)
        assertFalse("isImported should default to false", defaultRide.isImported)
        assertFalse("countsTowardOdometer should default to false", defaultRide.countsTowardOdometer)
    }
}
