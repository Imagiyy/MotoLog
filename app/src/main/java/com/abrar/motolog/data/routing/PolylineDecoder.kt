package com.abrar.motolog.data.routing

import com.abrar.motolog.shared.domain.model.LocationPoint

/**
 * Decodes Google / GraphHopper encoded polyline format strings into [LocationPoint] coordinates.
 *
 * Supports standard 5-decimal precision (factor 1e5) and 6-decimal precision (factor 1e6).
 * Pure Kotlin, zero external dependencies.
 */
object PolylineDecoder {

    /**
     * Decode an encoded polyline string.
     *
     * @param encoded The polyline string to decode.
     * @param precision Decimal precision factor, default is 1e5 (GraphHopper / Google default).
     * @return List of decoded [LocationPoint] objects.
     */
    fun decode(encoded: String, precision: Double = 1e5): List<LocationPoint> {
        val points = mutableListOf<LocationPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            shift = 0
            result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            val decodedLat = lat / precision
            val decodedLng = lng / precision

            points.add(
                LocationPoint(
                    latitude = decodedLat,
                    longitude = decodedLng,
                    speedMps = null,
                    accuracyMeters = 0f,
                    timestamp = 0L
                )
            )
        }

        return points
    }
}
