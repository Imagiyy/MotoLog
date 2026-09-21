package com.abrar.motolog.data.io

import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.RideStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class GpxImporterTest {

    @Test
    fun roundTrip_exportAndParse_matchesPointsAndName() {
        val ride = RideEntity(
            id = 1L,
            name = "Test Highway Ride",
            startTime = 1700000000000L,
            endTime = 1700003600000L,
            distanceMeters = 50000.0,
            elapsedTimeMs = 3600000L,
            movingTimeMs = 3000000L,
            avgMovingSpeedMs = 16.6,
            overallAvgSpeedMs = 13.8,
            maxSpeedMs = 25.0,
            status = RideStatus.COMPLETED
        )

        val entities = mutableListOf<RidePointEntity>()
        var time = 1700000000000L
        for (i in 0 until 50) {
            entities.add(
                RidePointEntity(
                    id = i.toLong(),
                    rideId = 1L,
                    timestamp = time,
                    latitude = 12.9716 + (i * 0.001),
                    longitude = 77.5946 + (i * 0.001),
                    speedMs = 20.0,
                    accuracyMeters = 4f,
                    altitudeMeters = 900.0,
                    isPaused = false,
                    isGap = false
                )
            )
            time += 1000L
        }

        val out = ByteArrayOutputStream()
        GpxExporter.exportRide(ride, entities, out)

        val parseResult = GpxImporter.parse(ByteArrayInputStream(out.toByteArray()))
        assertEquals(emptyList<String>(), parseResult.errors)
        assertEquals(1, parseResult.tracks.size)

        val track = parseResult.tracks[0]
        assertEquals("Test Highway Ride", track.name)
        assertTrue(track.hasTimestamps)
        assertTrue(track.hasElevation)
        assertEquals(50, track.points.size)
        assertEquals(12.9716, track.points[0].latitude, 0.0001)
    }

    @Test
    fun missingTimestamps_setsHasTimestampsFalse() {
        val gpxXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
                <trk>
                    <name>No Timestamp Track</name>
                    <trkseg>
                        <trkpt lat="12.9716" lon="77.5946"><ele>920.0</ele></trkpt>
                        <trkpt lat="12.9720" lon="77.5946"><ele>920.0</ele></trkpt>
                    </trkseg>
                </trk>
            </gpx>
        """.trimIndent()

        val parseResult = GpxImporter.parse(ByteArrayInputStream(gpxXml.toByteArray(Charsets.UTF_8)))
        assertTrue(parseResult.errors.isEmpty())
        assertEquals(1, parseResult.tracks.size)
        assertFalse(parseResult.tracks[0].hasTimestamps)
        assertEquals(2, parseResult.tracks[0].points.size)
    }

    @Test
    fun missingElevation_setsHasElevationFalse() {
        val gpxXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
                <trk>
                    <name>No Elevation Track</name>
                    <trkseg>
                        <trkpt lat="12.9716" lon="77.5946"><time>2023-11-14T22:13:20Z</time></trkpt>
                        <trkpt lat="12.9720" lon="77.5946"><time>2023-11-14T22:13:22Z</time></trkpt>
                    </trkseg>
                </trk>
            </gpx>
        """.trimIndent()

        val parseResult = GpxImporter.parse(ByteArrayInputStream(gpxXml.toByteArray(Charsets.UTF_8)))
        assertTrue(parseResult.errors.isEmpty())
        assertEquals(1, parseResult.tracks.size)
        assertTrue(parseResult.tracks[0].hasTimestamps)
        assertFalse(parseResult.tracks[0].hasElevation)
    }

    @Test
    fun xxeAttack_isDefusedSafely() {
        val maliciousGpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
            <gpx version="1.1" creator="Hacker">
                <trk>
                    <name>&xxe;</name>
                    <trkseg>
                        <trkpt lat="12.9716" lon="77.5946"><time>2023-11-14T22:13:20Z</time></trkpt>
                    </trkseg>
                </trk>
            </gpx>
        """.trimIndent()

        val parseResult = GpxImporter.parse(ByteArrayInputStream(maliciousGpx.toByteArray(Charsets.UTF_8)))
        if (parseResult.tracks.isNotEmpty()) {
            val name = parseResult.tracks[0].name
            assertFalse(name.contains("root:"))
        }
    }

    @Test
    fun malformedXml_returnsErrorResultWithoutCrashing() {
        val corruptedXml = "<gpx><trk><unclosed></trk>"
        val parseResult = GpxImporter.parse(ByteArrayInputStream(corruptedXml.toByteArray(Charsets.UTF_8)))
        assertTrue(parseResult.errors.isNotEmpty())
    }

    @Test
    fun multipleTracks_parsesAllTracks() {
        val multiGpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
                <trk>
                    <name>Track 1</name>
                    <trkseg>
                        <trkpt lat="12.9716" lon="77.5946"><time>2023-11-14T22:13:20Z</time></trkpt>
                    </trkseg>
                </trk>
                <trk>
                    <name>Track 2</name>
                    <trkseg>
                        <trkpt lat="13.0000" lon="77.6000"><time>2023-11-14T23:00:00Z</time></trkpt>
                    </trkseg>
                </trk>
            </gpx>
        """.trimIndent()

        val parseResult = GpxImporter.parse(ByteArrayInputStream(multiGpx.toByteArray(Charsets.UTF_8)))
        assertTrue(parseResult.errors.isEmpty())
        assertEquals(2, parseResult.tracks.size)
        assertEquals("Track 1", parseResult.tracks[0].name)
        assertEquals("Track 2", parseResult.tracks[1].name)
    }
}
