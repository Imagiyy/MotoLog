package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.FuelFill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FuelMileageCalculatorTest {

    @Test
    fun emptyListReturnsEmptyStats() {
        val stats = FuelMileageCalculator.calculate(emptyList())
        assertEquals(0.0, stats.totalLitres, 0.001)
        assertEquals(0.0, stats.totalCost, 0.001)
        assertNull(stats.averageMileageKml)
        assertNull(stats.latestMileageKml)
        assertEquals(0, stats.measuredIntervalsCount)
    }

    @Test
    fun singleEntryReturnsTotalsButNoMileage() {
        val log = FuelFill(
            id = 1,
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
    fun normalSequenceOfTwoFullTanksComputesExactMileage() {
        // Fill 1: odo 10,000 km, full tank. (Baseline, litres not counted toward interval distance)
        val fill1 = FuelFill(id = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 20.0, isFullTank = true)
        // Fill 2: odo 10_300 km, 10.0 litres added to fill back up to full.
        // Distance = 300 km, Litres used = 10.0 -> Mileage = 30.0 km/l
        val fill2 = FuelFill(id = 2, timestampEpochMs = 2000L, odometerKm = 10_300.0, litres = 10.0, totalCost = 20.0, isFullTank = true)

        val stats = FuelMileageCalculator.calculate(listOf(fill1, fill2))
        assertEquals(20.0, stats.totalLitres, 0.001)
        assertEquals(40.0, stats.totalCost, 0.001)
        assertEquals(1, stats.measuredIntervalsCount)
        assertNotNull(stats.latestMileageKml)
        assertEquals(30.0, stats.latestMileageKml!!, 0.001)
        assertEquals(30.0, stats.averageMileageKml!!, 0.001)
    }

    @Test
    fun partialFillsComputeMileageAndDistancePerCostDirectly() {
        // Fill 1: odo 10,000, 10.0 litres, cost 500 (NOT full tank)
        val f1 = FuelFill(id = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 500.0, isFullTank = false)
        // Fill 2: odo 10,150, partial fill 4.0 litres, cost 400 (NOT full tank)
        // Interval 1: 150 km / 4 L = 37.5 km/l, 150 km / 400 = 0.375 km/currency
        val f2 = FuelFill(id = 2, timestampEpochMs = 2000L, odometerKm = 10_150.0, litres = 4.0, totalCost = 400.0, isFullTank = false)
        // Fill 3: odo 10,300, 6.0 litres, cost 600
        // Interval 2: 150 km / 6 L = 25.0 km/l, 150 km / 600 = 0.25 km/currency
        val f3 = FuelFill(id = 3, timestampEpochMs = 3000L, odometerKm = 10_300.0, litres = 6.0, totalCost = 600.0, isFullTank = false)

        val stats = FuelMileageCalculator.calculate(listOf(f1, f2, f3))
        assertEquals(20.0, stats.totalLitres, 0.001)
        assertEquals(1500.0, stats.totalCost, 0.001)
        assertEquals(2, stats.measuredIntervalsCount)

        // Interval checks
        assertEquals(37.5, stats.intervals[0].kmPerLitre, 0.001)
        assertEquals(0.375, stats.intervals[0].kmPerCost!!, 0.001)

        assertEquals(25.0, stats.intervals[1].kmPerLitre, 0.001)
        assertEquals(0.25, stats.intervals[1].kmPerCost!!, 0.001)

        // Overall stats
        assertEquals(25.0, stats.latestMileageKml!!, 0.001)
        assertEquals(30.0, stats.averageMileageKml!!, 0.001)
        assertEquals(0.25, stats.latestKmPerCost!!, 0.001)
        assertEquals(300.0 / 1000.0, stats.averageKmPerCost!!, 0.001) // 300 km / (400 + 600) = 0.30 km/rupee
        assertEquals(1000.0 / 300.0, stats.averageCostPerKm!!, 0.001) // 1000 / 300 = 3.333 rupee/km
    }

    @Test
    fun handlesOutOfOrderListCorrectly() {
        // Logs provided newest first (e.g. from UI query)
        val f1 = FuelFill(id = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 1000.0, isFullTank = false)
        val f2 = FuelFill(id = 2, timestampEpochMs = 2000L, odometerKm = 10_250.0, litres = 10.0, totalCost = 1000.0, isFullTank = false)

        // Pass descending order
        val stats = FuelMileageCalculator.calculate(listOf(f2, f1))
        assertEquals(1, stats.measuredIntervalsCount)
        assertEquals(25.0, stats.latestMileageKml!!, 0.001)
        assertEquals(0.25, stats.latestKmPerCost!!, 0.001)
    }

    @Test
    fun multipleIntervalsComputeSeparateLatestAndAverageMileage() {
        // Fill 1: odo 10,000
        val f1 = FuelFill(id = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 500.0, isFullTank = true)
        // Fill 2: odo 10,300, 10 L, 600 cost -> 300 km / 10 L = 30 km/l, 300 / 600 = 0.5 km/cost
        val f2 = FuelFill(id = 2, timestampEpochMs = 2000L, odometerKm = 10_300.0, litres = 10.0, totalCost = 600.0, isFullTank = true)
        // Fill 3: odo 10,500, 10 L, 400 cost -> 200 km / 10 L = 20 km/l, 200 / 400 = 0.5 km/cost
        val f3 = FuelFill(id = 3, timestampEpochMs = 3000L, odometerKm = 10_500.0, litres = 10.0, totalCost = 400.0, isFullTank = true)

        // Total distance across intervals = 300 + 200 = 500 km
        // Total litres consumed in intervals = 10 + 10 = 20 L
        // Overall average = 500 / 20 = 25.0 km/l
        // Total cost in intervals = 600 + 400 = 1000
        // Overall average km/cost = 500 / 1000 = 0.5 km/cost
        val stats = FuelMileageCalculator.calculate(listOf(f1, f2, f3))
        assertEquals(2, stats.measuredIntervalsCount)
        assertEquals(20.0, stats.latestMileageKml!!, 0.001)
        assertEquals(25.0, stats.averageMileageKml!!, 0.001)
        assertEquals(0.5, stats.latestKmPerCost!!, 0.001)
        assertEquals(0.5, stats.averageKmPerCost!!, 0.001)
        assertEquals(2.0, stats.averageCostPerKm!!, 0.001)
    }

    @Test
    fun zeroDistanceOrLowerOdometerDoesNotProduceNaNOrInfinity() {
        val f1 = FuelFill(id = 1, timestampEpochMs = 1000L, odometerKm = 10_000.0, litres = 10.0, totalCost = 20.0, isFullTank = true)
        val f2 = FuelFill(id = 2, timestampEpochMs = 2000L, odometerKm = 10_000.0, litres = 5.0, totalCost = 10.0, isFullTank = true)

        val stats = FuelMileageCalculator.calculate(listOf(f1, f2))
        // Distance is 0 -> should not produce NaN or Infinity
        assertNull(stats.averageMileageKml)
        assertNull(stats.latestMileageKml)
        assertNull(stats.averageKmPerCost)
    }

    @Test
    fun singleEntryWithCurrentOdometerComputesDirectMileage() {
        val log = FuelFill(
            id = 1,
            timestampEpochMs = 1_000L,
            odometerKm = 300.0,
            litres = 10.0,
            totalCost = 1000.0,
            isFullTank = true
        )
        // Bike has 400 km on the dash odometer, and 10 L of fuel logged
        val stats = FuelMileageCalculator.calculate(listOf(log), currentOdometerKm = 400.0)
        assertEquals(10.0, stats.totalLitres, 0.001)
        assertEquals(1000.0, stats.totalCost, 0.001)
        assertEquals(400.0, stats.totalRecordedDistanceKm, 0.001)
        assertNotNull(stats.averageMileageKml)
        assertEquals(40.0, stats.averageMileageKml!!, 0.001) // 400 km / 10 L = 40 km/l
        assertNotNull(stats.averageKmPerCost)
        assertEquals(400.0 / 1000.0, stats.averageKmPerCost!!, 0.001) // 400 / 1000 = 0.4 km/currency
        assertEquals(1000.0 / 400.0, stats.averageCostPerKm!!, 0.001) // 1000 / 400 = 2.5 currency/km
        assertNull(stats.latestMileageKml) // only 1 log, so no interval between logs
    }

    @Test
    fun multipleEntriesWithCurrentOdometerIncludesUnloggedRideCalibration() {
        // Fill 1: odo 1000 km, 10 L, 500 cost
        val f1 = FuelFill(id = 1, timestampEpochMs = 1000L, odometerKm = 1000.0, litres = 10.0, totalCost = 500.0, isFullTank = true)
        // Fill 2: odo 1300 km, 10 L, 500 cost
        val f2 = FuelFill(id = 2, timestampEpochMs = 2000L, odometerKm = 1300.0, litres = 10.0, totalCost = 500.0, isFullTank = true)
        // Rider then went for a ride without logging and calibrated dash to 1500 km
        val stats = FuelMileageCalculator.calculate(listOf(f1, f2), currentOdometerKm = 1500.0)

        // Total distance = 1500 km, Total fuel = 20 L, Total cost = 1000
        assertEquals(20.0, stats.totalLitres, 0.001)
        assertEquals(1000.0, stats.totalCost, 0.001)
        assertEquals(1500.0, stats.totalRecordedDistanceKm, 0.001)
        assertNotNull(stats.averageMileageKml)
        assertEquals(75.0, stats.averageMileageKml!!, 0.001) // 1500 km / 20 L = 75 km/l
        assertNotNull(stats.averageKmPerCost)
        assertEquals(1500.0 / 1000.0, stats.averageKmPerCost!!, 0.001) // 1.5 km/currency
        assertEquals(1000.0 / 1500.0, stats.averageCostPerKm!!, 0.001)

        // Latest interval is still the most recent fill-to-fill interval
        assertEquals(30.0, stats.latestMileageKml!!, 0.001) // (1300 - 1000) / 10 L = 30 km/l
    }
}
