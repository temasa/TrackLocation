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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.kolee.tracklocation.feature.observer.trip.OrderCard
import com.kolee.tracklocation.feature.observer.trip.OrderPhase
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

@Composable
fun GojekOrderCard(
    order: OrderCard,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
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
            // Phase chip: text + dot, so the phase is not conveyed by color alone.
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
                if (onDismiss != null) {
                    // PROVISIONAL (design handoff pending): lets a cancelled order (never "Selesai")
                    // release the card and the trip Start/Stop control.
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
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
                PlaceBlock(label = "PICKUP", name = order.pickupName, address = order.pickupAddress)
            }
            if (order.dropAddress != null) {
                Spacer(Modifier.height(8.dp))
                PlaceBlock(label = "DROP", name = order.dropName, address = order.dropAddress)
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

@Composable
private fun PlaceBlock(label: String, name: String?, address: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$label, ${name?.let { "$it, " } ?: ""}$address"
            }
    ) {
        Text(text = label, style = CardLabelStyle)
        if (name != null) {
            Text(text = name, style = PlaceNameStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text = address, style = PlaceAddressStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
