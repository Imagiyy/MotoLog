package com.abrar.motolog.shared.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformFormatterTest {

    @Test
    fun testHalfWayAndBinaryImpreciseBoundaryDecimals() {
        // Boundary values specifically requested to test half-up and floating point precision
        assertEquals("1.01", PlatformFormatter.formatDecimal(1.005, 2))
        assertEquals("1.1", PlatformFormatter.formatDecimal(1.05, 1))
        assertEquals("2.68", PlatformFormatter.formatDecimal(2.675, 2))
        assertEquals("0.15", PlatformFormatter.formatDecimal(0.145, 2))

        // Standard ride metrics decimals
        assertEquals("621.4", PlatformFormatter.formatDecimal(621.371, 1))
        assertEquals("25.0", PlatformFormatter.formatDecimal(25.0, 1))
        assertEquals("0.0", PlatformFormatter.formatDecimal(0.0, 1))
        assertEquals("0", PlatformFormatter.formatDecimal(0.0, 0))
        assertEquals("100", PlatformFormatter.formatDecimal(100.4, 0))
    }

    @Test
    fun testThousandsFormatting() {
        assertEquals("0", PlatformFormatter.formatThousands(0.0))
        assertEquals("500", PlatformFormatter.formatThousands(500.0))
        assertEquals("1,000", PlatformFormatter.formatThousands(1000.0))
        assertEquals("1,200", PlatformFormatter.formatThousands(1200.0))
        assertEquals("10,000", PlatformFormatter.formatThousands(10000.0))
        assertEquals("1,234,568", PlatformFormatter.formatThousands(1234567.8))
    }
}
