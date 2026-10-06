package com.kolee.tracklocation.feature.observer.trip.route

import com.google.android.gms.maps.model.LatLng

/**
 * ADR-011/017: port for geocoding + driving-route lookups. Implementations never throw; every
 * failure yields null so the order overlay stays fail-soft.
 */
interface RouteProvider {

    /** Best accepted point for a place [name] (optional) / [address], biased to [focus], or null. */
    suspend fun geocode(name: String?, address: String, focus: LatLng?): LatLng?

    /** Driving route polyline origin -> [waypoint]? -> destination, or null. */
    suspend fun route(origin: LatLng, destination: LatLng, waypoint: LatLng? = null): List<LatLng>?
}
