package com.kolee.tracklocation.feature.observer.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kolee.tracklocation.feature.observer.presentation.components.AllowlistBottomSheet
import com.kolee.tracklocation.feature.observer.presentation.components.AutoScrollReadout
import com.kolee.tracklocation.feature.observer.presentation.components.CapturePausedBanner
import com.kolee.tracklocation.feature.observer.presentation.components.CaptureChip
import com.kolee.tracklocation.feature.observer.presentation.components.EventRow
import com.kolee.tracklocation.feature.observer.presentation.components.FeedHeaderBar
import com.kolee.tracklocation.feature.observer.presentation.components.JumpToLatestFab
import com.kolee.tracklocation.feature.observer.presentation.components.ObserverEmptyState
import com.kolee.tracklocation.feature.observer.presentation.components.ServiceBanner
import com.kolee.tracklocation.feature.observer.presentation.viewmodel.ObserverViewModel
import com.kolee.tracklocation.ui.theme.ObserverAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ObserverFeedScreen(navController: NavController) {
    val viewModel: ObserverViewModel = viewModel(factory = ObserverViewModel.Factory)
    val uiState by viewModel.uiState.collectAsState()
    val allowlistState by viewModel.allowlistUiState.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAllowlist by remember { mutableStateOf(false) }

    // In-memory auto-scroll state — resets to true on screen entry per spec
    var autoScrollRunning by remember { mutableStateOf(true) }
    var showFab by remember { mutableStateOf(false) }
    var programmaticScroll by remember { mutableStateOf(false) }

    // Drag/scroll → pause auto-scroll (ignore programmatic scrolls)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling && autoScrollRunning && !programmaticScroll) {
                    autoScrollRunning = false
                }
            }
    }

    // New events while auto-scroll is running → scroll to newest
    val prevEventCount = remember { mutableStateOf(0) }
    LaunchedEffect(uiState.events.size) {
        val hasNew = uiState.events.size > prevEventCount.value
        prevEventCount.value = uiState.events.size
        if (hasNew && autoScrollRunning && uiState.events.isNotEmpty()) {
            programmaticScroll = true
            listState.animateScrollToItem(uiState.events.lastIndex)
            programmaticScroll = false
        }
    }

    // Snackbar messages from ViewModel
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val tapListToggle = {
        val wasPaused = !autoScrollRunning
        autoScrollRunning = !autoScrollRunning
        if (wasPaused) {
            // Paused → Running: show transient FAB for ~2 s
            coroutineScope.launch {
                showFab = true
                delay(2_000)
                showFab = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F4F0))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Custom top app bar (avoids Material3 TopAppBar API version concerns)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F4F0))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button — 48dp tap target
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
                    text = "Observer",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    color = Color(0xFF0A0A0A),
                    modifier = Modifier.weight(1f),
                )
                // Allowlist icon with optional draft dot
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { showAllowlist = true }
                        .semantics {
                            contentDescription = if (uiState.allowlistDraftPending)
                                "Allowlist, draft changes not applied"
                            else "Allowlist"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        tint = Color(0xFF0A0A0A),
                        modifier = Modifier.size(24.dp),
                    )
                    if (uiState.allowlistDraftPending) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .padding(end = 6.dp, top = 6.dp)
                                .background(ObserverAmber, CircleShape)
                                .border(1.5.dp, Color(0xFFF1F4F0), CircleShape)
                        )
                    }
                }
            }

            // Service status banner
            ServiceBanner(enabled = uiState.serviceEnabled)

            Spacer(modifier = Modifier.height(8.dp))

            // Capture chip + auto-scroll readout (50/50 row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CaptureChip(
                    running = uiState.captureRunning,
                    onToggle = viewModel::toggleCapture,
                    modifier = Modifier.weight(1f),
                )
                AutoScrollReadout(
                    running = autoScrollRunning,
                    modifier = Modifier.weight(1f),
                )
            }

            // Capture-paused sub-banner
            if (!uiState.captureRunning) {
                CapturePausedBanner()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Event list with sticky feed header
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { tapListToggle() })
                    },
            ) {
                stickyHeader {
                    FeedHeaderBar(
                        count = uiState.totalEventCount,
                        scope = uiState.scope,
                        captureRunning = uiState.captureRunning,
                    )
                }

                if (uiState.events.isEmpty() && uiState.captureRunning) {
                    item {
                        ObserverEmptyState()
                    }
                } else {
                    itemsIndexed(uiState.events) { index, event ->
                        EventRow(
                            event = event,
                            isEven = index % 2 == 0,
                            autoScrollPaused = !autoScrollRunning,
                            onLongPress = { viewModel.copyToClipboard(event) },
                            onTap = tapListToggle,
                        )
                    }
                }
            }
        }

        // Jump-to-latest transient FAB
        JumpToLatestFab(
            visible = showFab,
            onClick = {
                coroutineScope.launch {
                    if (uiState.events.isNotEmpty()) {
                        programmaticScroll = true
                        listState.animateScrollToItem(uiState.events.lastIndex)
                        programmaticScroll = false
                    }
                }
                showFab = false
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp),
        )

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        )
    }

    // Allowlist bottom sheet
    if (showAllowlist) {
        AllowlistBottomSheet(
            state = allowlistState,
            onAddRule = viewModel::addDraftRule,
            onUpdateRule = viewModel::updateDraftRule,
            onDeleteRule = viewModel::deleteDraftRule,
            onApply = viewModel::applyAllowlist,
            onClose = { showAllowlist = false },
        )
    }
}
