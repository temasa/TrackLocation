package com.kolee.tracklocation.screens.details.components

import android.util.Log
import androidx.annotation.DrawableRes
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.kolee.tracklocation.screens.track.components.LoadingCircularProgress
import com.kolee.tracklocation.utils.POLYLINE_WIDTH
import com.kolee.tracklocation.utils.bitmapDescriptorFromVector
import com.kolee.tracklocation.R


private const val TAG = "DetailsTrackMap"
@Composable
fun BoxScope.DetailsTrackMap(
    modifier: Modifier = Modifier,
    pathPoints: List<LatLng>
) {
    var isMapLoaded by remember { mutableStateOf(false) }
    var cameraPositionState = rememberCameraPositionState()
    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true
            )
        )
    }

    LoadingCircularProgress(isMapLoaded)

    if (pathPoints.size >= 2) {
        val bound = remember {
            LatLngBounds.builder().apply {
                includeAll(pathPoints)
            }.build()
        }

        LaunchedEffect(key1 = isMapLoaded) {
            if (isMapLoaded) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bound, 50)
                )
            }
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        uiSettings = mapUiSettings,
        cameraPositionState = cameraPositionState,
        onMapLoaded = {isMapLoaded = true}
    ) {
        if (pathPoints.size >= 2) {
            Polyline(
                points = pathPoints,
                color = Color.Blue.copy(alpha = 0.7f),
                width = POLYLINE_WIDTH
            )

            PutMarker(
                position = pathPoints.first(),
                icon = R.drawable.baseline_adjust_24,
                color = Color.Blue,
                scale = 1.0
            )

            PutMarker(
                position = pathPoints.last(),
                icon = R.drawable.ic_finish,
                color = Color.Blue,
                scale = 1.0
            )
        }
    }
}

@Composable
fun PutMarker(
    position: LatLng,
    @DrawableRes icon: Int,
    color: Color,
    scale: Double
) {
    Marker(
        icon = bitmapDescriptorFromVector(
            context = LocalContext.current,
            vectorResId = icon,
            tint = color.toArgb(),
            scale = scale
        ),
        state = rememberMarkerState(position = position),
        anchor = Offset(0.3f, 0.3f)
    )
}

private fun LatLngBounds.Builder.includeAll(pathPoints: List<LatLng>): LatLngBounds.Builder {
    pathPoints.forEach(::include)

    return this
}