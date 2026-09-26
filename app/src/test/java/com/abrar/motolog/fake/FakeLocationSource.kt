package com.abrar.motolog.fake

import com.abrar.motolog.shared.domain.location.LocationSource
import com.abrar.motolog.shared.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeLocationSource : LocationSource {

    val locationFlow = MutableSharedFlow<LocationPoint>(replay = 1)
    var stopCalled: Boolean = false
        private set

    override fun getLocationUpdates(trackingMode: com.abrar.motolog.shared.domain.model.TrackingMode): Flow<LocationPoint> = locationFlow

    override fun stopLocationUpdates() {
        stopCalled = true
    }

    suspend fun emitLocation(point: LocationPoint) {
        locationFlow.emit(point)
    }

    fun reset() {
        stopCalled = false
    }
}
