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
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.ui.theme.TrackLocationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Close any sessions that were left active by a previous process kill or device shutdown.
        // Guard: if the service is still running (e.g. config change), skip to avoid closing a live session.
        lifecycleScope.launch(Dispatchers.IO) {
            if (!TrackingService.locationUiState.value.isAlwaysRecording) {
                (application as TrackApp).sessionDao.closeAllActiveSessions(System.currentTimeMillis())
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
