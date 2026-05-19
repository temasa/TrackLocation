package com.kolee.tracklocation.screens.list.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.R
import com.kolee.tracklocation.tracking.LocationUiState
import com.kolee.tracklocation.ui.theme.MonospaceFontFamily
import com.kolee.tracklocation.ui.theme.TripBackground
import com.kolee.tracklocation.ui.theme.TripBlue
import com.kolee.tracklocation.ui.theme.TripBorder
import com.kolee.tracklocation.ui.theme.TripGold
import com.kolee.tracklocation.ui.theme.TripGreen
import com.kolee.tracklocation.ui.theme.TripGreenDark
import com.kolee.tracklocation.ui.theme.TripGreenMid
import com.kolee.tracklocation.ui.theme.TripHeroBg
import com.kolee.tracklocation.ui.theme.TripHeroBrandGreen
import com.kolee.tracklocation.ui.theme.TripHeroDim
import com.kolee.tracklocation.ui.theme.TripHeroDotHalo
import com.kolee.tracklocation.ui.theme.TripHeroDotIdle
import com.kolee.tracklocation.ui.theme.TripHeroEyebrow
import com.kolee.tracklocation.ui.theme.TripHeroIndicatorWash
import com.kolee.tracklocation.ui.theme.TripHeroReadoutIdle
import com.kolee.tracklocation.ui.theme.TripHeroReadoutLive
import com.kolee.tracklocation.ui.theme.TripHeroStopRed
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import com.kolee.tracklocation.ui.theme.TripSurface
import com.kolee.tracklocation.ui.theme.TripSurfaceMuted
import com.kolee.tracklocation.viewmodel.Response
import com.kolee.tracklocation.viewmodel.ShareViewModel
import kotlinx.coroutines.delay
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
            val uiState by viewModel.locationUiState.collectAsState()
            CurrentTripCard(
                uiState = uiState,
                onCtaTap = { viewModel.onTripCtaTap() }
            )
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
private fun CurrentTripCard(
    uiState: LocationUiState,
    onCtaTap: () -> Unit = {}
) {
    val isRecording = uiState.isTracking && !uiState.isPaused
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
                as? AccessibilityManager
        // isReduceMotionEnabled is not directly available; treat enabled-with-touch-exploration
        // or animator-duration-scale = 0 as reduced motion. Conservative default: false.
        am?.let {
            try {
                android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
                ) == 0f
            } catch (_: Throwable) { false }
        } ?: false
    }

    val now by produceState(initialValue = System.currentTimeMillis(), isRecording) {
        if (isRecording) {
            while (true) {
                value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }
    val elapsedText = if (isRecording && uiState.tripStartedAt > 0L) {
        formatElapsed(now - uiState.tripStartedAt)
    } else {
        "00:00:00"
    }

    val outerColor by animateColorAsState(
        targetValue = if (isRecording) TripHeroIndicatorWash else TripHeroDim,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    )
    val dotColor by animateColorAsState(
        targetValue = if (isRecording) TripHeroBrandGreen else TripHeroDotIdle,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    )
    val buttonBg by animateColorAsState(
        targetValue = if (isRecording) TripHeroStopRed else TripHeroBrandGreen,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
    )

    val blinkAlpha: Float = if (isRecording && !reduceMotion) {
        val transition = rememberInfiniteTransition()
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
        animated
    } else {
        1f
    }

    val titleText = if (isRecording) "Recording" else "Ready"
    val liveAnnouncement = if (isRecording) "Trip started" else "Trip stopped"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(TripHeroBg)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = liveAnnouncement
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRecording && !reduceMotion) {
                PulseRing(delayMs = 0)
                PulseRing(delayMs = 600)
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(outerColor),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    // Halo
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(TripHeroDotHalo)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .alpha(blinkAlpha)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Current trip",
                color = TripHeroEyebrow,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                lineHeight = 22.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = elapsedText,
                color = if (isRecording) TripHeroReadoutLive else TripHeroReadoutIdle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MonospaceFontFamily,
                letterSpacing = 0.sp,
                lineHeight = 13.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { contentDescription = "Elapsed time, $elapsedText" }
            )
        }

        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(buttonBg)
                .clickable(onClick = onCtaTap)
                .padding(horizontal = 22.dp)
                .semantics {
                    contentDescription = if (isRecording) "Stop trip" else "Start trip"
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = Color.White,
                            shape = if (isRecording) RoundedCornerShape(1.5.dp) else CircleShape
                        )
                )
                Text(
                    text = if (isRecording) "Stop" else "Start",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PulseRing(delayMs: Int) {
    val transition = rememberInfiniteTransition()
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing, delayMillis = delayMs),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing, delayMillis = delayMs),
            repeatMode = RepeatMode.Restart,
        ),
    )
    Box(
        modifier = Modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = ringAlpha
            }
            .border(2.dp, TripHeroBrandGreen, CircleShape)
    )
}

private fun formatElapsed(elapsedMs: Long): String {
    val total = (elapsedMs.coerceAtLeast(0L)) / 1000L
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val s = total % 60L
    return String.format(Locale.ENGLISH, "%02d:%02d:%02d", h, m, s)
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
