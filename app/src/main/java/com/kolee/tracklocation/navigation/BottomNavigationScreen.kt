package com.kolee.tracklocation.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.ui.theme.TripGreen
import com.kolee.tracklocation.ui.theme.TripInk
import com.kolee.tracklocation.ui.theme.TripMuted
import com.kolee.tracklocation.ui.theme.TripSurface
import com.kolee.tracklocation.ui.theme.TripSurfaceMuted

@Composable
fun BottomNavigationScreen(
    navController: NavController
) {
    val listBottomItems = listOf(
        Screen.ListScreen,
        Screen.TrackScreen,
        Screen.SettingsScreen
    )

    NavigationBar(
        containerColor = TripSurface
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        listBottomItems.forEach { bottomItem ->
            NavigationBarItem(
                selected = currentRoute == bottomItem.route,
                onClick = {
                    navController.navigate(bottomItem.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id)
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(bottomItem.iconId),
                        contentDescription = bottomItem.route
                    )
                },
                label = {
                    Text(
                        text = stringResource(bottomItem.title),
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    TripGreen,
                    TripMuted,
                    TripInk,
                    TripMuted,
                    TripSurfaceMuted
                )
            )
        }
    }
}
