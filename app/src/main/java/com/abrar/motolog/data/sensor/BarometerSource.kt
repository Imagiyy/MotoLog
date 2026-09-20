package com.abrar.motolog.data.sensor

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for barometric pressure sensor.
 *
 * Provides altitude readings derived from atmospheric pressure.
 * The domain layer depends solely on this interface, allowing
 * barometer to be faked in tests.
 */
interface BarometerSource {
    /**
     * Flow of altitude readings in meters derived from pressure.
     * Uses standard atmosphere reference for conversion.
     */
    fun getAltitudeUpdates(): Flow<Double>

    /** Stop listening and release sensor resources. */
    fun stopListening()

    /** Whether the device has a barometer sensor. */
    fun isAvailable(): Boolean
}
