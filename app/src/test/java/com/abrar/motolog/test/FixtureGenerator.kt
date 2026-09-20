package com.abrar.motolog.test

import java.io.File
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin

/**
 * Programmatic generator for GPX test fixtures.
 * Produces deterministic fixtures with documented mathematical models and analytical ground truths.
 */
object FixtureGenerator {

    private const val BASE_LAT = 12.971600
    private const val BASE_LON = 77.594600
    private const val METERS_PER_DEGREE_LAT = 111_139.0

    fun generateAll(outputDir: File) {
        outputDir.mkdirs()
        generateStationaryJitter(File(outputDir, "stationary_jitter.gpx"))
        generateHighwayRide(File(outputDir, "highway_ride.gpx"))
        generateStopAndGo(File(outputDir, "stop_and_go.gpx"))
        generateTunnelGap(File(outputDir, "tunnel_gap.gpx"))
        generateCityRide(File(outputDir, "city_ride.gpx"))
    }

    /**
     * 5 minutes (300 s at 1 Hz) of phone resting on a table.
     * GPS drift produces small synthetic jitter (within +/- 1.5m), speed < 1.0 km/h (< 0.28 m/s).
     * Analytical expectation: Distance added < 10m, Moving time = 0s.
     */
    private fun generateStationaryJitter(file: File) {
        val startEpoch = 1774000000000L
        val points = mutableListOf<String>()

        for (i in 0 until 300) {
            val t = startEpoch + (i * 1000L)
            // Deterministic pseudo-random drift using sin/cos
            val latOffsetMeters = 1.2 * sin(i * 0.1)
            val lonOffsetMeters = 1.0 * cos(i * 0.15)
            val lat = BASE_LAT + (latOffsetMeters / METERS_PER_DEGREE_LAT)
            val lon = BASE_LON + (lonOffsetMeters / (METERS_PER_DEGREE_LAT * cos(Math.toRadians(BASE_LAT))))
            val speedMps = (0.15 + 0.1 * sin(i * 0.2)).toFloat() // ~0.5 to 0.9 km/h (< 1.5 km/h threshold)
            val accuracy = (8.0 + 3.0 * cos(i * 0.05)).toFloat()

            points.add(formatTrkpt(lat, lon, 920.0, t, speedMps, accuracy, 0.5f))
        }
        writeGpx(file, "Stationary Jitter 5min", points)
    }

    /**
     * 20 minutes (1200 s) at steady 90 km/h (25.0 m/s) heading North.
     * Analytical expectation: Distance = 30,000 m (30.0 km), Moving time = 1200 s, Max speed = 90.0 km/h.
     */
    private fun generateHighwayRide(file: File) {
        val startEpoch = 1774000000000L
        val points = mutableListOf<String>()
        val speedMps = 25.0f // 90 km/h

        for (i in 0 until 1200) {
            val t = startEpoch + (i * 1000L)
            val distanceMeters = i * 25.0
            val lat = BASE_LAT + (distanceMeters / METERS_PER_DEGREE_LAT)
            val lon = BASE_LON

            points.add(formatTrkpt(lat, lon, 850.0, t, speedMps, 5.0f, 0.2f))
        }
        writeGpx(file, "Highway Ride 20min", points)
    }

    /**
     * 10 minutes (600 s) alternating:
     * - 60s moving at 36 km/h (10.0 m/s)
     * - 60s stopped (0 m/s)
     * Total moving: 300 s, Total stopped: 300 s.
     * Analytical expectation: Distance = 3,000 m (3.0 km), Avg Moving = 36 km/h, Avg Overall = 18 km/h.
     */
    private fun generateStopAndGo(file: File) {
        val startEpoch = 1774000000000L
        val points = mutableListOf<String>()
        var distanceMeters = 0.0

        for (i in 0 until 600) {
            val t = startEpoch + (i * 1000L)
            val cycleSec = i % 120
            val isMoving = cycleSec < 60

            val speedMps = if (isMoving) 10.0f else 0.0f
            if (isMoving && i > 0) {
                distanceMeters += 10.0
            }

            val lat = BASE_LAT + (distanceMeters / METERS_PER_DEGREE_LAT)
            val lon = BASE_LON

            points.add(formatTrkpt(lat, lon, 900.0, t, speedMps, 6.0f, 0.3f))
        }
        writeGpx(file, "Stop and Go 10min", points)
    }

    /**
     * Highway ride with a 45s tunnel gap:
     * - 300s moving at 90 km/h (25 m/s) -> 7,500m
     * - 45s tunnel blackout (no points emitted)
     * - 300s moving at 90 km/h (25 m/s) -> 7,500m
     * Analytical expectation: Distance = 15,000 m (15.0 km), Gap count = 1, Moving time = 600 s, Elapsed = 645 s.
     */
    private fun generateTunnelGap(file: File) {
        val startEpoch = 1774000000000L
        val points = mutableListOf<String>()
        val speedMps = 25.0f // 90 km/h

        // Phase 1: 300s before tunnel
        for (i in 0 until 300) {
            val t = startEpoch + (i * 1000L)
            val dist = i * 25.0
            val lat = BASE_LAT + (dist / METERS_PER_DEGREE_LAT)
            points.add(formatTrkpt(lat, BASE_LON, 800.0, t, speedMps, 5.0f, 0.2f))
        }

        // Tunnel happens from t = 300s to t = 345s (45 second gap, tunnel length = 45 * 25 = 1125m)
        val postTunnelStartEpoch = startEpoch + (345 * 1000L)
        val postTunnelBaseDist = (300 * 25.0) + (45 * 25.0) // 8,625m

        // Phase 2: 300s after tunnel
        for (i in 0 until 300) {
            val t = postTunnelStartEpoch + (i * 1000L)
            val dist = postTunnelBaseDist + (i * 25.0)
            val lat = BASE_LAT + (dist / METERS_PER_DEGREE_LAT)
            points.add(formatTrkpt(lat, BASE_LON, 800.0, t, speedMps, 5.0f, 0.2f))
        }

        writeGpx(file, "Tunnel Gap Ride", points)
    }

    /**
     * 15-minute urban ride (900 s) at 54 km/h (15.0 m/s) with 3 red light stops of 45s each (135s stopped).
     * Moving time: 765 s.
     * Distance: 765 * 15.0 = 11,475 m (11.475 km).
     */
    private fun generateCityRide(file: File) {
        val startEpoch = 1774000000000L
        val points = mutableListOf<String>()
        var dist = 0.0

        for (i in 0 until 900) {
            val t = startEpoch + (i * 1000L)
            // Stops at [180..224], [400..444], [650..694]
            val isStopped = (i in 180 until 225) || (i in 400 until 445) || (i in 650 until 695)
            val speedMps = if (isStopped) 0.0f else 15.0f

            if (!isStopped && i > 0 && points.isNotEmpty()) {
                dist += 15.0
            }

            val lat = BASE_LAT + (dist / METERS_PER_DEGREE_LAT)
            points.add(formatTrkpt(lat, BASE_LON, 910.0, t, speedMps, 7.0f, 0.4f))
        }
        writeGpx(file, "City Ride 15min", points)
    }

    private fun formatTrkpt(
        lat: Double, lon: Double, ele: Double,
        epochMs: Long, speedMps: Float, accuracy: Float, speedAccuracy: Float
    ): String {
        val isoTime = Instant.ofEpochMilli(epochMs).toString()
        return """
            <trkpt lat="$lat" lon="$lon">
                <ele>$ele</ele>
                <time>$isoTime</time>
                <extensions>
                    <speed>$speedMps</speed>
                    <accuracy>$accuracy</accuracy>
                    <speedAccuracy>$speedAccuracy</speedAccuracy>
                </extensions>
            </trkpt>
        """.trimIndent()
    }

    private fun writeGpx(file: File, name: String, points: List<String>) {
        val content = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="MotoLog Test Generator" xmlns="http://www.topografix.com/GPX/1/1">
    <trk>
        <name>$name</name>
        <trkseg>
""")
            for (p in points) {
                append("            ").append(p).append("\n")
            }
            append("""        </trkseg>
    </trk>
</gpx>
""")
        }
        file.writeText(content)
    }
}
