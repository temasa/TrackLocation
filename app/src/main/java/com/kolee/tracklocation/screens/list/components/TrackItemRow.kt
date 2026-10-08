package com.kolee.tracklocation.screens.list.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.ui.theme.MonospaceFontFamily
import com.kolee.tracklocation.ui.theme.TripBorder
import com.kolee.tracklocation.ui.theme.TripGreen
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import com.kolee.tracklocation.ui.theme.TripSurface
import com.kolee.tracklocation.utils.TimeUtilFormatter
import com.kolee.tracklocation.utils.formatCompactRupiah
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackItemRow(
    item: TrackEntity,
    costText: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCostClick: () -> Unit = {},
    costColor: Color = TripInk,
    // ADR-022: price-per-litre in effect at trip start (0.0 = none); used for the order net.
    pricePerLiter: Double = 0.0
) {
    val distanceText = "${String.format("%.2f", item.distance / 1000f)} km"
    val timeText = TimeUtilFormatter.getTime(item.duration)
    val averageSpeedText = if (item.duration > 0) {
        val hours = item.duration / 3_600_000f
        val kilometers = item.distance / 1000f
        "${String.format(Locale.ENGLISH, "%.1f", kilometers / hours)} km/h"
    } else {
        ""
    }
    val efficiencyText = if (item.obdFuelConsumedL > 0.0) {
        String.format(Locale.ENGLISH, "%.1f", (item.distance / 1000f) / item.obdFuelConsumedL)
    } else {
        "—"
    }
    // ADR-022: order trips show "price / net" (net = earnings - litres x price at trip start).
    val orderLabel = item.orderLabel
    val orderPriceNetText = if (orderLabel != null) {
        val earnings = item.orderEarningsRp
        if (earnings == null) {
            "— / —"
        } else {
            val cost = if (item.obdFuelConsumedL > 0.0 && pricePerLiter > 0.0) {
                Math.round(item.obdFuelConsumedL * pricePerLiter)
            } else null
            "${formatCompactRupiah(earnings)} / ${cost?.let { formatCompactRupiah(earnings - it) } ?: "—"}"
        }
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TripSurface)
            .border(1.dp, TripBorder, RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(modifier = Modifier.weight(1f)) {
            RouteIndicator()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = orderLabel ?: "Track #${item.idx}",
                    color = TripInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "",
                    color = TripInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 28.dp)
                )
            }
            Text(
                text = SimpleDateFormat("EEE, HH:mm", Locale.ENGLISH).format(item.timestamp),
                color = TripMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TripBorder)
        )
        // Base row — distance / duration / avg speed (mirrors ActiveTripRow's two-row grid so
        // the five metrics no longer clip in a single cramped 5-column row).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            TrackStat(
                value = distanceText,
                label = "km",
                modifier = Modifier.weight(1f)
            )
            TrackStat(
                value = timeText,
                label = "duration",
                modifier = Modifier.weight(1f)
            )
            TrackStat(
                value = averageSpeedText,
                label = "avg speed",
                modifier = Modifier.weight(1f)
            )
        }
        // Fuel row — km/L / cost / (order trips) price / net; the third cell is otherwise empty to
        // keep columns aligned with the base row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            TrackStat(
                value = efficiencyText,
                label = "km/L",
                modifier = Modifier.weight(1f)
            )
            TrackStat(
                value = costText,
                label = "cost",
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCostClick),
                valueColor = costColor
            )
            if (orderPriceNetText != null) {
                TrackStat(
                    value = orderPriceNetText,
                    label = "price / net",
                    // Wider + smaller than its neighbours: "Rp28k / Rp19.6k" does not fit a 1/3 cell.
                    modifier = Modifier
                        .weight(1.5f)
                        .semantics { contentDescription = "Order price and net profit, $orderPriceNetText" },
                    valueFontSize = 12.sp
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RouteIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .border(1.dp, TripGreen, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(width = 2.dp, height = 30.dp)
                .background(TripGreen.copy(alpha = 0.55f))
        )
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(TripGreen)
        )
    }
}

@Composable
private fun TrackStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TripInk,
    valueFontSize: TextUnit = 14.sp
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = valueColor,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Medium,
            fontFamily = MonospaceFontFamily,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            color = TripMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start
        )
    }
}
