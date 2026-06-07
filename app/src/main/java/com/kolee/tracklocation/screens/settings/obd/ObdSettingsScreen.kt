package com.kolee.tracklocation.screens.settings.obd

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.kolee.tracklocation.R
import com.kolee.tracklocation.TrackApp
import com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.feature.obd.service.ObdUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ObdSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val obdUiState = ObdPollingService.obdUiState.collectAsState().value
    val scope = rememberCoroutineScope()
    val prefs = (context.applicationContext as TrackApp).run { ObdPreferencesDataStore(context) }

    val bluetoothPermission = rememberPermissionState(android.Manifest.permission.BLUETOOTH_CONNECT)
    val showDevicePicker = remember { mutableStateOf(false) }
    val bondedDevices = remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val refreshDevices = { bondedDevices.value = getBluetoothBondedDevices() }

    val serviceEnabled = prefs.obdServiceEnabled.collectAsState(initial = false).value
    val deviceMac = prefs.obdDeviceMac.collectAsState(initial = "").value
    val pollHz = prefs.obdPollHz.collectAsState(initial = 2).value
    val retentionDays = prefs.obdRetentionDays.collectAsState(initial = 7).value
    val retryMaxSeconds = prefs.obdRetryMaxSeconds.collectAsState(initial = 120).value
    val engineDisplacementCc = prefs.obdEngineDisplacementCc.collectAsState(initial = 1193).value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F4F0))
    ) {
        // Custom top bar
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
                    ObdStatusRow(
                        state = obdUiState,
                        isEnabled = serviceEnabled,
                        onToggle = { enabled ->
                            scope.launch {
                                if (enabled && android.os.Build.VERSION.SDK_INT >= 31) {
                                    try {
                                        val permission = android.Manifest.permission.BLUETOOTH_CONNECT
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context, permission
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        if (!hasPermission) {
                                            bluetoothPermission.launchPermissionRequest()
                                        } else {
                                            prefs.setObdServiceEnabled(true)
                                            context.startForegroundService(
                                                Intent(context, ObdPollingService::class.java).apply {
                                                    action = ObdPollingService.ACTION_START
                                                }
                                            )
                                        }
                                    } catch (e: Exception) {
                                        prefs.setObdServiceEnabled(true)
                                        context.startForegroundService(
                                            Intent(context, ObdPollingService::class.java).apply {
                                                action = ObdPollingService.ACTION_START
                                            }
                                        )
                                    }
                                } else if (enabled) {
                                    prefs.setObdServiceEnabled(true)
                                    context.startForegroundService(
                                        Intent(context, ObdPollingService::class.java).apply {
                                            action = ObdPollingService.ACTION_START
                                        }
                                    )
                                } else {
                                    prefs.setObdServiceEnabled(false)
                                    context.startService(
                                        Intent(context, ObdPollingService::class.java).apply {
                                            action = ObdPollingService.ACTION_STOP
                                        }
                                    )
                                }
                            }
                        },
                        onReconnect = {
                            context.startService(
                                Intent(context, ObdPollingService::class.java).apply {
                                    action = ObdPollingService.ACTION_RECONNECT_NOW
                                }
                            )
                        }
                    )
                }
            }

            if (serviceEnabled) {
                item { ObdSectionHeader("DEVICE") }
                item {
                    ObdSectionGroup {
                        if (deviceMac.isNotBlank()) {
                            val deviceName = bondedDevices.value.find { it.second == deviceMac }?.first ?: deviceMac
                            ObdDeviceRow(
                                name = deviceName,
                                mac = deviceMac,
                                onChangeClick = { showDevicePicker.value = true }
                            )
                        }
                        ObdPairRow {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        }
                    }
                }

                item { ObdSectionHeader("PREFERENCES") }
                item {
                    ObdSectionGroup {
                        ObdPollRateRow(
                            current = pollHz,
                            onChange = { scope.launch { prefs.setObdPollHz(it) } }
                        )
                        ObdRetentionRow(
                            current = retentionDays,
                            onChange = { scope.launch { prefs.setObdRetentionDays(it) } }
                        )
                        ObdRetryCapRow(
                            current = retryMaxSeconds,
                            onChange = { scope.launch { prefs.setObdRetryMaxSeconds(it) } }
                        )
                        ObdDisplacementRow(
                            current = engineDisplacementCc,
                            onChange = { scope.launch { prefs.setObdEngineDisplacementCc(it) } }
                        )
                    }
                }
            }

            if (serviceEnabled && android.os.Build.VERSION.SDK_INT >= 31) {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.BLUETOOTH_CONNECT
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    item {
                        Text(
                            text = "Bluetooth access is required to connect to an OBD adapter.",
                            fontSize = 13.sp,
                            color = Color(0xFFE5484D),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDevicePicker.value) {
        refreshDevices()
        DevicePickerDialog(
            devices = bondedDevices.value,
            onDeviceSelected = { mac ->
                scope.launch {
                    prefs.setObdDeviceMac(mac)
                    showDevicePicker.value = false
                }
            },
            onDismiss = { showDevicePicker.value = false }
        )
    }

    if (serviceEnabled && deviceMac.isBlank()) {
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= 31) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (hasPermission) {
            showDevicePicker.value = true
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
private fun ObdStatusRow(
    state: ObdUiState,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onReconnect: () -> Unit,
) {
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
            checked = isEnabled,
            onCheckedChange = onToggle,
            enabled = true,
        )
    }

    if (state is ObdUiState.Waiting) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onReconnect)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Reconnect now",
                fontSize = 14.sp,
                color = Color(0xFF0E8DFF),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ObdDeviceRow(
    name: String,
    mac: String,
    onChangeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0A0A0A),
                letterSpacing = (-0.1).sp,
            )
            Text(
                text = mac,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF737373),
            )
        }

        Text(
            text = "Change",
            fontSize = 14.sp,
            color = Color(0xFF0E8DFF),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onChangeClick)
        )
    }
}

@Composable
private fun ObdPairRow(onPairClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPairClick)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Pair a new device",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0E8DFF),
            letterSpacing = (-0.1).sp,
        )
    }
}

@Composable
private fun ObdPollRateRow(current: Int, onChange: (Int) -> Unit) {
    ObdPreferenceRow(
        label = "Poll Rate",
        current = "$current Hz",
        options = listOf(1, 2, 5),
        onSelect = { onChange(it) }
    )
}

@Composable
private fun ObdRetentionRow(current: Int, onChange: (Int) -> Unit) {
    ObdPreferenceRow(
        label = "Retention",
        current = "$current days",
        options = listOf(1, 7, 14, 30),
        onSelect = { onChange(it) }
    )
}

@Composable
private fun ObdRetryCapRow(current: Int, onChange: (Int) -> Unit) {
    ObdPreferenceRow(
        label = "Retry Cap",
        current = "${current}s",
        options = listOf(30, 60, 120, 300),
        onSelect = { onChange(it) }
    )
}

@Composable
private fun ObdDisplacementRow(current: Int, onChange: (Int) -> Unit) {
    // Used for the indirect (speed-density) fuel estimate when the vehicle exposes no MAF/fuel-rate PID.
    ObdPreferenceRow(
        label = "Engine Displacement",
        current = "$current cc",
        options = listOf(1000, 1193, 1200, 1500, 1600, 1800, 2000, 2400, 3000),
        onSelect = { onChange(it) }
    )
}

@Composable
private fun ObdPreferenceRow(
    label: String,
    current: String,
    options: List<Int>,
    onSelect: (Int) -> Unit,
) {
    val showOptions = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showOptions.value = true }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0A0A0A),
                letterSpacing = (-0.1).sp,
            )
            Text(
                text = current,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF737373),
            )
        }
    }

    if (showOptions.value) {
        AlertDialog(
            onDismissRequest = { showOptions.value = false },
            title = { Text(label) },
            text = {
                Column {
                    options.forEach { opt ->
                        Text(
                            text = opt.toString(),
                            modifier = Modifier
                                .clickable {
                                    onSelect(opt)
                                    showOptions.value = false
                                }
                                .padding(12.dp)
                                .fillMaxWidth(),
                            fontSize = 14.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptions.value = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DevicePickerDialog(
    devices: List<Pair<String, String>>,
    onDeviceSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Bonded Device") },
        text = {
            Column {
                if (devices.isEmpty()) {
                    Text("No bonded devices found. Pair a device in Bluetooth settings.")
                } else {
                    devices.forEach { (name, mac) ->
                        Text(
                            text = "$name\n$mac",
                            modifier = Modifier
                                .clickable { onDeviceSelected(mac) }
                                .padding(12.dp)
                                .fillMaxWidth(),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getBluetoothBondedDevices(): List<Pair<String, String>> {
    return try {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.bondedDevices?.map { device ->
            device.name to device.address
        } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
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
