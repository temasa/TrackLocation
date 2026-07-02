package com.kolee.tracklocation.screens.track.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.screens.track.TrackPanelState
import com.kolee.tracklocation.screens.track.TripState
import com.kolee.tracklocation.ui.theme.BrandGreen
import com.kolee.tracklocation.ui.theme.BrandGreenDark
import com.kolee.tracklocation.ui.theme.MonospaceFontFamily
import com.kolee.tracklocation.ui.theme.PanelBg
import com.kolee.tracklocation.ui.theme.PanelBgFallback
import com.kolee.tracklocation.ui.theme.PanelBorder
import com.kolee.tracklocation.ui.theme.PanelTextPrimary
import com.kolee.tracklocation.ui.theme.PanelTextSecondary
import com.kolee.tracklocation.ui.theme.PanelTextTertiary
import com.kolee.tracklocation.ui.theme.StatusPaused
import com.kolee.tracklocation.utils.TimeUtilFormatter
import kotlin.math.cos
import kotlin.math.sin

private val EyebrowStyle = TextStyle(
    fontFamily = MonospaceFontFamily,
    fontSize = 10.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.2.sp,
    color = PanelTextSecondary
)

private val TimerStyle = TextStyle(
    fontFamily = MonospaceFontFamily,
    fontSize = 30.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.8).sp,
    color = PanelTextPrimary,
    fontFeatureSettings = "tnum"
)

private val UnitLabelStyle = TextStyle(
    fontSize = 10.sp,
    letterSpacing = 0.4.sp,
    color = PanelTextSecondary
)

private val MetricValueStyle = TextStyle(
    fontFamily = MonospaceFontFamily,
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.3).sp,
    color = PanelTextPrimary,
    fontFeatureSettings = "tnum"
)

@Composable
fun TripPanel(
    state: TrackPanelState,
    onCtaTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelBg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PanelBg else PanelBgFallback

    // Hidden text that changes on state transitions to trigger liveRegion announcements
    val liveAnnouncement = when (state.tripState) {
        TripState.LIVE -> "Trip started"
        TripState.PAUSED -> "Trip paused at ${TimeUtilFormatter.getTime(state.elapsedMs)}"
        else -> ""
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x470A1410),
                spotColor = Color(0x470A1410)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(panelBg)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) backdropBlurModifier()
                else Modifier
            )
            .border(1.dp, PanelBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        Column {
            EyebrowRow(state = state)
            Spacer(Modifier.height(4.dp))
            TimerCtaRow(state = state, onCtaTap = onCtaTap)
            Spacer(Modifier.height(10.dp))
            StatsRow(state = state)
            if (state.obdConnected) {
                Spacer(Modifier.height(9.dp))
                ObdRow(state = state)
            }
        }

        // Invisible live-region for state-change announcements
        if (liveAnnouncement.isNotEmpty()) {
            Text(
                text = liveAnnouncement,
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0f)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
private fun EyebrowRow(state: TrackPanelState) {
    val infiniteTransition = rememberInfiniteTransition()
    // Total cycle: 800ms forward + 800ms reverse = 1600ms per spec
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val dotColor = when (state.tripState) {
        TripState.READY -> PanelTextTertiary
        TripState.LIVE -> BrandGreen
        TripState.PAUSED -> StatusPaused
    }
    val eyebrowText = when (state.tripState) {
        TripState.READY -> "READY FOR TRIP"
        TripState.LIVE -> "TRIP IN PROGRESS"
        TripState.PAUSED -> "TRIP PAUSED"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(if (state.tripState == TripState.LIVE) pulseAlpha else 1f)
                .background(color = dotColor, shape = CircleShape)
        )
        Text(text = eyebrowText, style = EyebrowStyle)
    }
}

@Composable
private fun TimerCtaRow(state: TrackPanelState, onCtaTap: () -> Unit) {
    val elapsed = TimeUtilFormatter.getTime(state.elapsedMs)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = elapsed,
            style = TimerStyle,
            modifier = Modifier.semantics {
                contentDescription = "Elapsed time, ${formatElapsedAccessibility(state.elapsedMs)}"
            }
        )
        TripCtaButton(tripState = state.tripState, onTap = onCtaTap)
    }
}

@Composable
private fun TripCtaButton(tripState: TripState, onTap: () -> Unit) {
    val desc = when (tripState) {
        TripState.READY -> "Start trip"
        TripState.LIVE -> "Pause trip"
        TripState.PAUSED -> "Resume trip"
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color(0x8C22C55E),
                spotColor = Color(0x8C22C55E)
            )
            .clip(CircleShape)
            .background(BrandGreen)
            .clickable(onClick = onTap)
            .semantics { contentDescription = desc }
    ) {
        val glyphSizeDp = if (tripState == TripState.LIVE) 16.dp else 18.dp
        Canvas(modifier = Modifier.size(glyphSizeDp)) {
            when (tripState) {
                TripState.LIVE -> drawPauseGlyph(BrandGreenDark)
                else -> drawPlayGlyph(BrandGreenDark)
            }
        }
    }
}

@Composable
private fun StatsRow(state: TrackPanelState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = PanelBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MetricCell(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
            isDistance = true,
            unit = "KM",
            value = "%.2f".format(state.distanceKm),
            accessibilityText = "%.2f kilometers".format(state.distanceKm)
        )
        MetricCell(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            isDistance = false,
            unit = "KM/HR",
            value = "%.1f".format(state.speedKmh),
            accessibilityText = "%.1f kilometers per hour".format(state.speedKmh)
        )
    }
}

@Composable
private fun MetricCell(
    modifier: Modifier,
    horizontalAlignment: Alignment.Horizontal,
    isDistance: Boolean,
    unit: String,
    value: String,
    accessibilityText: String
) {
    Column(
        modifier = modifier.semantics { contentDescription = accessibilityText },
        horizontalAlignment = horizontalAlignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                if (isDistance) drawDistanceIcon(PanelTextSecondary)
                else drawSpeedIcon(PanelTextSecondary)
            }
            Text(text = unit, style = UnitLabelStyle)
        }
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MetricValueStyle)
    }
}

// OBD Phase 2 Slice 4 — fuel row: instant km/L (or idle L/h) + live trip-average km/L.
@Composable
private fun ObdRow(state: TrackPanelState) {
    val fuelValue = when {
        state.instantKmL != null -> "%.1f km/L".format(state.instantKmL)
        state.idleFuelLph != null -> "%.1f L/h".format(state.idleFuelLph)
        else -> "—"
    }
    val avgValue = state.tripAvgKmL?.let { "%.1f km/L".format(it) } ?: "—"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = PanelBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ObdMetricCell(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
            label = "FUEL",
            value = fuelValue,
            accessibilityText = "Fuel, $fuelValue"
        )
        ObdMetricCell(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            label = "TRIP AVG",
            value = avgValue,
            accessibilityText = "Trip average, $avgValue"
        )
    }
}

@Composable
private fun ObdMetricCell(
    modifier: Modifier,
    horizontalAlignment: Alignment.Horizontal,
    label: String,
    value: String,
    accessibilityText: String
) {
    Column(
        modifier = modifier.semantics { contentDescription = accessibilityText },
        horizontalAlignment = horizontalAlignment
    ) {
        Text(text = label, style = UnitLabelStyle)
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MetricValueStyle)
    }
}

// --- Canvas draw helpers ---

// Play glyph: M 8 5 L 20 12 L 8 19 Z (24×24 viewbox scaled to canvas size)
private fun DrawScope.drawPlayGlyph(color: Color) {
    val sx = size.width / 24f; val sy = size.height / 24f
    val path = Path().apply {
        moveTo(8f * sx, 5f * sy)
        lineTo(20f * sx, 12f * sy)
        lineTo(8f * sx, 19f * sy)
        close()
    }
    drawPath(path, color)
}

// Pause glyph: two rounded rects x=6,w=4.5 and x=13.5,w=4.5 (24×24 viewbox)
private fun DrawScope.drawPauseGlyph(color: Color) {
    val sx = size.width / 24f; val sy = size.height / 24f
    val rx = CornerRadius(1.2f * sx, 1.2f * sy)
    val path = Path().apply {
        addRoundRect(RoundRect(6f * sx, 5f * sy, 10.5f * sx, 19f * sy, rx))
        addRoundRect(RoundRect(13.5f * sx, 5f * sy, 18f * sx, 19f * sy, rx))
    }
    drawPath(path, color)
}

// Distance (location pin) icon: circle head + tapered sides + inner circle
private fun DrawScope.drawDistanceIcon(color: Color) {
    val w = size.width; val h = size.height
    val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val cx = w * 0.5f
    val headCy = h * 0.40f
    val headR = w * 0.34f

    drawCircle(color, radius = headR, center = Offset(cx, headCy), style = stroke)

    // Tapered lines from circle base to pin tip
    val angRad = Math.toRadians(28.0).toFloat()
    val lx = cx - headR * sin(angRad)
    val rx = cx + headR * sin(angRad)
    val baseY = headCy + headR * cos(angRad)
    val tipY = h * 0.93f

    drawLine(color, Offset(lx, baseY), Offset(cx, tipY), 2.2.dp.toPx(), StrokeCap.Round)
    drawLine(color, Offset(rx, baseY), Offset(cx, tipY), 2.2.dp.toPx(), StrokeCap.Round)

    // Inner circle (hole in pin head)
    drawCircle(color, radius = headR * 0.37f, center = Offset(cx, headCy), style = stroke)
}

// Speed (lightning bolt) icon: M13 2 4 14h7l-1 8 9-12h-7l1-8z
private fun DrawScope.drawSpeedIcon(color: Color) {
    val sx = size.width / 24f; val sy = size.height / 24f
    val path = Path().apply {
        moveTo(13f * sx, 2f * sy)
        lineTo(4f * sx, 14f * sy)
        lineTo(11f * sx, 14f * sy)
        lineTo(10f * sx, 22f * sy)
        lineTo(19f * sx, 10f * sy)
        lineTo(12f * sx, 10f * sy)
        close()
    }
    drawPath(path, color, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@RequiresApi(Build.VERSION_CODES.S)
private fun backdropBlurModifier(): Modifier = Modifier.graphicsLayer {
    renderEffect = android.graphics.RenderEffect
        .createBlurEffect(28f, 28f, android.graphics.Shader.TileMode.CLAMP)
        .asComposeRenderEffect()
}

private fun formatElapsedAccessibility(elapsedMs: Long): String {
    val total = elapsedMs / 1000
    val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
    return buildString {
        if (h > 0) append("$h ${if (h == 1L) "hour" else "hours"} ")
        if (m > 0) append("$m ${if (m == 1L) "minute" else "minutes"} ")
        append("$s ${if (s == 1L) "second" else "seconds"}")
    }
}
