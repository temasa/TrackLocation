package com.kolee.tracklocation.screens.track.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ADR-026 / UI-SPEC §3j (PROVISIONAL): compact dismissible legend telling the route lines apart by
 * dash/weight as well as hue. [showOptimized] is false on the trip details map (optimized is never saved).
 * [onDismiss] null hides the close control (no dismiss on details).
 */
@Composable
fun RouteLegend(
    modifier: Modifier = Modifier,
    showOptimized: Boolean = true,
    onDismiss: (() -> Unit)? = null
) {
    val description = if (showOptimized) {
        "Route legend. Planned route is a thin dashed line. Optimized route is a bold orange line. Actual route is a blue line."
    } else {
        "Route legend. Planned route is a thin dashed line. Actual route is a blue line."
    }
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(start = 8.dp, end = if (onDismiss == null) 8.dp else 0.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LegendEntry("Planned", PLANNED_LINE_COLOR, strokeDp = 2.5f, dashed = true)
        if (showOptimized) LegendEntry("Optimized", OPTIMIZED_LINE_COLOR, strokeDp = 5.5f, dashed = false)
        LegendEntry("Actual", Color.Blue, strokeDp = 4f, dashed = false)
        if (onDismiss != null) {
            // >= 48dp touch target (UI-SPEC §3j).
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss route legend",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendEntry(label: String, color: Color, strokeDp: Float, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(modifier = Modifier.size(width = 22.dp, height = 8.dp)) {
            val y = size.height / 2f
            val dash = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeDp.dp.toPx(),
                cap = StrokeCap.Butt,
                pathEffect = dash
            )
        }
        Text(text = label, color = Color.White, fontSize = 11.sp)
    }
}
