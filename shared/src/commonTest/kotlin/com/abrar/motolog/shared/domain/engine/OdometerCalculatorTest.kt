package com.abrar.motolog.shared.domain.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class OdometerCalculatorTest {

    @Test
    fun initialOdometerPlusRecordedDistanceWithoutOffset() {
        val odo = OdometerCalculator.calculateCurrentOdometerKm(
            initialOdometerKm = 10_000.0,
            recordedDistanceMeters = 125_400.0, // 125.4 km
            odometerOffsetKm = 0.0
        )
        assertEquals(10_125.4, odo, 0.001)
    }

    @Test
    fun odometerWithPositiveCalibrationOffset() {
        val odo = OdometerCalculator.calculateCurrentOdometerKm(
            initialOdometerKm = 5_000.0,
            recordedDistanceMeters = 50_000.0, // 50 km
            odometerOffsetKm = 15.5
        )
        assertEquals(5_065.5, odo, 0.001)
    }

    @Test
    fun odometerWithNegativeCalibrationOffset() {
        val odo = OdometerCalculator.calculateCurrentOdometerKm(
            initialOdometerKm = 5_000.0,
            recordedDistanceMeters = 50_000.0, // 50 km
            odometerOffsetKm = -10.0
        )
        assertEquals(5_040.0, odo, 0.001)
    }

    @Test
    fun computeOffsetForTargetDashboardOdometer() {
        // App recorded 10,000 initial + 150 km recorded (150,000 m) = 10,150.
        // Dash shows 10,200 (drift or unrecorded riding).
        val offset = OdometerCalculator.computeOffsetForTarget(
            initialOdometerKm = 10_000.0,
            recordedDistanceMeters = 150_000.0,
            targetOdometerKm = 10_200.0
        )
        assertEquals(50.0, offset, 0.001)

        val recalculated = OdometerCalculator.calculateCurrentOdometerKm(
            initialOdometerKm = 10_000.0,
            recordedDistanceMeters = 150_000.0,
            odometerOffsetKm = offset
        )
        assertEquals(10_200.0, recalculated, 0.001)
    }

    @Test
    fun rideDeletedOrUnassignedCorrectlyReducesDistance() {
        // Initially 2 rides totaling 100,000 meters (100 km)
        var recordedMeters = 100_000.0
        var odo = OdometerCalculator.calculateCurrentOdometerKm(5_000.0, recordedMeters, 0.0)
        assertEquals(5_100.0, odo, 0.001)

        // Deleting a 40 km ride (40,000 meters)
        recordedMeters -= 40_000.0
        odo = OdometerCalculator.calculateCurrentOdometerKm(5_000.0, recordedMeters, 0.0)
        assertEquals(5_060.0, odo, 0.001)
    }

    @Test
    fun rideReassignedFromBikeAToBikeB() {
        // Bike A has 100 km recorded, Bike B has 50 km recorded.
        // Reassign a 30 km ride from Bike A to Bike B.
        var bikeARecordedMeters = 100_000.0
        var bikeBRecordedMeters = 50_000.0

        val rideDistanceMeters = 30_000.0
        bikeARecordedMeters -= rideDistanceMeters
        bikeBRecordedMeters += rideDistanceMeters

        val bikeAOdo = OdometerCalculator.calculateCurrentOdometerKm(1_000.0, bikeARecordedMeters, 0.0)
        val bikeBOdo = OdometerCalculator.calculateCurrentOdometerKm(2_000.0, bikeBRecordedMeters, 0.0)

        assertEquals(1_070.0, bikeAOdo, 0.001)
        assertEquals(2_080.0, bikeBOdo, 0.001)
    }
}
