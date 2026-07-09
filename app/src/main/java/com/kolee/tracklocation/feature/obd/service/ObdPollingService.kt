package com.kolee.tracklocation.feature.obd.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kolee.tracklocation.data.roomdb.ObdSampleEntity
import com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
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
        val tripAvgKmL: Double? = null,
        // Fuel Cost (FR-12): litres consumed this session / trip, for cost = litres × price.
        // Null when zero (cost shows "—").
        val sessionFuelConsumedL: Double? = null,
        val tripFuelConsumedL: Double? = null
    ) : ObdUiState()
    data class Retrying(val attemptSeconds: Int = 0, val maxSeconds: Int = 120) : ObdUiState()
    data class Waiting(val lastError: String = "") : ObdUiState()
}

class ObdPollingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var retryDelaySeconds = 1
    private var maxRetryDelaySeconds = 120
    private var bluetoothSocket: BluetoothSocket? = null
    // Single coroutine owning the whole connect → init → poll → backoff lifecycle.
    // Guarantees only one connection attempt exists at a time (no socket-stomping races).
    private var connectionJob: kotlinx.coroutines.Job? = null

    private var sessionDistanceKm = 0.0
    private var sessionFuelLiters = 0.0
    private var emaKmL: Double? = null
    // ADR-007 Slice 2: in-memory O(1) trip fuel accumulator (replaces the 2s obd_sample re-query).
    private var tripFuelLiters = 0.0
    private var tripAccumStartedAt = 0L
    private var lastPollTimeMs = 0L
    // After this many consecutive cycles with no fuel PID answer, stop probing fuel so the
    // wasted round-trips don't slow the RPM/speed refresh on vehicles lacking 015E/0110.
    private var fuelProbeFailStreak = 0
    private var fuelKnownUnsupported = false // direct (015E) + MAF (0110) both unsupported
    private var capabilitiesScanned = false
    // Speed-density estimate state: IAT and commanded-lambda change slowly, so cache them and
    // refresh only every few cycles; MAP and RPM are read every cycle.
    private var sdCycle = 0
    private var cachedIatK = 293.15
    private var cachedLambda = 1.0

    companion object {
        private const val TAG = "ObdPollingService"
        val obdUiState = MutableStateFlow<ObdUiState>(ObdUiState.Idle)
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        const val ACTION_START = "com.kolee.tracklocation.obd.ACTION_START"
        const val ACTION_STOP = "com.kolee.tracklocation.obd.ACTION_STOP"
        const val ACTION_SESSION_ON = "com.kolee.tracklocation.obd.ACTION_SESSION_ON"
        const val ACTION_SESSION_OFF = "com.kolee.tracklocation.obd.ACTION_SESSION_OFF"
        const val ACTION_RECONNECT_NOW = "com.kolee.tracklocation.obd.ACTION_RECONNECT_NOW"
        // String id of the recording_session that ACTION_SESSION_ON refers to.
        const val EXTRA_SESSION_ID = "com.kolee.tracklocation.obd.EXTRA_SESSION_ID"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                startForegroundNotification()
                // Don't tear down a live/in-progress connection on a redundant start
                // (START_STICKY redelivery, repeated launches). Only (re)connect when idle.
                if (connectionJob?.isActive != true) {
                    attemptConnection()
                }
            }
            ACTION_STOP -> {
                closeConnection()
                stopSelf()
            }
            ACTION_SESSION_ON -> {
                // ADR-012: intents are advisory only (accumulation now gates on
                // TrackingService.locationUiState.isAlwaysRecording, not this intent). Still ensure
                // we run as a proper foreground service so the poll loop survives backgrounding,
                // even if this is the first command the service received (no prior ACTION_START),
                // and reset the per-session accumulators for the new session.
                startForegroundNotification()
                sessionDistanceKm = 0.0
                sessionFuelLiters = 0.0
                emaKmL = null
            }
            ACTION_SESSION_OFF -> {
                // ADR-012: no-op — accumulation gating no longer depends on this intent.
            }
            ACTION_RECONNECT_NOW -> {
                retryDelaySeconds = 1
                attemptConnection()
            }
        }
        return START_STICKY
    }

    /**
     * (Re)start the single connection lifecycle. Cancels any in-flight attempt and its
     * socket first, so there is never more than one connect/poll loop running — concurrent
     * attempts to the same RFCOMM device stomp on each other ("Broken pipe").
     */
    private fun attemptConnection() {
        connectionJob?.cancel()
        closeSocketQuietly()
        retryDelaySeconds = 1
        connectionJob = serviceScope.launch {
            val prefs = ObdPreferencesDataStore(this@ObdPollingService)
            val deviceMac = prefs.obdDeviceMac.first()
            maxRetryDelaySeconds = prefs.obdRetryMaxSeconds.first()

            if (deviceMac.isBlank()) {
                obdUiState.value = ObdUiState.Idle
                return@launch
            }

            while (isActive) {
                obdUiState.value = ObdUiState.Connecting
                Log.d(TAG, "attemptConnection: connecting to $deviceMac")
                try {
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                    if (adapter == null) {
                        // No Bluetooth hardware/stack — retrying can never succeed.
                        Log.w(TAG, "attemptConnection: no Bluetooth adapter available")
                        obdUiState.value = ObdUiState.Waiting("Bluetooth not available")
                        prefs.setObdLastError("Bluetooth not available")
                        prefs.setObdLastState("Waiting")
                        return@launch
                    }
                    // Discovery actively running slows/breaks RFCOMM connects.
                    if (adapter.isDiscovering) adapter.cancelDiscovery()
                    val device = adapter.getRemoteDevice(deviceMac)
                    val socket = connectRfcomm(device)
                    bluetoothSocket = socket
                    Log.d(TAG, "attemptConnection: RFCOMM connected, initializing ELM327")
                    initElm327(socket)
                    if (!capabilitiesScanned) {
                        scanCapabilities(socket)
                        capabilitiesScanned = true
                    }
                    obdUiState.value = ObdUiState.Connected()
                    retryDelaySeconds = 1
                    prefs.setObdLastState("Connected")
                    prefs.setObdLastError("")
                    runPollingLoop(socket, prefs) // suspends until the socket drops or errors
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    closeSocketQuietly()
                    val errorMsg = e.message ?: "Connection failed"
                    Log.w(TAG, "attemptConnection: failed — $errorMsg", e)
                    obdUiState.value = ObdUiState.Waiting(errorMsg)
                    prefs.setObdLastError(errorMsg)
                    prefs.setObdLastState("Waiting")
                }

                if (!isActive) break

                // Serialized backoff before the next attempt within this same coroutine.
                obdUiState.value = ObdUiState.Retrying(retryDelaySeconds, maxRetryDelaySeconds)
                Log.d(TAG, "attemptConnection: retrying in ${retryDelaySeconds}s")
                delay(retryDelaySeconds * 1000L)
                if (retryDelaySeconds >= maxRetryDelaySeconds) {
                    obdUiState.value = ObdUiState.Waiting("Max retries reached")
                    break
                }
                retryDelaySeconds = (retryDelaySeconds * 2).coerceAtMost(maxRetryDelaySeconds)
            }
        }
    }

    /**
     * Connect an RFCOMM/SPP socket with fallbacks. Cheap ELM327 clones (e.g. KONNWEI)
     * often reject the secure SDP-based socket with "read failed, socket might closed";
     * the insecure socket and the reflection channel-1 socket are the documented
     * workarounds.
     */
    private fun connectRfcomm(device: BluetoothDevice): BluetoothSocket {
        // 1) Secure RFCOMM via SDP.
        try {
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            Log.d(TAG, "connectRfcomm: secure SPP socket connected")
            return socket
        } catch (e: Exception) {
            Log.w(TAG, "connectRfcomm: secure SPP failed (${e.message}); trying insecure")
        }
        // 2) Insecure RFCOMM via SDP.
        try {
            val socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            Log.d(TAG, "connectRfcomm: insecure SPP socket connected")
            return socket
        } catch (e: Exception) {
            Log.w(TAG, "connectRfcomm: insecure SPP failed (${e.message}); trying reflection ch1")
        }
        // 3) Reflection fallback on channel 1 (legacy workaround).
        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        val socket = method.invoke(device, 1) as BluetoothSocket
        socket.connect()
        Log.d(TAG, "connectRfcomm: reflection channel-1 socket connected")
        return socket
    }

    /**
     * Reset and configure the ELM327: echo off, linefeeds off, auto protocol.
     * Spaces are left ON (default) because the PID parsers split on spaces.
     */
    private fun initElm327(socket: BluetoothSocket) {
        val initCommands = listOf("ATZ", "ATE0", "ATL0", "ATSP0")
        for (cmd in initCommands) {
            val resp = writeATCommand(socket, cmd, maxWaitTicks = 500)
            Log.d(TAG, "initElm327: $cmd -> ${resp.replace("\r", "\\r").trim()}")
        }
    }

    /**
     * One-time diagnostic: read the mode-01 supported-PID bitmasks (0100/0120/0140/0160/0180)
     * and log every supported PID, plus the raw responses for the PIDs relevant to an indirect
     * fuel-rate estimate. Results go to Logcat under the [TAG]; this does not change runtime
     * behavior. Useful for deciding how to derive fuel consumption per vehicle.
     */
    private fun scanCapabilities(socket: BluetoothSocket) {
        val supported = sortedSetOf<Int>()
        for (base in intArrayOf(0x00, 0x20, 0x40, 0x60, 0x80)) {
            val cmd = "01" + String.format("%02X", base)
            val resp = writeATCommand(socket, cmd, maxWaitTicks = 300)
            val bytes = extractDataBytes(resp, 0x41, base)
            if (bytes == null || bytes.size < 4) {
                Log.d(TAG, "scanCapabilities: $cmd -> no/short data (${resp.replace("\r", "\\r").trim()})")
                break
            }
            var mask = 0L
            for (b in bytes.take(4)) mask = (mask shl 8) or (b.toLong() and 0xFF)
            for (i in 0 until 32) {
                if ((mask shr (31 - i)) and 1L == 1L) supported.add(base + i + 1)
            }
            Log.d(TAG, "scanCapabilities: $cmd raw=${bytes.take(4).joinToString(" ") { String.format("%02X", it) }}")
            if ((mask and 1L) == 0L) break // bit0 = "next range supported"; stop if absent
        }
        val supportedHex = supported.joinToString(" ") { String.format("01%02X", it) }
        Log.d(TAG, "scanCapabilities: SUPPORTED PIDs = $supportedHex")

        // Now that PID requests have forced auto-negotiation to resolve, read the concrete
        // OBD-II transport by name (ATDP) and number (ATDPN).
        val protocol = writeATCommand(socket, "ATDP", maxWaitTicks = 200)
        val protocolNum = writeATCommand(socket, "ATDPN", maxWaitTicks = 200)
        Log.d(TAG, "scanCapabilities: OBD-II protocol ATDP=${protocol.replace("\r", "\\r").trim()} ATDPN=${protocolNum.replace("\r", "\\r").trim()}")

        // Probe the PIDs that enable indirect fuel-rate estimation, log raw + decoded.
        val probes = listOf(
            0x10 to "MAF air flow (g/s)",
            0x5E to "Engine fuel rate (L/h, direct)",
            0x04 to "Calculated engine load (%)",
            0x43 to "Absolute load (%)",
            0x0B to "Intake MAP (kPa)",
            0x0F to "Intake air temp (C)",
            0x33 to "Barometric pressure (kPa)",
            0x46 to "Ambient air temp (C)",
            0x44 to "Commanded AFR (lambda)",
            0x51 to "Fuel type",
            0x5C to "Engine oil temp (C)"
        )
        for ((pid, name) in probes) {
            val cmd = "01" + String.format("%02X", pid)
            val resp = writeATCommand(socket, cmd, maxWaitTicks = 250)
            val supportedMark = if (supported.contains(pid)) "SUPPORTED" else "not in bitmask"
            Log.d(TAG, "scanCapabilities: probe $cmd ($name) [$supportedMark] -> ${resp.replace("\r", "\\r").trim()}")
        }
    }

    /**
     * Extract the data bytes following a "<mode> <pid>" header (e.g. 41 0B) from a space-formatted
     * ELM327 response. Returns null if the header is absent (NO DATA / unsupported).
     */
    private fun extractDataBytes(response: String, mode: Int, pid: Int): List<Int>? {
        val tokens = response
            .replace("\r", " ").replace(">", " ")
            .split(" ")
            .map { it.trim() }
            .filter { it.length == 2 && it.matches(Regex("[0-9A-Fa-f]{2}")) }
        val modeHex = String.format("%02X", mode)
        val pidHex = String.format("%02X", pid)
        for (i in 0 until tokens.size - 1) {
            if (tokens[i].equals(modeHex, true) && tokens[i + 1].equals(pidHex, true)) {
                return tokens.drop(i + 2).map { it.toInt(16) }
            }
        }
        return null
    }

    /**
     * Poll the adapter until the socket drops or an IO error occurs. Errors propagate to the
     * caller (attemptConnection's loop), which owns reconnection/backoff. Runs on the
     * connection coroutine — no separate job, so it cannot race the connect path.
     */
    private suspend fun runPollingLoop(socket: BluetoothSocket, prefs: ObdPreferencesDataStore) {
        val pollHz = prefs.obdPollHz.first()
        val retentionDays = prefs.obdRetentionDays.first()
        val displacementCc = prefs.obdEngineDisplacementCc.first()

        lastPollTimeMs = System.currentTimeMillis()
        fuelProbeFailStreak = 0
        fuelKnownUnsupported = false
        sdCycle = 0

        while (currentCoroutineContext().isActive && bluetoothSocket === socket) {
                try {
                    val pollStartMs = System.currentTimeMillis()
                    val dtSeconds = (pollStartMs - lastPollTimeMs) / 1000.0
                    lastPollTimeMs = pollStartMs

                    val speedResponse = writeATCommand(socket, "010D")
                    val rpmResponse = writeATCommand(socket, "010C")
                    val obdSpeedKmh = parseObdSpeed(speedResponse)
                    val rpm = parseObdRpm(rpmResponse)

                    // Fuel-rate chain: direct (015E) → MAF-derived (0110) → speed-density estimate
                    // (MAP/IAT/RPM) → UNAVAILABLE. Once direct+MAF prove unsupported we stop probing
                    // them every cycle and go straight to the speed-density estimate.
                    var fuelRateLph: Double? = null
                    var fuelSource = "UNAVAILABLE"
                    var mafGramsPerSec: Double? = null
                    if (!fuelKnownUnsupported) {
                        val direct = parseObdFuel(writeATCommand(socket, "015E"))
                        fuelRateLph = direct.first
                        fuelSource = direct.second
                        if (fuelRateLph == null) {
                            val maf = parseObdFuel(writeATCommand(socket, "0110"))
                            fuelRateLph = maf.first
                            fuelSource = maf.second
                        }
                        if (fuelRateLph == null) {
                            if (++fuelProbeFailStreak >= 5) {
                                fuelKnownUnsupported = true
                                Log.d(TAG, "runPollingLoop: 015E/0110 unsupported — switching to speed-density estimate")
                            }
                        } else {
                            fuelProbeFailStreak = 0
                        }
                    }
                    // Indirect estimate when no direct/MAF reading is available.
                    if (fuelRateLph == null && displacementCc > 0 && rpm != null && rpm > 0) {
                        val sd = computeSpeedDensityFuel(socket, rpm, displacementCc)
                        if (sd != null) {
                            fuelRateLph = sd.first
                            mafGramsPerSec = sd.second
                            fuelSource = "SPEED_DENSITY"
                        }
                    }

                    val now = System.currentTimeMillis()
                    Log.d(TAG, "poll: speed=$obdSpeedKmh rpm=$rpm fuel=${fuelRateLph?.let { String.format("%.2f", it) }}($fuelSource)")

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

                    // Session-average km/L, sourced from the PERSISTED accumulators so it survives
                    // app/service restarts (Slice 3). Null until some fuel has been consumed.
                    var avgKmL: Double? = null
                    var tripAvgKmL: Double? = null
                    // Fuel Cost (FR-12): session litres from the persisted accumulator; null when 0.
                    var sessionFuelConsumedL: Double? = null
                    if (gpsState.isAlwaysRecording) {
                        // Guard against a huge increment after a long stall/gap (e.g. adapter
                        // reconnect, backgrounding): only integrate plausible poll intervals.
                        val dtGuardOk = dtSeconds > 0 && dtSeconds < 60.0
                        val fuelIncrementL = if (fuelRateLph != null && dtGuardOk) {
                            fuelRateLph * dtSeconds / 3600.0
                        } else 0.0
                        val distIncrementKm = if (dtGuardOk) gpsSpeedKmh * dtSeconds / 3600.0 else 0.0
                        sessionFuelLiters += fuelIncrementL
                        sessionDistanceKm += distIncrementKm

                        // ADR-007 Slice 2: trip fuel as an O(1) in-memory accumulator, reseeded from
                        // obd_sample on a new trip or after a mid-trip process restart.
                        val gpsTripStartedAt = gpsState.tripStartedAt
                        if (gpsState.isTracking && gpsTripStartedAt > 0L) {
                            if (gpsTripStartedAt != tripAccumStartedAt) {
                                val obdDao = (applicationContext as com.kolee.tracklocation.TrackApp).obdSampleDao
                                tripFuelLiters = com.kolee.tracklocation.feature.obd.ObdFuelMath
                                    .integrateFuelLiters(obdDao.samplesBetweenOnce(gpsTripStartedAt, now))
                                tripAccumStartedAt = gpsTripStartedAt
                            } else {
                                tripFuelLiters += fuelIncrementL
                            }
                            val tripDistKm = gpsState.distanceInMeters / 1000.0
                            if (tripFuelLiters > 0.01 && tripDistKm > 0.01) {
                                tripAvgKmL = tripDistKm / tripFuelLiters
                            }
                        } else {
                            tripFuelLiters = 0.0
                            tripAccumStartedAt = 0L
                        }

                        // ADR-012: single getActiveSession() read reused both to persist the
                        // increment and to derive avgKmL, instead of a separate read before and after.
                        val sessionDao = (applicationContext as com.kolee.tracklocation.TrackApp).sessionDao
                        val activeSession = sessionDao.getActiveSession()
                        val sid = activeSession?.id
                        if (sid != null && fuelIncrementL > 0.0) {
                            sessionDao.addObdAccumulator(sid, fuelIncrementL)
                        }
                        // Reflect the just-written increment locally so avgKmL stays accurate with
                        // one query (the persisted row read above predates this cycle's increment).
                        val persistedFuelL = (activeSession?.obdFuelConsumedL ?: 0.0) + (if (sid != null) fuelIncrementL else 0.0)
                        val persistedDistKm = (activeSession?.distanceMeters ?: 0.0) / 1000.0
                        if (persistedFuelL > 0.01 && persistedDistKm > 0.01) {
                            avgKmL = persistedDistKm / persistedFuelL
                        }
                        sessionFuelConsumedL = persistedFuelL.takeIf { it > 0.0 }
                    }

                    obdUiState.value = ObdUiState.Connected(
                        rpm = rpm,
                        obdSpeedKmh = obdSpeedKmh,
                        fuelRateLph = fuelRateLph,
                        fuelSource = fuelSource,
                        instantKmL = instantKmL,
                        avgKmL = avgKmL,
                        tripAvgKmL = tripAvgKmL,
                        sessionFuelConsumedL = sessionFuelConsumedL,
                        tripFuelConsumedL = tripFuelLiters.takeIf { gpsState.isTracking && it > 0.0 }
                    )

                    if (gpsState.isAlwaysRecording) {
                        val sample = ObdSampleEntity(
                            timestampMs = now,
                            rpm = rpm,
                            obdSpeedKmh = obdSpeedKmh,
                            fuelRateLph = fuelRateLph,
                            mafGramsPerSecond = mafGramsPerSec,
                            fuelRateSource = fuelSource,
                            adapterElapsedMs = (now - lastPollTimeMs).coerceAtLeast(0L)
                        )
                        val obdDao = (applicationContext as com.kolee.tracklocation.TrackApp).obdSampleDao
                        obdDao.insert(sample)
                    }

                    if (gpsState.isAlwaysRecording && now % 21600000L < 500L) {
                        val cutoffTime = now - (retentionDays * 86400000L)
                        val obdDao = (applicationContext as com.kolee.tracklocation.TrackApp).obdSampleDao
                        obdDao.deleteOlderThan(cutoffTime)
                    }

                    delay((1000L / pollHz).coerceAtLeast(100L))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "runPollingLoop: poll error — ${e.message}")
                    closeSocketQuietly()
                    throw e // let attemptConnection handle Waiting + backoff
                }
        }
    }

    /**
     * Estimate fuel rate (L/h) via the speed-density method when the ECU exposes neither direct
     * fuel rate (015E) nor MAF (0110). Reads intake MAP (010B) every call; IAT (010F) and commanded
     * lambda (0144) change slowly and are cached, refreshed every 8 cycles.
     *
     *   MAF(g/s) = (RPM × MAP_kPa × VE × Displacement_L × M_air) / (120 × R × IAT_K)
     *   fuel(g/s) = MAF / (AFR_stoich × λ);  fuel(L/h) = fuel(g/s) × 3600 / ρ_fuel
     *
     * Constants assume gasoline: VE≈0.85, M_air=28.97 g/mol, R=8.314, AFR_stoich=14.7, ρ=745 g/L.
     * Returns (fuelLph, mafGramsPerSec), or null if MAP is unavailable.
     */
    private fun computeSpeedDensityFuel(socket: BluetoothSocket, rpm: Int, displacementCc: Int): Pair<Double, Double>? {
        val mapKpa = extractDataBytes(writeATCommand(socket, "010B"), 0x41, 0x0B)?.getOrNull(0) ?: return null

        if (sdCycle % 8 == 0) {
            extractDataBytes(writeATCommand(socket, "010F"), 0x41, 0x0F)?.getOrNull(0)?.let { a ->
                cachedIatK = (a - 40) + 273.15
            }
            extractDataBytes(writeATCommand(socket, "0144"), 0x41, 0x44)?.let { b ->
                if (b.size >= 2) {
                    val lambda = (2.0 / 65536.0) * (b[0] * 256 + b[1])
                    if (lambda > 0.1) cachedLambda = lambda
                }
            }
        }
        sdCycle++

        val displacementL = displacementCc / 1000.0
        val ve = 0.85
        val molarMassAir = 28.97
        val r = 8.314
        val mafGs = (rpm * mapKpa * ve * displacementL * molarMassAir) / (120.0 * r * cachedIatK)
        if (mafGs <= 0.0) return null
        val afr = 14.7 * cachedLambda
        val fuelGs = mafGs / afr
        val fuelLph = fuelGs * 3600.0 / 745.0
        return Pair(fuelLph, mafGs)
    }

    private fun writeATCommand(socket: BluetoothSocket, command: String, maxWaitTicks: Int = 200): String {
        val output = socket.outputStream
        val input = socket.inputStream
        val cmd = command.uppercase() + "\r"
        output.write(cmd.toByteArray())
        output.flush()

        val response = StringBuilder()
        val buffer = ByteArray(1024)
        var timeout = 0
        while (timeout < maxWaitTicks) {
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

    private fun closeSocketQuietly() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            // Ignore
        } finally {
            bluetoothSocket = null
        }
    }

    private fun closeConnection() {
        connectionJob?.cancel()
        connectionJob = null
        closeSocketQuietly()
        obdUiState.value = ObdUiState.Idle
        serviceScope.launch {
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
        try {
            startForeground(2001, notification)
        } catch (e: Exception) {
            // Android 14+ can reject a connectedDevice FGS if BLUETOOTH_CONNECT is missing
            // (SecurityException / ForegroundServiceStartNotAllowedException). Don't crash —
            // the connect attempt will surface the permission problem as a Waiting state.
            Log.w(TAG, "startForegroundNotification: could not start foreground — ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeConnection()
        serviceScope.coroutineContext.cancelChildren()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
