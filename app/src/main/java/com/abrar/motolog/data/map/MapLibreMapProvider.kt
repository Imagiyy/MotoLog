package com.abrar.motolog.data.map

import com.abrar.motolog.shared.domain.map.MapProvider
import com.abrar.motolog.shared.domain.map.MapStyleProvider
import javax.inject.Inject
import javax.inject.Singleton

/** MapLibre-backed provider; the route domain never depends on this SDK. */
@Singleton
class MapLibreMapProvider @Inject constructor(
    override val styleProvider: MapStyleProvider
) : MapProvider
