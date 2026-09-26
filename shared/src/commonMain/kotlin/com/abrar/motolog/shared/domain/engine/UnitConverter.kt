package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.model.FuelUnit
import com.abrar.motolog.shared.domain.util.PlatformFormatter
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToLong

/**
 * Pure Kotlin utility for conversions between metric (internal storage) and imperial units.
 *
 * Core Principle:
 * All internal domain and database values are stored in metric:
 * - Distance: meters / kilometres
 * - Speed: km/h / m/s
 * - Fuel: litres
 * - Elevation: meters
 *
 * Conversions occur strictly at the presentation / display layer.
 */
object UnitConverter {

    const val METERS_PER_MILE: Double = 1609.344
    const val KM_PER_MILE: Double = 1.609344
    const val MILES_PER_KM: Double = 1.0 / KM_PER_MILE // ~0.621371192
    const val FEET_PER_METER: Double = 3.280839895
    const val METERS_PER_FOOT: Double = 1.0 / FEET_PER_METER

    const val LITERS_PER_GALLON_US: Double = 3.785411784
    const val LITERS_PER_GALLON_UK: Double = 4.54609

    // Factor for converting km/l to mpg
    // mpg = (km / km_per_mile) / (liters / liters_per_gal) = (km / liters) * (liters_per_gal / km_per_mile)
    const val KM_L_TO_MPG_US: Double = LITERS_PER_GALLON_US / KM_PER_MILE // ~2.35214583
    const val KM_L_TO_MPG_UK: Double = LITERS_PER_GALLON_UK / KM_PER_MILE // ~2.82480936

    // ============================================================
    // Distance Conversion
    // ============================================================

    fun metersToUserDistance(meters: Double, isMetric: Boolean): Double {
        return if (isMetric) meters / 1000.0 else meters / METERS_PER_MILE
    }

    fun kmToUserDistance(km: Double, isMetric: Boolean): Double {
        return if (isMetric) km else km * MILES_PER_KM
    }

    fun userDistanceToMeters(userDistance: Double, isMetric: Boolean): Double {
        return if (isMetric) userDistance * 1000.0 else userDistance * METERS_PER_MILE
    }

    fun userDistanceToKm(userDistance: Double, isMetric: Boolean): Double {
        return if (isMetric) userDistance else userDistance * KM_PER_MILE
    }

    // ============================================================
    // Speed Conversion
    // ============================================================

    fun kmhToMph(kmh: Double): Double = kmh * MILES_PER_KM

    fun mphToKmh(mph: Double): Double = mph * KM_PER_MILE

    fun userSpeedToKmh(userSpeed: Double, isMetric: Boolean): Double {
        return if (isMetric) userSpeed else mphToKmh(userSpeed)
    }

    fun kmhToUserSpeed(kmh: Double, isMetric: Boolean): Double {
        return if (isMetric) kmh else kmhToMph(kmh)
    }

    // ============================================================
    // Elevation Conversion
    // ============================================================

    fun metersToFeet(meters: Double): Double = meters * FEET_PER_METER

    fun feetToMeters(feet: Double): Double = feet * METERS_PER_FOOT

    fun metersToUserElevation(meters: Double, isMetric: Boolean): Double {
        return if (isMetric) meters else metersToFeet(meters)
    }

    // ============================================================
    // Fuel Economy Conversion
    // ============================================================

    /**
     * Converts raw km/L fuel economy to the target [FuelUnit].
     */
    fun convertKmLToFuelEconomy(kmPerLiter: Double, fuelUnit: FuelUnit): Double {
        if (kmPerLiter <= 0.0) return 0.0
        return when (fuelUnit) {
            FuelUnit.KM_PER_LITER -> kmPerLiter
            FuelUnit.L_PER_100KM -> 100.0 / kmPerLiter
            FuelUnit.MPG_US -> kmPerLiter * KM_L_TO_MPG_US
            FuelUnit.MPG_UK -> kmPerLiter * KM_L_TO_MPG_UK
        }
    }

    // ============================================================
    // Drift-Resistant Interval Conversion
    // ============================================================

    /**
     * Converts a stored interval (in km) to the user's unit, snapping to an integer
     * if it is within a small epsilon (0.02) of a whole number.
     * Prevents drift such as 3000 miles -> 4828.032 km -> 2999.999 miles -> 2999.9.
     */
    fun getIntervalForDisplay(intervalKm: Double, isMetric: Boolean): Double {
        val converted = kmToUserDistance(intervalKm, isMetric)
        val rounded = round(converted)
        return if (abs(converted - rounded) < 0.02) rounded else converted
    }

    /**
     * Formats an interval in user units, omitting decimal places if it is an integer.
     */
    fun formatInterval(intervalKm: Double, isMetric: Boolean): String {
        val value = getIntervalForDisplay(intervalKm, isMetric)
        val unit = distanceUnit(isMetric)
        return if (abs(value - round(value)) < 0.01) {
            "${value.roundToLong()} $unit"
        } else {
            "${PlatformFormatter.formatDecimal(value, 1)} $unit"
        }
    }

    // ============================================================
    // Formatting Helpers
    // ============================================================

    fun distanceUnit(isMetric: Boolean): String = if (isMetric) "km" else "mi"

    fun speedUnit(isMetric: Boolean): String = if (isMetric) "km/h" else "mph"

    fun elevationUnit(isMetric: Boolean): String = if (isMetric) "m" else "ft"

    fun formatDistance(meters: Double, isMetric: Boolean, decimals: Int = 1): String {
        val dist = metersToUserDistance(meters, isMetric)
        return PlatformFormatter.formatDecimal(dist, decimals)
    }

    fun formatDistanceWithUnit(meters: Double, isMetric: Boolean, decimals: Int = 1): String {
        return "${formatDistance(meters, isMetric, decimals)} ${distanceUnit(isMetric)}"
    }

    fun formatSpeed(kmh: Double, isMetric: Boolean, decimals: Int = 0): String {
        val spd = kmhToUserSpeed(kmh, isMetric)
        return if (decimals == 0) {
            "${round(spd).toInt()}"
        } else {
            PlatformFormatter.formatDecimal(spd, decimals)
        }
    }

    fun formatSpeedFromMs(speedMs: Double, isMetric: Boolean, decimals: Int = 0): String {
        return formatSpeed(speedMs * 3.6, isMetric, decimals)
    }

    fun formatSpeedWithUnit(kmh: Double, isMetric: Boolean, decimals: Int = 0): String {
        return "${formatSpeed(kmh, isMetric, decimals)} ${speedUnit(isMetric)}"
    }

    fun formatSpeedFromMsWithUnit(speedMs: Double, isMetric: Boolean, decimals: Int = 0): String {
        return "${formatSpeedFromMs(speedMs, isMetric, decimals)} ${speedUnit(isMetric)}"
    }

    fun formatElevation(meters: Double, isMetric: Boolean): String {
        val elev = metersToUserElevation(meters, isMetric)
        return "${round(elev).toInt()}"
    }

    fun formatElevationWithUnit(meters: Double, isMetric: Boolean): String {
        return "${formatElevation(meters, isMetric)} ${elevationUnit(isMetric)}"
    }

    fun formatFuelEconomy(kmPerLiter: Double, fuelUnit: FuelUnit): String {
        val converted = convertKmLToFuelEconomy(kmPerLiter, fuelUnit)
        return "${PlatformFormatter.formatDecimal(converted, 1)} ${fuelUnit.unitLabel}"
    }
}
