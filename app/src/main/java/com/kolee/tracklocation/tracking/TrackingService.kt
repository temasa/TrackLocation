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
import com.kolee.tracklocation.utils.CHANNEL_ID
import com.kolee.tracklocation.utils.FASTEST_LOCATION_INTERVAL
import com.kolee.tracklocation.utils.LOCATION_UPDATE_INTERVAL
import com.kolee.tracklocation.utils.LocationUtils
import com.kolee.tracklocation.utils.TimeUtilFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.RoundingMode
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

private const val TAG = "TrackingService"

class TrackingService: Service() {

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private var timer: Timer? = null
    private var startTime = 0L

    companion object {
        private const val TAG = "TrackingService"
        private val _locationUiState = MutableStateFlow(LocationUiState())
        val locationUiState = _locationUiState.asStateFlow()
        val NOTIFICATION_ID = Random.nextInt(999) + 100
    }

    private var isTracking = false
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
            Actions.START.name -> startTracking()
            Actions.STOP.name -> stopTracking()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startTracking() {
        isTracking = true

        requestLocationUpdate()
        startTimer()
        startForeground(
            NOTIFICATION_ID,
            createNotification(this, "Tracking current locaton..."))
    }

    private fun stopTracking() {
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
                    it.copy(durationTimer = getDurationTimer())
                }
            }
        }, 1, 1)
    }

    private fun getDurationTimer(): String {
        return TimeUtilFormatter.getTime(
            System.currentTimeMillis() - startTime
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdate() {
        if (isTracking) {
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
        }
    }

    private fun removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)

            Log.d(TAG, "onLocationResult, result: ${result}")

            if (isTracking) {
                result.locations.forEach { location ->
                    addPathPoints(location)
                    Log.d(TAG, "New locationPoint: ${location.latitude}, ${location.longitude}")
                }
            }
        }
    }

    private fun addPathPoints(location: Location?) = location?.let {
        val pos = LatLng(it.latitude, it.longitude)

        _locationUiState.update { state ->
            val pathPoints = state.pathPoints + pos

            state.copy(
                currentLocation = pos,
                pathPoints = pathPoints,
                distanceInMeters = state.distanceInMeters.run {
                    var distance =  this
                    if (pathPoints.size > 1) {
                        distance += LocationUtils.getDistanceBetweenPathPoints(
                            pathPoints1 = pathPoints[pathPoints.size - 1],
                            pathPoints2 = pathPoints[pathPoints.size - 2]
                        )
                    }

                    distance
                },
                speedInKMH = (it.speed * 3.6f).toBigDecimal()
                    .setScale(2, RoundingMode.HALF_UP).toFloat()
            )
        }
    }

    private fun createNotification(context: Context, msg: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_location_on_24)
            .setContentTitle("GPS Tracker")
            .setContentText(msg)
            .setOngoing(true)
            .build()
    }
}