package com.kolee.tracklocation.screens.track

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.kolee.tracklocation.screens.track.components.TrackMap
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.permission.CheckAndRequestPermissions
import com.kolee.tracklocation.screens.track.components.RunningCard
import com.kolee.tracklocation.tracking.Actions
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.utils.LocationUtils
import com.kolee.tracklocation.viewmodel.ShareViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "TrackScreen"

@Composable
fun TrackScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: ShareViewModel = viewModel(
        factory = ShareViewModel.Factory
    )

    val locationUiState by viewModel.locationUiState.collectAsState()
    var performRequestPermission by remember { mutableStateOf(true) }
    var allPermissionsGranted by remember { mutableStateOf(false) }

    var isShowRunningCard by remember { mutableStateOf(false)}

    LaunchedEffect(key1 = Unit) {
        delay(300)
        isShowRunningCard = true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        TrackMap(
            currentLocation = locationUiState.currentLocation,
            pathPoints = locationUiState.pathPoints
        )

        if (allPermissionsGranted) {
            AnimatedVisibility(
                visible = isShowRunningCard,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = EnterTransition.None,
                exit = fadeOut()
            ) {
                RunningCard(
                    modifier = Modifier.padding(16.dp),
                    locationUiState = locationUiState
                ) {
                    Log.d(TAG, "RunningCard, onPlayStopClicked trigger")
                    if (locationUiState.isTracking) {
                        scope.launch {
                            val startLocationId = locationUiState.activeTripStartLocationId
                            val endLocationId = locationUiState.activeTripEndLocationId
                            if (
                                startLocationId != null
                                && endLocationId != null
                                && endLocationId >= startLocationId
                            ) {
                                viewModel.insertTrack(
                                    TrackEntity(
                                        timestamp = locationUiState.tripStartedAt,
                                        distance = locationUiState.distanceInMeters,
                                        duration = locationUiState.durationTimer,
                                        pathPoints = LocationUtils.pathPointsToString(locationUiState.pathPoints),
                                        startLocationId = startLocationId,
                                        endLocationId = endLocationId
                                    )
                                )
                            }
                            Log.d(TAG, "Tracking location stop")
                            performTrackingService(context, Actions.STOP_TRIP)
                        }
                    }
                    else {
                        Log.d(TAG, "Tracking location start")
                        performTrackingService(context, Actions.START_TRIP)
                    }
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

private fun performTrackingService(
    context: Context,
    actions: Actions
) {
    Intent(context, TrackingService::class.java).also {
        it.action = actions.name
        ContextCompat.startForegroundService(context, it)
    }
}
