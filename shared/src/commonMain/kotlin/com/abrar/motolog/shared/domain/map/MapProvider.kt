package com.abrar.motolog.shared.domain.map

/** Rendering backend contract; route math remains independent of the map SDK. */
interface MapProvider {
    val styleProvider: MapStyleProvider
}
