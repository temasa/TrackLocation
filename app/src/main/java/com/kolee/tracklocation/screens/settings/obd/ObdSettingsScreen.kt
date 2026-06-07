package com.kolee.tracklocation.screens.settings.obd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.kolee.tracklocation.R
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.feature.obd.service.ObdUiState

@Composable
fun ObdSettingsScreen(navController: NavController) {
    val obdUiState = ObdPollingService.obdUiState.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F4F0))
    ) {
        // Custom top bar — avoids Material3 TopAppBar API version concerns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F4F0))
                .padding(horizontal = 4.dp, vertical = 4.dp),
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
                text = "OBD Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = Color(0xFF0A0A0A),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        ) {
            item { ObdSectionHeader("STATUS") }
            item {
                ObdSectionGroup {
                    ObdStatusRow(obdUiState)
                }
            }
        }
    }
}

@Composable
private fun ObdSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF737373),
        letterSpacing = 1.4.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun ObdSectionGroup(content: @Composable () -> Unit) {
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
private fun ObdStatusRow(state: ObdUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                painter = painterResource(id = R.drawable.baseline_settings_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF0A0A0A),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = getStatusLabel(state),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0A0A0A),
                letterSpacing = (-0.1).sp,
            )
            Text(
                text = getStatusSubtitle(state),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF737373),
                lineHeight = 17.sp,
            )
        }

        Switch(
            checked = false,
            onCheckedChange = {},
            enabled = false,
        )
    }
}

private fun getStatusLabel(state: ObdUiState): String = when (state) {
    ObdUiState.Idle -> "Idle"
    ObdUiState.Connecting -> "Connecting…"
    is ObdUiState.Connected -> "Connected"
    is ObdUiState.Retrying -> "Retrying"
    is ObdUiState.Waiting -> "Waiting"
}

private fun getStatusSubtitle(state: ObdUiState): String = when (state) {
    ObdUiState.Idle -> "Service not enabled"
    ObdUiState.Connecting -> "Looking for adapter…"
    is ObdUiState.Connected -> "Adapter connected"
    is ObdUiState.Retrying -> "Attempt ${state.attemptSeconds}/${state.maxSeconds}s"
    is ObdUiState.Waiting -> state.lastError.ifBlank { "Waiting to reconnect" }
}
