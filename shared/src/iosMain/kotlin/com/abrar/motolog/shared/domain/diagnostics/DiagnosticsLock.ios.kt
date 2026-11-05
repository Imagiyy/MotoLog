package com.abrar.motolog.shared.domain.diagnostics

import platform.Foundation.NSLock

actual class DiagnosticsLock actual constructor() {
    private val lock = NSLock()

    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }
}
