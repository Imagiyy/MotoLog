package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.model.MaintenanceEvaluation
import com.abrar.motolog.domain.model.MaintenanceStatus
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Pure Kotlin calculator evaluating maintenance item due status.
 *
 * Supports distance-only, time-only, or "whichever comes first" dual intervals.
 * Free of Android framework dependencies.
 */
object MaintenanceCalculator {

    private const val MS_PER_DAY = 86_400_000L

    fun evaluate(
        item: MaintenanceItemEntity,
        currentOdometerKm: Double,
        currentEpochMs: Long,
        dueSoonKmThreshold: Double = TrackingConstants.DUE_SOON_KM_THRESHOLD,
        dueSoonDaysThreshold: Int = TrackingConstants.DUE_SOON_DAYS_THRESHOLD
    ): MaintenanceEvaluation {
        val hasKm = item.intervalKm != null && item.intervalKm > 0.0
        val hasDays = item.intervalDays != null && item.intervalDays > 0

        if (!hasKm && !hasDays) {
            return MaintenanceEvaluation(
                item = item,
                status = MaintenanceStatus.OK,
                remainingKm = null,
                remainingDays = null,
                progress = 0f,
                summary = "No interval set"
            )
        }

        var remainingKm: Double? = null
        var kmProgress = 0f
        var isKmOverdue = false
        var isKmDueSoon = false

        if (hasKm) {
            val intervalKm = item.intervalKm
            val distSinceService = (currentOdometerKm - item.lastDoneOdometerKm).coerceAtLeast(0.0)
            val rem = intervalKm - distSinceService
            remainingKm = rem
            kmProgress = (distSinceService / intervalKm).toFloat()
            isKmOverdue = rem <= 0.0
            isKmDueSoon = rem <= dueSoonKmThreshold
        }

        var remainingDays: Int? = null
        var daysProgress = 0f
        var isDaysOverdue = false
        var isDaysDueSoon = false

        if (hasDays) {
            val intervalDays = item.intervalDays
            val elapsedMs = (currentEpochMs - item.lastDoneDateEpochMs).coerceAtLeast(0L)
            val daysElapsed = (elapsedMs / MS_PER_DAY).toInt()
            val rem = intervalDays - daysElapsed
            remainingDays = rem
            daysProgress = (daysElapsed.toFloat() / intervalDays.toFloat())
            isDaysOverdue = rem <= 0
            isDaysDueSoon = rem <= dueSoonDaysThreshold
        }

        val status = when {
            isKmOverdue || isDaysOverdue -> MaintenanceStatus.OVERDUE
            isKmDueSoon || isDaysDueSoon -> MaintenanceStatus.DUE_SOON
            else -> MaintenanceStatus.OK
        }

        val progress = max(kmProgress, daysProgress).coerceAtLeast(0f)

        val summary = buildSummary(status, remainingKm, remainingDays)

        return MaintenanceEvaluation(
            item = item,
            status = status,
            remainingKm = remainingKm,
            remainingDays = remainingDays,
            progress = progress,
            summary = summary
        )
    }

    /**
     * Determine if a notification should be posted for a maintenance item.
     * Prevents spamming: notifies once per due event, suppressing repeat notifications
     * within [antiSpamDurationMs] (default 7 days) unless marked done and due again.
     */
    fun shouldNotify(
        item: MaintenanceItemEntity,
        status: MaintenanceStatus,
        currentEpochMs: Long,
        antiSpamDurationMs: Long = 7L * 24 * 60 * 60 * 1000 // 7 days
    ): Boolean {
        if (status == MaintenanceStatus.OK) return false
        val lastNotified = item.lastNotifiedDueEpochMs ?: return true
        if (item.lastDoneDateEpochMs > lastNotified) return true
        return (currentEpochMs - lastNotified) >= antiSpamDurationMs
    }

    private fun buildSummary(status: MaintenanceStatus, remainingKm: Double?, remainingDays: Int?): String {
        return when (status) {
            MaintenanceStatus.OVERDUE -> {
                val parts = mutableListOf<String>()
                if (remainingKm != null && remainingKm <= 0.0) {
                    parts.add("${String.format(Locale.US, "%,.0f", abs(remainingKm))} km overdue")
                }
                if (remainingDays != null && remainingDays <= 0) {
                    parts.add("${abs(remainingDays)} days overdue")
                }
                parts.joinToString(", ").ifEmpty { "Service overdue" }
            }
            MaintenanceStatus.DUE_SOON -> {
                val parts = mutableListOf<String>()
                if (remainingKm != null && remainingKm <= TrackingConstants.DUE_SOON_KM_THRESHOLD) {
                    parts.add("${String.format(Locale.US, "%,.0f", remainingKm)} km remaining")
                }
                if (remainingDays != null && remainingDays <= TrackingConstants.DUE_SOON_DAYS_THRESHOLD) {
                    parts.add("$remainingDays days remaining")
                }
                "Due soon: ${parts.joinToString(", ")}"
            }
            MaintenanceStatus.OK -> {
                val parts = mutableListOf<String>()
                if (remainingKm != null) {
                    parts.add("${String.format(Locale.US, "%,.0f", remainingKm)} km left")
                }
                if (remainingDays != null) {
                    parts.add("$remainingDays days left")
                }
                parts.joinToString(", ").ifEmpty { "Service up to date" }
            }
        }
    }
}
