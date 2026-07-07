package com.kolee.tracklocation.feature.observer.presentation.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kolee.tracklocation.feature.observer.domain.model.AllowlistDraftRule
import com.kolee.tracklocation.feature.observer.domain.model.MatchType
import com.kolee.tracklocation.feature.observer.presentation.components.AllowlistBottomSheet
import com.kolee.tracklocation.feature.observer.presentation.viewmodel.ObserverViewModel
import com.kolee.tracklocation.ui.theme.ObserverAmber
import com.kolee.tracklocation.ui.theme.TripGreen

@Composable
fun ObserverSettingsScreen(navController: NavController) {
    val viewModel: ObserverViewModel = viewModel(factory = ObserverViewModel.Factory)
    val uiState by viewModel.uiState.collectAsState()
    val allowlistUiState by viewModel.allowlistUiState.collectAsState()

    var showAllowlist by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F4F0))
            .padding(horizontal = 18.dp)
    ) {
        item {
            // Top bar (matches ObserverFeedScreen's custom top bar treatment)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { navController.navigateUp() }
                        .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF0A0A0A),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = "Observer Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    color = Color(0xFF0A0A0A),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SettingsSectionHeader("SERVICE") }
        item {
            SettingsSectionGroup {
                ServiceStatusRow(enabled = uiState.serviceEnabled)
                SettingsRowDivider()
                OpenAccessibilitySettingsRow()
            }
            Text(
                text = "When enabled, the app can capture accessibility events from other apps.",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF737373),
                lineHeight = 17.sp,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }

        item { SettingsSectionHeader("CAPTURE") }
        item {
            SettingsSectionGroup {
                CaptureToggleRow(
                    running = uiState.captureRunning,
                    onToggle = viewModel::toggleCapture,
                )
            }
        }

        item { SettingsSectionHeader("ALLOWLIST") }
        item {
            SettingsSectionGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Package allowlist",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0A0A0A),
                        letterSpacing = (-0.1).sp,
                    )
                    Text(
                        text = "Only capture events from these packages. Empty = capture all.",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF737373),
                        lineHeight = 17.sp,
                    )
                }
                SettingsRowDivider()

                if (allowlistUiState.rules.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "No rules — capturing all packages.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF737373),
                        )
                    }
                } else {
                    allowlistUiState.rules.forEachIndexed { index, rule ->
                        AllowlistRuleSummaryRow(rule = rule)
                        if (index < allowlistUiState.rules.lastIndex) {
                            SettingsRowDivider()
                        }
                    }
                }

                SettingsRowDivider()
                ManageAllowlistRow(onClick = { showAllowlist = true })
            }
            Text(
                text = "Observer history cannot be cleared or deleted.",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF737373),
                lineHeight = 17.sp,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showAllowlist) {
        AllowlistBottomSheet(
            state = allowlistUiState,
            onAddRule = viewModel::addDraftRule,
            onUpdateRule = viewModel::updateDraftRule,
            onDeleteRule = viewModel::deleteDraftRule,
            onApply = viewModel::applyAllowlist,
            onClose = { showAllowlist = false },
        )
    }
}

@Composable
private fun ServiceStatusRow(enabled: Boolean) {
    val dotColor = if (enabled) TripGreen else Color(0xFFB3261E)
    val statusText = if (enabled) "Enabled" else "Disabled"
    val statusColor = if (enabled) TripGreen else Color(0xFFB3261E)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = "Accessibility service, $statusText" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFF1F4F0), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, shape = CircleShape)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Accessibility service",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0A0A0A),
                letterSpacing = (-0.1).sp,
            )
        }

        Text(
            text = statusText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = statusColor,
        )
    }
}

@Composable
private fun OpenAccessibilitySettingsRow() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            .semantics { contentDescription = "Open accessibility settings" }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFF1F4F0), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF0A0A0A),
            )
        }
        Text(
            text = "Open settings",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0A0A0A),
            modifier = Modifier.weight(1f),
            letterSpacing = (-0.1).sp,
        )
    }
}

@Composable
private fun CaptureToggleRow(running: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                contentDescription = if (running) "Capture events, running" else "Capture events, paused"
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFF1F4F0), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (running) TripGreen else ObserverAmber, shape = CircleShape)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Capture events",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0A0A0A),
                letterSpacing = (-0.1).sp,
            )
            Text(
                text = "Pause or resume event capture",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF737373),
                lineHeight = 17.sp,
            )
        }

        Switch(
            checked = running,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun AllowlistRuleSummaryRow(rule: AllowlistDraftRule) {
    val tagText = if (rule.matchType == MatchType.EXACT) "EXACT" else "REGEX"
    val textColor = if (rule.enabled) Color(0xFF0A0A0A) else Color(0xFFA3A3A3)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rule.pattern.ifBlank { "(empty pattern)" },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .background(Color(0xFFF1F4F0), shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = tagText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF737373),
                letterSpacing = 0.6.sp,
            )
        }
        if (!rule.enabled) {
            Text(
                text = "Off",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFA3A3A3),
            )
        }
    }
}

@Composable
private fun ManageAllowlistRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { contentDescription = "Manage allowlist" }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Manage allowlist",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0A0A0A),
            modifier = Modifier.weight(1f),
            letterSpacing = (-0.1).sp,
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF737373),
        letterSpacing = 1.4.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsSectionGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(18.dp))
            .border(1.5.dp, Color(0xFFEEF0EC), RoundedCornerShape(18.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color(0xFFEEF0EC))
    )
}
