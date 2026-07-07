package com.kolee.tracklocation.feature.obd.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kolee.tracklocation.ui.theme.TripBorder
import com.kolee.tracklocation.ui.theme.TripGreen
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import com.kolee.tracklocation.ui.theme.TripSurface
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Format an IDR amount as "Rp " + grouped integer with '.' thousands separators, no decimals.
 * e.g. 12500.0 -> "Rp 12.500". Fuel Cost (FR-12).
 */
fun formatIdr(amount: Double): String {
    val symbols = DecimalFormatSymbols().apply { groupingSeparator = '.' }
    val df = DecimalFormat("#,##0", symbols)
    return "Rp " + df.format(amount)
}

/**
 * Fuel Cost (FR-12) price editor. Stateless except for the in-progress text field; the two
 * screens own the "show" boolean and observe [com.kolee.tracklocation.feature.obd.FuelPriceController].
 */
@Composable
fun FuelCostEditorDialog(
    current: Double,
    canUndo: Boolean,
    canRedo: Boolean,
    onSave: (Double) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Prefill with the current price as an integer (no decimals in the IDR domain).
    var text by remember { mutableStateOf(if (current > 0.0) current.toLong().toString() else "") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Fuel price (Rp / litre)",
                color = TripInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TripSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, TripBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter { it.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = TripInk, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box {
                            if (text.isBlank()) {
                                Text(text = "0", color = TripMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            innerTextField()
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Undo / redo row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UndoRedoButton(glyph = "↶", label = "Undo", enabled = canUndo, onClick = onUndo)
                UndoRedoButton(glyph = "↷", label = "Redo", enabled = canRedo, onClick = onRedo)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save / Cancel row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cancel",
                    color = TripMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save",
                    color = TripGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            val parsed = text.toDoubleOrNull() ?: 0.0
                            onSave(parsed)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun UndoRedoButton(
    glyph: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) TripInk else TripMuted.copy(alpha = 0.4f)
    Row(
        modifier = Modifier
            .height(36.dp)
            .background(TripSurface, RoundedCornerShape(8.dp))
            .border(1.dp, TripBorder, RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = glyph, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
