package com.kolee.tracklocation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.R
import com.kolee.tracklocation.feature.obd.ui.formatIdrPlain
import com.kolee.tracklocation.ui.theme.FuelCriticalRed
import com.kolee.tracklocation.ui.theme.FuelLowAmber
import com.kolee.tracklocation.ui.theme.MonospaceFontFamily
import com.kolee.tracklocation.ui.theme.PanelBorder
import com.kolee.tracklocation.ui.theme.TripBorder
import com.kolee.tracklocation.ui.theme.TripHeroBg
import com.kolee.tracklocation.ui.theme.TripHeroBrandGreen
import com.kolee.tracklocation.ui.theme.TripSurface
import com.kolee.tracklocation.utils.FuelBalance
import com.kolee.tracklocation.viewmodel.FuelDockUiState
import java.util.Locale

/** ADR-027 / UI-SPEC §5b fuel dock. Sits above the bottom navigation. */
@Composable
fun FuelDock(
    theme: MetricTheme,
    state: FuelDockUiState,
    onResetClick: () -> Unit,
    onPriceEditClick: () -> Unit,
    onFillUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (theme == MetricTheme.Dark) TripHeroBg else TripSurface
    val border = if (theme == MetricTheme.Dark) PanelBorder else TripBorder

    val levelColor: Color? = when (state.level) {
        FuelBalance.FuelLevel.Critical -> FuelCriticalRed
        FuelBalance.FuelLevel.Low -> FuelLowAmber
        FuelBalance.FuelLevel.Normal -> null
    }
    val approx = if (state.lowConfidence) "~" else ""
    val fuelText = state.fuelLeftL?.let { approx + String.format(Locale.US, "%.1f", it) } ?: MetricEmpty
    val rangeText = state.rangeKm?.let { approx + Math.round(it).toString() } ?: MetricEmpty
    val priceText = if (state.price > 0.0) formatIdrPlain(state.price) else MetricEmpty

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(bg)
            .drawBehind {
                drawLine(
                    color = border,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockCell(
                label = "FUEL LEFT (L)",
                value = fuelText,
                theme = theme,
                valueColor = if (state.fuelLeftL != null) levelColor else null,
                pencilDescription = "Reset fuel",
                onPencilClick = onResetClick,
                modifier = Modifier.weight(1f),
            )
            DockCell(
                label = "RANGE (km)",
                value = rangeText,
                theme = theme,
                valueColor = if (state.rangeKm != null) levelColor else null,
                modifier = Modifier.weight(1f),
            )
            DockCell(
                label = "PRICE (Rp/L)",
                value = priceText,
                theme = theme,
                pencilDescription = "Edit fuel price",
                onPencilClick = onPriceEditClick,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(TripHeroBrandGreen)
                .semantics { contentDescription = "Fill up" }
                .clickable { onFillUpClick() }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fuel_dock),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Fill up",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DockCell(
    label: String,
    value: String,
    theme: MetricTheme,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    pencilDescription: String? = null,
    onPencilClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.semantics {
            contentDescription = "$label, $value"
        },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = theme.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.4.sp,
            ),
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = TextStyle(
                    color = valueColor ?: theme.value,
                    fontFamily = MonospaceFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
            )
            if (onPencilClick != null) {
                // 28dp tap area centred on the 12dp glyph; the glyph is the only tap target.
                Box(
                    modifier = Modifier
                        .offset(x = (-4).dp) // 28dp box - 12dp glyph = 8dp each side; pull in to a 4dp visual gap
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .semantics { contentDescription = pencilDescription ?: "Edit" }
                        .clickable { onPencilClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_pencil),
                        contentDescription = null,
                        tint = theme.label,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}
