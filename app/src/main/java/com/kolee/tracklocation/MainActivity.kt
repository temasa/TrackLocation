package com.kolee.tracklocation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kolee.tracklocation.navigation.BottomNavigationScreen
import com.kolee.tracklocation.navigation.NavGraph
import androidx.core.content.ContextCompat
import com.kolee.tracklocation.tracking.Actions
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.ui.theme.TrackLocationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Grace window for the launch-time orphan reaper. A live session refreshes its last-point
// timestamp every LOCATION_UPDATE_INTERVAL (5 s); 2 min comfortably covers a sticky-restart
// resume gap while still reaping sessions abandoned by a force-stop/shutdown.
private const val RESUME_GRACE_MILLIS = 2 * 60 * 1000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        }
        @OptIn(ExperimentalMaterial3Api::class)
        setContent {
            TrackLocationTheme {
                val navController = rememberNavController()
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
