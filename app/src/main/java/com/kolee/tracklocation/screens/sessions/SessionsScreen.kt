@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kolee.tracklocation.screens.sessions

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kolee.tracklocation.R
import com.kolee.tracklocation.data.roomdb.SessionEntity
import com.kolee.tracklocation.feature.obd.FuelPriceController
import com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.feature.obd.service.ObdUiState
import com.kolee.tracklocation.feature.obd.ui.FuelCostEditorDialog
import com.kolee.tracklocation.feature.obd.ui.formatIdr
import com.kolee.tracklocation.permission.CheckAndRequestPermissions
import com.kolee.tracklocation.tracking.Actions
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.ui.theme.TripBackground
import com.kolee.tracklocation.ui.theme.TripBorder
import com.kolee.tracklocation.ui.theme.TripBorderSoft
import com.kolee.tracklocation.ui.theme.TripGreen
import com.kolee.tracklocation.ui.theme.TripGreenDark
import com.kolee.tracklocation.ui.theme.TripGreenLabel
import com.kolee.tracklocation.ui.theme.TripGreenMid
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import com.kolee.tracklocation.ui.theme.TripSurface
import com.kolee.tracklocation.ui.theme.TripTertiary
import com.kolee.tracklocation.utils.TimeUtilFormatter
import com.kolee.tracklocation.viewmodel.ShareViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SessionUiItem(
    val index: Int,
    val start: String,
    val durationMs: Long,
    val distanceKm: Float,
    val points: Int,
    val active: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen() {
    val context = LocalContext.current
    val viewModel: ShareViewModel = viewModel(
        viewModelStoreOwner = context as ComponentActivity,
        factory = ShareViewModel.Factory
    )
    val locationUiState by viewModel.locationUiState.collectAsState()
    val obdState by ObdPollingService.obdUiState.collectAsState()

    // Fuel Cost (FR-12): attach the shared price controller once, then observe its state.
    LaunchedEffect(Unit) {
        FuelPriceController.attach(
            (context.applicationContext as com.kolee.tracklocation.TrackApp).fuelPriceDao,
            ObdPreferencesDataStore(context.applicationContext)
        )
    }
    val priceState by FuelPriceController.state.collectAsState()
    var showPriceDialog by remember { mutableStateOf(false) }

    val sessions = viewModel.sessionsState.mapIndexed { index, session ->
        session.toUiItem(index = sessionsIndex(viewModel.sessionsState.size, index))
    }

    var requestRecordingPermission by remember { mutableStateOf(false) }
    var guardMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = TripBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TripBackground)
                .padding(padding)
        ) {
            PageHeader()
            StatusCard(
                on = locationUiState.isAlwaysRecording,
                isTripActive = locationUiState.isTracking,
                helperText = guardMessage,
                onAlwaysRecordingChange = { enabled ->
                    guardMessage = null
                    if (enabled) {
                        requestRecordingPermission = true
                    } else if (locationUiState.isTracking) {
                        guardMessage = "Always-recording is required while a trip is running."
                    } else {
                        performTrackingService(context, Actions.STOP_RECORDING)
                    }
                }
            )

            if (obdState is ObdUiState.Connected || obdState is ObdUiState.Waiting) {
                val sessionFuelConsumedL = (obdState as? ObdUiState.Connected)?.sessionFuelConsumedL
                val price = priceState.currentPrice
                val costText = if (sessionFuelConsumedL != null && price > 0.0) {
                    formatIdr(sessionFuelConsumedL * price)
                } else "—"
                ObdStatusCard(
                    obdState = obdState,
                    locationUiState = locationUiState,
                    costText = costText,
                    onCostClick = { showPriceDialog = true },
                    onReconnect = {
                        val intent = Intent(context, ObdPollingService::class.java).apply {
                            action = ObdPollingService.ACTION_RECONNECT_NOW
                        }
                        context.startService(intent)
                    }
                )
            }

            if (sessions.isNotEmpty()) {
                SessionsList(
                    sessions = sessions,
                    modifier = Modifier.weight(1f)
                )
            } else {
                EmptySessionsState(modifier = Modifier.weight(1f))
            }
        }

        if (requestRecordingPermission) {
            CheckAndRequestPermissions(
                isGranted = {
                    requestRecordingPermission = false
                    performTrackingService(context, Actions.START_RECORDING)
                }
            )
        }

        // Fuel Cost (FR-12): shared price editor with in-memory undo/redo.
        if (showPriceDialog) {
            FuelCostEditorDialog(
                current = priceState.currentPrice,
                canUndo = priceState.canUndo,
                canRedo = priceState.canRedo,
                onSave = { newPrice ->
                    FuelPriceController.set(newPrice)
                    showPriceDialog = false
                },
                onUndo = { FuelPriceController.undo() },
                onRedo = { FuelPriceController.redo() },
                onDismiss = { showPriceDialog = false }
            )
        }
    }
}

@Composable
private fun PageHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 14.dp, end = 22.dp, bottom = 6.dp)
    ) {
        Text(
            text = "Trip Tracker",
            color = TripMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = "Sessions",
            color = TripInk,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 42.sp,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun StatusCard(
    on: Boolean,
    isTripActive: Boolean,
    helperText: String?,
    onAlwaysRecordingChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
            ) == 0f
        } catch (_: Throwable) { false }
    }

    val statusText = if (on) "Active" else "Inactive"
    val descriptionText = when {
        helperText != null -> helperText
        on -> "Recording location sessions in the background."
        else -> "Location sessions are not being recorded."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 12.dp, end = 22.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = TripGreenDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Outer Box has no clip so rings can ripple beyond the circle boundary.
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                // Clipped background circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (on) TripGreen.copy(alpha = 0.18f) else TripGreenMid)
                )
                if (on && !reduceMotion) {
                    SessionPulseRing(delayMs = 0)
                    SessionPulseRing(delayMs = 600)
                }
                PulseDot(
                    color = if (on) TripGreen else Color(0xFF9AA9A1),
                    size = if (on) 18 else 14,
                    pulsing = on && !reduceMotion,
                    pulseTargetAlpha = 0.35f,
                    pulseDurationMs = 1200,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Always-recording",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isTripActive && !on) {
                        Text(
                            text = "Required while a trip is running",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = descriptionText,
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Switch(
                checked = on,
                onCheckedChange = onAlwaysRecordingChange
            )
        }
    }
}

@Composable
private fun EmptySessionsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(TripSurface)
                .border(1.5f.dp, TripBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_session_signal),
                contentDescription = null,
                tint = TripTertiary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = "No sessions recorded yet",
            color = TripInk,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text = buildAnnotatedString {
                append("Sessions appear here when always-recording is turned ")
                withStyle(SpanStyle(color = TripInk, fontWeight = FontWeight.SemiBold)) {
                    append("ON")
                }
                append(".")
            },
            color = TripMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 14.dp)
                .width(280.dp)
        )
    }
}

private fun SessionEntity.toUiItem(index: Int): SessionUiItem {
    return SessionUiItem(
        index = index,
        start = SimpleDateFormat("EEE, HH:mm", Locale.ENGLISH).format(Date(startedAt)),
        durationMs = durationMillis,
        distanceKm = (distanceMeters / 1000.0).toFloat(),
        points = pointCount,
        active = isActive
    )
}

private fun sessionsIndex(size: Int, index: Int): Int = size - index

@Composable
private fun SessionsList(
    sessions: List<SessionUiItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, top = 18.dp, end = 22.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Recorded sessions",
                    color = TripInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Newest first",
                    color = TripMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        items(sessions.size) { index ->
            SessionRow(item = sessions[index])
        }
    }
}

@Composable
private fun SessionRow(item: SessionUiItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 12.dp, end = 22.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TripSurface)
            .border(
                width = 1.5f.dp,
                color = if (item.active) TripGreen else TripBorderSoft,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(start = 20.dp, top = 18.dp, end = 18.dp, bottom = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MarkerGutter(accent = if (item.active) TripGreen else TripGreenLabel)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Session #${item.index}",
                        color = TripInk,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = item.start,
                            color = TripMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                        )
                        if (item.active) ActiveBadge()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 14.dp)
                        .offset(x = (-34).dp)
                        .height(1.dp)
                        .background(TripBorderSoft)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SessionMetric(
                        value = TimeUtilFormatter.getTime(item.durationMs),
                        label = "duration",
                        modifier = Modifier.weight(1f)
                    )
                    SessionMetric(
                        value = String.format(Locale.ENGLISH, "%.2f km", item.distanceKm),
                        label = "distance",
                        modifier = Modifier.weight(1f)
                    )
                    SessionMetric(
                        value = item.points.toString(),
                        label = "points",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(TripGreen.copy(alpha = 0.12f))
            .padding(start = 8.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PulseDot(color = TripGreen, size = 7, pulsing = true)
        Text(
            text = "ACTIVE",
            color = TripGreenLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun MarkerGutter(accent: Color) {
    Column(
        modifier = Modifier
            .width(14.dp)
            .padding(top = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(TripSurface)
                .border(2.5f.dp, accent, CircleShape)
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = 2.5f.dp, height = 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
    }
}

@Composable
private fun SessionMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = TripInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 17.sp,
            letterSpacing = 0.sp,
            maxLines = 1
        )
        Text(
            text = label,
            color = TripMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun PulseDot(
    color: Color,
    size: Int,
    pulsing: Boolean,
    pulseTargetAlpha: Float = 0.55f,
    pulseDurationMs: Int = 1600,
) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) pulseTargetAlpha else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDurationMs),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun SessionPulseRing(delayMs: Int) {
    val transition = rememberInfiniteTransition()
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing, delayMillis = delayMs),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing, delayMillis = delayMs),
            repeatMode = RepeatMode.Restart,
        ),
    )
    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .alpha(ringAlpha)
            .border(2.dp, TripGreen, CircleShape)
    )
}

@Composable
private fun ObdStatusCard(
    obdState: ObdUiState,
    locationUiState: com.kolee.tracklocation.tracking.LocationUiState,
    costText: String,
    onCostClick: () -> Unit,
    onReconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, top = 12.dp, end = 22.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TripSurface.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_session_signal),
                        contentDescription = null,
                        tint = TripGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "OBD Status",
                        color = TripInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                val (statusText, statusColor) = when (obdState) {
                    is ObdUiState.Connected -> Pair("Connected", TripGreen)
                    is ObdUiState.Waiting -> Pair("Waiting", Color(0xFFE5484D))
                    else -> Pair("—", TripMuted)
                }
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (obdState is ObdUiState.Connected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column {
                        Text(
                            text = "RPM",
                            color = TripMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = obdState.rpm?.toString() ?: "—",
                            color = TripInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "SPEED",
                            color = TripMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = obdState.obdSpeedKmh?.let { "$it km/h" } ?: "—",
                            color = TripInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "EFFICIENCY",
                            color = TripMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        val shouldShowKmL = obdState.instantKmL != null &&
                                locationUiState.speedInKMH > 3f &&
                                locationUiState.accuracyMeters <= 20f
                        Text(
                            text = if (shouldShowKmL) String.format("%.1f km/L", obdState.instantKmL) else "—",
                            color = TripInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "FUEL RATE",
                            color = TripMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = obdState.fuelRateLph?.takeIf { it > 0.0 }
                                ?.let { String.format("%.1f L/h", it) } ?: "—",
                            color = TripInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Session-average km/L (persisted; survives app restart) — OBD Phase 2 Slice 3.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    Column {
                        Text(
                            text = "SESSION AVG",
                            color = TripMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = obdState.avgKmL?.let { String.format("%.1f km/L", it) } ?: "—",
                            color = TripInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fuel Cost (FR-12): tappable COST cell (litres × shared price). "—" when unset.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCostClick() }
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Column {
                        Text(
                            text = "COST",
                            color = TripMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = costText,
                            color = TripInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fuel-source chip reflecting the actual source of the rate reading.
                val fuelSourceLabel = when (obdState.fuelSource) {
                    "DIRECT_FUEL_RATE" -> "Direct (OBD)"
                    "MAF_DERIVED" -> "Inferred (MAF)"
                    "SPEED_DENSITY" -> "Estimated"
                    else -> "Unavailable"
                }
                val fuelSourceTint = if (obdState.fuelSource == "DIRECT_FUEL_RATE") TripGreen else Color(0xFFB8860B)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .height(36.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = fuelSourceTint.copy(alpha = 0.12f)
                        )
                    ) {
                        Text(
                            text = fuelSourceLabel,
                            color = fuelSourceTint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (obdState is ObdUiState.Waiting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column {
                        Text("RPM", color = TripMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("—", color = TripInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("SPEED", color = TripMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("—", color = TripInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("EFFICIENCY", color = TripMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("—", color = TripInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onReconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_session_signal),
                        contentDescription = null,
                        tint = TripGreen,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 6.dp)
                    )
                    Text(
                        text = "RECONNECT",
                        color = TripGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun performTrackingService(
    context: Context,
    actions: Actions
) {
    Intent(context, TrackingService::class.java).also {
        it.action = actions.name
        ContextCompat.startForegroundService(context, it)
    }
}
