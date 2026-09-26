package com.abrar.motolog.shared.domain.model

/**
 * Pure domain representation of a motorcycle maintenance/service task.
 * Decoupled from any database persistence framework (Room, SQLDelight, etc.).
 */
data class MaintenanceTask(
    val id: Long = 0L,
    val bikeId: Long = 0L,
    val name: String,
    val intervalKm: Double? = null,
    val intervalDays: Int? = null,
    val lastDoneOdometerKm: Double = 0.0,
    val lastDoneDateEpochMs: Long = 0L,
    val lastNotifiedDueEpochMs: Long? = null
)
