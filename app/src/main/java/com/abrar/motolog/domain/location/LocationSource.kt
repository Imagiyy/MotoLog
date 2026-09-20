package com.abrar.motolog.domain.location

import com.abrar.motolog.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for GPS location tracking.
 *
 * The ViewModel and domain layers depend solely on this interface,
 * allowing tracking to be tested with test fakes, and smoothly transitioned
 * into a foreground service in Stage 3.
 */
interface LocationSource {
    /**
     * Flow providing continuous GPS location fixes.
     * Starts receiving updates when collected and ceases updates when cancelled or stopped.
     */
    fun getLocationUpdates(): Flow<LocationPoint>

    /**
     * Explicitly stop any active location updates and release hardware resources.
     */
    fun stopLocationUpdates()
}
