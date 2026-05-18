package com.kolee.tracklocation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Monospace family used for trip timer and metric values.
// Uses the system monospace font (Roboto Mono on Android).
// To use JetBrains Mono instead: add the ui-text-google-fonts dependency and configure
// a GoogleFont.Provider pointing to "JetBrains Mono", then replace this value.
val MonospaceFontFamily: FontFamily = FontFamily.Monospace

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
