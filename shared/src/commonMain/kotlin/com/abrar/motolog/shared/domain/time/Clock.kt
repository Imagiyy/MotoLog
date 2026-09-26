package com.abrar.motolog.shared.domain.time

import kotlinx.datetime.Clock as KxClock

/**
 * Time abstraction allowing deterministic simulation of timeouts
 * (auto-pause delays, signal loss timeouts) in unit tests without real delays.
 */
fun interface Clock {
    fun currentTimeMillis(): Long
}

/**
 * Production system clock backed by kotlinx-datetime.
 */
object DefaultClock : Clock {
    override fun currentTimeMillis(): Long = KxClock.System.now().toEpochMilliseconds()
}
