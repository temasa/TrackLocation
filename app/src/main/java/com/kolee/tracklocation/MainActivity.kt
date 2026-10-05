package com.kolee.tracklocation

import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.kolee.tracklocation.navigation.Screen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kolee.tracklocation.navigation.BottomNavigationScreen
import com.kolee.tracklocation.navigation.NavGraph
import androidx.core.content.ContextCompat
import com.kolee.tracklocation.tracking.Actions
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.ui.theme.TrackLocationTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Grace window for the launch-time orphan reaper. A live session refreshes its last-point
// timestamp every LOCATION_UPDATE_INTERVAL (5 s); 2 min comfortably covers a sticky-restart
// resume gap while still reaping sessions abandoned by a force-stop/shutdown.
private const val RESUME_GRACE_MILLIS = 2 * 60 * 1000L

class MainActivity : ComponentActivity() {

    companion object {
        // ADR-014: set by the Observer service to bring the Track screen forward.
        const val EXTRA_OPEN_TRACK = "open_track"
    }

    // Incremented per takeover request so repeated requests re-trigger the navigation effect.
    private var openTrackRequest by mutableStateOf(0)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTakeoverIntent(intent)
    }

    private fun handleTakeoverIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_TRACK, false) == true) {
            intent.removeExtra(EXTRA_OPEN_TRACK)
            openTrackRequest++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleTakeoverIntent(intent)
        // Close sessions left active by a previous process kill or device shutdown.
        // Guard 1: if the service is still running (e.g. config change), the static flag is intact —
        // skip entirely to avoid closing a live session.
        // Guard 2: only reap STALE sessions (no location point within the grace window). After a
        // low-memory kill, START_STICKY resurrects the service and resumes the open session; that
        // session was updated seconds ago, so it is not stale and survives even if this cleanup
        // runs before the service's resume completes.
        lifecycleScope.launch(Dispatchers.IO) {
            if (!TrackingService.locationUiState.value.isAlwaysRecording) {
                val now = System.currentTimeMillis()
                val sessionDao = (application as TrackApp).sessionDao
                sessionDao.closeStaleActiveSessions(
                    endedAt = now,
                    staleBefore = now - RESUME_GRACE_MILLIS
                )
                // Always-recording is meant to survive a restart. A user force-stop kills the
                // process without a START_STICKY redelivery, so a still-open (non-stale) session
                // would be left with no running service — the Session card reads Inactive while
                // the list still shows the session ACTIVE. If such a session survives the stale
                // reap, resume recording by re-entering the (healed) always-recording path, which
                // adopts the open session instead of creating a duplicate.
                if (sessionDao.getActiveSession() != null) {
                    val resumeIntent = Intent(this@MainActivity, TrackingService::class.java).apply {
                        action = Actions.START_RECORDING.name
                    }
                    ContextCompat.startForegroundService(this@MainActivity, resumeIntent)
                }
            }
            // Auto-start OBD service if enabled. Read the launch-time value only (first());
            // collecting the flow would re-fire ACTION_START on every DataStore write. Runtime
            // enable/disable is handled directly by ObdSettingsScreen.
            val prefs = com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore(this@MainActivity)
            if (prefs.obdServiceEnabled.first()) {
                val intent = Intent(this@MainActivity, com.kolee.tracklocation.feature.obd.service.ObdPollingService::class.java)
                intent.action = com.kolee.tracklocation.feature.obd.service.ObdPollingService.ACTION_START
                androidx.core.content.ContextCompat.startForegroundService(this@MainActivity, intent)
            }
            // Hybrid map initialization: fetch last known location at app startup to populate
            // the map immediately, before Compose UI is set up. Guard with permission checks
            // and handle SecurityException gracefully.
            if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    LocationServices.getFusedLocationProviderClient(this@MainActivity)
                        .lastLocation
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                TrackingService.seedLastKnownLocation(
                                    LatLng(location.latitude, location.longitude)
                                )
                            }
                        }
                } catch (e: SecurityException) {
                    // Location permission lost; log and continue without blocking
                    android.util.Log.e("MainActivity", "getLastLocation: permission lost", e)
                }
            }
        }
        @OptIn(ExperimentalMaterial3Api::class)
        setContent {
            TrackLocationTheme {
                val navController = rememberNavController()
                LaunchedEffect(openTrackRequest) {
                    if (openTrackRequest > 0) {
                        navController.navigate(Screen.TrackScreen.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavigationScreen(navController = navController) }
                ) { paddingValues ->
                    NavGraph(
                        navHostController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
