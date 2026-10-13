package com.kolee.tracklocation.feature.overlay.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.R
import com.kolee.tracklocation.feature.obd.FillUpRecorder
import com.kolee.tracklocation.feature.obd.FuelPriceController
import com.kolee.tracklocation.ui.theme.BrandGreen
import com.kolee.tracklocation.ui.theme.BrandGreenDark
import com.kolee.tracklocation.ui.theme.OverlayDisabled
import com.kolee.tracklocation.ui.theme.OverlayFormSurfaceDark
import com.kolee.tracklocation.ui.theme.OverlayFormSurfaceLight
import com.kolee.tracklocation.ui.theme.OverlayInputDark
import com.kolee.tracklocation.ui.theme.OverlayInputLight
import com.kolee.tracklocation.ui.theme.OverlayLabelDark
import com.kolee.tracklocation.ui.theme.OverlayLabelLight
import com.kolee.tracklocation.ui.theme.OverlayLoss
import com.kolee.tracklocation.ui.theme.OverlayValueDark
import com.kolee.tracklocation.ui.theme.OverlayValueLight

// ADR-029 Decision 9 / UI-SPEC 5d (states 5/6): inline Fill-up form. Validation mirrors FillUpDialog via
// FillUpRecorder.parseLitres/parsePrice; the write itself happens in the service (FillUpRecorder.record).

private class FormColors(
    val surface: Color,
    val input: Color,
    val text: Color,
    val label: Color,
    val border: Color
)

private val FormDark = FormColors(
    surface = OverlayFormSurfaceDark,
    input = OverlayInputDark,
    text = OverlayValueDark,
    label = OverlayLabelDark,
    border = Color(0x2EFFFFFF)
)

private val FormLight = FormColors(
    surface = OverlayFormSurfaceLight,
    input = OverlayInputLight,
    text = OverlayValueLight,
    label = OverlayLabelLight,
    border = Color(0x24000000)
)

/** Keeps digits and a single decimal separator; a comma is treated as a dot (same as FillUpDialog). */
private fun sanitizeLitres(raw: String): String {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OverlayFillUpForm(
    onCancel: () -> Unit,
    onSave: (litres: Double, price: Double?) -> Unit,
    windowHasFocus: Boolean = true
) {
    val colors = if (isSystemInDarkTheme()) FormDark else FormLight
    // Field state lives only while the form is composed, so closing the form resets it.
    var litresText by remember { mutableStateOf("") }
    val defaultPrice = remember { FuelPriceController.state.value.currentPrice }
    var priceText by remember {
        mutableStateOf(if (defaultPrice > 0.0) defaultPrice.toLong().toString() else "")
    }
    val litres = FillUpRecorder.parseLitres(litresText)
    val valid = litres != null
    val showError = litresText.isNotEmpty() && !valid

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // ERR-013: the window turns focusable asynchronously after this composes; showSoftInput is ignored until
    // the window has input focus, so request focus + keyboard only once it reports window focus.
    LaunchedEffect(windowHasFocus) {
        if (windowHasFocus) {
            try {
                focusRequester.requestFocus()
            } catch (e: IllegalStateException) {
                // Not attached yet: the IME is still requested through softInputMode.
            }
            keyboard?.show()
        }
    }

    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_fuel_dock),
                contentDescription = null,
                tint = BrandGreen,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Fill up",
                style = TextStyle(color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(
                label = "Litres added (L)",
                value = litresText,
                onValueChange = { litresText = sanitizeLitres(it) },
                keyboardType = KeyboardType.Decimal,
                colors = colors,
                isError = showError,
                modifier = Modifier.weight(1f),
                focusRequester = focusRequester
            )
            FormField(
                label = "Price (Rp/L) - optional",
                value = priceText,
                onValueChange = { new -> priceText = new.filter { it.isDigit() } },
                keyboardType = KeyboardType.Number,
                colors = colors,
                isError = false,
                modifier = Modifier.weight(1f),
                focusRequester = null
            )
        }
        if (showError) {
            Text(
                text = "Enter litres greater than 0",
                style = TextStyle(color = OverlayLoss, fontSize = 11.sp)
            )
        }
        Text(
            text = "Price defaults to the latest. Changing it saves a new price from now.",
            style = TextStyle(color = colors.label, fontSize = 11.sp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FormButton(
                text = "Cancel",
                background = colors.input,
                textColor = colors.text,
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onCancel
            )
            FormButton(
                text = "Save",
                background = if (valid) BrandGreen else OverlayDisabled,
                textColor = if (valid) BrandGreenDark else colors.text.copy(alpha = 0.7f),
                enabled = valid,
                modifier = Modifier.weight(1f),
                onClick = { litres?.let { onSave(it, FillUpRecorder.parsePrice(priceText)) } }
            )
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    colors: FormColors,
    isError: Boolean,
    modifier: Modifier,
    focusRequester: FocusRequester?
) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = TextStyle(color = colors.label, fontSize = 11.sp), maxLines = 1)
        val fieldModifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(colors.input)
            .border(BorderStroke(1.dp, if (isError) OverlayLoss else colors.border), shape)
            .semantics { contentDescription = label }
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.text, fontSize = 16.sp),
            cursorBrush = SolidColor(BrandGreen),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = fieldModifier,
            decorationBox = { inner ->
                Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(text = "0", style = TextStyle(color = colors.label.copy(alpha = 0.5f), fontSize = 16.sp))
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun FormButton(
    text: String,
    background: Color,
    textColor: Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .semantics { contentDescription = text }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
}
