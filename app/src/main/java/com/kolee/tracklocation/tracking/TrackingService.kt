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
import com.kolee.tracklocation.utils.CHANNEL_ID
import com.kolee.tracklocation.utils.FASTEST_LOCATION_INTERVAL
import com.kolee.tracklocation.utils.LOCATION_UPDATE_INTERVAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.random.Random

private const val TAG = "TrackingService"

class TrackingService: Service() {

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timer: Timer? = null
    private var tripStartTime = 0L
    private var locationUpdatesRequested = false
    private var pendingTripStartBoundary = false
    private var activeSession: SessionEntity? = null
    private var lastSessionLocation: Location? = null
    private var lastTripLocation: Location? = null
    private val database by lazy { TrackDatabase.getDatabase(applicationContext) }

    companion object {
        private const val TAG = "TrackingService"
        private val _locationUiState = MutableStateFlow(LocationUiState())
        val locationUiState = _locationUiState.asStateFlow()
        val NOTIFICATION_ID = Random.nextInt(999) + 100
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
        }

        return START_STICKY
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startAlwaysRecording() {
        if (!isAlwaysRecording) {
            activeSession = SessionEntity(
                id = UUID.randomUUID().toString(),
                startedAt = System.currentTimeMillis(),
                isActive = true
            )
            serviceScope.launch {
                activeSession?.let { database.sessionDao.insertSession(it) }
            }
        }

        isAlwaysRecording = true
        requestLocationUpdate()
        startForeground(
            NOTIFICATION_ID,
            createNotification(this, "Recording location history...")
        )
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

        stopForeground(NOTIFICATION_ID)
        stopSelf()
    }

    private fun startTimer() {
        timer?.cancel()

        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                _locationUiState.update {
                    it.copy(durationTimer = System.currentTimeMillis() - tripStartTime)
                }
            }
        }, 1, 1)
    }

//    private fun getDurationTimer(): String {
//        return TimeUtilFormatter.getTime(
//            System.currentTimeMillis() - startTime
//        )
//    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdate() {
        if (!locationUpdatesRequested) {
            val locationRequest = LocationRequest.create().apply {
                interval = LOCATION_UPDATE_INTERVAL
                fastestInterval = FASTEST_LOCATION_INTERVAL
                maxWaitTime = LOCATION_UPDATE_INTERVAL
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            locationUpdatesRequested = true
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
            val insertedId = database.locationDao.insertLocation(
                LocationEntity(
                    timestamp = currentLocation.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    latitude = currentLocation.latitude,
                    longitude = currentLocation.longitude,
                    accuracyMeters = currentLocation.accuracy,
                    speedMetersPerSecond = currentLocation.speed,
                    bearingDegrees = currentLocation.bearing,
                    altitudeMeters = currentLocation.altitude
                )
            )
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
                    speedInKMH = kmh(location)
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
