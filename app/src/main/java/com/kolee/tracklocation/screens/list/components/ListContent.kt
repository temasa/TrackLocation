package com.kolee.tracklocation.screens.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.R
import com.kolee.tracklocation.ui.theme.TripBackground
import com.kolee.tracklocation.ui.theme.TripBlue
import com.kolee.tracklocation.ui.theme.TripBorder
import com.kolee.tracklocation.ui.theme.TripGold
import com.kolee.tracklocation.ui.theme.TripGreen
import com.kolee.tracklocation.ui.theme.TripGreenDark
import com.kolee.tracklocation.ui.theme.TripGreenMid
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import com.kolee.tracklocation.ui.theme.TripSurface
import com.kolee.tracklocation.ui.theme.TripSurfaceMuted
import com.kolee.tracklocation.viewmodel.Response
import com.kolee.tracklocation.viewmodel.ShareViewModel
import java.util.Locale


@Composable
fun ListContent(
    modifier: Modifier = Modifier,
    responseState: Response,
    onSelect: (trackIdx: Int) -> Unit,
    viewModel: ShareViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TripBackground)
    ) {
        when (responseState) {
            is Response.Loading -> LoadingIndicator()

            is Response.Success -> {
                val trackList = responseState.data

                TrackSuccessState(
                    trackList = trackList,
                    onSelect = onSelect,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun TrackSuccessState(
    trackList: List<TrackEntity>,
    onSelect: (trackIdx: Int) -> Unit,
    viewModel: ShareViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 24.dp,
            end = 20.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ListHeader() }
        item {
            CurrentTripCard(onStartTrip = { viewModel.onTripCtaTap() })
        }
        item {
            MetricsRow(trackList = trackList)
        }
        item {
            SearchBar()
        }
        item {
            RecentTripsHeader()
        }

        if (trackList.isEmpty()) {
            item {
                EmptyTripsCard()
            }
        } else {
            items(
                items = trackList,
                key = { trackItem -> trackItem.idx }
            ) { item ->
                var showDialogForDeletion by remember { mutableStateOf(false) }

                TrackItemRow(
                    item = item,
                    onClick = { onSelect.invoke(item.idx) },
                    onLongClick = { showDialogForDeletion = true }
                )

                if (showDialogForDeletion) {
                    CustomAlertDialog(
                        title = "Are you sure to delete?"
                    ) { isDelete ->
                        showDialogForDeletion = false
                        if (isDelete) viewModel.deleteTrack(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Trip Tracker",
                color = TripMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Trips",
                color = TripInk,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 42.sp
            )
        }
    }
}

@Composable
private fun CurrentTripCard(onStartTrip: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(TripGreenDark)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(TripGreenMid),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TripGreen)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Current trip",
                color = Color(0xFFCFE3DC),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Ready to record",
                color = TripSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )
            Text(
                text = "",
                color = Color(0xFFCFE3DC),
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(TripGreen)
                .clickable(onClick = onStartTrip)
                .padding(horizontal = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Start",
                color = TripSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricsRow(trackList: List<TrackEntity>) {
    val totalTrips = trackList.size
    val totalDistanceKm = trackList.sumOf { it.distance } / 1000f
    val totalHours = trackList.sumOf { it.duration } / 3_600_000f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        MetricCard(
            value = totalTrips.toString(),
            label = "Trips",
            color = TripGreen,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            value = formatMetric(totalDistanceKm),
            label = "Distance",
            color = TripBlue,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            value = formatMetric(totalHours),
            label = "Hours",
            color = TripGold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TripSurface)
            .border(1.dp, TripBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            color = TripMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SearchBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TripSurface)
            .border(1.dp, TripBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "",
            color = TripMuted,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TripSurfaceMuted)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "",
                color = TripInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecentTripsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent trips",
            color = TripInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Newest first",
            color = TripMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyTripsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TripSurface)
            .border(1.dp, TripBorder, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.no_item_description),
            color = TripMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun formatMetric(value: Float): String {
    if (value == 0f) return "0"
    return if (value >= 100) {
        String.format(Locale.ENGLISH, "%,.0f", value)
    } else {
        String.format(Locale.ENGLISH, "%.1f", value)
    }
}
