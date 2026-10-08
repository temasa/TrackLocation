package com.kolee.tracklocation.screens.list.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.feature.obd.FuelPriceController
import com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.feature.obd.service.ObdUiState
import com.kolee.tracklocation.feature.obd.ui.FuelCostEditorDialog
import com.kolee.tracklocation.feature.obd.ui.formatIdr
import com.kolee.tracklocation.R
import com.kolee.tracklocation.tracking.LocationUiState
import com.kolee.tracklocation.ui.theme.MonospaceFontFamily
import com.kolee.tracklocation.ui.theme.TripBackground
import com.kolee.tracklocation.ui.theme.TripBorder
import com.kolee.tracklocation.ui.theme.TripGreen
import com.kolee.tracklocation.ui.theme.TripGreenDark
import com.kolee.tracklocation.ui.theme.TripGreenMid
import com.kolee.tracklocation.ui.theme.TripHeroBg
import com.kolee.tracklocation.ui.theme.TripHeroBrandGreen
import com.kolee.tracklocation.ui.theme.TripHeroDim
import com.kolee.tracklocation.ui.theme.TripHeroDotHalo
import com.kolee.tracklocation.ui.theme.TripHeroDotIdle
import com.kolee.tracklocation.ui.theme.TripHeroEyebrow
import com.kolee.tracklocation.ui.theme.TripHeroIndicatorWash
import com.kolee.tracklocation.ui.theme.TripHeroReadoutIdle
import com.kolee.tracklocation.ui.theme.TripHeroReadoutLive
import com.kolee.tracklocation.ui.theme.TripHeroStopRed
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import com.kolee.tracklocation.ui.theme.TripSurface
import com.kolee.tracklocation.viewmodel.Response
import com.kolee.tracklocation.viewmodel.ShareViewModel
import kotlinx.coroutines.delay
import java.util.Locale


@Composable
fun ListContent(
    modifier: Modifier = Modifier,
    responseState: Response,
    onSelect: (trackIdx: Int) -> Unit,
    viewModel: ShareViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TripBackground)
    ) {
        when (responseState) {
            is Response.Loading -> LoadingIndicator()

            is Response.Success -> {
                val trackList = responseState.data

                TrackSuccessState(
                    trackList = trackList,
                    onSelect = onSelect,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun TrackSuccessState(
    trackList: List<TrackEntity>,
    onSelect: (trackIdx: Int) -> Unit,
    viewModel: ShareViewModel
) {
    val uiState by viewModel.locationUiState.collectAsState()
    val isTripActive = uiState.isTracking
    val obdState by ObdPollingService.obdUiState.collectAsState()
    val connected = obdState as? ObdUiState.Connected

    // Fuel Cost (ADR-008): attach the shared price controller once, observe its state.
    val context = LocalContext.current
    val fuelPriceDao = remember {
        (context.applicationContext as com.kolee.tracklocation.TrackApp).fuelPriceDao
    }
    LaunchedEffect(Unit) {
        FuelPriceController.attach(fuelPriceDao, ObdPreferencesDataStore(context.applicationContext))
    }
    val priceState by FuelPriceController.state.collectAsState()
    var showPriceDialog by remember { mutableStateOf(false) }
    val tripFuelConsumedL = connected?.tripFuelConsumedL
    val price = priceState.currentPrice
    val costText = if (tripFuelConsumedL != null && price > 0.0) {
        formatIdr(tripFuelConsumedL * price)
    } else "—"

    // ADR-008: active-row OBD stats, separated from the base row.
    val instantKmLText = if (connected?.instantKmL != null &&
        uiState.speedInKMH > 3f &&
        uiState.accuracyMeters <= 20f
    ) {
        String.format(Locale.ENGLISH, "%.1f", connected.instantKmL)
    } else "—"
    val lphText = connected?.fuelRateLph?.takeIf { it > 0.0 }
        ?.let { String.format(Locale.ENGLISH, "%.1f", it) }
        ?: "—"
    val tripAvgKmLText = connected?.tripAvgKmL
        ?.let { String.format(Locale.ENGLISH, "%.1f", it) }
        ?: "—"

    // ADR-008: completed-trip costs, precomputed in batch keyed by trip idx.
    val tripCosts = remember { mutableStateMapOf<Int, String>() }
    val tripPrices = remember { mutableStateMapOf<Int, Double>() }
    val tripCostColorFlags = remember { mutableStateMapOf<Int, Boolean>() }
    LaunchedEffect(trackList, priceState) {
        var toggle = false
        var prevCost: String? = null
        for (t in trackList) {
            val p = fuelPriceDao.priceEffectiveAt(t.timestamp)
            val cost = if (t.obdFuelConsumedL > 0.0 && p != null && p.pricePerLiter > 0.0) {
                formatIdr(t.obdFuelConsumedL * p.pricePerLiter)
            } else "—"
            tripCosts[t.idx] = cost
            tripPrices[t.idx] = p?.pricePerLiter ?: 0.0
            if (prevCost != null && cost != prevCost) toggle = !toggle
            tripCostColorFlags[t.idx] = toggle
            prevCost = cost
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp,
            top = 24.dp,
            end = 22.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ListHeader(onFuelClick = { showPriceDialog = true }) }
        item {
            CurrentTripCard(
                uiState = uiState,
                onCtaTap = { viewModel.onTripCtaTap() }
            )
        }
        item {
            RecentTripsHeader()
        }

        if (isTripActive) {
            item(key = "active-trip-row") {
                ActiveTripRow(
                    uiState = uiState,
                    instantKmLText = instantKmLText,
                    lphText = lphText,
                    tripAvgKmLText = tripAvgKmLText,
                    costText = costText,
                    onCostClick = { showPriceDialog = true }
                )
            }
        }

        if (trackList.isEmpty()) {
            if (!isTripActive) {
                item {
                    EmptyTripsCard()
                }
            }
        } else {
            items(
                items = trackList,
                key = { trackItem -> trackItem.idx }
            ) { item ->
                var showDialogForDeletion by remember { mutableStateOf(false) }

                TrackItemRow(
                    item = item,
                    costText = tripCosts[item.idx] ?: "—",
                    onClick = { onSelect.invoke(item.idx) },
                    onLongClick = { showDialogForDeletion = true },
                    onCostClick = {
                        showTripFuelPriceToast(context, item.idx, tripPrices[item.idx] ?: 0.0)
                    },
                    costColor = if (tripCostColorFlags[item.idx] == true) TripGreen else TripInk,
                    pricePerLiter = tripPrices[item.idx] ?: 0.0
                )

                if (showDialogForDeletion) {
                    CustomAlertDialog(
                        title = "Are you sure to delete?"
                    ) { isDelete ->
                        showDialogForDeletion = false
                        if (isDelete) viewModel.deleteTrack(item)
                    }
                }
            }
        }
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

@Composable
private fun ActiveTripRow(
    uiState: LocationUiState,
    instantKmLText: String,
    lphText: String,
    tripAvgKmLText: String,
    costText: String,
    onCostClick: () -> Unit
) {
    val now by produceState(initialValue = System.currentTimeMillis(), uiState.isTracking) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val elapsedMs = if (uiState.tripStartedAt > 0L) {
        (now - uiState.tripStartedAt).coerceAtLeast(0L)
    } else 0L
    val distanceKm = uiState.distanceInMeters / 1000f
    val distanceText = "${String.format(Locale.ENGLISH, "%.2f", distanceKm)} km"
    val durationText = formatElapsed(elapsedMs)
    val avgSpeedText = run {
        val hours = elapsedMs / 3_600_000f
        if (hours > 0f) "${String.format(Locale.ENGLISH, "%.1f", distanceKm / hours)} km/h" else "0.0 km/h"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TripSurface)
            .border(1.5.dp, TripGreen, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(modifier = Modifier.weight(1f)) {
            ActiveRouteIndicator()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = "Trip in progress",
                    color = TripInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "",
                    color = TripInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 28.dp)
                )
            }
            ActiveTripBadge()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TripBorder)
        )
        // ADR-008: base row — distance / duration / avg speed.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            ActiveTripStat(value = distanceText, label = "km", modifier = Modifier.weight(1f))
            ActiveTripStat(value = durationText, label = "duration", modifier = Modifier.weight(1f))
            ActiveTripStat(value = avgSpeedText, label = "avg speed", modifier = Modifier.weight(1f))
        }
        // ADR-008: OBD row — instant km/L / L/h / trip-average km/L / cost (tappable).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            ActiveTripStat(value = instantKmLText, label = "km/L", modifier = Modifier.weight(1f))
            ActiveTripStat(value = lphText, label = "L/h", modifier = Modifier.weight(1f))
            ActiveTripStat(value = tripAvgKmLText, label = "avg km/L", modifier = Modifier.weight(1f))
            ActiveTripStat(
                value = costText,
                label = "cost",
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCostClick() }
            )
        }
    }
}

@Composable
private fun ActiveRouteIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .border(1.dp, TripGreen, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(width = 2.dp, height = 30.dp)
                .background(TripGreen.copy(alpha = 0.55f))
        )
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(TripGreen)
        )
    }
}

@Composable
private fun ActiveTripBadge() {
    val transition = rememberInfiniteTransition()
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(TripGreen.copy(alpha = 0.12f))
            .padding(start = 8.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(TripGreen)
        )
        Text(
            text = "ACTIVE",
            color = TripGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ActiveTripStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = TripInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            color = TripMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ListHeader(onFuelClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Trip Tracker",
                color = TripMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = "Trips",
                color = TripInk,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp,
                letterSpacing = 0.sp
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_fuel_pump),
            contentDescription = "Set fuel price",
            tint = TripInk,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onFuelClick)
                .padding(6.dp)
                .size(26.dp)
        )
    }
}

private fun showTripFuelPriceToast(
    context: android.content.Context,
    trackIdx: Int,
    pricePerLiter: Double
) {
    val msg = if (pricePerLiter > 0.0) {
        "Track #$trackIdx fuel price: ${formatIdr(pricePerLiter)} / litre"
    } else {
        "No fuel price recorded for this trip"
    }
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
private fun CurrentTripCard(
    uiState: LocationUiState,
    onCtaTap: () -> Unit = {}
) {
    val isRecording = uiState.isTracking && !uiState.isPaused
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
                as? AccessibilityManager
        // isReduceMotionEnabled is not directly available; treat enabled-with-touch-exploration
        // or animator-duration-scale = 0 as reduced motion. Conservative default: false.
        am?.let {
            try {
                android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
                ) == 0f
            } catch (_: Throwable) { false }
        } ?: false
    }

    val now by produceState(initialValue = System.currentTimeMillis(), isRecording) {
        if (isRecording) {
            while (true) {
                value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }
    val elapsedText = if (isRecording && uiState.tripStartedAt > 0L) {
        formatElapsed(now - uiState.tripStartedAt)
    } else {
        "00:00:00"
    }

    val outerColor by animateColorAsState(
        targetValue = if (isRecording) TripHeroIndicatorWash else TripHeroDim,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    )
    val dotColor by animateColorAsState(
        targetValue = if (isRecording) TripHeroBrandGreen else TripHeroDotIdle,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    )
    val buttonBg by animateColorAsState(
        targetValue = if (isRecording) TripHeroStopRed else TripHeroBrandGreen,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
    )

    val blinkAlpha: Float = if (isRecording && !reduceMotion) {
        val transition = rememberInfiniteTransition()
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
        animated
    } else {
        1f
    }

    val titleText = if (isRecording) "Recording" else "Ready"
    val liveAnnouncement = if (isRecording) "Trip started" else "Trip stopped"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(TripHeroBg)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = liveAnnouncement
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRecording && !reduceMotion) {
                PulseRing(delayMs = 0)
                PulseRing(delayMs = 600)
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(outerColor),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    // Halo
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(TripHeroDotHalo)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .alpha(blinkAlpha)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Current trip",
                color = TripHeroEyebrow,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = titleText,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                lineHeight = 22.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = elapsedText,
                color = if (isRecording) TripHeroReadoutLive else TripHeroReadoutIdle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MonospaceFontFamily,
                letterSpacing = 0.sp,
                lineHeight = 13.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { contentDescription = "Elapsed time, $elapsedText" }
            )
        }

        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(buttonBg)
                .clickable(onClick = onCtaTap)
                .padding(horizontal = 22.dp)
                .semantics {
                    contentDescription = if (isRecording) "Stop trip" else "Start trip"
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = Color.White,
                            shape = if (isRecording) RoundedCornerShape(1.5.dp) else CircleShape
                        )
                )
                Text(
                    text = if (isRecording) "Stop" else "Start",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PulseRing(delayMs: Int) {
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
            .size(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = ringAlpha
            }
            .border(2.dp, TripHeroBrandGreen, CircleShape)
    )
}

private fun formatElapsed(elapsedMs: Long): String {
    val total = (elapsedMs.coerceAtLeast(0L)) / 1000L
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val s = total % 60L
    return String.format(Locale.ENGLISH, "%02d:%02d:%02d", h, m, s)
}

@Composable
private fun RecentTripsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent trips",
            color = TripInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
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

@Composable
private fun EmptyTripsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TripSurface)
            .border(1.dp, TripBorder, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.no_item_description),
            color = TripMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

