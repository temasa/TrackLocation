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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.kolee.tracklocation.ui.components.MetricCell
import com.kolee.tracklocation.ui.components.MetricEmpty
import com.kolee.tracklocation.ui.components.MetricStrip
import com.kolee.tracklocation.ui.components.MetricTheme
import com.kolee.tracklocation.ui.components.formatMetricPair
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
            .border(1.dp, PanelBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        Column {
            EyebrowRow(state = state)
            Spacer(Modifier.height(4.dp))
            TimerCtaRow(state = state, onCtaTap = onCtaTap)
            Spacer(Modifier.height(10.dp))
            CombinedMetricsRow(state = state)
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
        TripState.LIVE -> "Stop trip"
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
                TripState.LIVE -> drawStopGlyph(BrandGreenDark)
                else -> drawPlayGlyph(BrandGreenDark)
            }
        }
    }
}

// UI-SPEC §3g / §5a: single MetricStrip; OBD cells only appear when obdConnected.
@Composable
private fun CombinedMetricsRow(state: TrackPanelState) {
    val distText = "%.1f".format(state.distanceKm)
    val speedText = "%.1f".format(state.speedKmh)
    MetricStrip(modifier = Modifier.fillMaxWidth(), theme = MetricTheme.Dark) {
        MetricCell(
            label = "DIST (km)",
            value = distText,
            modifier = Modifier.weight(1f),
            contentDescription = "$distText kilometers"
        )
        MetricCell(
            label = "SPEED (km/h)",
            value = speedText,
            modifier = Modifier.weight(1f),
            contentDescription = "$speedText kilometers per hour"
        )
        if (state.obdConnected) {
            val lphText = state.fuelRateLph?.takeIf { it > 0.0 }?.let { "%.1f".format(it) } ?: MetricEmpty
            MetricCell(
                label = "FLOW (L/h)",
                value = lphText,
                modifier = Modifier.weight(1f),
                contentDescription = "Fuel rate $lphText litres per hour"
            )
            val avgText = state.tripAvgKmL?.let { "%.1f".format(it) }
            val instText = state.instantKmL?.let { "%.1f".format(it) }
            MetricCell(
                label = "AVG/INST (km/L)",
                value = formatMetricPair(avgText, instText),
                modifier = Modifier.weight(1.4f),
                contentDescription = "Average ${avgText ?: "unavailable"}, instant ${instText ?: "unavailable"} kilometers per litre"
            )
        }
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

// Stop glyph: filled rounded square (24×24 viewbox)
private fun DrawScope.drawStopGlyph(color: Color) {
    val sx = size.width / 24f; val sy = size.height / 24f
    val path = Path().apply {
        addRoundRect(RoundRect(6f * sx, 6f * sy, 18f * sx, 18f * sy, CornerRadius(2f * sx, 2f * sy)))
    }
    drawPath(path, color)
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun backdropBlurModifier(): Modifier = Modifier.graphicsLayer {
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
