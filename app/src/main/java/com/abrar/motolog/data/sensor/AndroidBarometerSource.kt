package com.abrar.motolog.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.abrar.motolog.domain.TrackingConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [BarometerSource] using TYPE_PRESSURE sensor.
 *
 * Converts atmospheric pressure to altitude using
 * [SensorManager.getAltitude] with standard atmosphere reference.
 *
 * For elevation **gain/loss** calculation we only need relative changes,
 * so using standard atmosphere as reference is acceptable (the absolute
 * altitude may be offset but the deltas are accurate).
 *
 * Sampling rate: SENSOR_DELAY_NORMAL (~200ms), throttled to ~1 Hz
 * to match GPS update rate and minimize CPU wakeups.
 */
@Singleton
class AndroidBarometerSource @Inject constructor(
    @ApplicationContext private val context: Context
) : BarometerSource {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val pressureSensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    }
    private val isListening = AtomicBoolean(false)
    private var activeListener: SensorEventListener? = null

    override fun isAvailable(): Boolean = pressureSensor != null

    override fun getAltitudeUpdates(): Flow<Double> = callbackFlow {
        val sensor = pressureSensor
        if (sensor == null) {
            close()
            return@callbackFlow
        }

        var lastEmitTimeNanos = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = event.timestamp
                // Throttle to ~1 Hz
                if (lastEmitTimeNanos != 0L && now - lastEmitTimeNanos < TrackingConstants.BAROMETER_SAMPLE_INTERVAL_MS * 1_000_000L) return
                lastEmitTimeNanos = now

                val pressureHpa = event.values[0]
                val altitude = SensorManager.getAltitude(
                    SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                    pressureHpa
                ).toDouble()
                trySend(altitude)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed for pressure sensor
            }
        }

        activeListener = listener
        isListening.set(true)
        sensorManager.registerListener(
            listener,
            sensor,
            TrackingConstants.BAROMETER_SAMPLE_INTERVAL_MS.toInt() * 1_000,
            TrackingConstants.BAROMETER_MAX_REPORT_LATENCY_MS.toInt() * 1_000
        )

        awaitClose {
            stopListening()
        }
    }

    override fun stopListening() {
        if (isListening.getAndSet(false)) {
            activeListener?.let { sensorManager.unregisterListener(it) }
            activeListener = null
        }
    }
}
