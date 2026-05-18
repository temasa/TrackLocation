package com.kolee.tracklocation.screens.track

enum class TripState { READY, LIVE, PAUSED }

data class TrackPanelState(
    val tripState: TripState,
    val elapsedMs: Long,
    val distanceKm: Double,
    val speedKmh: Double,
)
