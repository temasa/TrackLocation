@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kolee.tracklocation.screens.sessions

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.R
import com.kolee.tracklocation.data.roomdb.SessionEntity
import com.kolee.tracklocation.permission.CheckAndRequestPermissions
import com.kolee.tracklocation.tracking.Actions
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.ui.theme.TripBackground
import com.kolee.tracklocation.ui.theme.TripBorder
import com.kolee.tracklocation.ui.theme.TripBorderSoft
import com.kolee.tracklocation.ui.theme.TripGreen
import com.kolee.tracklocation.ui.theme.TripGreenDark
import com.kolee.tracklocation.ui.theme.TripGreenLabel
import com.kolee.tracklocation.ui.theme.TripGreenMid
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import com.kolee.tracklocation.ui.theme.TripSurface
import com.kolee.tracklocation.ui.theme.TripTertiary
import com.kolee.tracklocation.utils.TimeUtilFormatter
import com.kolee.tracklocation.viewmodel.ShareViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SessionUiItem(
    val index: Int,
    val start: String,
    val durationMs: Long,
    val distanceKm: Float,
    val points: Int,
    val active: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen() {
    val context = LocalContext.current
    val viewModel: ShareViewModel = viewModel(factory = ShareViewModel.Factory)
    val locationUiState by viewModel.locationUiState.collectAsState()
    val sessions = viewModel.sessionsState.mapIndexed { index, session ->
        session.toUiItem(index = sessionsIndex(viewModel.sessionsState.size, index))
    }

    var requestRecordingPermission by remember { mutableStateOf(false) }
    var guardMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = TripBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TripBackground)
                .padding(padding)
        ) {
            PageHeader()
            StatusCard(
                on = locationUiState.isAlwaysRecording,
                isTripActive = locationUiState.isTracking,
                helperText = guardMessage,
                onAlwaysRecordingChange = { enabled ->
                    guardMessage = null
                    if (enabled) {
                        requestRecordingPermission = true
                    } else if (locationUiState.isTracking) {
                        guardMessage = "Always-recording is required while a trip is running."
                    } else {
                        performTrackingService(context, Actions.STOP_RECORDING)
                    }
                }
            )

            if (sessions.isNotEmpty()) {
                SessionsList(
                    sessions = sessions,
                    modifier = Modifier.weight(1f)
                )
            } else {
                EmptySessionsState(modifier = Modifier.weight(1f))
            }
        }

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

@Composable
private fun PageHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 14.dp, end = 22.dp, bottom = 6.dp)
    ) {
        Text(
            text = "Trip Tracker",
            color = TripMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = "Sessions",
            color = TripInk,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 42.sp,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun StatusCard(
    on: Boolean,
    isTripActive: Boolean,
    helperText: String?,
    onAlwaysRecordingChange: (Boolean) -> Unit
) {
    val statusText = if (on) "Active" else "Inactive"
    val descriptionText = when {
        helperText != null -> helperText
        on -> "Recording location sessions in the background."
        else -> "Location sessions are not being recorded."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 12.dp, end = 22.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = TripGreenDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (on) TripGreen.copy(alpha = 0.18f) else TripGreenMid),
                contentAlignment = Alignment.Center
            ) {
                PulseDot(
                    color = if (on) TripGreen else Color(0xFF9AA9A1),
                    size = 14,
                    pulsing = on
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Always-recording",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isTripActive && !on) {
                        Text(
                            text = "Required while a trip is running",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = descriptionText,
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Switch(
                checked = on,
                onCheckedChange = onAlwaysRecordingChange
            )
        }
    }
}

@Composable
private fun EmptySessionsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(TripSurface)
                .border(1.5f.dp, TripBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_session_signal),
                contentDescription = null,
                tint = TripTertiary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = "No sessions recorded yet",
            color = TripInk,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text = buildAnnotatedString {
                append("Sessions appear here when always-recording is turned ")
                withStyle(SpanStyle(color = TripInk, fontWeight = FontWeight.SemiBold)) {
                    append("ON")
                }
                append(".")
            },
            color = TripMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 14.dp)
                .width(280.dp)
        )
    }
}

private fun SessionEntity.toUiItem(index: Int): SessionUiItem {
    return SessionUiItem(
        index = index,
        start = SimpleDateFormat("EEE, HH:mm", Locale.ENGLISH).format(Date(startedAt)),
        durationMs = durationMillis,
        distanceKm = (distanceMeters / 1000.0).toFloat(),
        points = pointCount,
        active = isActive
    )
}

private fun sessionsIndex(size: Int, index: Int): Int = size - index

@Composable
private fun SessionsList(
    sessions: List<SessionUiItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, top = 18.dp, end = 22.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Recorded sessions",
                    color = TripInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Newest first",
                    color = TripMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        items(sessions.size) { index ->
            SessionRow(item = sessions[index])
        }
    }
}

@Composable
private fun SessionRow(item: SessionUiItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 12.dp, end = 22.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TripSurface)
            .border(
                width = 1.5f.dp,
                color = if (item.active) TripGreen else TripBorderSoft,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(start = 20.dp, top = 18.dp, end = 18.dp, bottom = 18.dp)
    ) {
        if (item.active) {
            ActiveBadge(
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MarkerGutter(accent = if (item.active) TripGreen else TripGreenLabel)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Session #${item.index}",
                        color = TripInk,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.start,
                        color = TripMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(end = if (item.active) 70.dp else 0.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 14.dp)
                        .offset(x = (-34).dp)
                        .height(1.dp)
                        .background(TripBorderSoft)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SessionMetric(
                        value = TimeUtilFormatter.getTime(item.durationMs),
                        label = "duration",
                        modifier = Modifier.weight(1f)
                    )
                    SessionMetric(
                        value = String.format(Locale.ENGLISH, "%.2f km", item.distanceKm),
                        label = "distance",
                        modifier = Modifier.weight(1f)
                    )
                    SessionMetric(
                        value = item.points.toString(),
                        label = "points",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(TripGreen.copy(alpha = 0.12f))
            .padding(start = 8.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PulseDot(color = TripGreen, size = 7, pulsing = true)
        Text(
            text = "ACTIVE",
            color = TripGreenLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun MarkerGutter(accent: Color) {
    Column(
        modifier = Modifier
            .width(14.dp)
            .padding(top = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(TripSurface)
                .border(2.5f.dp, accent, CircleShape)
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = 2.5f.dp, height = 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
    }
}

@Composable
private fun SessionMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = TripInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 17.sp,
            letterSpacing = 0.sp,
            maxLines = 1
        )
        Text(
            text = label,
            color = TripMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun PulseDot(
    color: Color,
    size: Int,
    pulsing: Boolean
) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) 0.55f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
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
