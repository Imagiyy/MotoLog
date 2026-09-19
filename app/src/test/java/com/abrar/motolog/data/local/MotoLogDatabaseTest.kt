package com.abrar.motolog.data.local

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Basic Room database validation test.
 * This test verifies that the database class and entities compile
 * correctly and the schema is valid.
 *
 * Note: Full Room database tests with insert/query operations require
 * an Android context (instrumented test or Robolectric). Those will
 * be added in later stages when ride tracking logic needs database
 * integration testing.
 */
class MotoLogDatabaseTest {

    @Test
    fun `database entities are defined`() {
        // Verify entity classes are accessible and constructable
        val bike = com.abrar.motolog.data.local.entity.BikeEntity(
            name = "Test Bike",
            makeModel = "Honda CB300R"
        )
        assertNotNull(bike)
        assert(bike.name == "Test Bike")
        assert(bike.makeModel == "Honda CB300R")
        assert(bike.odometerOffsetKm == 0.0)
    }

    @Test
    fun `ride entity has correct defaults`() {
        val ride = com.abrar.motolog.data.local.entity.RideEntity()
        assertNotNull(ride)
        assert(ride.status == com.abrar.motolog.data.local.entity.RideStatus.ACTIVE)
        assert(ride.distanceMeters == 0.0)
        assert(ride.endTime == 0L)
    }

    @Test
    fun `ride point entity is constructable`() {
        val point = com.abrar.motolog.data.local.entity.RidePointEntity(
            rideId = 1L,
            timestamp = System.currentTimeMillis(),
            latitude = 12.9716,
            longitude = 77.5946,
            speedMs = 15.0,
            accuracyMeters = 5.0f
        )
        assertNotNull(point)
        assert(point.rideId == 1L)
        assert(!point.isPaused)
        assert(!point.isGap)
    }

    @Test
    fun `ride status enum values are correct`() {
        val statuses = com.abrar.motolog.data.local.entity.RideStatus.entries
        assert(statuses.size == 3)
        assert(statuses.contains(com.abrar.motolog.data.local.entity.RideStatus.ACTIVE))
        assert(statuses.contains(com.abrar.motolog.data.local.entity.RideStatus.COMPLETED))
        assert(statuses.contains(com.abrar.motolog.data.local.entity.RideStatus.RECOVERED))
    }

    @Test
    fun `converters roundtrip ride status`() {
        val converters = Converters()
        com.abrar.motolog.data.local.entity.RideStatus.entries.forEach { status ->
            val serialized = converters.fromRideStatus(status)
            val deserialized = converters.toRideStatus(serialized)
            assert(deserialized == status) {
                "Converter roundtrip failed for $status"
            }
        }
    }
}
