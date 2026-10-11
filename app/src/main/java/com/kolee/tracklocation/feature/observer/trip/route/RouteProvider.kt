package com.kolee.tracklocation.feature.observer.trip.route

import com.google.android.gms.maps.model.LatLng

/**
 * ADR-011/017: port for geocoding + driving-route lookups. Implementations never throw; every
 * failure yields null so the order overlay stays fail-soft.
 */
interface RouteProvider {

    /** Best accepted point for a place [name] (optional) / [address], biased to [focus], or null. */
    suspend fun geocode(name: String?, address: String, focus: LatLng?): LatLng?

    /** Place name + full address label nearest ([lat], [lng]), or null (also when offline). */
    suspend fun reverseGeocode(lat: Double, lng: Double): ReverseGeocodeResult?

    /** Driving route origin -> [waypoint]? -> destination (ADR-026: with summary + raw geometry), or null. */
    suspend fun route(origin: LatLng, destination: LatLng, waypoint: LatLng? = null): RouteResult?
}

/** ADR-023: reverse-geocode hit; [name] is the short place name, [address] the full label. */
data class ReverseGeocodeResult(val name: String?, val address: String?)

/**
 * ADR-026: a directions result. [encoded] is the ORS polyline (precision 5) exactly as returned,
 * [distanceM]/[durationS] come from `routes[0].summary` (null when absent).
 */
data class RouteResult(
    val points: List<LatLng>,
    val distanceM: Int?,
    val durationS: Int?,
    val encoded: String
)
