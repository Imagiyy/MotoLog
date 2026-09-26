package com.abrar.motolog.shared.domain.util

/**
 * Platform-agnostic number and string formatter ensuring deterministic
 * US English formatting and half-up rounding across Android and iOS.
 */
expect object PlatformFormatter {
    /**
     * Formats a floating-point number with a fixed number of decimal places using US English formatting
     * (dot decimal separator, half-up rounding).
     *
     * Example: formatDecimal(621.371, 1) -> "621.4"
     * Example: formatDecimal(1.005, 2) -> "1.01"
     */
    fun formatDecimal(value: Double, decimals: Int): String

    /**
     * Formats a number with thousands comma grouping and no decimal places.
     *
     * Example: formatThousands(1200.0) -> "1,200"
     */
    fun formatThousands(value: Double): String
}
