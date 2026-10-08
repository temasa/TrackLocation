package com.kolee.tracklocation.utils

import java.util.Locale

/**
 * Compact Rupiah (FR-12/FR-16): 850 -> "Rp850", 8_400 -> "Rp8.4k", 1_200_000 -> "Rp1.2jt";
 * negatives get a U+2212 prefix. Unit is chosen after rounding so 999_960 reads "Rp1.0jt".
 */
fun formatCompactRupiah(value: Long): String {
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
