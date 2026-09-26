package com.abrar.motolog.shared.domain.map

/**
 * Abstraction for map tile source configuration.
 *
 * Allows swapping tile providers (OpenFreeMap, self-hosted, etc.)
 * without touching any UI or rendering code.
 */
interface MapStyleProvider {
    /** Returns the style URL appropriate for the current theme. */
    fun getStyleUrl(isDarkTheme: Boolean): String

    /** Returns the attribution string to display on the map. */
    fun getAttribution(): String
}
