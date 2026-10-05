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
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.feature.obd.service.ObdUiState
import com.kolee.tracklocation.permission.CheckAndRequestPermissions
import com.kolee.tracklocation.screens.track.components.GojekOrderCard
import com.kolee.tracklocation.screens.track.components.MapControls
import com.kolee.tracklocation.screens.track.components.TrackMap
import com.kolee.tracklocation.screens.track.components.TripPanel
import com.kolee.tracklocation.viewmodel.ShareViewModel
import kotlinx.coroutines.delay

@Composable
fun TrackScreen() {
    val activity = LocalContext.current as ComponentActivity
    val viewModel: ShareViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = ShareViewModel.Factory
    )
    val locationUiState by viewModel.locationUiState.collectAsState()
    val activeOrder by viewModel.activeOrder.collectAsState()
    val obdState by ObdPollingService.obdUiState.collectAsState()

    var performRequestPermission by remember { mutableStateOf(true) }
    var allPermissionsGranted by remember { mutableStateOf(false) }
    var isShowPanel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isShowPanel = true
    }

    // OBD Phase 2 Slice 4 — fuel metrics for the Trip panel.
    val connectedObd = obdState as? ObdUiState.Connected
    val obdRpm = connectedObd?.rpm ?: 0
    // Instant km/L only when moving with a good GPS fix; idle fuel rate (L/h) when essentially stopped.
    val instantKmL = connectedObd?.instantKmL
        ?.takeIf { locationUiState.speedInKMH > 3f && locationUiState.accuracyMeters <= 20f }
    val idleFuelLph = connectedObd?.fuelRateLph
        ?.takeIf { locationUiState.speedInKMH < 3f && obdRpm > 0 }

    // Live trip-average km/L: ADR-007 Slice 2 — sourced from the ObdPollingService in-memory
    // O(1) trip accumulator (replaces the 2s obd_sample re-query LaunchedEffect loop).
    val tripAvgKmL = connectedObd?.tripAvgKmL

    val panelState = TrackPanelState(
        tripState = when {
            locationUiState.isPaused -> TripState.PAUSED
            locationUiState.isTracking -> TripState.LIVE
            else -> TripState.READY
        },
        elapsedMs = locationUiState.durationTimer,
        distanceKm = locationUiState.distanceInMeters / 1000.0,
        speedKmh = locationUiState.speedInKMH.toDouble(),
        obdConnected = connectedObd != null,
        instantKmL = instantKmL,
        idleFuelLph = idleFuelLph,
        fuelRateLph = connectedObd?.fuelRateLph,
        tripAvgKmL = tripAvgKmL,
        fuelSource = connectedObd?.fuelSource
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
                val order = activeOrder
                if (order != null) {
                    // ADR-014 (provisional UI): order card replaces the trip panel while an order is active.
                    GojekOrderCard(
                        order = order,
                        modifier = Modifier
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    )
                } else {
                    TripPanel(
                        state = panelState,
                        onCtaTap = { viewModel.onTripCtaTap() },
                        modifier = Modifier
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    )
                }
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
