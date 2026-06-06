package com.kolee.tracklocation.screens.settings.obd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.feature.obd.service.ObdUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObdSettingsScreen(navController: NavController) {
    val obdUiState = ObdPollingService.obdUiState.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("OBD Settings", modifier = Modifier.padding(bottom = 16.dp))

        // Status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status: ${getStatusLabel(obdUiState)}")
            }
        }

        // Enable toggle (disabled in Slice 1)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Enable OBD Polling")
                Switch(
                    checked = false,
                    onCheckedChange = { },
                    enabled = false
                )
            }
        }
    }
}

private fun getStatusLabel(state: ObdUiState): String = when (state) {
    ObdUiState.Idle -> "Idle"
    ObdUiState.Connecting -> "Connecting…"
    is ObdUiState.Connected -> "Connected"
    is ObdUiState.Retrying -> "Retrying (${state.attemptSeconds}/${state.maxSeconds}s)"
    is ObdUiState.Waiting -> "Waiting: ${state.lastError}"
}
