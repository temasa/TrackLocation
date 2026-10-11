package com.kolee.tracklocation.screens.list.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
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
import com.kolee.tracklocation.ui.components.MetricCell
import com.kolee.tracklocation.ui.components.MetricEmpty
import com.kolee.tracklocation.ui.components.MetricSize
import com.kolee.tracklocation.ui.components.MetricStrip
import com.kolee.tracklocation.ui.components.MetricTheme
import com.kolee.tracklocation.ui.components.formatCompactAmount
import com.kolee.tracklocation.ui.components.formatMetricDecimal
import com.kolee.tracklocation.ui.components.formatMetricTime
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
    costColor: Color = TripInk,
    // ADR-022: price-per-litre in effect at trip start (0.0 = none); used for the order net.
    pricePerLiter: Double = 0.0
) {
    val timeText = formatMetricTime(item.duration)
    val distanceText = formatMetricDecimal(item.distance / 1000.0)
    val averageSpeedText = if (item.duration > 0) {
        formatMetricDecimal((item.distance / 1000.0) / (item.duration / 3_600_000.0))
    } else {
        MetricEmpty
    }
    // UI-SPEC §5a: compact amount without "Rp" prefix; "—" when no cost (same condition as costText).
    val costCompactText = if (costText != MetricEmpty && item.obdFuelConsumedL > 0.0 && pricePerLiter > 0.0) {
        formatCompactAmount(Math.round(item.obdFuelConsumedL * pricePerLiter))
    } else {
        MetricEmpty
    }
    // ADR-024: covered distance / fuel when available; legacy distance / fuel for pre-v13 trips.
    val efficiencyText = if (item.obdFuelConsumedL > 0.0 && item.obdCoveredDistanceKm > 0.0) {
        String.format(Locale.ENGLISH, "%.1f", item.obdCoveredDistanceKm / item.obdFuelConsumedL)
    } else if (item.obdFuelConsumedL > 0.0) {
        String.format(Locale.ENGLISH, "%.1f", (item.distance / 1000f) / item.obdFuelConsumedL)
    } else {
        MetricEmpty
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

    // ADR-023: order rows with snapshotted names get a 2-3 line header (ACCEPTED/PICKUP/DROP) and
    // grow to fit; v11 rows without a match keep the one-line label and the fixed height.
    val showOrderHeader = orderLabel != null && (item.pickupName != null || item.dropName != null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showOrderHeader) Modifier.heightIn(min = 132.dp) else Modifier.height(132.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(TripSurface)
            .border(1.dp, TripBorder, RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(modifier = if (showOrderHeader) Modifier.padding(bottom = 12.dp) else Modifier.weight(1f)) {
            RouteIndicator()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                if (showOrderHeader) {
                    item.acceptedName?.let { OrderHeaderLine("ACCEPTED", it) }
                    OrderHeaderLine("PICKUP", item.pickupName ?: "—")
                    OrderHeaderLine("DROP", item.dropName ?: "—")
                } else {
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
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = SimpleDateFormat("EEE, HH:mm", Locale.ENGLISH).format(item.timestamp),
                    color = TripMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                // ADR-025: persisted order outcome chip (order-linked trips only).
                OrderStatusChip(
                    status = item.orderStatus,
                    offline = item.orderOffline,
                    modifier = Modifier.padding(top = 6.dp)
                )
                // ADR-022 / UI-SPEC §3b-order: order price / net now lives in the header area.
                if (orderPriceNetText != null) {
                    Text(
                        text = orderPriceNetText,
                        color = TripInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MonospaceFontFamily,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .semantics { contentDescription = "Order price and net profit, $orderPriceNetText" }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TripBorder)
        )
        // UI-SPEC §3b #7 / §5a: ONE strip, canonical order, widths 48 / 44 / 82 / 52 / 76.
        MetricStrip(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            theme = MetricTheme.Light,
            showSeparator = false,
            spacing = 2.dp
        ) {
            MetricCell(
                label = "TIME",
                value = timeText,
                modifier = Modifier.weight(48f),
                theme = MetricTheme.Light,
                size = MetricSize.Compact
            )
            MetricCell(
                label = "DIST (km)",
                value = distanceText,
                modifier = Modifier.weight(44f),
                theme = MetricTheme.Light,
                size = MetricSize.Compact
            )
            MetricCell(
                label = "AVG SPEED (km/h)",
                value = averageSpeedText,
                modifier = Modifier.weight(82f),
                theme = MetricTheme.Light,
                size = MetricSize.Compact
            )
            MetricCell(
                label = "COST (Rp.)",
                value = costCompactText,
                modifier = Modifier.weight(52f),
                theme = MetricTheme.Light,
                size = MetricSize.Compact,
                valueColor = costColor
            )
            MetricCell(
                label = "AVG FUEL (km/L)",
                value = efficiencyText,
                modifier = Modifier.weight(76f),
                theme = MetricTheme.Light,
                size = MetricSize.Compact
            )
        }
    }
}

/** ADR-023: one header line — small muted tag + single-line ellipsized name. */
@Composable
private fun OrderHeaderLine(tag: String, name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text = tag,
            color = TripMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = name,
            color = TripInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
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

// ADR-025 / UI-SPEC §3b-order: Completed / Completed · Offline (green) / Cancelled (neutral); null = none.
@Composable
private fun OrderStatusChip(status: String?, offline: Boolean, modifier: Modifier = Modifier) {
    val label = when (status) {
        "COMPLETED" -> if (offline) "Completed · Offline" else "Completed"
        "CANCELLED" -> "Cancelled"
        else -> return
    }
    val tint = if (status == "COMPLETED") TripGreen else TripMuted
    Text(
        text = label,
        color = tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .border(1.dp, tint, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
