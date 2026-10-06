package com.kolee.tracklocation.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** One display-only live fix (ADR-018). Never persisted anywhere. */
data class LiveFix(
    val latLng: LatLng,
    val speedMps: Float,
    val bearingDeg: Float,
    val accuracyM: Float,
    val timeMs: Long
)

/**
 * Foreground-only live location for the Track screen map (ADR-018).
 * Independent of TrackingService: fixes are exposed as a StateFlow and never written to Room/log.
 */
class LiveLocationSource(context: Context) {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    private val _fix = MutableStateFlow<LiveFix?>(null)
    val fix: StateFlow<LiveFix?> = _fix

    private var started = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val loc = locationResult.lastLocation ?: return
            _fix.value = LiveFix(
                latLng = LatLng(loc.latitude, loc.longitude),
                speedMps = loc.speed,
                bearingDeg = loc.bearing,
                accuracyM = loc.accuracy,
                timeMs = loc.time
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return
        @Suppress("DEPRECATION")
        val request = LocationRequest.create().apply {
            interval = 1000L
            fastestInterval = 500L
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            started = true
            // Seed once so the dot appears immediately, before the first update arrives.
            if (_fix.value == null) {
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null && _fix.value == null) {
                        _fix.value = LiveFix(
                            latLng = LatLng(loc.latitude, loc.longitude),
                            speedMps = loc.speed,
                            bearingDeg = loc.bearing,
                            accuracyM = loc.accuracy,
                            timeMs = loc.time
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission revoked at runtime — nothing to show.
            started = false
        }
    }

    fun stop() {
        if (!started) return
        client.removeLocationUpdates(callback)
        started = false
    }
}

/**
 * Remembers a [LiveLocationSource] and runs it only while the lifecycle is STARTED and [enabled].
 */
@Composable
fun rememberLiveFix(enabled: Boolean): State<LiveFix?> {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val source = remember { LiveLocationSource(context) }

    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (enabled) source.start()
                Lifecycle.Event.ON_STOP -> source.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (enabled && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            source.start()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            source.stop()
        }
    }

    return source.fix.collectAsState()
}
