package com.abrar.motolog.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedColorScaleTest {

    @Test
    fun `insufficient points returns null`() {
        assertNull(SpeedColorScale.computeScale(emptyList()))
        assertNull(SpeedColorScale.computeScale(listOf(50.0, 60.0)))
        assertNull(SpeedColorScale.computeScale(listOf(0.0, 0.0, 0.0)))
    }

    @Test
    fun `normal distribution calculates valid P5 and P95 scale bounds`() {
        val speeds = (10..100).map { it.toDouble() }
        val scale = SpeedColorScale.computeScale(speeds)

        assertNotNull(scale)
        assertTrue(scale!!.minSpeedKmh < scale.maxSpeedKmh)
        assertTrue(scale.medianSpeedKmh in scale.minSpeedKmh..scale.maxSpeedKmh)
    }

    @Test
    fun `fraction maps values accurately and clamps extremes`() {
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
    fun `hue maps slow to green and fast to red`() {
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
    fun `narrow speed range uses fallback window`() {
        // Uniform speeds around 50 km/h with range < 5 km/h
        val speeds = listOf(50.0, 50.5, 51.0, 50.2, 50.8, 50.1, 50.4)
        val scale = SpeedColorScale.computeScale(speeds)

        assertNotNull(scale)
        assertTrue(scale!!.maxSpeedKmh - scale.minSpeedKmh >= 15.0)
    }

    @Test
    fun `hslToArgb conversion produces correct RGB values`() {
        // Green (120 deg) -> high G, low R, low B
        val greenRgb = SpeedColorScale.hslToArgb(120f, 1.0f, 0.5f)
        val g = (greenRgb shr 8) and 0xFF
        val r = (greenRgb shr 16) and 0xFF
        assertTrue("Green should be dominant", g > 200 && r == 0)

        // Red (0 deg) -> high R, low G, low B
        val redRgb = SpeedColorScale.hslToArgb(0f, 1.0f, 0.5f)
        val redR = (redRgb shr 16) and 0xFF
        val redG = (redRgb shr 8) and 0xFF
        assertTrue("Red should be dominant", redR > 200 && redG == 0)
    }
}
