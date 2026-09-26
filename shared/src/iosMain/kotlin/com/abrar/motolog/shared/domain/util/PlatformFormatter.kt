package com.abrar.motolog.shared.domain.util

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterRoundHalfUp
import platform.Foundation.NSLocale
import platform.Foundation.localeWithLocaleIdentifier

actual object PlatformFormatter {
    actual fun formatDecimal(value: Double, decimals: Int): String {
        val formatter = NSNumberFormatter().apply {
            locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
            minimumFractionDigits = decimals.toULong()
            maximumFractionDigits = decimals.toULong()
            roundingMode = NSNumberFormatterRoundHalfUp
            usesGroupingSeparator = false
        }
        return formatter.stringFromNumber(NSNumber(value)) ?: ""
    }

    actual fun formatThousands(value: Double): String {
        val formatter = NSNumberFormatter().apply {
            locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
            numberStyle = NSNumberFormatterDecimalStyle
            minimumFractionDigits = 0u
            maximumFractionDigits = 0u
            roundingMode = NSNumberFormatterRoundHalfUp
            usesGroupingSeparator = true
        }
        return formatter.stringFromNumber(NSNumber(value)) ?: ""
    }
}
