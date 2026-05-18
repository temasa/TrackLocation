package com.kolee.tracklocation.screens.track

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.permission.CheckAndRequestPermissions
import com.kolee.tracklocation.screens.track.components.MapControls
import com.kolee.tracklocation.screens.track.components.TrackMap
import com.kolee.tracklocation.screens.track.components.TripPanel
import com.kolee.tracklocation.viewmodel.ShareViewModel
import kotlinx.coroutines.delay

@Composable
fun TrackScreen() {
    val viewModel: ShareViewModel = viewModel(factory = ShareViewModel.Factory)
    val locationUiState by viewModel.locationUiState.collectAsState()

    var performRequestPermission by remember { mutableStateOf(true) }
    var allPermissionsGranted by remember { mutableStateOf(false) }
    var isShowPanel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isShowPanel = true
    }

    val panelState = TrackPanelState(
        tripState = when {
            locationUiState.isPaused -> TripState.PAUSED
            locationUiState.isTracking -> TripState.LIVE
            else -> TripState.READY
        },
        elapsedMs = locationUiState.durationTimer,
        distanceKm = locationUiState.distanceInMeters / 1000.0,
        speedKmh = locationUiState.speedInKMH.toDouble()
    )

    Box(modifier = Modifier.fillMaxSize()) {
        TrackMap(
            currentLocation = locationUiState.currentLocation,
            pathPoints = locationUiState.pathPoints
        )

        if (allPermissionsGranted) {
            // Floating map controls (recenter + layers), pinned to bottom-right
            MapControls(modifier = Modifier.align(Alignment.BottomEnd))

            AnimatedVisibility(
                visible = isShowPanel,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = EnterTransition.None,
                exit = fadeOut()
            ) {
                TripPanel(
                    state = panelState,
                    onCtaTap = { viewModel.onTripCtaTap() },
                    modifier = Modifier
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                )
            }
        }
    }

    if (performRequestPermission) {
        CheckAndRequestPermissions(
            isGranted = {
                performRequestPermission = false
                allPermissionsGranted = true
            }
        )
    }
}
