package com.kolee.tracklocation.feature.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kolee.tracklocation.R
import com.kolee.tracklocation.feature.overlay.canDrawOverlays
import com.kolee.tracklocation.feature.overlay.data.OverlayPreferencesDataStore
import com.kolee.tracklocation.feature.overlay.overlaySettingsIntent
import com.kolee.tracklocation.feature.overlay.service.OverlayService
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.ui.theme.TripGreen
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import kotlinx.coroutines.launch

/**
 * One-time "Display over other apps" prompt (UI-SPEC 5d, state 7). Shown when a session is
 * active, the permission is missing and the prompt has never been shown. Re-checks the
 * permission on ON_RESUME and starts the overlay service once it becomes granted.
 */
@Composable
fun OverlayPermissionPromptHost() {
    val context = LocalContext.current
    val prefs = remember { OverlayPreferencesDataStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    // Defaults to true so the dialog cannot flash before the stored value is read.
    val promptShown by prefs.promptShown.collectAsState(initial = true)
    val sessionActive by TrackingService.locationUiState.collectAsState()
    var granted by remember { mutableStateOf(canDrawOverlays(context)) }
    // Dismissing hides the dialog immediately, before the DataStore write is observed.
    var dismissed by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = canDrawOverlays(context)
                val justGranted = nowGranted && !granted
                granted = nowGranted
                // App is in the foreground here; the service decides whether to show the strip.
                if (justGranted && TrackingService.locationUiState.value.isAlwaysRecording) {
                    OverlayService.start(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (sessionActive.isAlwaysRecording && !granted && !promptShown && !dismissed) {
        OverlayPermissionDialog(
            onNotNow = {
                dismissed = true
                scope.launch { prefs.setPromptShown(true) }
            },
            onOpenSettings = {
                dismissed = true
                scope.launch { prefs.setPromptShown(true) }
                context.startActivity(overlaySettingsIntent(context))
            },
        )
    }
}

@Composable
private fun OverlayPermissionDialog(onNotNow: () -> Unit, onOpenSettings: () -> Unit) {
    Dialog(onDismissRequest = onNotNow) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(TripGreen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fuel_dock),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Show session totals over other apps",
                color = TripInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Allow Display over other apps to see fuel left, range and session earnings " +
                    "on top of navigation and driver apps while a session is recording.",
                color = TripMuted,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tracking and recording keep working without it.",
                color = TripInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Not now",
                    color = TripMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNotNow() }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open settings",
                    color = TripGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onOpenSettings() }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
