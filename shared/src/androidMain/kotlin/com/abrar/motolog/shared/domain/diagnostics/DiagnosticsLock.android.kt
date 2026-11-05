package com.abrar.motolog.shared.domain.diagnostics

actual class DiagnosticsLock actual constructor() {
    private val lock = Any()

    actual fun <T> withLock(block: () -> T): T = synchronized(lock, block)
}
