package com.kolee.tracklocation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.Arrangement
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

/** Shared card shell for the fuel dialogs (same look as the Fuel price dialog). */
@Composable
internal fun FuelDialogCard(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(text = title, color = TripInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
internal fun FuelFieldLabel(text: String) {
    Text(
        text = text,
        color = TripMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
internal fun FuelHelperText(text: String) {
    Text(
        text = text,
        color = TripMuted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
internal fun FuelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TripSurface, RoundedCornerShape(10.dp))
            .border(1.dp, TripBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(color = TripInk, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isBlank()) {
                        Text(text = placeholder, color = TripMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
internal fun FuelDialogActions(
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
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
            color = if (saveEnabled) TripGreen else TripMuted.copy(alpha = 0.4f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .then(if (saveEnabled) Modifier.clickable { onSave() } else Modifier)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** Keeps digits and a single decimal separator; a comma is treated as a dot. */
private fun sanitizeDecimal(raw: String): String {
    var seenDot = false
    val sb = StringBuilder()
    for (c in raw) {
        when {
            c.isDigit() -> sb.append(c)
            (c == '.' || c == ',') && !seenDot -> {
                seenDot = true
                sb.append('.')
            }
        }
    }
    return sb.toString()
}

/** ADR-027 Fill up: litres added (required, > 0) and an optional price (defaults to the latest). */
@Composable
fun FillUpDialog(
    defaultPrice: Double,
    onSave: (litres: Double, price: Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var litresText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf(if (defaultPrice > 0.0) defaultPrice.toLong().toString() else "") }
    val litres = litresText.toDoubleOrNull()
    val valid = litres != null && litres > 0.0

    FuelDialogCard(title = "Fill up", onDismiss = onDismiss) {
        FuelFieldLabel("Litres added (L)")
        FuelTextField(
            value = litresText,
            onValueChange = { litresText = sanitizeDecimal(it) },
            placeholder = "0",
            keyboardType = KeyboardType.Decimal,
        )
        Spacer(modifier = Modifier.height(14.dp))
        FuelFieldLabel("Price (Rp/L) - optional")
        FuelTextField(
            value = priceText,
            onValueChange = { new -> priceText = new.filter { it.isDigit() } },
            placeholder = "0",
            keyboardType = KeyboardType.Number,
        )
        FuelHelperText("Price defaults to the latest. Changing it saves a new price from now.")
        Spacer(modifier = Modifier.height(16.dp))
        FuelDialogActions(
            saveEnabled = valid,
            onSave = {
                val entered = priceText.toDoubleOrNull()
                val price = entered?.takeIf { it > 0.0 && it != defaultPrice }
                onSave(litres ?: 0.0, price)
            },
            onDismiss = onDismiss,
        )
    }
}

/** ADR-027 Reset: the fuel in the tank right now (required, >= 0). */
@Composable
fun ResetFuelDialog(
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val litres = text.toDoubleOrNull()
    val valid = litres != null && litres >= 0.0

    FuelDialogCard(title = "Reset fuel", onDismiss = onDismiss) {
        FuelFieldLabel("Fuel in tank now (L)")
        FuelTextField(
            value = text,
            onValueChange = { text = sanitizeDecimal(it) },
            placeholder = "0",
            keyboardType = KeyboardType.Decimal,
        )
        FuelHelperText("Sets the starting amount. Fuel left counts down from here.")
        Spacer(modifier = Modifier.height(16.dp))
        FuelDialogActions(
            saveEnabled = valid,
            onSave = { onSave(litres ?: 0.0) },
            onDismiss = onDismiss,
        )
    }
}
