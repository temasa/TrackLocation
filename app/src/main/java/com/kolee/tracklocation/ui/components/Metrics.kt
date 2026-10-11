package com.kolee.tracklocation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.ui.theme.MetricSeparatorLight
import com.kolee.tracklocation.ui.theme.MonospaceFontFamily
import com.kolee.tracklocation.ui.theme.PanelBorder
import com.kolee.tracklocation.ui.theme.PanelTextPrimary
import com.kolee.tracklocation.ui.theme.PanelTextSecondary
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import java.util.Locale

// UI-SPEC §5a Metric Display Standard.

const val MetricEmpty = "—"

enum class MetricTheme(
    val label: Color,
    val value: Color,
    val separator: Color,
    val horizontalAlignment: Alignment.Horizontal,
    val textAlign: TextAlign,
) {
    Dark(PanelTextSecondary, PanelTextPrimary, PanelBorder, Alignment.CenterHorizontally, TextAlign.Center),
    Light(TripMuted, TripInk, MetricSeparatorLight, Alignment.Start, TextAlign.Start),
}

enum class MetricSize(
    val labelSize: Float,
    val labelSpacing: Float,
    val valueSize: Float,
    val valueSpacing: Float,
) {
    Regular(10f, 0.4f, 16f, -0.3f),
    Compact(8f, 0f, 12f, -0.3f),
}

@Composable
fun MetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    theme: MetricTheme = MetricTheme.Dark,
    size: MetricSize = MetricSize.Regular,
    valueColor: Color? = null,
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    Column(
        modifier = semanticsModifier,
        horizontalAlignment = theme.horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = theme.label,
                fontSize = size.labelSize.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = size.labelSpacing.sp,
                textAlign = theme.textAlign,
            ),
            maxLines = 1,
        )
        Text(
            text = value,
            style = TextStyle(
                color = valueColor ?: theme.value,
                fontFamily = MonospaceFontFamily,
                fontSize = size.valueSize.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = size.valueSpacing.sp,
                fontFeatureSettings = "tnum",
                textAlign = theme.textAlign,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun MetricStrip(
    modifier: Modifier = Modifier,
    theme: MetricTheme = MetricTheme.Dark,
    showSeparator: Boolean = true,
    spacing: Dp = 6.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val base = if (showSeparator) {
        modifier
            .drawBehind {
                drawLine(
                    color = theme.separator,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(top = 9.dp)
    } else {
        modifier
    }
    Row(
        modifier = base,
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

// 42_000 -> "42s", 2_712_000 -> "45m12s", 8_020_000 -> "2h13m". Negative input -> "—".
fun formatMetricTime(ms: Long): String {
    if (ms < 0) return MetricEmpty
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h >= 1 -> String.format(Locale.US, "%dh%02dm", h, m)
        m >= 1 -> String.format(Locale.US, "%dm%02ds", m, s)
        else -> "${s}s"
    }
}

fun formatMetricDecimal(v: Double?): String =
    if (v == null || v.isNaN() || v.isInfinite()) MetricEmpty
    else String.format(Locale.US, "%.1f", v)

fun formatMetricCount(n: Long): String = String.format(Locale.US, "%,d", n)

fun formatMetricCount(n: Int): String = formatMetricCount(n.toLong())

// 850 -> "850", 8_400 -> "8.4k", 1_200_000 -> "1.2jt"; negatives get a U+2212 prefix.
fun formatCompactAmount(value: Long): String {
    val abs = Math.abs(value)
    val body = when {
        abs < 1_000L -> "$abs"
        else -> {
            val k = String.format(Locale.US, "%.1f", abs / 1000.0)
            if (abs < 1_000_000L && k != "1000.0") "${k}k"
            else String.format(Locale.US, "%.1f", abs / 1_000_000.0) + "jt"
        }
    }
    return if (value < 0) "−$body" else body
}

// "a/b" with "—" for a missing part, e.g. (null, null) -> "—/—".
fun formatMetricPair(a: String?, b: String?): String =
    "${a ?: MetricEmpty}/${b ?: MetricEmpty}"
