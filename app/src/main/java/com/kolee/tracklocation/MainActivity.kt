package com.kolee.tracklocation

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
