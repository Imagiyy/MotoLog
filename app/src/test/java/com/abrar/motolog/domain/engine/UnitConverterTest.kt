package com.abrar.motolog.domain.engine

import com.abrar.motolog.domain.model.FuelUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConverterTest {

    private val delta = 0.0001

    @Test
    fun distanceConversions_metricVsImperial() {
        // 1000 meters = 1.0 km
        assertEquals(1.0, UnitConverter.metersToUserDistance(1000.0, isMetric = true), delta)
        // 1609.344 meters = 1.0 mile
        assertEquals(1.0, UnitConverter.metersToUserDistance(1609.344, isMetric = false), delta)

        // 100 km = 62.1371 miles
        assertEquals(100.0, UnitConverter.kmToUserDistance(100.0, isMetric = true), delta)
        assertEquals(62.1371192, UnitConverter.kmToUserDistance(100.0, isMetric = false), 0.001)

        // Inverse
        assertEquals(1000.0, UnitConverter.userDistanceToMeters(1.0, isMetric = true), delta)
        assertEquals(1609.344, UnitConverter.userDistanceToMeters(1.0, isMetric = false), delta)

        assertEquals(100.0, UnitConverter.userDistanceToKm(100.0, isMetric = true), delta)
        assertEquals(160.9344, UnitConverter.userDistanceToKm(100.0, isMetric = false), 0.001)
    }

    @Test
    fun speedConversions_roundTrip() {
        val speedKmh = 100.0
        val speedMph = UnitConverter.kmhToMph(speedKmh)
        assertEquals(62.1371, speedMph, 0.01)

        val backToKmh = UnitConverter.mphToKmh(speedMph)
        assertEquals(speedKmh, backToKmh, 0.0001)

        // Speed from m/s
        val speedMs = 27.7778 // ~100 km/h
        assertEquals("100", UnitConverter.formatSpeedFromMs(speedMs, isMetric = true))
        assertEquals("62", UnitConverter.formatSpeedFromMs(speedMs, isMetric = false))
    }

    @Test
    fun elevationConversions_metricVsFeet() {
        val meters = 100.0
        val feet = UnitConverter.metersToFeet(meters)
        assertEquals(328.084, feet, 0.01)
        assertEquals(meters, UnitConverter.feetToMeters(feet), 0.001)

        assertEquals("100", UnitConverter.formatElevation(meters, isMetric = true))
        assertEquals("328", UnitConverter.formatElevation(meters, isMetric = false))
        assertEquals("100 m", UnitConverter.formatElevationWithUnit(meters, isMetric = true))
        assertEquals("328 ft", UnitConverter.formatElevationWithUnit(meters, isMetric = false))
    }

    @Test
    fun fuelEconomyConversions_allUnits() {
        // 25 km/L baseline
        val kmPerLiter = 25.0

        // KM_PER_LITER
        assertEquals(25.0, UnitConverter.convertKmLToFuelEconomy(kmPerLiter, FuelUnit.KM_PER_LITER), delta)

        // L_PER_100KM: 100 / 25 = 4.0 L/100km
        assertEquals(4.0, UnitConverter.convertKmLToFuelEconomy(kmPerLiter, FuelUnit.L_PER_100KM), delta)

        // MPG US: 25 * (3.785411784 / 1.609344) = ~58.8036
        val mpgUs = UnitConverter.convertKmLToFuelEconomy(kmPerLiter, FuelUnit.MPG_US)
        assertEquals(58.8036, mpgUs, 0.01)

        // MPG UK: 25 * (4.54609 / 1.609344) = ~70.6202
        val mpgUk = UnitConverter.convertKmLToFuelEconomy(kmPerLiter, FuelUnit.MPG_UK)
        assertEquals(70.6202, mpgUk, 0.01)

        // Formatting
        assertEquals("25.0 km/l", UnitConverter.formatFuelEconomy(kmPerLiter, FuelUnit.KM_PER_LITER))
        assertEquals("4.0 l/100km", UnitConverter.formatFuelEconomy(kmPerLiter, FuelUnit.L_PER_100KM))
        assertEquals("58.8 mpg", UnitConverter.formatFuelEconomy(kmPerLiter, FuelUnit.MPG_US))
        assertEquals("70.6 mpg", UnitConverter.formatFuelEconomy(kmPerLiter, FuelUnit.MPG_UK))
    }

    @Test
    fun driftResistantInterval_preventsSnappingErrors() {
        // User sets a 3000-mile service interval:
        // Stored in km = 3000 * 1.609344 = 4828.032 km
        val milesEntered = 3000.0
        val storedKm = UnitConverter.userDistanceToKm(milesEntered, isMetric = false)
        assertEquals(4828.032, storedKm, delta)

        // When retrieved for display in imperial, it must snap to exactly 3000.0 without floating point drift
        val displayedMiles = UnitConverter.getIntervalForDisplay(storedKm, isMetric = false)
        assertEquals(3000.0, displayedMiles, delta)
        assertEquals("3000 mi", UnitConverter.formatInterval(storedKm, isMetric = false))

        // When user enters 1000 km
        val kmEntered = 1000.0
        val displayedKm = UnitConverter.getIntervalForDisplay(kmEntered, isMetric = true)
        assertEquals(1000.0, displayedKm, delta)
        assertEquals("1000 km", UnitConverter.formatInterval(kmEntered, isMetric = true))

        // 1000 km displayed in imperial: 621.37 mi (not integer, shows 1 decimal place without crashing)
        val displayedInMiles = UnitConverter.getIntervalForDisplay(kmEntered, isMetric = false)
        assertEquals(621.371, displayedInMiles, 0.01)
        assertEquals("621.4 mi", UnitConverter.formatInterval(kmEntered, isMetric = false))
    }
}
