package com.abrar.motolog.shared.domain.location

import com.abrar.motolog.shared.domain.model.LocationPoint
import com.abrar.motolog.shared.domain.model.TrackingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Abstraction for GPS location tracking.
 *
 * The ViewModel and domain layers depend solely on this interface,
 * allowing tracking to be tested with test fakes, and smoothly implemented
 * by Android FusedLocationProvider and iOS CoreLocation CLLocationManager.
 */
interface LocationSource {
    /**
     * Flow providing continuous GPS location fixes.
     * Starts receiving updates when collected and ceases updates when cancelled or stopped.
     */
    fun getLocationUpdates(trackingMode: TrackingMode = TrackingMode.HIGH_ACCURACY): Flow<LocationPoint>

    /**
     * Flow providing GPS location availability status.
     * Emits false when GPS hardware fix is lost or unavailable.
     */
    fun getLocationAvailability(): Flow<Boolean> = flowOf(true)

    /**
     * Explicitly stop any active location updates and release hardware resources.
     */
    fun stopLocationUpdates()
}
