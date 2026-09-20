package com.abrar.motolog.domain.model

/**
 * Suggested service presets that riders can select and customize.
 */
data class MaintenancePreset(
    val name: String,
    val defaultIntervalKm: Double?,
    val defaultIntervalDays: Int?
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            MaintenancePreset("Chain Lube & Clean", 500.0, 30),
            MaintenancePreset("Engine Oil & Filter", 3000.0, 180),
            MaintenancePreset("Tyre Pressure & Tread", null, 14),
            MaintenancePreset("Brake Pads & Fluid", 5000.0, 365),
            MaintenancePreset("Air Filter Replacement", 10000.0, 365),
            MaintenancePreset("Spark Plugs Check", 12000.0, null)
        )
    }
}
