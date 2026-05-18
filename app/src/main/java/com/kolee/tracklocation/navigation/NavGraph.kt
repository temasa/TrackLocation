package com.kolee.tracklocation.navigation

import android.R.attr.type
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kolee.tracklocation.feature.observer.presentation.screens.ObserverFeedScreen
import com.kolee.tracklocation.screens.details.DetailsScreen
import com.kolee.tracklocation.screens.list.ListScreen
import com.kolee.tracklocation.screens.sessions.SessionsScreen
import com.kolee.tracklocation.screens.settings.SettingsScreen
import com.kolee.tracklocation.screens.track.TrackScreen

@Composable
fun NavGraph(
    navHostController: NavHostController,
    modifier: Modifier
) {
    NavHost(
        navController = navHostController,
        startDestination = Screen.SessionScreen.route,
        modifier = modifier
    ) {
        composable(Screen.SessionScreen.route) {
            SessionsScreen()
        }
        composable(Screen.ListScreen.route) {
            ListScreen(
                onSelect = { trackIdx ->
                    navHostController.navigate(
                        Screen.DetailScreen.route + "?trackIdx=$trackIdx"
                    )
                }
            )
        }
        composable(Screen.TrackScreen.route) {
            TrackScreen()
        }
        composable(Screen.SettingsScreen.route) {
            SettingsScreen(navController = navHostController)
        }
        composable(Screen.ObserverFeedScreen.route) {
            ObserverFeedScreen(navController = navHostController)
        }
        composable(
            route = Screen.DetailScreen.route + "?trackIdx={trackIdx}",
            arguments = listOf(navArgument("trackIdx") {
                type = NavType.IntType
                defaultValue = 0
            })
        ) { entry ->
            DetailsScreen(
                trackIdx = entry.arguments?.getInt("trackIdx") as Int,
                onNavigateUp = { navHostController.navigateUp() }
            )
        }
    }
}
