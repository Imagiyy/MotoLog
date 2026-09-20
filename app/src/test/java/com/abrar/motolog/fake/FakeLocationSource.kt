package com.abrar.motolog.fake

import com.abrar.motolog.domain.location.LocationSource
import com.abrar.motolog.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeLocationSource : LocationSource {

    val locationFlow = MutableSharedFlow<LocationPoint>(replay = 1)
    var stopCalled: Boolean = false
        private set

    override fun getLocationUpdates(): Flow<LocationPoint> = locationFlow

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
