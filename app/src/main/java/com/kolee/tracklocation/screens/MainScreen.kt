package com.kolee.tracklocation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kolee.tracklocation.feature.obd.FuelPriceController
import com.kolee.tracklocation.feature.obd.ui.FuelCostEditorDialog
import com.kolee.tracklocation.navigation.BottomNavigationScreen
import com.kolee.tracklocation.navigation.NavGraph
import com.kolee.tracklocation.navigation.Screen
import com.kolee.tracklocation.ui.components.FillUpDialog
import com.kolee.tracklocation.ui.components.FuelDock
import com.kolee.tracklocation.ui.components.MetricTheme
import com.kolee.tracklocation.ui.components.ResetFuelDialog
import com.kolee.tracklocation.viewmodel.FuelDockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val dockViewModel: FuelDockViewModel = viewModel(factory = FuelDockViewModel.Factory)
    val dockState by dockViewModel.uiState.collectAsState()
    val priceState by FuelPriceController.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    var showReset by remember { mutableStateOf(false) }
    var showPrice by remember { mutableStateOf(false) }
    var showFillUp by remember { mutableStateOf(false) }

    val dockTheme: MetricTheme? = when (route) {
        Screen.TrackScreen.route -> MetricTheme.Dark
        Screen.SessionScreen.route,
        Screen.ListScreen.route,
        Screen.SettingsScreen.route -> MetricTheme.Light
        else -> null
    }

    Scaffold(
        bottomBar = {
            Column {
                if (dockTheme != null) {
                    FuelDock(
                        theme = dockTheme,
                        state = dockState,
                        onResetClick = { showReset = true },
                        onPriceEditClick = { showPrice = true },
                        onFillUpClick = { showFillUp = true },
                    )
                }
                BottomNavigationScreen(navController = navController)
            }
        }
    ) { innerPadding ->
        NavGraph(
            navHostController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showReset) {
        ResetFuelDialog(
            onSave = { litres ->
                dockViewModel.reset(litres)
                showReset = false
            },
            onDismiss = { showReset = false },
        )
    }
    if (showPrice) {
        FuelCostEditorDialog(
            current = priceState.currentPrice,
            canUndo = priceState.canUndo,
            canRedo = priceState.canRedo,
            onSave = { newPrice ->
                FuelPriceController.set(newPrice)
                showPrice = false
            },
            onUndo = { FuelPriceController.undo() },
            onRedo = { FuelPriceController.redo() },
            onDismiss = { showPrice = false },
        )
    }
    if (showFillUp) {
        FillUpDialog(
            defaultPrice = priceState.currentPrice,
            onSave = { litres, price ->
                dockViewModel.fillUp(litres, price)
                showFillUp = false
            },
            onDismiss = { showFillUp = false },
        )
    }
}
