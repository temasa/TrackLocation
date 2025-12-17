package com.kolee.tracklocation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kolee.tracklocation.screens.list.ListScreen
import com.kolee.tracklocation.screens.settings.SettingsScreen
import com.kolee.tracklocation.screens.track.TrackScreen

@Composable
fun NavGraph(
    navHostController: NavHostController,
    modifier: Modifier
) {
    NavHost(
        navController = navHostController,
        startDestination = Screen.ListScreen.route,
        modifier = modifier
    ) {
        composable(Screen.ListScreen.route) {
            ListScreen(navHostController)
        }
        composable(Screen.TrackScreen.route) {
            TrackScreen()
        }
        composable(Screen.SettingsScreen.route) {
            SettingsScreen()
        }
    }
}