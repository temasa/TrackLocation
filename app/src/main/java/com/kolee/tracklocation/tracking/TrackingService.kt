package com.kolee.tracklocation.tracking

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.kolee.tracklocation.R
import com.kolee.tracklocation.data.roomdb.LocationEntity
import com.kolee.tracklocation.data.roomdb.SessionEntity
import com.kolee.tracklocation.data.roomdb.TrackDatabase
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.utils.CHANNEL_ID
import com.kolee.tracklocation.utils.FASTEST_LOCATION_INTERVAL
import com.kolee.tracklocation.utils.LOCATION_UPDATE_INTERVAL
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.random.Random

private const val TAG = "TrackingService"

class TrackingService: Service() {

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    // Without a handler, an uncaught exception in a fire-and-forget launch (e.g. a Room write
    // failing on a full/locked DB) reaches the default handler and crashes the process, silently
    // killing the foreground service. SupervisorJob alone does NOT prevent this — it only stops
    // sibling cancellation. Log and keep recording instead.
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "serviceScope coroutine failed; recording continues", throwable)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var timer: Timer? = null
    private var tripStartTime = 0L
    private var locationUpdatesRequested = false
    private var pendingTripStartBoundary = false
    private var activeSession: SessionEntity? = null
    private var lastSessionLocation: Location? = null
    private var lastTripLocation: Location? = null
    private var dwellAnchorId: Long? = null
    private var dwellAnchorLocation: Location? = null
    private var dwellingOnAnchor: Boolean = false
    private var dwellBreakStreak: Int = 0
    private val database by lazy { TrackDatabase.getDatabase(applicationContext) }

    companion object {
        private const val TAG = "TrackingService"
        private val _locationUiState = MutableStateFlow(LocationUiState())
        val locationUiState = _locationUiState.asStateFlow()
        val NOTIFICATION_ID = Random.nextInt(999) + 100
        private const val DWELL_TOLERANCE_MIN_METERS = 15f
        private const val DWELL_ACCURACY_FACTOR = 1.5f
        private const val DWELL_BREAK_CONFIRM_FIXES = 2
    }

    private var isAlwaysRecording = false
        set(value) {
            _locationUiState.update {
                it.copy(isAlwaysRecording = value)
            }
            field = value
        }

    private var isTripRecording = false
        set(value) {
            _locationUiState.update {
                it.copy(isTracking = value)
            }
            field = value
        }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand, intent.action: ${intent?.action}")
        when (intent?.action) {
            Actions.START_RECORDING.name -> startAlwaysRecording()
            Actions.STOP_RECORDING.name -> stopAlwaysRecording()
            Actions.START_TRIP.name -> startTrip()
            Actions.STOP_TRIP.name -> stopTrip()
            // START_STICKY redelivers a null intent after a system kill. The open session row
            // (isActive = 1) is the persisted "should be recording" state — resume from it so
            // recording doesn't silently stop after a low-memory kill.
            null -> resumeIfActiveSession()
        }

        return START_STICKY
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startAlwaysRecording() {
        val wasRecording = isAlwaysRecording
        isAlwaysRecording = true
        requestLocationUpdate()
        startForeground(
            NOTIFICATION_ID,
            createNotification(this, "Recording location history...")
        )

        if (!wasRecording) {
            // Enforce the single-active-session invariant. A prior session can be left
            // isActive=1 in the DB (e.g. force-stopped mid-session then reopened inside the
            // launch reaper's grace window, so it wasn't reaped and START_STICKY didn't resume
            // it). Close every open row first to heal any duplicates/orphans, then ADOPT the
            // most-recent open session (preserving its distance/points) or create a fresh one —
            // so exactly one row ends up isActive=1.
            serviceScope.launch {
                val now = System.currentTimeMillis()
                val existing = database.sessionDao.getActiveSession()
                database.sessionDao.closeAllActiveSessions(endedAt = now)
                val session = existing?.copy(isActive = true, endedAt = null)
                    ?: SessionEntity(
                        id = UUID.randomUUID().toString(),
                        startedAt = now,
                        isActive = true
                    )
                database.sessionDao.insertSession(session)
                activeSession = session
                startObdSession(session.id)
            }
        } else {
            startObdSession(activeSession?.id)
        }
    }

    private fun startObdSession(sessionId: String?) {
        val intent = Intent(this, ObdPollingService::class.java).apply {
            action = ObdPollingService.ACTION_SESSION_ON
            putExtra(ObdPollingService.EXTRA_SESSION_ID, sessionId)
        }
        startService(intent)
    }

    /**
     * Recover after a START_STICKY restart (null intent). If an open session exists in the DB,
     * adopt it and re-establish the foreground service + location updates so recording resumes
     * transparently. If none exists, there is nothing to record — shut the service down so it
     * doesn't linger as a zombie foreground service.
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun resumeIfActiveSession() {
        serviceScope.launch {
            val session = database.sessionDao.getActiveSession()
            withContext(Dispatchers.Main) {
                if (session == null) {
                    stopServiceIfIdle()
                    return@withContext
                }
                activeSession = session
                lastSessionLocation = null
                isAlwaysRecording = true
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(this@TrackingService, "Recording location history...")
                )
                requestLocationUpdate()

                val obdIntent = Intent(this@TrackingService, ObdPollingService::class.java).apply {
                    action = ObdPollingService.ACTION_SESSION_ON
                    putExtra(ObdPollingService.EXTRA_SESSION_ID, session.id)
                }
                startService(obdIntent)
            }
        }
    }

    private fun stopAlwaysRecording() {
        stopTrip()
        isAlwaysRecording = false
        val sessionToClose = activeSession
        val endedAt = System.currentTimeMillis()
        serviceScope.launch {
            if (sessionToClose != null) {
                database.sessionDao.insertSession(
                    sessionToClose.copy(
                        endedAt = endedAt,
                        endLocationId = sessionToClose.endLocationId,
                        durationMillis = endedAt - sessionToClose.startedAt,
                        isActive = false
                    )
                )
            }
        }
        activeSession = null
        lastSessionLocation = null
        dwellAnchorId = null
        dwellAnchorLocation = null
        dwellingOnAnchor = false
        dwellBreakStreak = 0

        val intent = Intent(this, ObdPollingService::class.java).apply {
            action = ObdPollingService.ACTION_SESSION_OFF
        }
        startService(intent)

        stopServiceIfIdle()
    }

    private fun startTrip() {
        if (!isAlwaysRecording) {
            startAlwaysRecording()
        }

        pendingTripStartBoundary = true
        lastTripLocation = null
        tripStartTime = System.currentTimeMillis()
        isTripRecording = true
        _locationUiState.update {
            it.copy(
                pathPoints = emptyList(),
                distanceInMeters = 0,
                durationTimer = 0L,
                speedInKMH = 0f,
                tripStartedAt = tripStartTime,
                activeTripStartLocationId = null,
                activeTripEndLocationId = null
            )
        }
        startTimer()
    }

    private fun stopTrip() {
        if (!isTripRecording) return

        isTripRecording = false
        pendingTripStartBoundary = false
        timer?.cancel()
    }

    private fun stopServiceIfIdle() {
        if (isAlwaysRecording || isTripRecording) return

        _locationUiState.update {
            LocationUiState()
        }
        timer?.cancel()
        removeLocationUpdates()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        timer?.cancel()

        timer = Timer()
        // 1 s period: the UI renders H:MM:SS, so a 1 ms tick (the old value) just flooded the
        // StateFlow ~1000×/s, burning CPU/battery and churning recompositions for no benefit.
        timer?.schedule(object : TimerTask() {
            override fun run() {
                _locationUiState.update {
                    it.copy(durationTimer = System.currentTimeMillis() - tripStartTime)
                }
            }
        }, 0L, 1000L)
    }

//    private fun getDurationTimer(): String {
//        return TimeUtilFormatter.getTime(
//            System.currentTimeMillis() - startTime
//        )
//    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdate() {
        if (!locationUpdatesRequested) {
            @Suppress("DEPRECATION")
            val locationRequest = LocationRequest.create().apply {
                interval = LOCATION_UPDATE_INTERVAL
                fastestInterval = FASTEST_LOCATION_INTERVAL
                priority = Priority.PRIORITY_HIGH_ACCURACY
                maxWaitTime = LOCATION_UPDATE_INTERVAL
            }

            try {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                locationUpdatesRequested = true
            } catch (e: SecurityException) {
                // Location permission can be revoked/downgraded at runtime (auto-revoke, user
                // toggling "While in use"). Don't crash the service — stop recording cleanly.
                Log.e(TAG, "requestLocationUpdate: location permission lost", e)
                locationUpdatesRequested = false
                stopAlwaysRecording()
            }
        }
    }

    private fun removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        locationUpdatesRequested = false
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)

            Log.d(TAG, "onLocationResult, result: ${result}")

            if (isAlwaysRecording) {
                result.locations.forEach { location ->
                    recordLocation(location)
                    Log.d(TAG, "New locationPoint: ${location.latitude}, ${location.longitude}")
                }
            }
        }
    }

    private fun recordLocation(location: Location?) = location?.let { currentLocation ->
        serviceScope.launch {
            val anchorLocation = dwellAnchorLocation
            val anchorId = dwellAnchorId
            if (anchorLocation != null && anchorId != null) {
                val distance = distanceBetween(anchorLocation, currentLocation)
                val tolerance = maxOf(
                    DWELL_TOLERANCE_MIN_METERS,
                    DWELL_ACCURACY_FACTOR * currentLocation.accuracy
                )
                if (distance <= tolerance) {
                    // Within the dwell radius: collapse onto the existing anchor.
                    // Bump last-seen timestamp + collapsedCount; add no session/trip distance.
                    dwellBreakStreak = 0
                    dwellingOnAnchor = true
                    val dwellTs = currentLocation.time.takeIf { it > 0L } ?: System.currentTimeMillis()
                    database.locationDao.updateDwellAnchor(anchorId, dwellTs)
                    return@launch
                }
                if (dwellingOnAnchor) {
                    // Out of radius while parked: require consecutive confirmations to reject a
                    // single GPS outlier before breaking the dwell.
                    dwellBreakStreak++
                    if (dwellBreakStreak < DWELL_BREAK_CONFIRM_FIXES) {
                        return@launch
                    }
                }
            }
            // Movement confirmed (or first fix / post-restart): insert a new anchor.
            dwellBreakStreak = 0
            dwellingOnAnchor = false
            val timestamp = currentLocation.time.takeIf { it > 0L } ?: System.currentTimeMillis()
            val insertedId = database.locationDao.insertLocation(
                LocationEntity(
                    timestamp = timestamp,
                    dwellStartTimestamp = timestamp,
                    latitude = currentLocation.latitude,
                    longitude = currentLocation.longitude,
                    accuracyMeters = currentLocation.accuracy,
                    speedMetersPerSecond = currentLocation.speed,
                    bearingDegrees = currentLocation.bearing,
                    altitudeMeters = currentLocation.altitude
                )
            )
            dwellAnchorId = insertedId
            dwellAnchorLocation = currentLocation
            updateActiveSession(currentLocation, insertedId)
            updateTripState(currentLocation, insertedId)
        }
    }

    private suspend fun updateActiveSession(location: Location, locationId: Long) {
        val session = activeSession ?: return
        val distanceToAdd = lastSessionLocation?.let { previous ->
            distanceBetween(previous, location)
        } ?: 0
        val updated = session.copy(
            startLocationId = session.startLocationId ?: locationId,
            endLocationId = locationId,
            distanceMeters = session.distanceMeters + distanceToAdd,
            durationMillis = System.currentTimeMillis() - session.startedAt,
            pointCount = session.pointCount + 1,
            isActive = true
        )
        activeSession = updated
        lastSessionLocation = location
        database.sessionDao.insertSession(updated)
    }

    private fun updateTripState(location: Location, locationId: Long) {
        val pos = LatLng(location.latitude, location.longitude)
        _locationUiState.update { state ->
            if (!isTripRecording) {
                return@update state.copy(
                    currentLocation = pos,
                    speedInKMH = kmh(location),
                    accuracyMeters = location.accuracy
                )
            }

            val startLocationId = when {
                state.activeTripStartLocationId != null -> state.activeTripStartLocationId
                pendingTripStartBoundary -> locationId
                else -> locationId
            }
            pendingTripStartBoundary = false

            val pathPoints = state.pathPoints + pos
            val distanceToAdd = lastTripLocation?.let { previous ->
                distanceBetween(previous, location)
            } ?: 0
            lastTripLocation = location

            state.copy(
                currentLocation = pos,
                pathPoints = pathPoints,
                distanceInMeters = state.distanceInMeters + distanceToAdd,
                speedInKMH = kmh(location),
                accuracyMeters = location.accuracy,
                activeTripStartLocationId = startLocationId,
                activeTripEndLocationId = locationId
            )
        }
    }

    private fun distanceBetween(previous: Location, current: Location): Int {
        val result = FloatArray(1)
        Location.distanceBetween(
            previous.latitude,
            previous.longitude,
            current.latitude,
            current.longitude,
            result
        )
        return result[0].toInt()
    }

    private fun kmh(location: Location): Float = location.speed * 3.6f

    private fun createNotification(context: Context, msg: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_location_on_24)
            .setContentTitle("GPS Tracker")
            .setContentText(msg)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        removeLocationUpdates()
        serviceScope.cancel()
    }
}
