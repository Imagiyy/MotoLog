package com.abrar.motolog.domain.time

/**
 * Time abstraction allowing deterministic simulation of timeouts
 * (auto-pause delays, signal loss timeouts) in JVM unit tests without real delays.
 */
fun interface Clock {
    fun currentTimeMillis(): Long
}

/**
 * Production system clock.
 */
object DefaultClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
