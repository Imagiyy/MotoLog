package com.abrar.motolog.shared.test

import com.abrar.motolog.shared.domain.model.GpsPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure Kotlin in-memory deterministic GPX fixture generator for commonTest.
 * Matches analytical models in FixtureGenerator without requiring XML parsing or file I/O.
 */
object FixturePoints {

    private const val BASE_LAT = 12.971600
    private const val BASE_LON = 77.594600
    private const val METERS_PER_DEGREE_LAT = 111_139.0

    private fun toRadians(deg: Double): Double = deg / 180.0 * PI

    fun stationaryJitter(): List<GpsPoint> {
        val startEpoch = 1774000000000L
        val points = ArrayList<GpsPoint>(300)

        for (i in 0 until 300) {
            val t = startEpoch + (i * 1000L)
            val latOffsetMeters = 1.2 * sin(i * 0.1)
            val lonOffsetMeters = 1.0 * cos(i * 0.15)
            val lat = BASE_LAT + (latOffsetMeters / METERS_PER_DEGREE_LAT)
            val lon = BASE_LON + (lonOffsetMeters / (METERS_PER_DEGREE_LAT * cos(toRadians(BASE_LAT))))
            val speedMps = (0.15 + 0.1 * sin(i * 0.2)).toFloat()
            val accuracy = (8.0 + 3.0 * cos(i * 0.05)).toFloat()

            points.add(
                GpsPoint(
                    timestampEpochMs = t,
                    latitude = lat,
                    longitude = lon,
                    speedMps = speedMps,
                    accuracyMeters = accuracy,
                    speedAccuracyMps = 0.5f,
                    altitudeMeters = 920.0
                )
            )
        }
        return points
    }

    fun highwayRide(): List<GpsPoint> {
        val startEpoch = 1774000000000L
        val points = ArrayList<GpsPoint>(1200)
        val speedMps = 25.0f

        for (i in 0 until 1200) {
            val t = startEpoch + (i * 1000L)
            val distanceMeters = i * 25.0
            val lat = BASE_LAT + (distanceMeters / METERS_PER_DEGREE_LAT)
            val lon = BASE_LON

            points.add(
                GpsPoint(
                    timestampEpochMs = t,
                    latitude = lat,
                    longitude = lon,
                    speedMps = speedMps,
                    accuracyMeters = 5.0f,
                    speedAccuracyMps = 0.2f,
                    altitudeMeters = 850.0
                )
            )
        }
        return points
    }

    fun stopAndGo(): List<GpsPoint> {
        val startEpoch = 1774000000000L
        val points = ArrayList<GpsPoint>(600)
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

            points.add(
                GpsPoint(
                    timestampEpochMs = t,
                    latitude = lat,
                    longitude = lon,
                    speedMps = speedMps,
                    accuracyMeters = 6.0f,
                    speedAccuracyMps = 0.3f,
                    altitudeMeters = 900.0
                )
            )
        }
        return points
    }

    fun tunnelGap(): List<GpsPoint> {
        val startEpoch = 1774000000000L
        val points = ArrayList<GpsPoint>(600)
        val speedMps = 25.0f

        // Phase 1: 300s before tunnel
        for (i in 0 until 300) {
            val t = startEpoch + (i * 1000L)
            val dist = i * 25.0
            val lat = BASE_LAT + (dist / METERS_PER_DEGREE_LAT)
            points.add(
                GpsPoint(
                    timestampEpochMs = t,
                    latitude = lat,
                    longitude = BASE_LON,
                    speedMps = speedMps,
                    accuracyMeters = 5.0f,
                    speedAccuracyMps = 0.2f,
                    altitudeMeters = 800.0
                )
            )
        }

        // Tunnel from t = 300s to 345s
        val postTunnelStartEpoch = startEpoch + (345 * 1000L)
        val postTunnelBaseDist = (300 * 25.0) + (45 * 25.0)

        // Phase 2: 300s after tunnel
        for (i in 0 until 300) {
            val t = postTunnelStartEpoch + (i * 1000L)
            val dist = postTunnelBaseDist + (i * 25.0)
            val lat = BASE_LAT + (dist / METERS_PER_DEGREE_LAT)
            points.add(
                GpsPoint(
                    timestampEpochMs = t,
                    latitude = lat,
                    longitude = BASE_LON,
                    speedMps = speedMps,
                    accuracyMeters = 5.0f,
                    speedAccuracyMps = 0.2f,
                    altitudeMeters = 800.0
                )
            )
        }
        return points
    }

    fun cityRide(): List<GpsPoint> {
        val startEpoch = 1774000000000L
        val points = ArrayList<GpsPoint>(900)
        var dist = 0.0

        for (i in 0 until 900) {
            val t = startEpoch + (i * 1000L)
            val isStopped = (i in 180 until 225) || (i in 400 until 445) || (i in 650 until 695)
            val speedMps = if (isStopped) 0.0f else 15.0f

            if (!isStopped && i > 0 && points.isNotEmpty()) {
                dist += 15.0
            }

            val lat = BASE_LAT + (dist / METERS_PER_DEGREE_LAT)
            points.add(
                GpsPoint(
                    timestampEpochMs = t,
                    latitude = lat,
                    longitude = BASE_LON,
                    speedMps = speedMps,
                    accuracyMeters = 7.0f,
                    speedAccuracyMps = 0.4f,
                    altitudeMeters = 910.0
                )
            )
        }
        return points
    }
}
