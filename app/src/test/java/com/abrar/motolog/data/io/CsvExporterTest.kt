package com.abrar.motolog.data.io

import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.data.local.entity.RideStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Locale

class CsvExporterTest {

    @Test
    fun formulaInjectionProtection_neutralizesDangerousPrefixes() {
        assertEquals("'=cmd|' /C calc'!A0", CsvExporter.escapeCsv("=cmd|' /C calc'!A0"))
        assertEquals("'+12345", CsvExporter.escapeCsv("+12345"))
        assertEquals("'-SOMETHING", CsvExporter.escapeCsv("-SOMETHING"))
        assertEquals("'@SUM(A1:A10)", CsvExporter.escapeCsv("@SUM(A1:A10)"))
        assertEquals("'\tTABBED", CsvExporter.escapeCsv("\tTABBED"))
        assertEquals("\"'\rRETURN\"", CsvExporter.escapeCsv("\rRETURN"))
        assertEquals("Safe Ride Name", CsvExporter.escapeCsv("Safe Ride Name"))
        assertEquals("\"Quotes \"\"inside\"\"\"", CsvExporter.escapeCsv("Quotes \"inside\""))
    }

    @Test
    fun ridesSummaryCsv_usesDotDecimalsAndProperQuoting() {
        val originalLocale = Locale.getDefault()
        try {
            // Force comma-decimal locale like Germany/France
            Locale.setDefault(Locale.GERMANY)

            val ride = RideEntity(
                id = 1L,
                startTime = 1700000000000L,
                endTime = 1700003600000L,
                distanceMeters = 54321.678,
                elapsedTimeMs = 3600000L,
                movingTimeMs = 3000000L,
                avgMovingSpeedMs = 18.107,
                overallAvgSpeedMs = 15.089,
                maxSpeedMs = 28.5,
                elevationGainMeters = 350.2,
                elevationLossMeters = 345.1,
                elevationSource = "barometer",
                name = "=MaliciousRide, with comma",
                status = RideStatus.COMPLETED
            )

            val outputStream = ByteArrayOutputStream()
            CsvExporter.exportRidesSummary(listOf(ride), outputStream)
            val csv = outputStream.toString(Charsets.UTF_8.name())

            val lines = csv.trim().lines()
            assertEquals(2, lines.size)
            assertTrue(lines[0].startsWith("ride_id,name,start_time_utc,end_time_utc,distance_meters"))

            val row = lines[1]
            // Name should be sanitized with ' and quoted due to comma
            assertTrue(row.contains("\"'=MaliciousRide, with comma\""))
            // Numbers must use dot decimals (e.g. 54321.7), never comma decimals
            assertTrue(row.contains("54321.7"))
            assertFalse(row.contains("54321,7"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun ridePointsCsv_formatsGpsPointsCorrectly() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)

            val points = listOf(
                RidePointEntity(
                    id = 1L,
                    rideId = 42L,
                    timestamp = 1700000000000L,
                    latitude = 12.9715987,
                    longitude = 77.5945627,
                    speedMs = 20.5,
                    accuracyMeters = 4.2f,
                    altitudeMeters = 920.5,
                    isPaused = false,
                    isGap = false
                ),
                RidePointEntity(
                    id = 2L,
                    rideId = 42L,
                    timestamp = 1700000001000L,
                    latitude = 12.9717000,
                    longitude = 77.5946000,
                    speedMs = 22.0,
                    accuracyMeters = 3.8f,
                    altitudeMeters = 921.0,
                    isPaused = false,
                    isGap = false
                )
            )

            val outputStream = ByteArrayOutputStream()
            CsvExporter.exportRidePoints(points, outputStream)
            val csv = outputStream.toString(Charsets.UTF_8.name())

            val lines = csv.trim().lines()
            assertEquals(3, lines.size)
            assertTrue(lines[0].contains("latitude,longitude,speed_mps,accuracy_meters"))
            // Verify dot decimals
            val row = lines[1]
            assertTrue(row.contains("12.971599,77.594563"))
            assertFalse(row.contains("12,971599"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
