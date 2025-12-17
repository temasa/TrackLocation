package com.kolee.tracklocation.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kolee.tracklocation.navigation.BottomNavigationScreen
import com.kolee.tracklocation.navigation.NavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationScreen(navController = navController )
        }
    ) { innerPadding ->
        NavGraph(
            navHostController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}