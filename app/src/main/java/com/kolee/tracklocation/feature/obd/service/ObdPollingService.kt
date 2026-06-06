package com.kolee.tracklocation.feature.obd.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow

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

    companion object {
        val obdUiState = MutableStateFlow<ObdUiState>(ObdUiState.Idle)

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
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_SESSION_ON -> {
                // Slice 3: gate OBD writes to session active
            }
            ACTION_SESSION_OFF -> {
                // Slice 3: stop writing OBD samples
            }
            ACTION_RECONNECT_NOW -> {
                // Slice 2: trigger immediate reconnect attempt
            }
        }
        return START_STICKY
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
        serviceScope.coroutineContext.cancelChildren()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
