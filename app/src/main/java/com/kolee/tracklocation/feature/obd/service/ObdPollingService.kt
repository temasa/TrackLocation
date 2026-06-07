package com.kolee.tracklocation.feature.obd.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import androidx.core.app.NotificationCompat
import com.kolee.tracklocation.TrackApp
import com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

sealed class ObdUiState {
    object Idle : ObdUiState()
    object Connecting : ObdUiState()
    data class Connected(
        val rpm: Int? = null,
        val obdSpeedKmh: Int? = null,
        val fuelRateLph: Double? = null,
        val fuelSource: String = "UNAVAILABLE",
        val instantKmL: Double? = null,
        val avgKmL: Double? = null,
        val sessionActive: Boolean = false
    ) : ObdUiState()
    data class Retrying(val attemptSeconds: Int = 0, val maxSeconds: Int = 120) : ObdUiState()
    data class Waiting(val lastError: String = "") : ObdUiState()
}

class ObdPollingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var retryDelaySeconds = 1
    private var maxRetryDelaySeconds = 120
    private var bluetoothSocket: Any? = null

    companion object {
        val obdUiState = MutableStateFlow<ObdUiState>(ObdUiState.Idle)
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        const val ACTION_START = "com.kolee.tracklocation.obd.ACTION_START"
        const val ACTION_STOP = "com.kolee.tracklocation.obd.ACTION_STOP"
        const val ACTION_SESSION_ON = "com.kolee.tracklocation.obd.ACTION_SESSION_ON"
        const val ACTION_SESSION_OFF = "com.kolee.tracklocation.obd.ACTION_SESSION_OFF"
        const val ACTION_RECONNECT_NOW = "com.kolee.tracklocation.obd.ACTION_RECONNECT_NOW"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                startForegroundNotification()
                attemptConnection()
            }
            ACTION_STOP -> {
                closeConnection()
                stopSelf()
            }
            ACTION_SESSION_ON -> {
                // Slice 3: gate OBD writes to session active
            }
            ACTION_SESSION_OFF -> {
                // Slice 3: stop writing OBD samples
            }
            ACTION_RECONNECT_NOW -> {
                retryDelaySeconds = 1
                attemptConnection()
            }
        }
        return START_STICKY
    }

    private fun attemptConnection() {
        serviceScope.launch {
            val prefs = ObdPreferencesDataStore(this@ObdPollingService)
            var deviceMac = ""
            var retryMaxSeconds = 120

            prefs.obdDeviceMac.collect { mac ->
                deviceMac = mac
            }
            prefs.obdRetryMaxSeconds.collect { maxSecs ->
                retryMaxSeconds = maxSecs
            }

            maxRetryDelaySeconds = retryMaxSeconds

            if (deviceMac.isBlank()) {
                obdUiState.value = ObdUiState.Idle
                return@launch
            }

            obdUiState.value = ObdUiState.Connecting

            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                val device = adapter.getRemoteDevice(deviceMac as String)
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                bluetoothSocket = socket
                obdUiState.value = ObdUiState.Connected()
                retryDelaySeconds = 1
                prefs.setObdLastState("Connected")
            } catch (e: Exception) {
                bluetoothSocket = null
                val errorMsg = e.message ?: "Connection failed"
                obdUiState.value = ObdUiState.Waiting(errorMsg)
                prefs.setObdLastError(errorMsg)
                prefs.setObdLastState("Waiting")
                startRetryBackoff()
            }
        }
    }

    private fun startRetryBackoff() {
        serviceScope.launch {
            while (retryDelaySeconds <= maxRetryDelaySeconds && obdUiState.value is ObdUiState.Waiting) {
                obdUiState.value = ObdUiState.Retrying(retryDelaySeconds, maxRetryDelaySeconds)
                delay(retryDelaySeconds * 1000L)

                if (obdUiState.value !is ObdUiState.Retrying) break

                retryDelaySeconds = (retryDelaySeconds * 2).coerceAtMost(maxRetryDelaySeconds)

                if (retryDelaySeconds > maxRetryDelaySeconds) {
                    obdUiState.value = ObdUiState.Waiting("Max retries reached")
                    break
                }

                attemptConnection()
            }
        }
    }

    private fun closeConnection() {
        serviceScope.launch {
            try {
                bluetoothSocket?.let {
                    (it as? android.bluetooth.BluetoothSocket)?.close()
                }
                bluetoothSocket = null
            } catch (e: IOException) {
                // Ignore
            }
            obdUiState.value = ObdUiState.Idle
            val prefs = ObdPreferencesDataStore(this@ObdPollingService)
            prefs.setObdLastState("Idle")
        }
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, "OBD_POLLING")
            .setContentTitle("OBD Polling")
            .setContentText("Idle")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(2001, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        closeConnection()
        serviceScope.coroutineContext.cancelChildren()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
