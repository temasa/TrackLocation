package com.kolee.tracklocation.navigation

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kolee.tracklocation.ui.theme.PurpleGrey80
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.kolee.tracklocation.ui.theme.Purple40
import com.kolee.tracklocation.ui.theme.Purple400

@Composable
fun BottomNavigationScreen(
    navController: NavController
) {
    val listBottomItems = listOf(
        Screen.ListScreen,
        Screen.TrackScreen,
        Screen.SettingsScreen
    )

    BottomNavigation(
        backgroundColor = PurpleGrey80
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        listBottomItems.forEach { bottomItem ->
            BottomNavigationItem(
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
                selectedContentColor = Purple400,
                unselectedContentColor = Purple40
            )
        }
    }
}