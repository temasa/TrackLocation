package com.kolee.tracklocation.feature.observer.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.feature.observer.domain.model.ObservedEvent
import com.kolee.tracklocation.ui.theme.ObserverCardAlt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventRow(
    event: ObservedEvent,
    isEven: Boolean,
    autoScrollPaused: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isEven) ObserverCardAlt else Color.White
    val cd = "${event.packageName}, ${event.activityName ?: ""}, ${event.eventType}, at ${timeFormatter.format(Date(event.timestampMs))}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(
                onClick = {},
                onLongClick = { if (autoScrollPaused) onLongPress() },
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .semantics { contentDescription = cd },
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // Line 1: package · event type chip · timestamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = event.packageName,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF0A0A0A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                letterSpacing = (-0.1).sp,
            )
            EventTypeChip(eventType = event.eventType)
            Text(
                text = timeFormatter.format(Date(event.timestampMs)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF737373),
            )
        }

        // Line 2: activity/class
        event.activityName?.let { activity ->
            Text(
                text = activity,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF737373),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.1).sp,
            )
        }

        // Line 3: optional text snippet
        event.textSummary?.let { snippet ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "↳",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFA3A3A3),
                )
                Text(
                    text = "\"$snippet\"",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF0A0A0A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    // Bottom divider
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFEEF0EC))
    )
}

@Composable
fun EventTypeChip(eventType: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when {
        eventType.startsWith("TYPE_VIEW_CLICK") ||
        eventType.startsWith("TYPE_TOUCH") ||
        eventType.startsWith("TYPE_GESTURE") -> Color(0xFFEEF6FF) to Color(0xFF1D4ED8)

        eventType.startsWith("TYPE_VIEW_SCROLL") ||
        eventType.startsWith("TYPE_VIEW_FOCUS") ||
        eventType.startsWith("TYPE_VIEW_HOVER") -> Color(0xFFF5F3FF) to Color(0xFF6D28D9)

        eventType.startsWith("TYPE_VIEW_TEXT") ||
        eventType.startsWith("TYPE_VIEW_SELECTION") ||
        eventType.contains("INPUT") -> Color(0xFFFEF7E6) to Color(0xFF92400E)

        eventType.startsWith("TYPE_WINDOW") ||
        eventType.startsWith("TYPE_VIEW_CONTENT") ||
        eventType.contains("STATE") -> Color(0xFFECFDF5) to Color(0xFF047857)

        eventType.startsWith("TYPE_ANNOUNCEMENT") ||
        eventType.startsWith("TYPE_NOTIFICATION") -> Color(0xFFFDF2F8) to Color(0xFF9D174D)

        else -> Color(0xFFF3F4F6) to Color(0xFF374151)
    }

    // Shorten the type label to fit in the chip
    val label = eventType
        .removePrefix("TYPE_")
        .replace("_", " ")
        .take(20)
        .trimEnd()

    Box(
        modifier = modifier
            .background(bg, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = fg,
            letterSpacing = 0.1.sp,
            maxLines = 1,
        )
    }
}
