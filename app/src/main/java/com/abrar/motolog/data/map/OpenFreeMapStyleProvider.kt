package com.abrar.motolog.data.map

import com.abrar.motolog.shared.domain.map.MapStyleProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenFreeMap implementation of [MapStyleProvider].
 *
 * Uses free, open-source vector tiles from OpenFreeMap.
 * No API key, no cookies, no tracking. Attribution is handled
 * automatically by MapLibre's built-in attribution control
 * (the style JSON contains the required attribution strings).
 *
 * Privacy: tile requests contain only standard HTTP headers
 * (User-Agent, tile coordinates). No ride data, coordinates,
 * or identifiers are ever sent by app code.
 */
@Singleton
class OpenFreeMapStyleProvider @Inject constructor() : MapStyleProvider {

    companion object {
        private const val DARK_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
        private const val LIGHT_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
    }

    override fun getStyleUrl(isDarkTheme: Boolean): String =
        if (isDarkTheme) DARK_STYLE_URL else LIGHT_STYLE_URL

    override fun getAttribution(): String =
        "© OpenMapTiles © OpenStreetMap contributors"
}
