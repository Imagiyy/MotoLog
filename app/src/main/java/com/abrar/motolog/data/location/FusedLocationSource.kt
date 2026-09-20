package com.abrar.motolog.data.location

import android.annotation.SuppressLint
import android.os.Looper
import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.location.LocationSource
import com.abrar.motolog.domain.model.LocationPoint
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

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<LocationPoint> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TrackingConstants.LOCATION_UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(TrackingConstants.LOCATION_FASTEST_INTERVAL_MS)
            setWaitForAccurateLocation(false)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val point = LocationPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speedMps = if (location.hasSpeed()) location.speed else null,
                        accuracyMeters = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE,
                        timestamp = location.elapsedRealtimeNanos / 1_000_000L
                    )
                    trySend(point)
                }
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
