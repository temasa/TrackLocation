package com.kolee.tracklocation.screens.sessions

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.R
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
import java.util.Locale

private data class SessionUiItem(
    val index: Int,
    val start: String,
    val durationMs: Long,
    val distanceKm: Float,
    val points: Int,
    val active: Boolean = false
)

private val sampleSessions = listOf(
    SessionUiItem(
        index = 7,
        start = "Today, 08:19",
        durationMs = (12 * 60 + 34) * 1000L,
        distanceKm = 1.84f,
        points = 184,
        active = true
    ),
    SessionUiItem(
        index = 6,
        start = "Wed, 07:02",
        durationMs = ((1 * 60 + 14) * 60 + 22) * 1000L,
        distanceKm = 9.42f,
        points = 1862
    ),
    SessionUiItem(
        index = 5,
        start = "Tue, 18:41",
        durationMs = (41 * 60 + 8) * 1000L,
        distanceKm = 3.07f,
        points = 742
    )
)

@Composable
fun SessionsScreen() {
    var recordingOn by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TripBackground)
    ) {
        PageHeader(
            title = "Sessions",
            right = {
                RecordingSwitch(
                    on = recordingOn,
                    onClick = { recordingOn = !recordingOn }
                )
            }
        )
        StatusHero(
            on = recordingOn,
            elapsed = "00:12:34",
            points = 184
        )

        if (recordingOn) {
            SessionsList(
                sessions = sampleSessions,
                modifier = Modifier.weight(1f)
            )
        } else {
            EmptySessionsState(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PageHeader(
    title: String,
    right: @Composable () -> Unit
) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = TripInk,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp,
                letterSpacing = (-1.2).sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            right()
        }
    }
}

@Composable
private fun RecordingSwitch(
    on: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(TripSurface)
            .border(
                width = 1.5f.dp,
                color = if (on) TripGreen else TripBorder,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (on) "ON" else "OFF",
            color = if (on) TripGreenLabel else TripMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (on) TripGreen else Color(0xFFD6D8D4))
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (on) 18.dp else 2.dp, y = 2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(TripSurface)
            )
        }
    }
}

@Composable
private fun StatusHero(
    on: Boolean,
    elapsed: String,
    points: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 14.dp, end = 22.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(TripGreenDark)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (on) TripGreen.copy(alpha = 0.18f) else TripGreenMid),
            contentAlignment = Alignment.Center
        ) {
            if (on) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TripGreen.copy(alpha = 0.22f))
                )
            }
            PulseDot(
                color = if (on) TripGreen else Color(0xFF9AA9A1),
                size = 16,
                pulsing = on
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Always-recording",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = if (on) "Recording" else "Idle",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 25.sp,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (on) {
                Text(
                    text = "$elapsed · $points points",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
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
            letterSpacing = (-0.2).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text = buildAnnotatedString {
                append("Sessions appear here when always-recording is turned ")
                withStyle(SpanStyle(color = TripInk, fontWeight = FontWeight.SemiBold)) {
                    append("ON")
                }
                append(" from the Sessions header.")
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
                    letterSpacing = (-0.2).sp,
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
                        letterSpacing = (-0.2).sp,
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
            letterSpacing = (-0.2).sp,
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
    val transition = rememberInfiniteTransition(label = "sessionPulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) 0.55f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sessionPulseAlpha"
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}
