package com.abrar.motolog.domain.model

import com.abrar.motolog.data.local.entity.MaintenanceItemEntity

/**
 * Lifecycle status of a motorcycle maintenance item.
 */
enum class MaintenanceStatus {
    /** Item is well within its service interval */
    OK,
    /** Item is nearing its service interval (within 100 km or 7 days) */
    DUE_SOON,
    /** Item has exceeded its service interval */
    OVERDUE
}

/**
 * Result of evaluating a maintenance item against the bike's current odometer and date.
 */
data class MaintenanceEvaluation(
    val item: MaintenanceItemEntity,
    val status: MaintenanceStatus,
    val remainingKm: Double? = null,
    val remainingDays: Int? = null,
    /** Progress from 0.0 (freshly serviced) to 1.0 (due), exceeding 1.0 when overdue */
    val progress: Float = 0f,
    val summary: String = ""
)
