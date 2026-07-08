package com.kolee.tracklocation.feature.observer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kolee.tracklocation.feature.observer.domain.model.ObservedEvent
import com.kolee.tracklocation.ui.theme.ObserverAmber
import com.kolee.tracklocation.ui.theme.ObserverAmberBg
import com.kolee.tracklocation.ui.theme.ObserverAmberHair
import com.kolee.tracklocation.ui.theme.ObserverGreenBg
import com.kolee.tracklocation.ui.theme.ObserverGreenLight
import com.kolee.tracklocation.ui.theme.ObserverRed
import com.kolee.tracklocation.ui.theme.ObserverRedBg
import com.kolee.tracklocation.ui.theme.ObserverRedHair
import com.kolee.tracklocation.ui.theme.TripGreenLabel
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Private types ─────────────────────────────────────────────────────────────

private data class SnapshotNode(val cls: String?, val text: String?, val cd: String?)

private enum class ContentMode { Formatted, Raw }

private data class ParsedSnapshot(
    val nodes: List<SnapshotNode>?,
    val rawJson: String,
    val parseError: String?,
)

// ── Parsing ───────────────────────────────────────────────────────────────────

private fun parseSnapshot(raw: String?): ParsedSnapshot {
    if (raw == null) return ParsedSnapshot(null, "", "No snapshot data")
    return try {
        val arr = JSONArray(raw)
        val nodes = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SnapshotNode(
                cls = o.optString("cls").ifEmpty { null },
                text = o.optString("text").ifEmpty { null },
                cd = o.optString("cd").ifEmpty { null },
            )
        }
        ParsedSnapshot(nodes, raw, null)
    } catch (e: Exception) {
        ParsedSnapshot(null, raw, e.message ?: "Parse error")
    }
}

private fun prettyJson(raw: String): String = try {
    JSONArray(raw).toString(2)
} catch (_: Exception) {
    raw
}

private fun buildFormattedText(nodes: List<SnapshotNode>): String = buildString {
    nodes.forEach { node ->
        appendLine(node.cls?.substringAfterLast('.') ?: "<node>")
        if (node.text != null) appendLine("  text: \"${node.text}\"")
        if (node.cd != null) appendLine("  desc: \"${node.cd}\"")
    }
}

// ── Date formatting ───────────────────────────────────────────────────────────

private val dateFormatter = SimpleDateFormat("MMM d, HH:mm:ss", Locale.ENGLISH)
private fun fmtTime(ms: Long): String = dateFormatter.format(Date(ms))

// ── Sheet entry point ─────────────────────────────────────────────────────────

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SnapshotViewerSheet(
    event: ObservedEvent,
    onDismiss: () -> Unit,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    canPrev: Boolean = false,
    canNext: Boolean = false,
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val sheetHeight = (screenHeightDp * 0.88f).dp

    // Parse once per open (keyed on event id)
    val parsed = remember(event.id) { parseSnapshot(event.treeSnapshot) }

    var mode by remember { mutableStateOf(ContentMode.Formatted) }
    var copied by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val hasReadableText = parsed.nodes?.any { it.text != null || it.cd != null } == true
    val canCopy = when {
        parsed.parseError != null -> mode == ContentMode.Raw
        !hasReadableText -> false
        else -> true
    }

    fun onCopy() {
        if (!canCopy) return
        val text = when (mode) {
            ContentMode.Formatted -> parsed.nodes?.let { buildFormattedText(it) } ?: parsed.rawJson
            ContentMode.Raw -> parsed.rawJson
        }
        clipboardManager.setText(AnnotatedString(text))
        copied = true
        coroutineScope.launch {
            delay(1400)
            copied = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},  // consume touches so scrim tap only fires outside
                    ),
            ) {
                DragHandle()
                TitleRow(
                    event = event,
                    onDismiss = onDismiss,
                    onPrev = onPrev,
                    onNext = onNext,
                    canPrev = canPrev,
                    canNext = canNext,
                )
                MetaStrip(event = event)
                ModeToolbar(
                    mode = mode,
                    onModeChange = { mode = it },
                    copied = copied,
                    canCopy = canCopy,
                    onCopy = { onCopy() },
                )
                ContentArea(
                    parsed = parsed,
                    mode = mode,
                    hasReadableText = hasReadableText,
                    truncationMetadata = event.truncationMetadata,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── Sheet sub-composables ─────────────────────────────────────────────────────

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFE7EAE6))
        )
    }
}

@Composable
private fun TitleRow(
    event: ObservedEvent,
    onDismiss: () -> Unit,
    onPrev: (() -> Unit)?,
    onNext: (() -> Unit)?,
    canPrev: Boolean,
    canNext: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Window content",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = TripInk,
            )
            Spacer(Modifier.height(2.dp))
            // Subtitle: pkg · activity · lastSeen
            val subtitle = buildAnnotatedString {
                withStyle(SpanStyle(color = TripInk, fontFamily = FontFamily.Monospace, fontSize = 12.sp)) {
                    append(event.packageName)
                }
                if (event.activityName != null) {
                    withStyle(SpanStyle(color = Color(0xFFCDD1CB), fontFamily = FontFamily.Monospace, fontSize = 12.sp)) {
                        append(" · ")
                    }
                    withStyle(SpanStyle(color = TripMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)) {
                        append(event.activityName)
                    }
                }
                withStyle(SpanStyle(color = Color(0xFFCDD1CB), fontFamily = FontFamily.Monospace, fontSize = 12.sp)) {
                    append(" · ")
                }
                withStyle(SpanStyle(color = TripMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)) {
                    append(fmtTime(event.timestampMs))
                }
            }
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Prev / Next navigation
        CircleIconButton(
            icon = Icons.Outlined.KeyboardArrowLeft,
            contentDescription = "Previous event",
            enabled = canPrev && onPrev != null,
            onClick = { onPrev?.invoke() },
        )
        CircleIconButton(
            icon = Icons.Outlined.KeyboardArrowRight,
            contentDescription = "Next event",
            enabled = canNext && onNext != null,
            onClick = { onNext?.invoke() },
        )
        // Close button
        CircleIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = "Close",
            enabled = true,
            onClick = onDismiss,
        )
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFFF1F4F0))
            .border(1.dp, Color(0xFFE7EAE6), CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) TripInk else Color(0xFFCDD1CB),
        )
    }
}

@Composable
private fun MetaStrip(event: ObservedEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Event type — full width, no label
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE7EAE6), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = event.eventType.removePrefix("TYPE_"),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TripGreenLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // First seen / Last seen (+ repeat)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetaChip(
                label = "FIRST SEEN",
                value = fmtTime(event.firstSeenMs),
                valueColor = TripInk,
                modifier = Modifier.weight(1f),
            )
            MetaChip(
                label = "LAST SEEN",
                value = fmtTime(event.timestampMs),
                valueColor = TripInk,
                modifier = Modifier.weight(1f),
            )
            if (event.repeatCount > 1) {
                MetaChip(
                    label = "REPEAT",
                    value = "×${event.repeatCount}",
                    valueColor = ObserverAmber,
                )
            }
        }
    }
}

@Composable
private fun MetaChip(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFFE7EAE6), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = Color(0xFFA3A3A3),
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ModeToolbar(
    mode: ContentMode,
    onModeChange: (ContentMode) -> Unit,
    copied: Boolean,
    canCopy: Boolean,
    onCopy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFFEEF0EC), shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SegmentedControl(
            options = listOf("Formatted", "Raw JSON"),
            selected = if (mode == ContentMode.Formatted) 0 else 1,
            onSelect = { i -> onModeChange(if (i == 0) ContentMode.Formatted else ContentMode.Raw) },
            modifier = Modifier.weight(1f),
        )
        // Copy button
        val copyBg = if (copied) ObserverGreenLight else Color.White
        val copyFg = if (copied) Color.White else TripInk
        val copyBorder = if (copied) ObserverGreenLight else TripInk
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(copyBg)
                .border(1.5.dp, copyBorder, RoundedCornerShape(999.dp))
                .then(
                    if (canCopy) Modifier.clickable(onClick = onCopy)
                    else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (canCopy) copyFg else Color(0xFFA3A3A3),
            )
            Text(
                text = if (copied) "Copied" else "Copy",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (canCopy) copyFg else Color(0xFFA3A3A3),
            )
        }
    }
}

@Composable
private fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, Color(0xFFE7EAE6), RoundedCornerShape(999.dp))
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val isActive = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isActive) TripInk else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp,
                    color = if (isActive) Color.White else TripInk,
                )
            }
        }
    }
}

@Composable
private fun TruncationBanner(truncationMetadata: String?) {
    if (truncationMetadata == null) return
    val (label, count) = try {
        val o = JSONObject(truncationMetadata)
        val l = when (o.getString("reason")) {
            "node_limit"  -> "node limit"
            "depth_limit" -> "depth limit"
            "size_limit"  -> "size limit"
            else -> return
        }
        l to o.getInt("nodesCaptured")
    } catch (e: Exception) { return }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ObserverAmberBg)
            .border(1.dp, ObserverAmberHair, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = ObserverAmber,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Tree truncated — $label",
            color = ObserverAmber,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.05).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "($count nodes captured)",
            color = ObserverAmber.copy(alpha = 0.65f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun ContentArea(
    parsed: ParsedSnapshot,
    mode: ContentMode,
    hasReadableText: Boolean,
    truncationMetadata: String?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    // Reset scroll when mode changes
    LaunchedEffect(mode) { scrollState.scrollTo(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFBFCFA))
            .border(width = 1.dp, color = Color(0xFFEEF0EC), shape = RoundedCornerShape(0.dp)),
    ) {
        TruncationBanner(truncationMetadata)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 24.dp),
        ) {
            when {
                parsed.parseError != null -> ParseErrorContent(parsed = parsed, mode = mode)
                !hasReadableText -> NoReadableTextContent()
                mode == ContentMode.Formatted -> FormattedContent(nodes = parsed.nodes ?: emptyList())
                else -> RawJsonContent(rawJson = parsed.rawJson)
            }
        }
    }
}

@Composable
private fun FormattedContent(nodes: List<SnapshotNode>) {
    nodes.forEachIndexed { index, node ->
        if (index > 0) Spacer(Modifier.height(8.dp))
        NodeRow(node = node)
    }
    if (nodes.isEmpty()) {
        Text(
            text = "No nodes captured.",
            fontSize = 12.sp,
            color = Color(0xFFA3A3A3),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun NodeRow(node: SnapshotNode) {
    val shortCls = node.cls?.substringAfterLast('.') ?: "<node>"
    val fullCls = node.cls

    Column {
        // Class line
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = shortCls,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TripInk,
            )
            if (fullCls != null && fullCls != shortCls) {
                Text(
                    text = "($fullCls)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCDD1CB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // text: line
        if (node.text != null) {
            Spacer(Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "text:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TripGreenLabel,
                )
                Box(
                    modifier = Modifier
                        .background(ObserverGreenBg, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "\"${node.text}\"",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TripInk,
                    )
                }
            }
        }
        // desc: line
        if (node.cd != null) {
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "desc:",
                    fontSize = 12.sp,
                    color = Color(0xFFA3A3A3),
                )
                Text(
                    text = "\"${node.cd}\"",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = TripMuted,
                )
            }
        }
    }
}

@Composable
private fun RawJsonContent(rawJson: String) {
    val pretty = remember(rawJson) { prettyJson(rawJson) }
    val lines = remember(pretty) { pretty.lines() }

    Column {
        lines.forEachIndexed { i, line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${i + 1}",
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFCDD1CB),
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
                Text(
                    text = line,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TripInk,
                    lineHeight = (11.5 * 1.55).sp,
                )
            }
        }
    }
}

@Composable
private fun ParseErrorContent(parsed: ParsedSnapshot, mode: ContentMode) {
    InlineMessageCard(
        icon = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ObserverRed, modifier = Modifier.size(20.dp)) },
        iconBg = ObserverRedBg,
        iconBorder = ObserverRedHair,
        title = "Unable to render snapshot",
        titleColor = ObserverRed,
        body = "The captured node tree could not be parsed. The raw payload is still available — switch to Raw JSON to copy it.",
        hint = parsed.parseError,
    )
    if (mode == ContentMode.Raw) {
        Spacer(Modifier.height(12.dp))
        val preview = if (parsed.rawJson.length > 280) parsed.rawJson.take(280) + "…truncated" else parsed.rawJson
        Text(
            text = preview,
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            color = TripMuted,
            lineHeight = (11.5 * 1.55).sp,
        )
    }
}

@Composable
private fun NoReadableTextContent() {
    InlineMessageCard(
        icon = { Icon(Icons.Outlined.TextSnippet, contentDescription = null, tint = Color(0xFFA3A3A3), modifier = Modifier.size(20.dp)) },
        iconBg = Color(0xFFF3F4F6),
        iconBorder = Color(0xFFE7EAE6),
        title = "No readable text found",
        titleColor = TripMuted,
        body = "The captured tree contains nodes but no text or content-description fields. This commonly happens for canvas-rendered surfaces or fully-icon UIs.",
        hint = null,
    )
}

@Composable
private fun InlineMessageCard(
    icon: @Composable () -> Unit,
    iconBg: Color,
    iconBorder: Color,
    title: String,
    titleColor: Color,
    body: String,
    hint: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .background(
                if (titleColor == ObserverRed) ObserverRedBg else Color(0xFFF3F4F6),
                RoundedCornerShape(14.dp),
            )
            .border(
                1.dp,
                if (titleColor == ObserverRed) ObserverRedHair else Color(0xFFE7EAE6),
                RoundedCornerShape(14.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, iconBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            fontSize = 12.5.sp,
            color = TripMuted,
            lineHeight = (12.5 * 1.4).sp,
        )
        if (hint != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, Color(0xFFE7EAE6), RoundedCornerShape(999.dp))
                    .background(Color.White, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = hint,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFA3A3A3),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
