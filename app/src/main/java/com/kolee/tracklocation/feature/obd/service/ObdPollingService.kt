package com.kolee.tracklocation.feature.obd.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kolee.tracklocation.data.roomdb.ObdSampleEntity
import com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    private var bluetoothSocket: BluetoothSocket? = null
    private var sessionActive = false
    private var pollingJob: kotlinx.coroutines.Job? = null

    private var sessionDistanceKm = 0.0
    private var sessionFuelLiters = 0.0
    private var emaKmL: Double? = null
    private var lastPollTimeMs = 0L

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
                sessionActive = true
                sessionDistanceKm = 0.0
                sessionFuelLiters = 0.0
                emaKmL = null
                updateConnectedState()
            }
            ACTION_SESSION_OFF -> {
                sessionActive = false
                updateConnectedState()
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
            val deviceMac = prefs.obdDeviceMac.first()
            val retryMaxSeconds = prefs.obdRetryMaxSeconds.first()
            maxRetryDelaySeconds = retryMaxSeconds

            if (deviceMac.isBlank()) {
                obdUiState.value = ObdUiState.Idle
                return@launch
            }

            obdUiState.value = ObdUiState.Connecting

            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                val device = adapter.getRemoteDevice(deviceMac)
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                bluetoothSocket = socket
                obdUiState.value = ObdUiState.Connected(sessionActive = sessionActive)
                retryDelaySeconds = 1
                prefs.setObdLastState("Connected")
                startPollingLoop()
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

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            val prefs = ObdPreferencesDataStore(this@ObdPollingService)
            val pollHz = prefs.obdPollHz.first()
            val retentionDays = prefs.obdRetentionDays.first()

            lastPollTimeMs = System.currentTimeMillis()

            while (bluetoothSocket != null && (obdUiState.value is ObdUiState.Connected || obdUiState.value is ObdUiState.Retrying)) {
                try {
                    val socket = bluetoothSocket ?: break
                    val now = System.currentTimeMillis()
                    val dtSeconds = (now - lastPollTimeMs) / 1000.0
                    lastPollTimeMs = now

                    val speedResponse = writeATCommand(socket, "010D")
                    val rpmResponse = writeATCommand(socket, "010C")
                    val fuelResponse = try {
                        writeATCommand(socket, "015E")
                    } catch (e: Exception) {
                        try {
                            writeATCommand(socket, "0110")
                        } catch (e: Exception) {
                            ""
                        }
                    }

                    val obdSpeedKmh = parseObdSpeed(speedResponse)
                    val rpm = parseObdRpm(rpmResponse)
                    val (fuelRateLph, fuelSource) = parseObdFuel(fuelResponse)

                    val gpsState = com.kolee.tracklocation.tracking.TrackingService.locationUiState.value
                    val gpsSpeedKmh = gpsState.speedInKMH
                    val gpsAccuracyM = gpsState.accuracyMeters

                    var instantKmL: Double? = null
                    if (fuelRateLph != null && fuelRateLph > 0.1 && gpsSpeedKmh > 3f && gpsAccuracyM <= 20f) {
                        val rawKmL = gpsSpeedKmh / fuelRateLph.toFloat()
                        emaKmL = if (emaKmL == null) {
                            rawKmL.toDouble()
                        } else {
                            0.2 * rawKmL + 0.8 * emaKmL!!
                        }
                        instantKmL = emaKmL
                    } else {
                        emaKmL = null
                    }

                    if (sessionActive) {
                        if (fuelRateLph != null && dtSeconds > 0) {
                            sessionFuelLiters += fuelRateLph * dtSeconds / 3600.0
                        }
                        sessionDistanceKm += gpsSpeedKmh * dtSeconds / 3600.0
                    }

                    val avgKmL = if (sessionFuelLiters > 0.01 && sessionDistanceKm > 0.01) {
                        sessionDistanceKm / sessionFuelLiters
                    } else {
                        null
                    }

                    obdUiState.value = ObdUiState.Connected(
                        rpm = rpm,
                        obdSpeedKmh = obdSpeedKmh,
                        fuelRateLph = fuelRateLph,
                        fuelSource = fuelSource,
                        instantKmL = instantKmL,
                        avgKmL = avgKmL,
                        sessionActive = sessionActive
                    )

                    if (sessionActive) {
                        val sample = ObdSampleEntity(
                            timestampMs = now,
                            rpm = rpm,
                            obdSpeedKmh = obdSpeedKmh,
                            fuelRateLph = fuelRateLph,
                            mafGramsPerSecond = null,
                            fuelRateSource = fuelSource,
                            adapterElapsedMs = (now - lastPollTimeMs).coerceAtLeast(0L)
                        )
                        val obdDao = (applicationContext as com.kolee.tracklocation.TrackApp).obdSampleDao
                        obdDao.insert(sample)
                    }

                    if (sessionActive && now % 21600000L < 500L) {
                        val cutoffTime = now - (retentionDays * 86400000L)
                        val obdDao = (applicationContext as com.kolee.tracklocation.TrackApp).obdSampleDao
                        obdDao.deleteOlderThan(cutoffTime)
                    }

                    delay((1000L / pollHz).coerceAtLeast(100L))
                } catch (e: Exception) {
                    bluetoothSocket = null
                    obdUiState.value = ObdUiState.Waiting(e.message ?: "Poll error")
                    startRetryBackoff()
                    break
                }
            }
        }
    }

    private fun writeATCommand(socket: BluetoothSocket, command: String): String {
        val output = socket.outputStream
        val input = socket.inputStream
        val cmd = command.uppercase() + "\r"
        output.write(cmd.toByteArray())
        output.flush()

        val response = StringBuilder()
        val buffer = ByteArray(1024)
        var timeout = 0
        while (timeout < 100) {
            try {
                val bytesRead = input.read(buffer)
                if (bytesRead > 0) {
                    val str = String(buffer, 0, bytesRead)
                    response.append(str)
                    if (str.contains(">")) break
                }
            } catch (e: Exception) {
                break
            }
            Thread.sleep(10)
            timeout++
        }
        return response.toString()
    }

    private fun parseObdSpeed(response: String): Int? {
        if (!response.contains("41 0D")) return null
        val parts = response.split(" ")
        val idx = parts.indexOfFirst { it.equals("0D", ignoreCase = true) }
        if (idx >= 0 && idx + 1 < parts.size) {
            return try {
                parts[idx + 1].toInt(16)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun parseObdRpm(response: String): Int? {
        if (!response.contains("41 0C")) return null
        val parts = response.split(" ")
        val idx = parts.indexOfFirst { it.equals("0C", ignoreCase = true) }
        if (idx >= 0 && idx + 2 < parts.size) {
            return try {
                val a = parts[idx + 1].toInt(16)
                val b = parts[idx + 2].toInt(16)
                (256 * a + b) / 4
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun parseObdFuel(response: String): Pair<Double?, String> {
        if (response.contains("41 5E")) {
            try {
                val parts = response.split(" ")
                val idx = parts.indexOfFirst { it.equals("5E", ignoreCase = true) }
                if (idx >= 0 && idx + 1 < parts.size) {
                    val raw = parts[idx + 1].toInt(16)
                    return Pair(raw * 0.05, "DIRECT_FUEL_RATE")
                }
            } catch (e: Exception) {}
        }
        if (response.contains("41 10")) {
            try {
                val parts = response.split(" ")
                val idx = parts.indexOfFirst { it.equals("10", ignoreCase = true) }
                if (idx >= 0 && idx + 2 < parts.size) {
                    val a = parts[idx + 1].toInt(16)
                    val b = parts[idx + 2].toInt(16)
                    val maf = (a * 256 + b) / 100.0
                    val fuelRate = maf * 3600.0 / (14.7 * 750.0)
                    return Pair(fuelRate, "MAF_DERIVED")
                }
            } catch (e: Exception) {}
        }
        return Pair(null, "UNAVAILABLE")
    }

    private fun updateConnectedState() {
        val current = obdUiState.value
        if (current is ObdUiState.Connected) {
            obdUiState.value = current.copy(sessionActive = sessionActive)
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
            pollingJob?.cancel()
            try {
                bluetoothSocket?.close()
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
