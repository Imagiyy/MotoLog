package com.abrar.motolog.data.io

import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.RideStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class GpxExporterTest {

    @Test
    fun gpxExport_writesValidGpx11WithGapSplitting() {
        val ride = RideEntity(
            id = 10L,
            startTime = 1700000000000L,
            endTime = 1700000010000L,
            distanceMeters = 200.0,
            elapsedTimeMs = 10000L,
            movingTimeMs = 10000L,
            avgMovingSpeedMs = 20.0,
            overallAvgSpeedMs = 20.0,
            maxSpeedMs = 25.0,
            name = "Morning Highway Sprint",
            status = RideStatus.COMPLETED
        )

        val points = listOf(
            RidePointEntity(1L, 10L, 1700000000000L, 12.9716, 77.5946, 20.0, 4f, 920.0, false, false),
            RidePointEntity(2L, 10L, 1700000001000L, 12.9718, 77.5946, 21.0, 4f, 921.0, false, false),
            // Gap point (tunnel exit or signal loss)
            RidePointEntity(3L, 10L, 1700000005000L, 12.9725, 77.5946, 22.0, 4f, 922.0, false, true),
            RidePointEntity(4L, 10L, 1700000006000L, 12.9727, 77.5946, 23.0, 4f, 923.0, false, false)
        )

        val out = ByteArrayOutputStream()
        GpxExporter.exportRide(ride, points, out)
        val gpxXml = out.toString(Charsets.UTF_8.name())

        // Verify GPX headers
        assertTrue(gpxXml.contains("<gpx version=\"1.1\" creator=\"MotoLog\""))
        assertTrue(gpxXml.contains("<name>Morning Highway Sprint</name>"))

        // Verify segments split at gap
        // There should be 2 <trkseg> blocks because point 3 has isGap=true
        val segStartCount = gpxXml.split("<trkseg>").size - 1
        val segEndCount = gpxXml.split("</trkseg>").size - 1
        assertEquals(2, segStartCount)
        assertEquals(2, segEndCount)

        // Verify trkpt attributes and extensions
        assertTrue(gpxXml.contains("<trkpt lat=\"12.971600\" lon=\"77.594600\">"))
        assertTrue(gpxXml.contains("<ele>920.0</ele>"))
        assertTrue(gpxXml.contains("<time>"))
        assertTrue(gpxXml.contains("<speed>20.00</speed>"))
        assertTrue(gpxXml.contains("<accuracy>4.0</accuracy>"))
    }
}
