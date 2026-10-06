package com.kolee.tracklocation.screens.track.components

import android.util.Log
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.kolee.tracklocation.utils.MAP_ZOOM
import com.kolee.tracklocation.utils.POLYLINE_WIDTH
import com.kolee.tracklocation.utils.bitmapDescriptorFromVector
import com.kolee.tracklocation.R

private const val TAG = "TrackMap"

@Composable
fun BoxScope.TrackMap(
    currentLocation: LatLng,
    pathPoints: List<LatLng>,
    plannedRoute: List<LatLng> = emptyList(),
    runtimeRoute: List<LatLng> = emptyList(),
    pickup: LatLng? = null,
    drop: LatLng? = null
) {
    Log.d(TAG, "TrackMap entered")
    var isMapLoaded by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true
        )
    }

    LoadingCircularProgress(isMapLoaded)

    Log.d(TAG, "isMapLoaded: ${isMapLoaded}")

    LaunchedEffect(key1 = currentLocation) {
        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(currentLocation, MAP_ZOOM)
            )
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        uiSettings = mapUiSettings,
        cameraPositionState = cameraPositionState,
        onMapLoaded = {
            Log.d(TAG, "onMapLoaded")
            isMapLoaded = true}
    ) {
        // PROVISIONAL styling (ADR-016 / UI-SPEC §3e): planned route (thin, muted) under the
        // runtime route (bold orange); both under the recorded trace and the car marker.
        if (plannedRoute.size > 1) {
            Polyline(
                points = plannedRoute,
                color = Color(0xFF78909C),
                width = POLYLINE_WIDTH * 0.6f
            )
        }
        if (runtimeRoute.size > 1) {
            Polyline(
                points = runtimeRoute,
                color = Color(0xFFFF8F00),
                width = POLYLINE_WIDTH
            )
        }
        if (pickup != null) {
            Marker(
                state = rememberMarkerState(key = "pickup-${pickup.latitude},${pickup.longitude}", position = pickup),
                title = "Pickup",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )
        }
        if (drop != null) {
            Marker(
                state = rememberMarkerState(key = "drop-${drop.latitude},${drop.longitude}", position = drop),
                title = "Drop",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
        }

        val currentMarkerState = rememberMarkerState()
        currentMarkerState.position = currentLocation

        Marker(
            icon = bitmapDescriptorFromVector(
                context = LocalContext.current,
                vectorResId = R.drawable.ic_location_pin,
                tint = Color.Blue.toArgb(),
                scale = 1.0
            ),
            state = currentMarkerState,
            anchor = Offset(0.5f, 0.5f)
        )

        if (pathPoints.size > 1) {
            Polyline(
                points = pathPoints,
                color = Color.Blue,
                width = POLYLINE_WIDTH
            )
        }
    }
}