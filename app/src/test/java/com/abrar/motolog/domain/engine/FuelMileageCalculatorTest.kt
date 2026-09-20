package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.FuelLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FuelMileageCalculatorTest {

    @Test
    fun `empty list returns empty stats`() {
        val stats = FuelMileageCalculator.calculate(emptyList())
        assertEquals(0.0, stats.totalLitres, 0.001)
        assertEquals(0.0, stats.totalCost, 0.001)
        assertNull(stats.averageMileageKml)
        assertNull(stats.latestMileageKml)
        assertEquals(0, stats.measuredIntervalsCount)
    }

    @Test
    fun `single entry returns totals but no mileage (cannot compute from 1 entry)`() {
        val log = FuelLogEntity(
            id = 1,
            bikeId = 1,
            timestampEpochMs = 1_000L,
            odometerKm = 10_000.0,
            litres = 12.0,
            totalCost = 24.0,
            isFullTank = true
        )
        val stats = FuelMileageCalculator.calculate(listOf(log))
        assertEquals(12.0, stats.totalLitres, 0.001)
        assertEquals(24.0, stats.totalCost, 0.001)
        assertNull(stats.averageMileageKml)
        assertNull(stats.latestMileageKml)
        assertEquals(0, stats.measuredIntervalsCount)
    }

    @Test
    fun `normal sequence of two full tanks computes exact mileage`() {
        // Fill 1: odo 10,000 km, full tank. (Baseline, litres not counted toward interval distance)
        val fill1 = FuelLogEntity(id = 1, bikeId = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 20.0, isFullTank = true)
        // Fill 2: odo 10_300 km, 10.0 litres added to fill back up to full.
        // Distance = 300 km, Litres used = 10.0 -> Mileage = 30.0 km/l
        val fill2 = FuelLogEntity(id = 2, bikeId = 1, timestampEpochMs = 2000L, odometerKm = 10_300.0, litres = 10.0, totalCost = 20.0, isFullTank = true)

        val stats = FuelMileageCalculator.calculate(listOf(fill1, fill2))
        assertEquals(20.0, stats.totalLitres, 0.001)
        assertEquals(40.0, stats.totalCost, 0.001)
        assertEquals(1, stats.measuredIntervalsCount)
        assertNotNull(stats.latestMileageKml)
        assertEquals(30.0, stats.latestMileageKml!!, 0.001)
        assertEquals(30.0, stats.averageMileageKml!!, 0.001)
    }

    @Test
    fun `partial fills are accumulated into next full tank interval`() {
        // Fill 1: odo 10,000, full tank
        val f1 = FuelLogEntity(id = 1, bikeId = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 20.0, isFullTank = true)
        // Fill 2: odo 10,150, partial fill 4.0 litres (NOT full tank)
        val f2 = FuelLogEntity(id = 2, bikeId = 1, timestampEpochMs = 2000L, odometerKm = 10_150.0, litres = 4.0, totalCost = 8.0, isFullTank = false)
        // Fill 3: odo 10_300, full tank, 6.0 litres added
        // Interval is between f1 and f3: Distance = 300 km, total litres used = 4.0 + 6.0 = 10.0 L -> 30 km/l
        val f3 = FuelLogEntity(id = 3, bikeId = 1, timestampEpochMs = 3000L, odometerKm = 10_300.0, litres = 6.0, totalCost = 12.0, isFullTank = true)

        val stats = FuelMileageCalculator.calculate(listOf(f1, f2, f3))
        assertEquals(20.0, stats.totalLitres, 0.001)
        assertEquals(1, stats.measuredIntervalsCount)
        assertEquals(30.0, stats.latestMileageKml!!, 0.001)
        assertEquals(30.0, stats.averageMileageKml!!, 0.001)
    }

    @Test
    fun `handles out of order list correctly`() {
        // Logs provided newest first (e.g. from UI query)
        val f1 = FuelLogEntity(id = 1, bikeId = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 20.0, isFullTank = true)
        val f2 = FuelLogEntity(id = 2, bikeId = 1, timestampEpochMs = 2000L, odometerKm = 10_250.0, litres = 10.0, totalCost = 20.0, isFullTank = true)

        // Pass descending order
        val stats = FuelMileageCalculator.calculate(listOf(f2, f1))
        assertEquals(1, stats.measuredIntervalsCount)
        assertEquals(25.0, stats.latestMileageKml!!, 0.001)
    }

    @Test
    fun `multiple intervals compute separate latest and average mileage`() {
        // Fill 1: odo 10,000
        val f1 = FuelLogEntity(id = 1, bikeId = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 20.0, isFullTank = true)
        // Fill 2: odo 10,300, 10 L -> 300 km / 10 L = 30 km/l
        val f2 = FuelLogEntity(id = 2, bikeId = 1, timestampEpochMs = 2000L, odometerKm = 10_300.0, litres = 10.0, totalCost = 20.0, isFullTank = true)
        // Fill 3: odo 10,500, 10 L -> 200 km / 10 L = 20 km/l
        val f3 = FuelLogEntity(id = 3, bikeId = 1, timestampEpochMs = 3000L, odometerKm = 10_500.0, litres = 10.0, totalCost = 20.0, isFullTank = true)

        // Total distance across intervals = 300 + 200 = 500 km
        // Total litres consumed in intervals = 10 + 10 = 20 L
        // Overall average = 500 / 20 = 25.0 km/l
        val stats = FuelMileageCalculator.calculate(listOf(f1, f2, f3))
        assertEquals(2, stats.measuredIntervalsCount)
        assertEquals(20.0, stats.latestMileageKml!!, 0.001)
        assertEquals(25.0, stats.averageMileageKml!!, 0.001)
    }

    @Test
    fun `zero distance or lower odometer does not produce NaN or infinity`() {
        val f1 = FuelLogEntity(id = 1, bikeId = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 20.0, isFullTank = true)
        val f2 = FuelLogEntity(id = 2, bikeId = 1, timestampEpochMs = 2000L, odometerKm = 10_000.0, litres = 5.0, totalCost = 10.0, isFullTank = true)

        val stats = FuelMileageCalculator.calculate(listOf(f1, f2))
        // Distance is 0 -> should not produce NaN or Infinity
        assertNull(stats.averageMileageKml)
        assertNull(stats.latestMileageKml)
    }
}
