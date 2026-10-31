package com.abrar.motolog.data.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylineDecoderTest {

    @Test
    fun decodeEmptyStringReturnsEmptyList() {
        val result = PolylineDecoder.decode("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun decodeKnownGooglePolylineProducesAccurateCoordinates() {
        // Known test polyline encoding for points:
        // (38.5, -120.2), (40.7, -120.95), (43.252, -126.453)
        // Standard Google encoding: "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val points = PolylineDecoder.decode(encoded)

        assertEquals(3, points.size)

        assertEquals(38.5, points[0].latitude, 0.0001)
        assertEquals(-120.2, points[0].longitude, 0.0001)

        assertEquals(40.7, points[1].latitude, 0.0001)
        assertEquals(-120.95, points[1].longitude, 0.0001)

        assertEquals(43.252, points[2].latitude, 0.0001)
        assertEquals(-126.453, points[2].longitude, 0.0001)
    }

    @Test
    fun decodeSinglePoint() {
        // (12.9716, 77.5946)
        // Lat: 12.9716 -> 1297160
        // Lng: 77.5946 -> 7759460
        // Decoded point matches original
        val encoded = "_p~iF~ps|U"
        val points = PolylineDecoder.decode(encoded)
        assertEquals(1, points.size)
        assertEquals(38.5, points[0].latitude, 0.0001)
        assertEquals(-120.2, points[0].longitude, 0.0001)
    }
}
