package com.kolee.tracklocation.screens.track.components

import android.util.Log
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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
import com.google.maps.android.compose.CameraMoveStartedReason
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
private const val NAVIGATION_ENTRY_ZOOM = 17f // ADR-020

@Composable
fun BoxScope.TrackMap(
    currentLocation: LatLng,
    pathPoints: List<LatLng>,
    plannedRoute: List<LatLng> = emptyList(),
    runtimeRoute: List<LatLng> = emptyList(),
    pickup: LatLng? = null,
    drop: LatLng? = null,
    followLocation: Boolean = true,
    recenterTick: Int = 0,
    navigationActive: Boolean = false,
    navigationBearingDeg: Float? = null,
    markerHeadingDeg: Float = 0f,
    onUserPan: () -> Unit = {}
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

    // Follow mode (UI-SPEC 3f): keep the user's zoom once zoomed in; fall back to MAP_ZOOM on first load.
    fun targetZoom(): Float =
        if (cameraPositionState.position.zoom >= 12f) cameraPositionState.position.zoom else MAP_ZOOM

    // ADR-020: heading-up while navigating (bearing = last valid heading, else north), north-up otherwise; tilt always 0.
    fun followPosition(target: LatLng, zoom: Float, navigating: Boolean, headingDeg: Float?): CameraPosition =
        CameraPosition.Builder()
            .target(target)
            .zoom(zoom)
            .bearing(if (navigating) (headingDeg ?: 0f) else 0f)
            .tilt(0f)
            .build()

    // ADR-020: navigation transitions are tracked independently of follow so a user pan cannot leave stale state.
    var wasNavigating by remember { mutableStateOf(false) }
    var navEntryPending by remember { mutableStateOf(false) }
    LaunchedEffect(navigationActive) {
        if (navigationActive && !wasNavigating) navEntryPending = true
        if (!navigationActive && wasNavigating) {
            // Exit: back to north-up, keeping the current target and zoom (works even if the user had panned away).
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder(cameraPositionState.position).bearing(0f).tilt(0f).build()
                )
            )
        }
        wasNavigating = navigationActive
    }

    // ADR-020: zoom 17 once on navigation entry (also when opened mid-trip); applied when follow is active.
    // Entry zoom is retried on each location update until the animation completes successfully.
    LaunchedEffect(currentLocation, followLocation, navigationActive, navigationBearingDeg) {
        if (followLocation) {
            val entering = navigationActive && navEntryPending
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    followPosition(
                        currentLocation,
                        if (entering) NAVIGATION_ENTRY_ZOOM else targetZoom(),
                        navigationActive,
                        navigationBearingDeg
                    )
                )
            )
            // Only a completed (not cancelled) animation consumes the entry; a cancelled one is retried on the next restart.
            if (entering) navEntryPending = false
        }
    }

    // Explicit Recenter taps: tick 0 is the initial composition, so skip it.
    val latestLocation by rememberUpdatedState(currentLocation)
    val latestNavigationActive by rememberUpdatedState(navigationActive)
    val latestNavigationBearing by rememberUpdatedState(navigationBearingDeg)
    LaunchedEffect(recenterTick) {
        if (recenterTick > 0) {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    followPosition(latestLocation, targetZoom(), latestNavigationActive, latestNavigationBearing)
                )
            )
        }
    }

    // User pans are reported via GESTURE; our own animate() calls report DEVELOPER_ANIMATION.
    val latestOnUserPan by rememberUpdatedState(onUserPan)
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.cameraMoveStartedReason }.collect {
            if (it == CameraMoveStartedReason.GESTURE) latestOnUserPan()
        }
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

        // UI-SPEC §3i: car marker from ui-design.pen; flat so it rotates with the map (heading-up camera keeps it pointing up).
        val context = LocalContext.current
        val carIcon = remember(context) {
            bitmapDescriptorFromVector(context = context, vectorResId = R.drawable.ic_car_marker, tint = null, scale = 1.0)
        }
        Marker(
            icon = carIcon,
            state = currentMarkerState,
            anchor = Offset(0.5f, 0.5f),
            flat = true,
            rotation = markerHeadingDeg
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