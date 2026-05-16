package com.kolee.tracklocation.screens.list

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.permission.CheckAndRequestPermissions
import com.kolee.tracklocation.screens.list.components.ListContent
import com.kolee.tracklocation.tracking.Actions
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.ui.theme.TripBackground
import com.kolee.tracklocation.viewmodel.ShareViewModel

@Composable
fun ListScreen(
    onSelect: (trackIdx: Int) -> Unit
) {
    val context = LocalContext.current
    val viewModel: ShareViewModel = viewModel(
        factory = ShareViewModel.Factory
    )
    val responseState = viewModel.responseState
    val locationUiState by viewModel.locationUiState.collectAsState()
    var requestRecordingPermission by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TripBackground,
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TripBackground)
                    .padding(padding)
            ) {
                ListContent(
                    responseState = responseState,
                    onSelect = onSelect,
                    viewModel = viewModel,
                    isAlwaysRecording = locationUiState.isAlwaysRecording,
                    onAlwaysRecordingChange = { enabled ->
                        if (enabled) {
                            requestRecordingPermission = true
                        } else {
                            performTrackingService(context, Actions.STOP_RECORDING)
                        }
                    }
                )

                if (requestRecordingPermission) {
                    CheckAndRequestPermissions(
                        isGranted = {
                            requestRecordingPermission = false
                            performTrackingService(context, Actions.START_RECORDING)
                        }
                    )
                }
            }
        }
    )
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
