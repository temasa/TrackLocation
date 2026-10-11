package com.kolee.tracklocation.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kolee.tracklocation.utils.RouteComparison
import kotlin.math.abs
import kotlin.math.roundToInt

/** ADR-026 / UI-SPEC §3j (PROVISIONAL): planned-vs-actual numbers; [comparison] null shows `—`. */
@Composable
fun RouteComparisonCard(
    comparison: RouteComparison?,
    modifier: Modifier = Modifier
) {
    val plannedText = comparison?.let { "%.1f km".format(it.plannedKm) } ?: DASH
    val actualText = comparison?.let { "%.1f km".format(it.actualKm) } ?: DASH
    val extraText = comparison?.let { signedKm(it.extraKm) } ?: DASH
    val deviationText = comparison?.let { "${it.maxDeviationM.roundToInt()} m" } ?: DASH

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Stat(
                    "Planned", plannedText,
                    comparison?.let { "Planned route ${"%.1f".format(it.plannedKm)} kilometres" } ?: "Planned route unavailable"
                )
                Stat(
                    "Actual", actualText,
                    comparison?.let { "Actual route ${"%.1f".format(it.actualKm)} kilometres" } ?: "Actual route unavailable"
                )
                Stat(
                    "Extra", extraText,
                    comparison?.let {
                        val sign = if (it.extraKm < 0) "minus" else "plus"
                        "Extra distance $sign ${"%.1f".format(abs(it.extraKm))} kilometres"
                    } ?: "Extra distance unavailable"
                )
                Stat(
                    "Max deviation", deviationText,
                    comparison?.let { "Maximum deviation from plan ${it.maxDeviationM.roundToInt()} metres" }
                        ?: "Maximum deviation from plan unavailable"
                )
            }
        }
    }
}

private const val DASH = "—"

private fun signedKm(km: Double): String {
    val sign = if (km < 0) "−" else "+" // U+2212 minus per UI-SPEC §3j
    return "$sign${"%.1f".format(abs(km))} km"
}

@Composable
private fun RowScope.Stat(label: String, value: String, spoken: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
