package com.kolee.tracklocation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.screens.details.components.RouteComparisonCard
import com.kolee.tracklocation.screens.track.components.RouteLegend
import com.kolee.tracklocation.utils.RouteComparison
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.screens.details.components.DetailsTrackMap
import com.kolee.tracklocation.screens.details.components.TopBar
import com.kolee.tracklocation.viewmodel.ShareViewModel

@Composable
fun DetailsScreen(
    trackIdx: Int,
    onNavigateUp: () -> Unit
) {
    val viewModel: ShareViewModel = viewModel(
        factory = ShareViewModel.Factory
    )

    val selectedTrackPathPoints = viewModel.selectedTrackPathPoints
    val plannedRoute = viewModel.selectedTrackPlannedRoute
    val plannedDistanceM = viewModel.selectedTrackPlannedDistanceM
    val track = viewModel.selectedTrackState
    // ADR-026: derived at display time, never persisted.
    val comparison = remember(plannedRoute, plannedDistanceM, selectedTrackPathPoints, track.distance) {
        plannedRoute?.let {
            RouteComparison.compare(it, plannedDistanceM, selectedTrackPathPoints, track.distance)
        }
    }

    LaunchedEffect(key1 = trackIdx) {
        if (trackIdx > 0) {
            viewModel.getTrack(trackIdx)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        DetailsTrackMap(
            pathPoints = selectedTrackPathPoints,
            plannedRoute = plannedRoute
        )

        if (plannedRoute != null) {
            // UI-SPEC §3j: two-entry legend (no Optimized; it is never saved), top-end.
            RouteLegend(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 24.dp),
                showOptimized = false
            )
        }

        // ADR-026 / UI-SPEC §3j: bottom stack of attribution + comparison card, or the quiet empty state.
        if (selectedTrackPathPoints.size >= 2) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (plannedRoute != null) {
                    Text(
                        text = "© openrouteservice.org | © OpenStreetMap contributors",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    RouteComparisonCard(comparison = comparison)
                } else {
                    Text(
                        text = "No planned route saved",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        TopBar(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            onNavigateUp.invoke()
        }
    }
}
