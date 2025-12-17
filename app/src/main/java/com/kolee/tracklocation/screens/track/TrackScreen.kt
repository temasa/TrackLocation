package com.kolee.tracklocation.screens.track

import android.content.Context
import android.content.Intent
import android.util.Log
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.permission.CheckAndRequestPermissions
import com.kolee.tracklocation.screens.track.components.RunningCard
import com.kolee.tracklocation.tracking.Actions
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.viewmodel.ShareViewModel
import kotlinx.coroutines.delay

private const val TAG = "TrackScreen"

@Composable
fun TrackScreen() {

    val context = LocalContext.current

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
                    performTrackingService(context, locationUiState.isTracking)
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
    isTracking: Boolean
) {
    Intent(context, TrackingService::class.java).also {
        Log.d(TAG, "performTrackingService")
        if (!isTracking) {
            Log.d(TAG, "performTrackingService, launch START action")
            it.action = Actions.START.name
            context.startService(it)
        }
        else {
            Log.d(TAG, "performTrackingService, launch STOP action")
            it.action = Actions.STOP.name
            context.startService(it)
        }
    }
}