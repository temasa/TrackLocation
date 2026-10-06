package com.kolee.tracklocation.screens.track.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.kolee.tracklocation.feature.observer.trip.OrderCard
import com.kolee.tracklocation.feature.observer.trip.OrderPhase
import com.kolee.tracklocation.screens.track.TrackPanelState
import com.kolee.tracklocation.screens.track.TripState
import com.kolee.tracklocation.ui.theme.BrandGreen
import com.kolee.tracklocation.ui.theme.MonospaceFontFamily
import com.kolee.tracklocation.ui.theme.PanelBg
import com.kolee.tracklocation.ui.theme.PanelBgFallback
import com.kolee.tracklocation.ui.theme.PanelBorder
import com.kolee.tracklocation.ui.theme.PanelTextPrimary
import com.kolee.tracklocation.ui.theme.PanelTextSecondary
import com.kolee.tracklocation.ui.theme.StatusPaused

// PROVISIONAL UI (ADR-014): layout is a stand-in until the Claude Design / Stitch handoff for the
// order card (UI-SPEC §3e) lands. Reuses TripPanel's glass look; replace once the design arrives.

private val CardLabelStyle = TextStyle(
    fontFamily = MonospaceFontFamily,
    fontSize = 10.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.2.sp,
    color = PanelTextSecondary
)

private val PlaceNameStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.SemiBold,
    color = PanelTextPrimary
)

private val PlaceAddressStyle = TextStyle(
    fontSize = 12.sp,
    color = PanelTextSecondary
)

private val EarningsStyle = TextStyle(
    fontFamily = MonospaceFontFamily,
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold,
    color = PanelTextPrimary
)

// PROVISIONAL trip strip (UI-SPEC §3e): private copies of TripPanel's unit-label / metric styles.
private val StripLabelStyle = TextStyle(
    fontSize = 10.sp,
    letterSpacing = 0.4.sp,
    color = PanelTextSecondary
)

private val StripValueStyle = TextStyle(
    fontFamily = MonospaceFontFamily,
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.3).sp,
    color = PanelTextPrimary,
    fontFeatureSettings = "tnum"
)

@Composable
fun GojekOrderCard(
    order: OrderCard,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    // PROVISIONAL: compact trip strip, shown only while a trip is LIVE/PAUSED (null/READY hides it).
    tripStripState: TrackPanelState? = null
) {
    val panelBg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PanelBg else PanelBgFallback
    val phaseLabel = when (order.phase) {
        OrderPhase.PICKUP -> "Pickup"
        OrderPhase.DROP -> "Drop"
        OrderPhase.FINISHED -> "Done"
    }
    val phaseColor = when (order.phase) {
        OrderPhase.PICKUP -> StatusPaused
        OrderPhase.DROP -> BrandGreen
        OrderPhase.FINISHED -> PanelTextSecondary
    }
    val earningsText = order.earningsRp?.let { formatRupiah(it) }

    // UI-SPEC §3e: expand/collapse toggles address visibility; strip-only shows only TripStrip.
    var isExpanded by remember { mutableStateOf(true) }
    var isStripOnly by remember { mutableStateOf(false) }

    val showStrip = tripStripState != null && tripStripState.tripState != TripState.READY

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
        if (isStripOnly && showStrip) {
            // Strip-only mode: tap to exit back to collapsed card.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = { isStripOnly = false; isExpanded = false }
                    )
                    .semantics { contentDescription = "Tap to expand order card" }
            ) {
                TripStrip(tripStripState!!, order.earningsRp)
            }
        } else {
            Column {
                // Phase chip + chevron + optional dismiss.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(color = phaseColor, shape = RoundedCornerShape(50))
                    )
                    Text(text = phaseLabel.uppercase(), style = CardLabelStyle)
                    // Chevron toggle (expand/collapse address visibility).
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable(
                                    role = Role.Button,
                                    onClick = { isExpanded = !isExpanded }
                                )
                                .semantics {
                                    contentDescription = if (isExpanded) "Collapse order details" else "Expand order details"
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isExpanded) "▲" else "▼",
                                style = CardLabelStyle
                            )
                        }
                        if (onDismiss != null) {
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clickable(role = Role.Button, onClick = onDismiss)
                                    .semantics { contentDescription = "Dismiss order card" }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Dismiss", style = CardLabelStyle)
                            }
                        }
                    }
                }

                if (order.phase != OrderPhase.DROP && order.pickupAddress != null) {
                    Spacer(Modifier.height(8.dp))
                    PlaceBlock(
                        label = "PICKUP",
                        name = order.pickupName,
                        address = order.pickupAddress,
                        showAddress = isExpanded,
                        onTap = if (showStrip) ({ isStripOnly = true }) else null
                    )
                }
                if (order.dropAddress != null) {
                    Spacer(Modifier.height(8.dp))
                    PlaceBlock(
                        label = "DROP",
                        name = order.dropName,
                        address = order.dropAddress,
                        showAddress = isExpanded,
                        onTap = if (showStrip) ({ isStripOnly = true }) else null
                    )
                }

                if (order.payment != null || earningsText != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "PAYMENT", style = CardLabelStyle)
                            Text(text = order.payment ?: "—", style = PlaceNameStyle)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "EARNINGS", style = CardLabelStyle)
                            Text(
                                text = earningsText ?: "—",
                                style = EarningsStyle,
                                color = BrandGreen
                            )
                        }
                    }
                }

                if (showStrip) {
                    Spacer(Modifier.height(10.dp))
                    TripStrip(tripStripState!!, order.earningsRp)
                }
            }
        }

        // Invisible live-region announcing phase changes (mirrors TripPanel).
        Text(
            text = "Order phase: $phaseLabel",
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

// PROVISIONAL: TIME / DIST / COST-NET / AVG-INST km/L, no extra interactivity.
@Composable
private fun TripStrip(state: TrackPanelState, earningsRp: Long?) {
    // COST / NET (FR-16): cost = trip litres x pump price; NET only when both cost and earnings exist.
    val tripFuelL = state.tripFuelL
    val costRp: Long? =
        if (state.obdConnected && tripFuelL != null && tripFuelL > 0.0 && state.fuelPricePerL > 0.0) {
            Math.round(tripFuelL * state.fuelPricePerL)
        } else null
    val netRp: Long? = if (costRp != null && earningsRp != null) earningsRp - costRp else null
    val costText = costRp?.let { formatCompactAmount(it) } ?: "—"
    val netText = netRp?.let { formatCompactAmount(it) } ?: "—"
    val costNetDesc = when {
        costRp == null -> "Fuel cost unavailable"
        netRp == null -> "Fuel cost ${formatRupiah(costRp)} rupiah, net profit unavailable"
        netRp < 0 -> "Fuel cost ${formatRupiah(costRp)} rupiah, net loss ${formatRupiah(-netRp)} rupiah"
        else -> "Fuel cost ${formatRupiah(costRp)} rupiah, net profit ${formatRupiah(netRp)} rupiah"
    }

    val avgText = if (state.obdConnected) state.tripAvgKmL?.let { "%.1f".format(it) } ?: "—" else "—"
    val instText = if (state.obdConnected) state.instantKmL?.let { "%.1f".format(it) } ?: "—" else "—"
    val elapsedDesc = formatSpokenElapsed(state.elapsedMs)
    val distText = "%.1f".format(state.distanceKm)
    val fuelDesc = if (avgText == "—" && instText == "—") {
        "Fuel economy unavailable"
    } else {
        "Average fuel economy ${if (avgText == "—") "unavailable" else avgText}, " +
            "instant ${if (instText == "—") "unavailable" else instText} kilometers per litre"
    }

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
        Column(
            modifier = Modifier
                .weight(0.7f)
                .semantics { contentDescription = "Elapsed $elapsedDesc" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "TIME", style = StripLabelStyle, maxLines = 1)
            Text(text = formatCompactElapsed(state.elapsedMs), style = StripValueStyle, maxLines = 1)
        }
        Column(
            modifier = Modifier
                .weight(0.7f)
                .semantics { contentDescription = "Distance $distText kilometers" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "DIST (km)", style = StripLabelStyle, maxLines = 1)
            Text(text = distText, style = StripValueStyle, maxLines = 1)
        }
        Column(
            modifier = Modifier
                .weight(1.4f)
                .semantics { contentDescription = costNetDesc },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "COST / NET (Rp.)", style = StripLabelStyle, maxLines = 1)
            Text(text = "$costText/$netText", style = StripValueStyle, maxLines = 1)
        }
        Column(
            modifier = Modifier
                .weight(1.4f)
                .semantics { contentDescription = fuelDesc },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "AVG / INST (km/L)", style = StripLabelStyle, maxLines = 1)
            Text(text = "$avgText/$instText", style = StripValueStyle, maxLines = 1)
        }
    }
}

// 850 -> "Rp850", 8_400 -> "Rp8.4k", 1_200_000 -> "Rp1.2jt"; negatives get a U+2212 prefix.
// Unit is chosen after rounding so 999_960 reads "Rp1.0jt", never "Rp1000.0k".
private fun formatCompactRupiah(value: Long): String {
    val abs = Math.abs(value)
    val body = when {
        abs < 1_000L -> "Rp$abs"
        else -> {
            val k = String.format(Locale.US, "%.1f", abs / 1000.0)
            if (abs < 1_000_000L && k != "1000.0") {
                "Rp${k}k"
            } else {
                "Rp${String.format(Locale.US, "%.1f", abs / 1_000_000.0)}jt"
            }
        }
    }
    return if (value < 0) "\u2212$body" else body
}

// No-prefix variant for TripStrip where the label already shows "(Rp.)".
// 850 -> "850", 8_400 -> "8.4k", 1_200_000 -> "1.2jt"; negatives get a U+2212 prefix.
private fun formatCompactAmount(value: Long): String {
    val abs = Math.abs(value)
    val body = when {
        abs < 1_000L -> "$abs"
        else -> {
            val k = String.format(Locale.US, "%.1f", abs / 1000.0)
            if (abs < 1_000_000L && k != "1000.0") "${k}k"
            else String.format(Locale.US, "%.1f", abs / 1_000_000.0) + "jt"
        }
    }
    return if (value < 0) "\u2212$body" else body
}

// 8_020_000 -> "2h13m", 822_000 -> "13m42s", 42_000 -> "42s".
private fun formatCompactElapsed(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h >= 1 -> "%dh%02dm".format(h, m)
        m >= 1 -> "%dm%02ds".format(m, s)
        else -> "${s}s"
    }
}

// Spoken form for TalkBack, e.g. "2 hours 13 minutes".
private fun formatSpokenElapsed(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h >= 1 -> "$h hours $m minutes"
        m >= 1 -> "$m minutes $s seconds"
        else -> "$s seconds"
    }
}

@Composable
private fun PlaceBlock(
    label: String,
    name: String?,
    address: String,
    showAddress: Boolean = true,
    onTap: (() -> Unit)? = null
) {
    val tapModifier = if (onTap != null) {
        Modifier
            .clickable(role = Role.Button, onClick = onTap)
            .semantics {
                contentDescription = if (showAddress) {
                    "$label, ${name?.let { "$it, " } ?: ""}$address. Tap to collapse"
                } else {
                    "$label, ${name ?: ""}. Tap to collapse"
                }
            }
    } else {
        Modifier.semantics {
            contentDescription = "$label, ${name?.let { "$it, " } ?: ""}$address"
        }
    }
    Column(modifier = Modifier.fillMaxWidth().then(tapModifier)) {
        Text(text = label, style = CardLabelStyle)
        if (name != null) {
            Text(text = name, style = PlaceNameStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showAddress) {
            Text(text = address, style = PlaceAddressStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// 36400 -> "Rp36.400" ('.' thousands separator, matching the source app).
private fun formatRupiah(value: Long): String {
    val digits = value.toString()
    val grouped = StringBuilder()
    digits.forEachIndexed { i, c ->
        if (i > 0 && (digits.length - i) % 3 == 0) grouped.append('.')
        grouped.append(c)
    }
    return "Rp$grouped"
}
