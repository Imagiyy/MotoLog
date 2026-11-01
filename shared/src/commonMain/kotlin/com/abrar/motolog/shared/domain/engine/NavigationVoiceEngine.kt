package com.abrar.motolog.shared.domain.engine

import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.model.ManeuverType
import com.abrar.motolog.shared.domain.model.NavigationStatus
import com.abrar.motolog.shared.domain.model.RouteInstruction
import kotlin.math.roundToInt

/**
 * Announcement distance tier for upcoming maneuvers.
 */
enum class AnnouncementTier {
    FAR,
    NEAR,
    IMMINENT
}

/**
 * A formatted acoustic guidance announcement ready for speech synthesis.
 *
 * @property text The localized, natural voice prompt string to speak.
 * @property isUrgent If true, bypasses minimum interval cooldown and flushes audio queue.
 * @property tier The distance tier associated with this announcement, if maneuver-based.
 */
data class VoiceAnnouncement(
    val text: String,
    val isUrgent: Boolean = false,
    val tier: AnnouncementTier? = null
)

/**
 * Pure Kotlin decision and formatting engine for motorcycle navigation voice guidance.
 *
 * Evaluates approach distances, manages announcement cadence and tiers (FAR, NEAR, IMMINENT),
 * applies hysteresis to avoid repetitive chatter in the helmet, and translates maneuvers into
 * glance-free acoustic instructions in metric or imperial units.
 */
class NavigationVoiceEngine {

    private var activeInstructionIndex: Int? = null
    private val announcedTiers = mutableSetOf<AnnouncementTier>()
    private var lastAnnouncementTimestampMs: Long? = null
    private var lastAnnouncedStatus: NavigationStatus? = null

    /**
     * Reset internal state machine (e.g. on route change or navigation session end).
     */
    fun reset() {
        activeInstructionIndex = null
        announcedTiers.clear()
        lastAnnouncementTimestampMs = null
        lastAnnouncedStatus = null
    }

    /**
     * Notify engine that a new route has been successfully loaded following a recalculation.
     */
    fun onRerouteCompleted(nextStreetName: String? = null): VoiceAnnouncement {
        reset()
        val prompt = if (!nextStreetName.isNullOrBlank()) {
            "New route found. Continue on $nextStreetName."
        } else {
            "New route calculated."
        }
        return VoiceAnnouncement(text = prompt, isUrgent = true)
    }

    /**
     * Evaluate incoming navigation progress and determine whether a voice announcement
     * should be delivered.
     *
     * @param instruction The upcoming navigation instruction maneuver.
     * @param instructionIndex Index of the current instruction in the route list.
     * @param distanceToManeuverMeters Distance in meters remaining to the maneuver location.
     * @param currentSpeedKmh Current filtered GPS speed in km/h.
     * @param isMetric True to speak in meters/kilometers; false for feet/miles.
     * @param status Current navigation status from progress engine.
     * @param nowMs Current monotonic or epoch timestamp in milliseconds.
     * @return [VoiceAnnouncement] if an announcement is due, or null otherwise.
     */
    fun evaluateAnnouncement(
        instruction: RouteInstruction?,
        instructionIndex: Int?,
        distanceToManeuverMeters: Double,
        currentSpeedKmh: Double,
        isMetric: Boolean,
        status: NavigationStatus,
        nowMs: Long
    ): VoiceAnnouncement? {
        // 1. Status Transition Announcements (High Priority)
        val statusAnnouncement = evaluateStatusChange(status, nowMs)
        if (statusAnnouncement != null) {
            lastAnnouncementTimestampMs = nowMs
            return statusAnnouncement
        }

        if (status != NavigationStatus.ON_ROUTE || instruction == null || instructionIndex == null) {
            return null
        }

        // 2. Maneuver Index Transition
        if (activeInstructionIndex != instructionIndex) {
            activeInstructionIndex = instructionIndex
            announcedTiers.clear()
        }

        // 3. Dynamic speed-adjusted thresholds for motorcycle speeds
        val imminentThreshold = if (currentSpeedKmh > 60.0) 65.0 else TrackingConstants.NAV_VOICE_DISTANCE_IMMINENT_METERS
        val nearThreshold = if (currentSpeedKmh > 60.0) 250.0 else TrackingConstants.NAV_VOICE_DISTANCE_NEAR_METERS
        val farThreshold = if (currentSpeedKmh > 80.0) 800.0 else TrackingConstants.NAV_VOICE_DISTANCE_FAR_METERS

        // 4. Determine which tier is eligible
        val candidateTier: AnnouncementTier? = when {
            distanceToManeuverMeters <= imminentThreshold -> AnnouncementTier.IMMINENT
            distanceToManeuverMeters <= nearThreshold -> AnnouncementTier.NEAR
            distanceToManeuverMeters <= farThreshold -> AnnouncementTier.FAR
            else -> null
        }

        if (candidateTier == null || announcedTiers.contains(candidateTier)) {
            return null
        }

        // Don't announce FAR or NEAR if we already announced a closer tier (e.g. rider jumped ahead)
        if (candidateTier == AnnouncementTier.FAR && (announcedTiers.contains(AnnouncementTier.NEAR) || announcedTiers.contains(AnnouncementTier.IMMINENT))) {
            return null
        }
        if (candidateTier == AnnouncementTier.NEAR && announcedTiers.contains(AnnouncementTier.IMMINENT)) {
            return null
        }

        // 5. Cadence Rate-Limiting (Urgent IMMINENT maneuvers bypass cooldown)
        val isUrgent = candidateTier == AnnouncementTier.IMMINENT
        val minIntervalMs = TrackingConstants.NAV_VOICE_MIN_INTERVAL_SECONDS * 1000L
        val lastTime = lastAnnouncementTimestampMs
        if (!isUrgent && lastTime != null && (nowMs - lastTime < minIntervalMs)) {
            return null
        }

        // 6. Format Speech Phrasing
        val announcementText = formatManeuverAnnouncement(
            instruction = instruction,
            tier = candidateTier,
            distanceMeters = distanceToManeuverMeters,
            isMetric = isMetric
        )

        announcedTiers.add(candidateTier)
        lastAnnouncementTimestampMs = nowMs

        return VoiceAnnouncement(
            text = announcementText,
            isUrgent = isUrgent,
            tier = candidateTier
        )
    }

    private fun evaluateStatusChange(status: NavigationStatus, nowMs: Long): VoiceAnnouncement? {
        if (status == lastAnnouncedStatus) return null

        val previous = lastAnnouncedStatus
        lastAnnouncedStatus = status

        return when (status) {
            NavigationStatus.ARRIVED -> {
                VoiceAnnouncement(
                    text = "You have arrived at your destination.",
                    isUrgent = true
                )
            }
            NavigationStatus.OFF_ROUTE_REROUTE_NEEDED -> {
                VoiceAnnouncement(
                    text = "Off route. Recalculating...",
                    isUrgent = true
                )
            }
            NavigationStatus.OFF_ROUTE_WARNING -> {
                if (previous != NavigationStatus.OFF_ROUTE_REROUTE_NEEDED) {
                    VoiceAnnouncement(
                        text = "Off route.",
                        isUrgent = false
                    )
                } else null
            }
            else -> null
        }
    }

    /**
     * Format a natural spoken phrasing for a maneuver based on distance tier and unit system.
     */
    fun formatManeuverAnnouncement(
        instruction: RouteInstruction,
        tier: AnnouncementTier,
        distanceMeters: Double,
        isMetric: Boolean
    ): String {
        val actionText = getActionPhrase(instruction)

        return when (tier) {
            AnnouncementTier.IMMINENT -> {
                actionText.replaceFirstChar { it.uppercase() } + "."
            }
            AnnouncementTier.NEAR, AnnouncementTier.FAR -> {
                val distancePhrase = formatDistanceForSpeech(distanceMeters, isMetric)
                "In $distancePhrase, ${actionText.replaceFirstChar { it.lowercase() }}."
            }
        }
    }

    private fun getActionPhrase(instruction: RouteInstruction): String {
        val street = instruction.streetName?.trim()
        val ontoStreet = if (!street.isNullOrBlank()) " onto $street" else ""
        val onStreet = if (!street.isNullOrBlank()) " on $street" else ""

        // If instructionText is descriptive, check if it's already well-formed
        val customText = instruction.instructionText.trim()
        if (customText.isNotEmpty() && instruction.maneuverType == ManeuverType.ROUNDABOUT) {
            return customText
        }

        return when (instruction.maneuverType) {
            ManeuverType.DEPART -> if (!street.isNullOrBlank()) "head towards $street" else "start ride"
            ManeuverType.CONTINUE -> "continue$onStreet"
            ManeuverType.TURN_SLIGHT_LEFT -> "slight left$ontoStreet"
            ManeuverType.TURN_LEFT -> "turn left$ontoStreet"
            ManeuverType.TURN_SHARP_LEFT -> "sharp left$ontoStreet"
            ManeuverType.TURN_SLIGHT_RIGHT -> "slight right$ontoStreet"
            ManeuverType.TURN_RIGHT -> "turn right$ontoStreet"
            ManeuverType.TURN_SHARP_RIGHT -> "sharp right$ontoStreet"
            ManeuverType.U_TURN -> "make a U-turn$ontoStreet"
            ManeuverType.ROUNDABOUT -> "enter the roundabout$ontoStreet"
            ManeuverType.MERGE -> "merge$ontoStreet"
            ManeuverType.RAMP -> "take the ramp$ontoStreet"
            ManeuverType.FORK -> "take the fork$ontoStreet"
            ManeuverType.ARRIVE -> "arrive at destination"
            ManeuverType.UNKNOWN -> if (customText.isNotEmpty()) customText else "continue$onStreet"
        }
    }

    private fun formatDistanceForSpeech(meters: Double, isMetric: Boolean): String {
        return if (isMetric) {
            if (meters >= 950.0) {
                val km = (meters / 1000.0)
                if (km >= 1.95) {
                    "${km.roundToInt()} kilometers"
                } else {
                    val roundedTenth = (km * 10.0).roundToInt() / 10.0
                    if (roundedTenth == 1.0) "1 kilometer" else "$roundedTenth kilometers"
                }
            } else {
                // Round to nearest 50m for natural acoustic clarity (e.g. "500 meters", "150 meters")
                val roundedMeters = ((meters / 50.0).roundToInt() * 50).coerceAtLeast(50)
                "$roundedMeters meters"
            }
        } else {
            val feet = meters * 3.28084
            if (feet >= 4500.0) {
                val miles = feet / 5280.0
                if (miles >= 1.95) {
                    "${miles.roundToInt()} miles"
                } else {
                    val roundedTenth = (miles * 10.0).roundToInt() / 10.0
                    if (roundedTenth == 1.0) "1 mile" else "$roundedTenth miles"
                }
            } else {
                // Round to nearest 100 feet
                val roundedFeet = ((feet / 100.0).roundToInt() * 100).coerceAtLeast(100)
                "$roundedFeet feet"
            }
        }
    }
}
