package com.kolee.tracklocation.tracking

import com.google.android.gms.maps.model.LatLng

data class LocationUiState(
    var currentLocation: LatLng = LatLng(37.5716, 126.9763),
    val pathPoints: List<LatLng> = emptyList(),
    val distanceInMeters: Int = 0,
    val durationTimer: String = "00:00:00",
    val speedInKMH: Float = 0f,
    val isTracking: Boolean = false
)