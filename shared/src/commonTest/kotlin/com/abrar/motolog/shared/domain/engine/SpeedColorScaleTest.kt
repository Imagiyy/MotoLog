package com.abrar.motolog.shared.domain.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeedColorScaleTest {

    @Test
    fun insufficientPointsReturnsNull() {
        assertNull(SpeedColorScale.computeScale(emptyList()))
        assertNull(SpeedColorScale.computeScale(listOf(50.0, 60.0)))
        assertNull(SpeedColorScale.computeScale(listOf(0.0, 0.0, 0.0)))
    }

    @Test
    fun normalDistributionCalculatesValidP5AndP95ScaleBounds() {
        val speeds = (10..100).map { it.toDouble() }
        val scale = SpeedColorScale.computeScale(speeds)

        assertNotNull(scale)
        assertTrue(scale.minSpeedKmh < scale.maxSpeedKmh)
        assertTrue(scale.medianSpeedKmh in scale.minSpeedKmh..scale.maxSpeedKmh)
    }

    @Test
    fun fractionMapsValuesAccuratelyAndClampsExtremes() {
        val scale = SpeedColorScale.Scale(
            minSpeedKmh = 20.0,
            maxSpeedKmh = 100.0,
            medianSpeedKmh = 60.0
        )

        assertEquals(0.0f, scale.fraction(20.0), 0.001f)
        assertEquals(0.5f, scale.fraction(60.0), 0.001f)
        assertEquals(1.0f, scale.fraction(100.0), 0.001f)
        // Clamping below and above
        assertEquals(0.0f, scale.fraction(10.0), 0.001f)
        assertEquals(1.0f, scale.fraction(120.0), 0.001f)
    }

    @Test
    fun hueMapsSlowToGreenAndFastToRed() {
        val scale = SpeedColorScale.Scale(
            minSpeedKmh = 0.0,
            maxSpeedKmh = 100.0,
            medianSpeedKmh = 50.0
        )

        assertEquals(120f, scale.hue(0.0), 0.1f)    // Green
        assertEquals(60f, scale.hue(50.0), 0.1f)    // Yellow
        assertEquals(0f, scale.hue(100.0), 0.1f)    // Red
    }

    @Test
    fun narrowSpeedRangeUsesFallbackWindow() {
        // Uniform speeds around 50 km/h with range < 5 km/h
        val speeds = listOf(50.0, 50.5, 51.0, 50.2, 50.8, 50.1, 50.4)
        val scale = SpeedColorScale.computeScale(speeds)

        assertNotNull(scale)
        assertTrue(scale.maxSpeedKmh - scale.minSpeedKmh >= 15.0)
    }

    @Test
    fun hslToArgbConversionProducesCorrectRgbValues() {
        // Green (120 deg) -> high G, low R, low B
        val greenRgb = SpeedColorScale.hslToArgb(120f, 1.0f, 0.5f)
        val g = (greenRgb shr 8) and 0xFF
        val r = (greenRgb shr 16) and 0xFF
        assertTrue(g > 200 && r == 0, "Green should be dominant")

        // Red (0 deg) -> high R, low G, low B
        val redRgb = SpeedColorScale.hslToArgb(0f, 1.0f, 0.5f)
        val redR = (redRgb shr 16) and 0xFF
        val redG = (redRgb shr 8) and 0xFF
        assertTrue(redR > 200 && redG == 0, "Red should be dominant")
    }
}
