package com.abrar.motolog.shared.domain.model

/**
 * Domain error representations for routing, geocoding, and navigation requests.
 */
sealed class RoutingError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** Network communication failed (e.g. no internet, DNS resolution error, connection dropped). */
    class NetworkError(message: String = "Network connection failed", cause: Throwable? = null) :
        RoutingError(message, cause)

    /** Provider returned no feasible route between origin and destination. */
    class NoRouteFound(message: String = "No route found to destination") :
        RoutingError(message)

    /** Routing provider quota, rate limit, or concurrent connection limit exceeded. */
    class RateLimitExceeded(message: String = "Routing provider quota or rate limit exceeded") :
        RoutingError(message)

    /** Provider returned a non-200 HTTP or business error response. */
    class ProviderError(val statusCode: Int, message: String) :
        RoutingError("Provider error ($statusCode): $message")

    /** Routing or geocoding API key is missing or invalid. */
    class ApiKeyMissing(message: String = "GraphHopper API key is missing or invalid") :
        RoutingError(message)

    /** Provided coordinates are out of bounds or invalid. */
    class InvalidCoordinates(message: String = "Invalid coordinates supplied") :
        RoutingError(message)

    /** Request was explicitly cancelled by the rider or superseded by a newer request. */
    class RequestCancelled(message: String = "Routing request was cancelled") :
        RoutingError(message)

    /** Request timed out before the provider responded. */
    class Timeout(message: String = "Routing request timed out") :
        RoutingError(message)
}
