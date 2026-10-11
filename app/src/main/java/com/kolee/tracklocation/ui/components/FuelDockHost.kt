package com.kolee.tracklocation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kolee.tracklocation.feature.obd.FuelPriceController
import com.kolee.tracklocation.feature.obd.ui.FuelCostEditorDialog
import com.kolee.tracklocation.navigation.Screen
import com.kolee.tracklocation.viewmodel.FuelDockViewModel

/**
 * Hosts the fuel dock and its dialogs. Place directly above the bottom navigation bar.
 * The dock is shown only on Track (dark), Session, Trips and Settings (light) routes.
 */
@Composable
fun FuelDockHost(navController: NavController) {
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

    if (dockTheme != null) {
        FuelDock(
            theme = dockTheme,
            state = dockState,
            onResetClick = { showReset = true },
            onPriceEditClick = { showPrice = true },
            onFillUpClick = { showFillUp = true },
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
