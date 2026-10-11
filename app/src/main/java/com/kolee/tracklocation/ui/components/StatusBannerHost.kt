package com.kolee.tracklocation.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.feature.obd.service.ObdUiState
import com.kolee.tracklocation.navigation.Screen
import com.kolee.tracklocation.viewmodel.StatusBannerViewModel

// Same route set as FuelDockHost (ADR-027 / ADR-028): keep the two in sync.
private val BannerRoutes = setOf(
    Screen.TrackScreen.route,
    Screen.SessionScreen.route,
    Screen.ListScreen.route,
    Screen.SettingsScreen.route,
)

private class ChipColors(val bg: Color, val border: Color, val ink: Color, val accent: Color)

private val ObdColors = ChipColors(
    bg = Color(0xFFFFF7E6), border = Color(0xFFF5B041), ink = Color(0xFF7A4A00), accent = Color(0xFFD97706),
)
private val ObserverColors = ChipColors(
    bg = Color(0xFFFEECEC), border = Color(0xFFF08A8A), ink = Color(0xFF8A1C1C), accent = Color(0xFFDC2626),
)

/**
 * Floating top overlay warning that OBD is not connected and/or the Observer accessibility
 * service is off (ADR-028, UI-SPEC 5c). Place in a Box over the nav graph; it only draws
 * (and only consumes touches on) the chips themselves.
 */
@Composable
fun StatusBannerHost(navController: NavController, modifier: Modifier = Modifier) {
    val vm: StatusBannerViewModel = viewModel(factory = StatusBannerViewModel.Factory)
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val obdProblem by vm.obdProblem.collectAsState()
    val observerProblem by vm.observerProblem.collectAsState()
    val collapsed by vm.collapsed.collectAsState()
    val obdState by ObdPollingService.obdUiState.collectAsState()

    // Poll the accessibility state only while the activity is started.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.startPolling()
                Lifecycle.Event.ON_STOP -> vm.stopPolling()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.stopPolling()
        }
    }

    if (route !in BannerRoutes || (!obdProblem && !observerProblem)) return

    val context = LocalContext.current
    val obdConnecting = obdState is ObdUiState.Connecting || obdState is ObdUiState.Retrying

    if (collapsed) {
        Box(modifier = modifier.fillMaxWidth().padding(top = 8.dp, end = 16.dp)) {
            Column(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (observerProblem) {
                    CollapsedChip(
                        icon = Icons.Filled.VisibilityOff,
                        colors = ObserverColors,
                        description = "Observer status. Expand",
                        onClick = { vm.setCollapsed(false) },
                    )
                }
                if (obdProblem) {
                    CollapsedChip(
                        icon = Icons.Filled.BluetoothDisabled,
                        colors = ObdColors,
                        description = "OBD status. Expand",
                        onClick = { vm.setCollapsed(false) },
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (observerProblem) {
                ExpandedChip(
                    icon = Icons.Filled.VisibilityOff,
                    colors = ObserverColors,
                    title = "Observer inactive",
                    hint = "Orders won't be captured · Tap to fix",
                    description = "Observer inactive. Orders won't be captured. Opens Accessibility settings",
                    onClick = { openSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS) },
                    onMinimize = { vm.setCollapsed(true) },
                )
            }
            if (obdProblem) {
                ExpandedChip(
                    icon = if (obdConnecting) Icons.Filled.Sync else Icons.Filled.BluetoothDisabled,
                    colors = ObdColors,
                    title = if (obdConnecting) "OBD connecting…" else "OBD not connected",
                    hint = "Tap to open Bluetooth settings",
                    description = (if (obdConnecting) "OBD connecting" else "OBD not connected") +
                        ". Opens Bluetooth settings",
                    onClick = { openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS) },
                    onMinimize = { vm.setCollapsed(true) },
                )
            }
        }
    }
}

private fun openSettings(context: Context, action: String) {
    try {
        context.startActivity(Intent(action))
    } catch (e: ActivityNotFoundException) {
        Log.w("StatusBannerHost", "No activity for $action", e)
    }
}

@Composable
private fun ExpandedChip(
    icon: ImageVector,
    colors: ChipColors,
    title: String,
    hint: String,
    description: String,
    onClick: () -> Unit,
    onMinimize: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(colors.bg)
            .border(BorderStroke(1.5.dp, colors.border), shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(title, color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(hint, color = colors.ink, fontSize = 11.sp, maxLines = 1)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
            Box(modifier = Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(colors.border)
        )
        // 48dp touch target around the 32dp visual.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onMinimize)
                .semantics { contentDescription = "Collapse status banner" },
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.UnfoldLess, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CollapsedChip(
    icon: ImageVector,
    colors: ChipColors,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.bg)
            .border(BorderStroke(1.5.dp, colors.border), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
    }
}
