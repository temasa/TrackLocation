package com.kolee.tracklocation.screens.track.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kolee.tracklocation.tracking.LocationUiState
import com.kolee.tracklocation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningCard(
    modifier: Modifier = Modifier,
    locationUiState: LocationUiState,
    onPlayStopClicked: () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        RunningCardTime(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            durationTimer = locationUiState.durationTimer,
            isTracking = locationUiState.isTracking,
            onPlayStopClicked = onPlayStopClicked
        )

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp)
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
        ) {
            RunningStatusItem(
                modifier = Modifier,
                painter = painterResource(id = R.drawable.running_boy),
                unit = "km",
                value = String.format("%.2f", locationUiState.distanceInMeters / 1000f)
            )
            RunningStatusItem(
                modifier = Modifier,
                painter = painterResource(id = R.drawable.bolt),
                unit = "km/hr",
                value = String.format("%.2f", locationUiState.speedInKMH)
            )
        }
    }
}