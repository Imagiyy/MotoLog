package com.abrar.motolog.domain.map

/** Rendering backend contract; route math remains independent of the map SDK. */
interface MapProvider {
    val styleProvider: MapStyleProvider
}
