package com.abrar.motolog.data.routing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for the GraphHopper Routing and Geocoding APIs.
 */

// ============================================================
// Routing DTOs
// ============================================================

@Serializable
data class GraphHopperRouteResponseDto(
    @SerialName("paths")
    val paths: List<GraphHopperPathDto> = emptyList(),
    @SerialName("message")
    val message: String? = null
)

@Serializable
data class GraphHopperPathDto(
    @SerialName("distance")
    val distance: Double = 0.0,
    @SerialName("time")
    val timeMs: Long = 0L,
    @SerialName("points")
    val pointsEncoded: String? = null,
    @SerialName("bbox")
    val bbox: List<Double>? = null,
    @SerialName("instructions")
    val instructions: List<GraphHopperInstructionDto> = emptyList()
)

@Serializable
data class GraphHopperInstructionDto(
    @SerialName("text")
    val text: String = "",
    @SerialName("street_name")
    val streetName: String? = null,
    @SerialName("distance")
    val distance: Double = 0.0,
    @SerialName("time")
    val timeMs: Long = 0L,
    @SerialName("interval")
    val interval: List<Int> = emptyList(),
    @SerialName("sign")
    val sign: Int = 0,
    @SerialName("heading")
    val heading: Double? = null
)

// ============================================================
// Geocoding DTOs
// ============================================================

@Serializable
data class GraphHopperGeocodeResponseDto(
    @SerialName("hits")
    val hits: List<GraphHopperHitDto> = emptyList(),
    @SerialName("message")
    val message: String? = null
)

@Serializable
data class GraphHopperHitDto(
    @SerialName("point")
    val point: GraphHopperPointDto? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("city")
    val city: String? = null,
    @SerialName("state")
    val state: String? = null,
    @SerialName("street")
    val street: String? = null,
    @SerialName("housenumber")
    val housenumber: String? = null,
    @SerialName("osm_id")
    val osmId: String? = null
)

@Serializable
data class GraphHopperPointDto(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double
)
