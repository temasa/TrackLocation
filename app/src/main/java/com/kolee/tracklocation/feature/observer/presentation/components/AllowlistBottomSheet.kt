package com.kolee.tracklocation.feature.observer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.feature.observer.domain.model.AllowlistDraftRule
import com.kolee.tracklocation.feature.observer.domain.model.AllowlistUiState
import com.kolee.tracklocation.feature.observer.domain.model.MatchType
import com.kolee.tracklocation.ui.theme.ObserverAmber
import com.kolee.tracklocation.ui.theme.ObserverAmberBg
import com.kolee.tracklocation.ui.theme.ObserverAmberDark
import com.kolee.tracklocation.ui.theme.ObserverAmberHair

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AllowlistBottomSheet(
    state: AllowlistUiState,
    onAddRule: () -> Unit,
    onUpdateRule: (AllowlistDraftRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onApply: () -> Unit,
    onClose: () -> Unit,
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onClose() },
            contentAlignment = Alignment.BottomCenter,
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (screenHeightDp * 0.62f).dp)
                .background(Color.White, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* consume clicks to prevent scrim dismiss */ }
                .verticalScroll(rememberScrollState())
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color(0xFFE0E3DF), shape = RoundedCornerShape(2.dp))
                )
            }
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Allowlist",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A0A0A),
                        letterSpacing = (-0.2).sp,
                    )
                    Text(
                        "Match by package name. Applies to future capture only.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF737373),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0A))
                        .clickable { onAddRule() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add rule",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Draft banner
            if (state.hasDraftChanges) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 10.dp)
                        .background(ObserverAmberBg, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, ObserverAmberHair, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ObserverAmberDark,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "Draft changes not applied",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ObserverAmberDark,
                    )
                }
            }

            // Rule list container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .border(1.dp, Color(0xFFEEF0EC), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
            ) {
                if (state.rules.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "No rules yet",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0A0A0A),
                        )
                        Text(
                            "Capturing all packages. Add rules to reduce noise.",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF737373),
                            lineHeight = 17.sp,
                        )
                    }
                } else {
                    state.rules.forEachIndexed { index, rule ->
                        AllowlistRuleRow(
                            rule = rule,
                            onUpdate = onUpdateRule,
                            onDelete = { onDeleteRule(rule.id) },
                        )
                        if (index < state.rules.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFEEF0EC))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Close button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.5.dp, Color(0xFFE7EAE6), RoundedCornerShape(22.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Close",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0A0A0A),
                    )
                }

                // Apply button
                val applyEnabled = state.hasDraftChanges
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (applyEnabled) Color(0xFF0A0A0A) else Color(0xFFE1E3DF))
                        .clickable(enabled = applyEnabled) { onApply() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Apply",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (applyEnabled) Color.White else Color(0xFFA3A3A3),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
        }
    }
}

@Composable
private fun AllowlistRuleRow(
    rule: AllowlistDraftRule,
    onUpdate: (AllowlistDraftRule) -> Unit,
    onDelete: () -> Unit,
) {
    val rowBg = if (rule.isDraft) Color(0xFFFFFDF7) else Color.White

    Box(modifier = Modifier.background(rowBg)) {
        // Draft indicator bar
        if (rule.isDraft) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .background(ObserverAmber)
                    .align(Alignment.CenterStart)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Match-type segmented toggle
            MatchTypeToggle(
                selected = rule.matchType,
                onSelect = { onUpdate(rule.copy(matchType = it)) },
            )

            // Pattern text field
            var patternText by remember(rule.id) { mutableStateOf(rule.pattern) }
            val placeholder = if (rule.matchType == MatchType.EXACT) "com.example.app" else "keyword"

            BasicTextField(
                value = patternText,
                onValueChange = { newText ->
                    patternText = newText
                    onUpdate(rule.copy(pattern = newText))
                },
                enabled = rule.enabled,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (rule.enabled) Color(0xFF0A0A0A) else Color(0xFFA3A3A3),
                ),
                cursorBrush = SolidColor(Color(0xFF0A0A0A)),
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .border(1.dp, Color(0xFFE7EAE6), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (patternText.isEmpty()) {
                            Text(
                                placeholder,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFFA3A3A3),
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Enable/disable switch
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onUpdate(rule.copy(enabled = it)) },
            )

            // Delete button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete rule",
                    tint = Color(0xFF737373),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun MatchTypeToggle(
    selected: MatchType,
    onSelect: (MatchType) -> Unit,
) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .background(Color(0xFFF1F4F0), shape = RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE7EAE6), RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(MatchType.EXACT to "Exact", MatchType.REGEX to "Regex").forEach { (type, label) ->
            val isActive = selected == type
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isActive) Color.White else Color.Transparent)
                    .clickable { onSelect(type) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isActive) Color(0xFF0A0A0A) else Color(0xFF737373),
                )
            }
        }
    }
}
