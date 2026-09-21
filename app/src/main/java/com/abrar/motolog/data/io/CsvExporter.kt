package com.abrar.motolog.data.io

import com.abrar.motolog.data.local.entity.FuelLogEntity
import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.data.local.entity.RidePointEntity
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Generates RFC 4180 compliant CSV files with spreadsheet formula injection protection.
 */
object CsvExporter {

    private val iso8601Format: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    /**
     * Sanitizes and escapes text for CSV cells:
     * - Neutralizes formula injection by prefixing '=', '+', '-', '@', '\t', '\r' with a single quote
     * - Doubles existing double quotes
     * - Surrounds with double quotes if contains commas, quotes, or newlines
     */
    fun escapeCsv(raw: String?): String {
        if (raw == null) return ""
        var text = raw

        // Formula injection mitigation (OWASP / RFC 4180 guidelines)
        if (text.isNotEmpty()) {
            val firstChar = text[0]
            if (firstChar == '=' || firstChar == '+' || firstChar == '-' ||
                firstChar == '@' || firstChar == '\t' || firstChar == '\r'
            ) {
                text = "'$text"
            }
        }

        val needsQuotes = text.contains(",") || text.contains("\"") ||
                text.contains("\n") || text.contains("\r")

        val escaped = text.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    /**
     * Exports a summary of rides (one row per ride).
     */
    fun exportRidesSummary(rides: List<RideEntity>, outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        val dateFormat = iso8601Format

        writer.write("ride_id,name,start_time_utc,end_time_utc,distance_meters,elapsed_time_ms,moving_time_ms,stopped_time_ms,avg_moving_speed_kmh,overall_avg_speed_kmh,max_speed_kmh,elevation_gain_meters,bike_id,is_imported\n")

        for (ride in rides) {
            val startIso = dateFormat.format(Date(ride.startTime))
            val endIso = if (ride.endTime > 0) dateFormat.format(Date(ride.endTime)) else ""
            val dist = String.format(Locale.US, "%.1f", ride.distanceMeters)
            val avgMovKmh = ride.avgMovingSpeedMs * 3.6
            val avgOverKmh = ride.overallAvgSpeedMs * 3.6
            val maxSpdKmh = ride.maxSpeedMs * 3.6
            val stoppedMs = (ride.elapsedTimeMs - ride.movingTimeMs).coerceAtLeast(0L)
            val avgMov = String.format(Locale.US, "%.2f", avgMovKmh)
            val avgOver = String.format(Locale.US, "%.2f", avgOverKmh)
            val maxSpd = String.format(Locale.US, "%.2f", maxSpdKmh)
            val elev = String.format(Locale.US, "%.1f", ride.elevationGainMeters)

            writer.write("${ride.id},${escapeCsv(ride.name)},$startIso,$endIso,$dist,${ride.elapsedTimeMs},${ride.movingTimeMs},$stoppedMs,$avgMov,$avgOver,$maxSpd,$elev,${ride.bikeId ?: ""},${ride.isImported}\n")
        }

        writer.flush()
    }

    /**
     * Exports raw GPS points for a single ride.
     */
    fun exportRidePoints(points: List<RidePointEntity>, outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        val dateFormat = iso8601Format

        writer.write("timestamp_epoch_ms,timestamp_utc,latitude,longitude,speed_mps,accuracy_meters,altitude_meters,is_paused,is_gap\n")

        for (point in points) {
            val isoTime = dateFormat.format(Date(point.timestamp))
            val lat = String.format(Locale.US, "%.6f", point.latitude)
            val lon = String.format(Locale.US, "%.6f", point.longitude)
            val spd = String.format(Locale.US, "%.2f", point.speedMs)
            val acc = String.format(Locale.US, "%.1f", point.accuracyMeters)
            val alt = String.format(Locale.US, "%.1f", point.altitudeMeters)

            writer.write("${point.timestamp},$isoTime,$lat,$lon,$spd,$acc,$alt,${point.isPaused},${point.isGap}\n")
        }

        writer.flush()
    }

    /**
     * Exports fuel logs for garage records.
     */
    fun exportFuelLogs(logs: List<FuelLogEntity>, outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        val dateFormat = iso8601Format

        writer.write("log_id,bike_id,date_utc,odometer_km,litres,total_cost,full_tank\n")

        for (log in logs) {
            val dateIso = dateFormat.format(Date(log.timestampEpochMs))
            val odo = String.format(Locale.US, "%.1f", log.odometerKm)
            val litres = String.format(Locale.US, "%.2f", log.litres)
            val cost = String.format(Locale.US, "%.2f", log.totalCost)

            writer.write("${log.id},${log.bikeId},$dateIso,$odo,$litres,$cost,${log.isFullTank}\n")
        }

        writer.flush()
    }

    /**
     * Exports maintenance items.
     */
    fun exportMaintenance(items: List<MaintenanceItemEntity>, outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        val dateFormat = iso8601Format

        writer.write("item_id,bike_id,name,interval_km,interval_days,last_done_odometer_km,last_done_date_utc\n")

        for (item in items) {
            val intervalKm = item.intervalKm?.let { String.format(Locale.US, "%.1f", it) } ?: ""
            val intervalDays = item.intervalDays?.toString() ?: ""
            val lastOdo = String.format(Locale.US, "%.1f", item.lastDoneOdometerKm)
            val lastDate = dateFormat.format(Date(item.lastDoneDateEpochMs))

            writer.write("${item.id},${item.bikeId},${escapeCsv(item.name)},$intervalKm,$intervalDays,$lastOdo,$lastDate\n")
        }

        writer.flush()
    }
}
