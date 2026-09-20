package com.abrar.motolog.domain.engine

import com.abrar.motolog.domain.TrackingConstants

/**
 * Computes a per-ride adaptive speed → color mapping.
 *
 * Pure Kotlin, no Android imports — fully unit testable on JVM.
 *
 * Design: Uses P5/P95 percentiles of the ride's speed distribution as the
 * scale bounds. Maps speed to a hue value via HSL interpolation:
 * Green (120°) at min → Yellow (60°) at mid → Red (0°) at max.
 *
 * If the speed range is narrower than [TrackingConstants.SPEED_COLOR_MIN_RANGE_KMH],
 * falls back to a fixed window around the median speed.
 */
object SpeedColorScale {

    /**
     * Computed color scale for a ride.
     *
     * @property minSpeedKmh Lower bound of the color scale (green).
     * @property maxSpeedKmh Upper bound of the color scale (red).
     * @property medianSpeedKmh Median speed for reference.
     */
    data class Scale(
        val minSpeedKmh: Double,
        val maxSpeedKmh: Double,
        val medianSpeedKmh: Double
    ) {
        /**
         * Map a speed value to a fraction [0.0, 1.0] within this scale.
         * 0.0 = min (green), 1.0 = max (red).
         * Values outside the range are clamped.
         */
        fun fraction(speedKmh: Double): Float {
            val range = maxSpeedKmh - minSpeedKmh
            if (range <= 0.0) return 0.5f
            return ((speedKmh - minSpeedKmh) / range).coerceIn(0.0, 1.0).toFloat()
        }

        /**
         * Map a speed value to an HSL hue angle.
         * Returns hue in degrees: 120 (green) → 60 (yellow) → 0 (red).
         */
        fun hue(speedKmh: Double): Float {
            return 120f * (1f - fraction(speedKmh))
        }

        /**
         * Map a speed value to an ARGB color int.
         * Green (slow) → Yellow (medium) → Red (fast).
         */
        fun colorArgb(speedKmh: Double): Int {
            val h = hue(speedKmh)
            return hslToArgb(h, 0.85f, 0.5f)
        }

        /**
         * Map a speed value to ARGB color components as [0..255].
         * Returns Triple(red, green, blue).
         */
        fun colorRgb(speedKmh: Double): Triple<Int, Int, Int> {
            val argb = colorArgb(speedKmh)
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            return Triple(r, g, b)
        }
    }

    /**
     * Compute the color scale from a list of speeds.
     *
     * @param speedsKmh List of speed values (km/h) for accepted, non-paused points.
     *        Should exclude paused and gap points.
     * @return [Scale] or null if there are not enough data points.
     */
    fun computeScale(speedsKmh: List<Double>): Scale? {
        val nonZero = speedsKmh.filter { it > 0.0 }
        if (nonZero.size < 3) return null

        val sorted = nonZero.sorted()
        val p5Index = (sorted.size * 0.05).toInt().coerceAtLeast(0)
        val p95Index = (sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)
        val medianIndex = sorted.size / 2

        var minSpeed = sorted[p5Index]
        var maxSpeed = sorted[p95Index]
        val medianSpeed = sorted[medianIndex]

        // Fallback if range is too narrow
        if (maxSpeed - minSpeed < TrackingConstants.SPEED_COLOR_MIN_RANGE_KMH) {
            minSpeed = (medianSpeed - TrackingConstants.SPEED_COLOR_FALLBACK_WINDOW_KMH).coerceAtLeast(0.0)
            maxSpeed = medianSpeed + TrackingConstants.SPEED_COLOR_FALLBACK_WINDOW_KMH
        }

        return Scale(
            minSpeedKmh = minSpeed,
            maxSpeedKmh = maxSpeed,
            medianSpeedKmh = medianSpeed
        )
    }

    /**
     * Convert HSL to ARGB color int.
     *
     * @param h Hue in degrees [0, 360)
     * @param s Saturation [0, 1]
     * @param l Lightness [0, 1]
     * @return ARGB color int (alpha = 0xFF)
     */
    internal fun hslToArgb(h: Float, s: Float, l: Float): Int {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
