package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.PlannedRoute
import com.abrar.motolog.shared.domain.model.RouteProgress
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure Kotlin deterministic route progress and off-route state machine.
 *
 * Implements:
 * 1. Nearest-segment matching using local equirectangular planar projection.
 * 2. Forward-compatible search window to avoid backwards jumps or cross-talk on parallel roads.
 * 3. Monotonic along-route distance tracking with GPS jitter suppression.
 * 4. Dual-mode arrival detection (immediate close proximity + confirmed arrival with low speed).
 * 5. Time-sustained off-route warning (40m for 5s) and automatic reroute trigger (80m for 10s).
 * 6. Reroute cooldown to prevent request loops and provider quota waste.
 * 7. Graceful handling of degraded GPS fixes and signal loss.
 *
 * No Android dependencies — fully unit-testable across JVM and iOS.
 */
class RouteProgressEngine(
    private val arrivalRadiusMeters: Double = TrackingConstants.NAV_ARRIVAL_RADIUS_METERS,
    private val arrivalSpeedConfirmationKmh: Double = TrackingConstants.NAV_ARRIVAL_SPEED_CONFIRMATION_KMH,
    private val immediateArrivalRadiusMeters: Double = TrackingConstants.NAV_IMMEDIATE_ARRIVAL_RADIUS_METERS,
    private val offRouteWarningMeters: Double = TrackingConstants.NAV_OFF_ROUTE_WARNING_METERS,
    private val offRouteWarningDelaySeconds: Int = TrackingConstants.NAV_OFF_ROUTE_WARNING_DELAY_SECONDS,
    private val offRouteRerouteMeters: Double = TrackingConstants.NAV_OFF_ROUTE_REROUTE_METERS,
    private val offRouteRerouteDelaySeconds: Int = TrackingConstants.NAV_OFF_ROUTE_REROUTE_DELAY_SECONDS,
    private val rerouteCooldownSeconds: Long = TrackingConstants.NAV_REROUTE_COOLDOWN_SECONDS,
    private val lookaheadSegments: Int = TrackingConstants.NAV_SEGMENT_LOOKAHEAD_COUNT,
    private val jitterToleranceMeters: Double = TrackingConstants.NAV_PROGRESS_JITTER_TOLERANCE_METERS
) {

    var currentRoute: PlannedRoute? = null
        private set

    var lastProgress: RouteProgress? = null
        private set

    private var cumulativeDistances: DoubleArray = DoubleArray(0)
    private var totalDistanceMeters: Double = 0.0

    private var lastMatchedSegmentIndex: Int = 0
    private var maxDistanceAlongRouteMeters: Double = 0.0

    private var warningStartTimeMs: Long? = null
    private var rerouteStartTimeMs: Long? = null
    private var lastRerouteTimestampMs: Long? = null

    /**
     * Set or replace the active navigation route.
     * Precomputes cumulative segment distances and initializes tracking state.
     */
    fun setRoute(route: PlannedRoute) {
        currentRoute = route
        val geometry = route.geometry
        if (geometry.size < 2) {
            cumulativeDistances = DoubleArray(geometry.size) { 0.0 }
            totalDistanceMeters = route.totalDistanceMeters
        } else {
            cumulativeDistances = DoubleArray(geometry.size)
            cumulativeDistances[0] = 0.0
            for (i in 0 until geometry.size - 1) {
                val segLen = RideCalculator.haversineDistanceMeters(
                    geometry[i].latitude, geometry[i].longitude,
                    geometry[i + 1].latitude, geometry[i + 1].longitude
                )
                cumulativeDistances[i + 1] = cumulativeDistances[i] + segLen
            }
            totalDistanceMeters = if (route.totalDistanceMeters > 0.0) {
                route.totalDistanceMeters
            } else {
                cumulativeDistances.last()
            }
        }

        lastMatchedSegmentIndex = 0
        maxDistanceAlongRouteMeters = 0.0
        warningStartTimeMs = null
        rerouteStartTimeMs = null
        lastRerouteTimestampMs = null

        lastProgress = RouteProgress(
            nearestPointIndex = 0,
            distanceAlongRouteMeters = 0.0,
            remainingDistanceMeters = totalDistanceMeters,
            progressFraction = 0f,
            crossTrackDistanceMeters = 0.0,
            status = NavigationStatus.ON_ROUTE,
            currentInstructionIndex = if (route.instructions.isNotEmpty()) 0 else null,
            timestamp = route.calculatedAtTimestamp
        )
    }

    /**
     * Process an incoming GPS location fix and return updated [RouteProgress].
     */
    fun onLocationUpdate(point: LocationPoint): RouteProgress {
        val route = currentRoute
        if (route == null || route.geometry.isEmpty()) {
            return RouteProgress(
                nearestPointIndex = 0,
                distanceAlongRouteMeters = 0.0,
                remainingDistanceMeters = 0.0,
                progressFraction = 1f,
                crossTrackDistanceMeters = 0.0,
                status = NavigationStatus.ARRIVED,
                timestamp = point.timestamp
            )
        }

        val nowMs = point.elapsedTimestampMs ?: point.timestamp

        // Filter: Reject low-accuracy fixes from affecting route progress
        if (point.accuracyMeters > TrackingConstants.MIN_GPS_ACCURACY_METERS) {
            val previous = lastProgress
            return if (previous != null) {
                val paused = previous.copy(
                    status = NavigationStatus.PAUSED_NO_GPS,
                    timestamp = point.timestamp
                )
                lastProgress = paused
                paused
            } else {
                val initial = RouteProgress(
                    nearestPointIndex = 0,
                    distanceAlongRouteMeters = 0.0,
                    remainingDistanceMeters = totalDistanceMeters,
                    progressFraction = 0f,
                    crossTrackDistanceMeters = 0.0,
                    status = NavigationStatus.PAUSED_NO_GPS,
                    timestamp = point.timestamp
                )
                lastProgress = initial
                initial
            }
        }

        val geometry = route.geometry

        // 1. Arrival Check
        val destination = geometry.last()
        val distanceToDest = RideCalculator.haversineDistanceMeters(
            point.latitude, point.longitude,
            destination.latitude, destination.longitude
        )
        val speedKmh = if (point.speedMps != null) point.speedMps * 3.6 else 0.0

        val hasArrived = distanceToDest <= immediateArrivalRadiusMeters ||
                (distanceToDest <= arrivalRadiusMeters && speedKmh <= arrivalSpeedConfirmationKmh)

        if (hasArrived) {
            val arrivedProgress = RouteProgress(
                nearestPointIndex = geometry.size - 1,
                distanceAlongRouteMeters = totalDistanceMeters,
                remainingDistanceMeters = 0.0,
                progressFraction = 1.0f,
                crossTrackDistanceMeters = distanceToDest,
                status = NavigationStatus.ARRIVED,
                currentInstructionIndex = if (route.instructions.isNotEmpty()) route.instructions.size - 1 else null,
                timestamp = point.timestamp
            )
            lastProgress = arrivedProgress
            return arrivedProgress
        }

        // If previously arrived, remain arrived
        if (lastProgress?.status == NavigationStatus.ARRIVED) {
            return lastProgress!!
        }

        // 2. Segment Matching
        val segmentCount = geometry.size - 1
        val matchResult: SegmentMatchResult = if (segmentCount <= 0) {
            SegmentMatchResult(0, 0.0, distanceToDest, 0.0)
        } else {
            val startSearch = max(0, lastMatchedSegmentIndex - 1)
            val endSearch = min(segmentCount - 1, lastMatchedSegmentIndex + lookaheadSegments)

            var best = findBestSegmentInRange(point, geometry, startSearch..endSearch)

            // If look-ahead candidate cross-track is beyond reroute threshold, expand search to entire route
            if (best.crossTrackDistanceMeters > offRouteRerouteMeters) {
                val fullBest = findBestSegmentInRange(point, geometry, 0 until segmentCount)
                if (fullBest.crossTrackDistanceMeters < best.crossTrackDistanceMeters) {
                    best = fullBest
                }
            }
            best
        }

        lastMatchedSegmentIndex = matchResult.segmentIndex

        // 3. Distance Calculation & Jitter Resistance
        var distanceAlongRoute = matchResult.distanceAlongRouteMeters
        if (distanceAlongRoute >= maxDistanceAlongRouteMeters) {
            maxDistanceAlongRouteMeters = distanceAlongRoute
        } else if (distanceAlongRoute >= maxDistanceAlongRouteMeters - jitterToleranceMeters) {
            // Minor backward GPS jitter: clamp to highest achieved progress
            distanceAlongRoute = maxDistanceAlongRouteMeters
        } else if (matchResult.crossTrackDistanceMeters <= offRouteWarningMeters) {
            // Sustained backward movement on route: update max
            maxDistanceAlongRouteMeters = distanceAlongRoute
        }

        val remainingDistance = max(0.0, totalDistanceMeters - distanceAlongRoute)
        val progressFraction = if (totalDistanceMeters > 0.0) {
            (distanceAlongRoute / totalDistanceMeters).toFloat().coerceIn(0f, 1f)
        } else {
            1f
        }

        // 4. Instruction Matching
        val currentInstructionIdx = findCurrentInstructionIndex(
            instructions = route.instructions,
            currentSegmentIndex = matchResult.segmentIndex
        )

        // 5. Off-Route Evaluation and Hysteresis
        val crossTrack = matchResult.crossTrackDistanceMeters
        val status: NavigationStatus = when {
            crossTrack <= offRouteWarningMeters -> {
                warningStartTimeMs = null
                rerouteStartTimeMs = null
                NavigationStatus.ON_ROUTE
            }

            crossTrack > offRouteRerouteMeters -> {
                warningStartTimeMs = null
                val rerouteStart = rerouteStartTimeMs
                if (rerouteStart == null) {
                    rerouteStartTimeMs = nowMs
                    NavigationStatus.OFF_ROUTE_WARNING
                } else if (nowMs - rerouteStart >= offRouteRerouteDelaySeconds * 1000L) {
                    // Check reroute cooldown
                    val lastReroute = lastRerouteTimestampMs
                    if (lastReroute == null || nowMs - lastReroute >= rerouteCooldownSeconds * 1000L) {
                        NavigationStatus.OFF_ROUTE_REROUTE_NEEDED
                    } else {
                        NavigationStatus.OFF_ROUTE_WARNING
                    }
                } else {
                    NavigationStatus.OFF_ROUTE_WARNING
                }
            }

            else -> {
                // Between 40m and 80m
                rerouteStartTimeMs = null
                val warningStart = warningStartTimeMs
                if (warningStart == null) {
                    warningStartTimeMs = nowMs
                    NavigationStatus.ON_ROUTE
                } else if (nowMs - warningStart >= offRouteWarningDelaySeconds * 1000L) {
                    NavigationStatus.OFF_ROUTE_WARNING
                } else {
                    NavigationStatus.ON_ROUTE
                }
            }
        }

        val progress = RouteProgress(
            nearestPointIndex = matchResult.segmentIndex,
            distanceAlongRouteMeters = distanceAlongRoute,
            remainingDistanceMeters = remainingDistance,
            progressFraction = progressFraction,
            crossTrackDistanceMeters = crossTrack,
            status = status,
            currentInstructionIndex = currentInstructionIdx,
            timestamp = point.timestamp
        )

        lastProgress = progress
        return progress
    }

    /**
     * Mark that a reroute was triggered or completed at [timestampMs].
     * Starts the reroute cooldown and clears deviation timers.
     */
    fun markRerouteTriggered(timestampMs: Long) {
        lastRerouteTimestampMs = timestampMs
        rerouteStartTimeMs = null
        warningStartTimeMs = null
    }

    /**
     * Notify that GPS signal was lost. Pauses progress updates.
     */
    fun onGpsLost(timestampMs: Long): RouteProgress {
        val previous = lastProgress
        val updated = if (previous != null) {
            previous.copy(
                status = NavigationStatus.PAUSED_NO_GPS,
                timestamp = timestampMs
            )
        } else {
            RouteProgress(
                nearestPointIndex = 0,
                distanceAlongRouteMeters = 0.0,
                remainingDistanceMeters = totalDistanceMeters,
                progressFraction = 0f,
                crossTrackDistanceMeters = 0.0,
                status = NavigationStatus.PAUSED_NO_GPS,
                timestamp = timestampMs
            )
        }
        lastProgress = updated
        return updated
    }

    /**
     * Reset the progress engine state.
     */
    fun reset() {
        currentRoute = null
        lastProgress = null
        cumulativeDistances = DoubleArray(0)
        totalDistanceMeters = 0.0
        lastMatchedSegmentIndex = 0
        maxDistanceAlongRouteMeters = 0.0
        warningStartTimeMs = null
        rerouteStartTimeMs = null
        lastRerouteTimestampMs = null
    }

    private fun findBestSegmentInRange(
        point: LocationPoint,
        geometry: List<LocationPoint>,
        range: IntProgression
    ): SegmentMatchResult {
        var bestIndex = range.first
        var bestFactor = 0.0
        var minCrossTrack = Double.MAX_VALUE
        var bestDistanceAlongRoute = 0.0

        for (i in range) {
            if (i < 0 || i >= geometry.size - 1) continue
            val p1 = geometry[i]
            val p2 = geometry[i + 1]

            val proj = projectPointOnSegment(
                pointLat = point.latitude, pointLon = point.longitude,
                startLat = p1.latitude, startLon = p1.longitude,
                endLat = p2.latitude, endLon = p2.longitude
            )

            // Favor forward progression slightly if distances are nearly equal
            val isSlightlyBetter = proj.crossTrackDistanceMeters < minCrossTrack - 0.5 ||
                    (proj.crossTrackDistanceMeters <= minCrossTrack + 0.5 && i >= lastMatchedSegmentIndex)

            if (isSlightlyBetter) {
                minCrossTrack = proj.crossTrackDistanceMeters
                bestIndex = i
                bestFactor = proj.factor
                val segLen = if (i + 1 < cumulativeDistances.size) {
                    cumulativeDistances[i + 1] - cumulativeDistances[i]
                } else {
                    proj.distanceAlongSegmentMeters
                }
                bestDistanceAlongRoute = (if (i < cumulativeDistances.size) cumulativeDistances[i] else 0.0) +
                        bestFactor * segLen
            }
        }

        return SegmentMatchResult(
            segmentIndex = bestIndex,
            factor = bestFactor,
            crossTrackDistanceMeters = minCrossTrack,
            distanceAlongRouteMeters = bestDistanceAlongRoute
        )
    }

    private fun findCurrentInstructionIndex(
        instructions: List<com.abrar.motolog.shared.domain.model.RouteInstruction>,
        currentSegmentIndex: Int
    ): Int? {
        if (instructions.isEmpty()) return null
        for (i in instructions.indices) {
            if (instructions[i].pointIndex >= currentSegmentIndex) {
                return i
            }
        }
        return instructions.size - 1
    }

    companion object {
        private const val EARTH_RADIUS = 6_371_000.0

        private fun toRadians(deg: Double): Double = deg / 180.0 * PI

        /**
         * Project a point onto a line segment in local equirectangular planar coordinates.
         */
        fun projectPointOnSegment(
            pointLat: Double, pointLon: Double,
            startLat: Double, startLon: Double,
            endLat: Double, endLon: Double
        ): ProjectionResult {
            val avgLat = toRadians((startLat + endLat) / 2.0)
            val cosLat = cos(avgLat)

            val x = toRadians(pointLon - startLon) * cosLat * EARTH_RADIUS
            val y = toRadians(pointLat - startLat) * EARTH_RADIUS

            val x2 = toRadians(endLon - startLon) * cosLat * EARTH_RADIUS
            val y2 = toRadians(endLat - startLat) * EARTH_RADIUS

            val lineLenSq = x2 * x2 + y2 * y2
            val segLen = sqrt(lineLenSq)

            if (lineLenSq < 1e-10) {
                // Degenerate segment: start and end are identical
                val dist = sqrt(x * x + y * y)
                return ProjectionResult(
                    factor = 0.0,
                    projectedLat = startLat,
                    projectedLon = startLon,
                    crossTrackDistanceMeters = dist,
                    distanceAlongSegmentMeters = 0.0
                )
            }

            val t = ((x * x2 + y * y2) / lineLenSq).coerceIn(0.0, 1.0)
            val qx = t * x2
            val qy = t * y2

            val crossTrack = sqrt((x - qx) * (x - qx) + (y - qy) * (y - qy))
            val projLat = startLat + t * (endLat - startLat)
            val projLon = startLon + t * (endLon - startLon)

            return ProjectionResult(
                factor = t,
                projectedLat = projLat,
                projectedLon = projLon,
                crossTrackDistanceMeters = crossTrack,
                distanceAlongSegmentMeters = t * segLen
            )
        }
    }

    data class ProjectionResult(
        val factor: Double,
        val projectedLat: Double,
        val projectedLon: Double,
        val crossTrackDistanceMeters: Double,
        val distanceAlongSegmentMeters: Double
    )

    private data class SegmentMatchResult(
        val segmentIndex: Int,
        val factor: Double,
        val crossTrackDistanceMeters: Double,
        val distanceAlongRouteMeters: Double
    )
}
