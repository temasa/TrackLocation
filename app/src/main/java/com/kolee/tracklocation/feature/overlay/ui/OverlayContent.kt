package com.kolee.tracklocation.feature.overlay.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.R
import com.kolee.tracklocation.feature.overlay.OverlayUiState
import com.kolee.tracklocation.feature.overlay.SessionTotals
import com.kolee.tracklocation.feature.overlay.formatOverlayCost
import com.kolee.tracklocation.feature.overlay.formatOverlayFuelCons
import com.kolee.tracklocation.feature.overlay.formatOverlayFuelLeft
import com.kolee.tracklocation.feature.overlay.formatOverlayIncome
import com.kolee.tracklocation.feature.overlay.formatOverlayRange
import com.kolee.tracklocation.feature.overlay.overlayFuelLevel
import com.kolee.tracklocation.ui.components.MetricEmpty
import com.kolee.tracklocation.ui.theme.BrandGreen
import com.kolee.tracklocation.ui.theme.BrandGreenDark
import com.kolee.tracklocation.ui.theme.FuelCriticalRed
import com.kolee.tracklocation.ui.theme.FuelLowAmber
import com.kolee.tracklocation.ui.theme.MonospaceFontFamily
import com.kolee.tracklocation.ui.theme.OverlayHaloDark
import com.kolee.tracklocation.ui.theme.OverlayHaloLight
import com.kolee.tracklocation.ui.theme.OverlayLabelDark
import com.kolee.tracklocation.ui.theme.OverlayLabelLight
import com.kolee.tracklocation.ui.theme.OverlayLoss
import com.kolee.tracklocation.ui.theme.OverlaySurfaceGlassDark
import com.kolee.tracklocation.ui.theme.OverlaySurfaceGlassLight
import com.kolee.tracklocation.ui.theme.OverlayValueDark
import com.kolee.tracklocation.ui.theme.OverlayValueLight
import com.kolee.tracklocation.utils.FuelBalance
import java.util.Locale

// ADR-029 / UI-SPEC 5d. Design frame px are dp here. No business logic: only formatting calls from OverlayFormat.kt.

private class OverlayColors(
    val surface: Color,
    val halo: Color,
    val label: Color,
    val value: Color,
    val border: Color,
    val solidSurface: Color,
    val circleFill: Color
)

private val DarkColors = OverlayColors(
    surface = OverlaySurfaceGlassDark,
    halo = OverlayHaloDark,
    label = OverlayLabelDark,
    value = OverlayValueDark,
    border = Color(0x2EFFFFFF),
    solidSurface = Color(0xFF1B3A2D),
    circleFill = Color(0xFF0E2A20)
)

private val LightColors = OverlayColors(
    surface = OverlaySurfaceGlassLight,
    halo = OverlayHaloLight,
    label = OverlayLabelLight,
    value = OverlayValueLight,
    border = Color(0x24000000),
    solidSurface = Color(0xFFFFFFFF),
    circleFill = Color(0xFFFFFFFF)
)

@Composable
fun OverlayContent(
    state: OverlayUiState,
    onShrink: () -> Unit,
    onExpand: () -> Unit,
    onFillUp: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onDragEnd: () -> Unit,
    fillUpOpen: Boolean = false,
    onFillUpCancel: () -> Unit = {},
    onFillUpSave: (litres: Double, price: Double?) -> Unit = { _, _ -> },
    windowHasFocus: Boolean = true
) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    if (state.collapsed) {
        // The circle never shows the form (the service also closes it on shrink).
        OverlayCircle(state, colors, onExpand, onDrag, onDragEnd)
    } else if (fillUpOpen) {
        OverlayFillUpForm(onCancel = onFillUpCancel, onSave = onFillUpSave, windowHasFocus = windowHasFocus)
    } else {
        OverlayStrip(state, colors, onShrink, onFillUp)
    }
}

// ---------- Strip ----------

@Composable
private fun OverlayStrip(
    state: OverlayUiState,
    colors: OverlayColors,
    onShrink: () -> Unit,
    onFillUp: () -> Unit
) {
    val totals = state.totals
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 12.dp, top = 4.dp, end = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StripCell(
                label = "FUEL LEFT",
                value = formatOverlayFuelLeft(state.fuelLeftL),
                colors = colors,
                modifier = Modifier.width(48.dp),
                description = state.fuelLeftL?.let { "Fuel left ${formatOverlayFuelLeft(it)} litres" }
                    ?: "Fuel left unavailable"
            )
            StripCell(
                label = "RANGE",
                value = formatOverlayRange(state.rangeKm),
                colors = colors,
                modifier = Modifier.width(32.dp),
                description = state.rangeKm?.let { "Range ${formatOverlayRange(it)} kilometres" }
                    ?: "Range unavailable"
            )
            StripCell(
                label = "FUEL CONS",
                value = formatOverlayFuelCons(state.avgKmL, state.instantKmL),
                colors = colors,
                modifier = Modifier.width(62.dp),
                description = fuelConsDescription(state.avgKmL, state.instantKmL)
            )
            StripCell(
                label = "COST",
                value = formatOverlayCost(totals),
                colors = colors,
                modifier = Modifier.width(40.dp),
                description = totals.costRp?.let { "Cost ${speakAmount(it)} rupiah" } ?: "Cost unavailable"
            )
            IncomeCell(totals, colors, Modifier.weight(1f))
            FillUpButton(onFillUp)
            ShrinkButton(colors, onShrink)
        }
        if (totals.partial) {
            PartialLine(colors)
        }
    }
}

private fun stripLabelStyle(colors: OverlayColors) = TextStyle(
    color = colors.label,
    fontSize = 9.sp,
    fontWeight = FontWeight.Normal,
    shadow = Shadow(color = colors.halo, offset = Offset.Zero, blurRadius = 3f)
)

private fun stripValueStyle(colors: OverlayColors, color: Color = colors.value) = TextStyle(
    color = color,
    fontFamily = MonospaceFontFamily,
    fontSize = 13.75.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.3).sp,
    shadow = Shadow(color = colors.halo, offset = Offset.Zero, blurRadius = 3f)
)

@Composable
private fun StripCell(
    label: String,
    value: String,
    colors: OverlayColors,
    modifier: Modifier,
    description: String
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, style = stripLabelStyle(colors), maxLines = 1, softWrap = false)
        Text(text = value, style = stripValueStyle(colors), maxLines = 1, softWrap = false)
    }
}

@Composable
private fun IncomeCell(totals: SessionTotals, colors: OverlayColors, modifier: Modifier) {
    val formatted = formatOverlayIncome(totals)
    val loss = (totals.netRp ?: 0L) < 0L
    val description = if (totals.grossRp == null || totals.netRp == null) {
        "Income unavailable"
    } else if (loss) {
        "Income gross ${speakAmount(totals.grossRp)}, net loss ${speakAmount(-totals.netRp)} rupiah"
    } else {
        "Income gross ${speakAmount(totals.grossRp)}, net ${speakAmount(totals.netRp)} rupiah"
    }
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = "INCOME", style = stripLabelStyle(colors), maxLines = 1, softWrap = false)
        if (formatted == MetricEmpty) {
            Text(text = formatted, style = stripValueStyle(colors), maxLines = 1, softWrap = false)
        } else {
            // gross, "/" and net as separate pieces with no gap (pen design), so only the net part can be red.
            Row {
                Text(text = formatted.substringBefore('/'), style = stripValueStyle(colors), maxLines = 1, softWrap = false)
                Text(text = "/", style = stripValueStyle(colors), maxLines = 1, softWrap = false)
                Text(
                    text = formatted.substringAfter('/'),
                    style = stripValueStyle(colors, if (loss) OverlayLoss else colors.value),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun FillUpButton(onFillUp: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(BrandGreen)
            .semantics { contentDescription = "Fill up" }
            .clickable(role = Role.Button, onClick = onFillUp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fuel_dock),
            contentDescription = null,
            tint = BrandGreenDark,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ShrinkButton(colors: OverlayColors, onShrink: () -> Unit) {
    // 44dp hit area, 32dp visual.
    Box(
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = "Shrink to circle" }
            .clickable(role = Role.Button, onClick = onShrink),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.solidSurface)
                .border(BorderStroke(1.dp, colors.border), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.UnfoldLess,
                contentDescription = null,
                tint = colors.value,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PartialLine(colors: OverlayColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .border(BorderStroke(1.dp, FuelLowAmber), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = FuelLowAmber,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "PARTIAL",
                style = stripLabelStyle(colors).copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false
            )
        }
        Text(
            text = "Orders without OBD or earnings excluded",
            style = stripLabelStyle(colors),
            maxLines = 1
        )
    }
}

// ---------- Circle ----------

@Composable
private fun OverlayCircle(
    state: OverlayUiState,
    colors: OverlayColors,
    onExpand: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val ring = when (overlayFuelLevel(state.fuelLeftL)) {
        FuelBalance.FuelLevel.Normal -> BrandGreen
        FuelBalance.FuelLevel.Low -> FuelLowAmber
        FuelBalance.FuelLevel.Critical -> FuelCriticalRed
    }
    val fuel = formatOverlayFuelLeft(state.fuelLeftL)
    val description = (state.fuelLeftL?.let { "Fuel left $fuel litres" } ?: "Fuel left unavailable") +
        ", drag to move, double tap to expand"
    Box(
        modifier = Modifier
            .size(OVERLAY_CIRCLE_SIZE_DP.dp)
            .clip(CircleShape)
            .background(colors.circleFill)
            .border(BorderStroke(3.dp, ring), CircleShape)
            // Drag consumes events after the touch slop, which cancels the tap detector below: a drag never expands.
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { onExpand() }) }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                onClick(label = "Expand") { onExpand(); true }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_fuel_dock),
                contentDescription = null,
                tint = colors.value,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = fuel,
                style = TextStyle(
                    color = colors.value,
                    fontFamily = MonospaceFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                softWrap = false
            )
        }
        // Grip bar: the drag affordance.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .width(14.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(colors.label)
        )
    }
}

// ---------- TalkBack wording (UI-SPEC 5d) ----------

private fun fuelConsDescription(avg: Double?, instant: Double?): String {
    if (avg == null && instant == null) return "Fuel consumption unavailable"
    val parts = StringBuilder("Fuel consumption")
    if (avg != null) parts.append(" average ").append(String.format(Locale.US, "%.1f", avg))
    if (instant != null) parts.append(" instant ").append(String.format(Locale.US, "%.1f", instant))
    return parts.append(" kilometres per litre").toString()
}

/** Spoken compact rupiah: 850, "8.4 thousand", "1.2 million" (mirrors formatCompactAmount's thresholds). */
private fun speakAmount(value: Long): String {
    val abs = Math.abs(value)
    val body = when {
        abs < 1_000L -> "$abs"
        abs < 1_000_000L -> String.format(Locale.US, "%.1f", abs / 1000.0) + " thousand"
        else -> String.format(Locale.US, "%.1f", abs / 1_000_000.0) + " million"
    }
    return if (value < 0) "minus $body" else body
}
