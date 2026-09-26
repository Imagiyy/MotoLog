package com.abrar.motolog.data.location

import android.annotation.SuppressLint
import android.os.Looper
import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.location.LocationSource
import com.abrar.motolog.shared.domain.model.LocationPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fused Location Provider implementation of [LocationSource].
 *
 * Configured with:
 * - Update interval: 1s
 * - Fastest interval: 1s
 * - Priority: High Accuracy (GPS)
 *
 * Removes location updates immediately upon cancellation or explicit stop
 * to release hardware GPS resources.
 */
@Singleton
class FusedLocationSource @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationSource {

    private val activeCallback = AtomicReference<LocationCallback?>(null)
    private val _isLocationAvailable = kotlinx.coroutines.flow.MutableStateFlow(true)

    override fun getLocationAvailability(): Flow<Boolean> = _isLocationAvailable

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(trackingMode: com.abrar.motolog.shared.domain.model.TrackingMode): Flow<LocationPoint> = callbackFlow {
        val (intervalMs, fastestMs) = when (trackingMode) {
            com.abrar.motolog.shared.domain.model.TrackingMode.HIGH_ACCURACY ->
                TrackingConstants.LOCATION_UPDATE_INTERVAL_MS to TrackingConstants.LOCATION_FASTEST_INTERVAL_MS
            com.abrar.motolog.shared.domain.model.TrackingMode.BATTERY_SAVER ->
                TrackingConstants.BATTERY_SAVER_UPDATE_INTERVAL_MS to TrackingConstants.BATTERY_SAVER_FASTEST_INTERVAL_MS
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        ).apply {
            setMinUpdateIntervalMillis(fastestMs)
            setWaitForAccurateLocation(false)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                _isLocationAvailable.value = true
                for (location in result.locations) {
                    val point = LocationPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speedMps = if (location.hasSpeed()) location.speed else null,
                        accuracyMeters = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE,
                        timestamp = if (location.time > 0L) location.time else System.currentTimeMillis(),
                        altitudeMeters = if (location.hasAltitude()) location.altitude else null
                    )
                    trySend(point)
                }
            }

            override fun onLocationAvailability(availability: com.google.android.gms.location.LocationAvailability) {
                _isLocationAvailable.value = availability.isLocationAvailable
            }
        }

        activeCallback.set(callback)

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { exception ->
            close(exception)
        }

        awaitClose {
            stopLocationUpdates()
        }
    }

    override fun stopLocationUpdates() {
        val callback = activeCallback.getAndSet(null)
        if (callback != null) {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
