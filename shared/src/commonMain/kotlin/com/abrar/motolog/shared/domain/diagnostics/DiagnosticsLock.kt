package com.abrar.motolog.shared.domain.diagnostics

expect class DiagnosticsLock() {
    fun <T> withLock(block: () -> T): T
}
