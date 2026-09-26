package com.abrar.motolog.data.io

import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import com.abrar.motolog.shared.domain.TrackingConstants
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Streams rides and GPS points into GPX 1.1 compliant XML.
 *
 * Designed to write directly to an [OutputStream] with buffering to prevent
 * loading entire datasets into heap memory.
 */
object GpxExporter {

    private val iso8601Format: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    /**
     * Escapes standard XML characters.
     */
    fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Exports a single ride with points to an [OutputStream].
     */
    fun exportRide(
        ride: RideEntity,
        points: List<RidePointEntity>,
        outputStream: OutputStream,
        onProgress: ((Float) -> Unit)? = null
    ) {
        exportRides(listOf(Pair(ride, points)), outputStream, onProgress)
    }

    /**
     * Exports multiple rides into a single GPX document with multiple <trk> tracks.
     */
    fun exportRides(
        ridesWithPoints: List<Pair<RideEntity, List<RidePointEntity>>>,
        outputStream: OutputStream,
        onProgress: ((Float) -> Unit)? = null
    ) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        val totalPoints = ridesWithPoints.sumOf { it.second.size }.coerceAtLeast(1)
        var pointsWritten = 0

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        writer.write("<gpx version=\"1.1\" creator=\"MotoLog\" ")
        writer.write("xmlns=\"http://www.topografix.com/GPX/1/1\" ")
        writer.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
        writer.write("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")

        val firstRide = ridesWithPoints.firstOrNull()?.first
        val metaTime = firstRide?.let { iso8601Format.format(Date(it.startTime)) }
            ?: iso8601Format.format(Date())

        writer.write("  <metadata>\n")
        writer.write("    <name>${escapeXml(firstRide?.name ?: "MotoLog Export")}</name>\n")
        writer.write("    <time>$metaTime</time>\n")
        writer.write("  </metadata>\n")

        val dateFormat = iso8601Format

        for ((ride, points) in ridesWithPoints) {
            writer.write("  <trk>\n")
            writer.write("    <name>${escapeXml(ride.name)}</name>\n")

            if (points.isNotEmpty()) {
                writer.write("    <trkseg>\n")
                var inSegment = true
                var lastPointTime: Long? = null

                for (point in points) {
                    val deltaMs = lastPointTime?.let { point.timestamp - it } ?: 0L
                    val isGap = point.isGap || deltaMs > TrackingConstants.GAP_THRESHOLD_MS

                    if (isGap && inSegment && lastPointTime != null) {
                        // Close segment across gap and open a new one
                        writer.write("    </trkseg>\n")
                        writer.write("    <trkseg>\n")
                    }

                    val lat = String.format(Locale.US, "%.6f", point.latitude)
                    val lon = String.format(Locale.US, "%.6f", point.longitude)
                    writer.write("      <trkpt lat=\"$lat\" lon=\"$lon\">\n")

                    if (point.altitudeMeters > 0.0) {
                        writer.write("        <ele>${String.format(Locale.US, "%.1f", point.altitudeMeters)}</ele>\n")
                    }

                    writer.write("        <time>${dateFormat.format(Date(point.timestamp))}</time>\n")

                    writer.write("        <extensions>\n")
                    writer.write("          <speed>${String.format(Locale.US, "%.2f", point.speedMs)}</speed>\n")
                    writer.write("          <accuracy>${String.format(Locale.US, "%.1f", point.accuracyMeters)}</accuracy>\n")
                    writer.write("        </extensions>\n")

                    writer.write("      </trkpt>\n")
                    lastPointTime = point.timestamp
                    pointsWritten++

                    if (pointsWritten % 50 == 0) {
                        onProgress?.invoke(pointsWritten.toFloat() / totalPoints)
                    }
                }

                if (inSegment) {
                    writer.write("    </trkseg>\n")
                }
            }

            writer.write("  </trk>\n")
        }

        writer.write("</gpx>\n")
        writer.flush()
        onProgress?.invoke(1.0f)
    }
}
