package com.kolee.tracklocation.screens.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.screens.details.components.DetailsTrackMap
import com.kolee.tracklocation.screens.details.components.TopBar
import com.kolee.tracklocation.viewmodel.ShareViewModel

@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier,
    trackIdx: Int,
    onNavigateUp: () -> Unit
) {
    val viewModel: ShareViewModel = viewModel(
        factory = ShareViewModel.Factory
    )

    val selectedTrackState = viewModel.selectedTrackState
    val selectedTrackPathPoints = viewModel.selectedTrackPathPoints

    LaunchedEffect(key1 = trackIdx) {
        if (trackIdx > 0) {
            viewModel.getTrack(trackIdx)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        DetailsTrackMap(
            pathPoints = selectedTrackPathPoints
        )

        TopBar(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            onNavigateUp.invoke()
        }
    }
}
