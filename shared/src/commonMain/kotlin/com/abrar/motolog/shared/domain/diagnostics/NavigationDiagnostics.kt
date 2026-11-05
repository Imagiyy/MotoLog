package com.abrar.motolog.shared.domain.diagnostics

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Anonymized, non-sensitive diagnostic snapshot for navigation and routing subsystems.
 * Contains ZERO GPS coordinates, search queries, or API credentials.
 */
@Serializable
data class NavigationDiagnosticsSnapshot(
    val totalRouteRequests: Long = 0L,
    val successfulRouteRequests: Long = 0L,
    val failedRouteRequests: Long = 0L,
    val networkErrors: Long = 0L,
    val timeoutErrors: Long = 0L,
    val rateLimitErrors: Long = 0L,
    val noRouteFoundErrors: Long = 0L,
    val apiKeyMissingErrors: Long = 0L,
    val totalReroutesTriggered: Long = 0L,
    val successfulReroutes: Long = 0L,
    val failedReroutes: Long = 0L,
    val arrivalsConfirmed: Long = 0L,
    val cancellations: Long = 0L,
    val voiceAnnouncementsSpoken: Long = 0L,
    val offRouteWarningCount: Long = 0L,
    val gpsSignalLossEvents: Long = 0L,
    val generatedTimestampMs: Long = 0L
)

/**
 * Thread-safe privacy-preserving diagnostics tracker for MotoLog navigation.
 * Collects aggregated, non-sensitive metrics that riders can inspect or export
 * without disclosing location data.
 */
class NavigationDiagnostics {

    private val lock = DiagnosticsLock()

    private var totalRouteRequests: Long = 0L
    private var successfulRouteRequests: Long = 0L
    private var failedRouteRequests: Long = 0L
    private var networkErrors: Long = 0L
    private var timeoutErrors: Long = 0L
    private var rateLimitErrors: Long = 0L
    private var noRouteFoundErrors: Long = 0L
    private var apiKeyMissingErrors: Long = 0L

    private var totalReroutesTriggered: Long = 0L
    private var successfulReroutes: Long = 0L
    private var failedReroutes: Long = 0L
    private var arrivalsConfirmed: Long = 0L
    private var cancellations: Long = 0L
    private var voiceAnnouncementsSpoken: Long = 0L
    private var offRouteWarningCount: Long = 0L
    private var gpsSignalLossEvents: Long = 0L

    private val json = Json { prettyPrint = true }

    fun recordRouteRequest() = lock.withLock { totalRouteRequests++ }
    fun recordRouteSuccess() = lock.withLock { successfulRouteRequests++ }
    fun recordRouteFailure(category: FailureCategory) = lock.withLock {
        failedRouteRequests++
        when (category) {
            FailureCategory.NETWORK -> networkErrors++
            FailureCategory.TIMEOUT -> timeoutErrors++
            FailureCategory.RATE_LIMIT -> rateLimitErrors++
            FailureCategory.NO_ROUTE -> noRouteFoundErrors++
            FailureCategory.API_KEY_MISSING -> apiKeyMissingErrors++
            FailureCategory.OTHER -> {}
        }
    }

    fun recordRerouteTriggered() = lock.withLock { totalReroutesTriggered++ }
    fun recordRerouteSuccess() = lock.withLock { successfulReroutes++ }
    fun recordRerouteFailure() = lock.withLock { failedReroutes++ }

    fun recordArrival() = lock.withLock { arrivalsConfirmed++ }
    fun recordCancellation() = lock.withLock { cancellations++ }
    fun recordVoiceAnnouncement() = lock.withLock { voiceAnnouncementsSpoken++ }
    fun recordOffRouteWarning() = lock.withLock { offRouteWarningCount++ }
    fun recordGpsSignalLoss() = lock.withLock { gpsSignalLossEvents++ }

    /**
     * Create an immutable snapshot of current counters.
     */
    fun getSnapshot(nowMs: Long = 0L): NavigationDiagnosticsSnapshot = lock.withLock {
        NavigationDiagnosticsSnapshot(
            totalRouteRequests = totalRouteRequests,
            successfulRouteRequests = successfulRouteRequests,
            failedRouteRequests = failedRouteRequests,
            networkErrors = networkErrors,
            timeoutErrors = timeoutErrors,
            rateLimitErrors = rateLimitErrors,
            noRouteFoundErrors = noRouteFoundErrors,
            apiKeyMissingErrors = apiKeyMissingErrors,
            totalReroutesTriggered = totalReroutesTriggered,
            successfulReroutes = successfulReroutes,
            failedReroutes = failedReroutes,
            arrivalsConfirmed = arrivalsConfirmed,
            cancellations = cancellations,
            voiceAnnouncementsSpoken = voiceAnnouncementsSpoken,
            offRouteWarningCount = offRouteWarningCount,
            gpsSignalLossEvents = gpsSignalLossEvents,
            generatedTimestampMs = nowMs
        )
    }

    /**
     * Export diagnostic counters as a formatted JSON document.
     * Contains zero PII, zero coordinates, and zero API tokens.
     */
    fun exportDiagnosticsJson(nowMs: Long = 0L): String {
        return json.encodeToString(getSnapshot(nowMs))
    }

    /**
     * Clear all counters.
     */
    fun reset() = lock.withLock {
        totalRouteRequests = 0L
        successfulRouteRequests = 0L
        failedRouteRequests = 0L
        networkErrors = 0L
        timeoutErrors = 0L
        rateLimitErrors = 0L
        noRouteFoundErrors = 0L
        apiKeyMissingErrors = 0L
        totalReroutesTriggered = 0L
        successfulReroutes = 0L
        failedReroutes = 0L
        arrivalsConfirmed = 0L
        cancellations = 0L
        voiceAnnouncementsSpoken = 0L
        offRouteWarningCount = 0L
        gpsSignalLossEvents = 0L
    }

    enum class FailureCategory {
        NETWORK,
        TIMEOUT,
        RATE_LIMIT,
        NO_ROUTE,
        API_KEY_MISSING,
        OTHER
    }
}
