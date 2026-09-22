package com.abrar.motolog.data.map

import android.util.Log

/**
 * Diagnostic and runtime availability check for MapLibre Native.
 *
 * MapLibre Native relies on `libjniMaplibreNativeC.so` which is compiled
 * for `arm64-v8a`, `armeabi-v7a`, and `x86_64`. On any unsupported architecture
 * or in restricted environments, [isNativeSupported] returns `false` safely
 * without terminating the application process.
 */
object MapSupport {
    private const val TAG = "MapSupport"

    val isNativeSupported: Boolean by lazy {
        try {
            org.maplibre.nativeffi.Maplibre.loadNativeLibrary()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "MapLibre Native binary unavailable: ${t.message}", t)
            false
        }
    }
}
