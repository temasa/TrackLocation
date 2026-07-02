package com.kolee.tracklocation.screens.track

enum class TripState { READY, LIVE, PAUSED }

data class TrackPanelState(
    val tripState: TripState,
    val elapsedMs: Long,
    val distanceKm: Double,
    val speedKmh: Double,
    // OBD Phase 2 Slice 4 — fuel metrics for the Trip panel (null/false when OBD not connected).
    val obdConnected: Boolean = false,
    val instantKmL: Double? = null,
    val idleFuelLph: Double? = null,
    val tripAvgKmL: Double? = null,
    val fuelSource: String? = null,
)
