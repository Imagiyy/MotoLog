package com.abrar.motolog.shared.domain.util

import java.util.Locale

actual object PlatformFormatter {
    actual fun formatDecimal(value: Double, decimals: Int): String {
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    actual fun formatThousands(value: Double): String {
        return String.format(Locale.US, "%,.0f", value)
    }
}
