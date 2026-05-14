package com.kolee.tracklocation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    secondary = Teal200
)

private val LightColorScheme = lightColorScheme(
    primary = TripGreen,
    secondary = TripBlue,
    background = TripBackground,
    surface = TripSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TripInk,
    onSurface = TripInk

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun TrackLocationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val systemUiController = rememberSystemUiController()
    if (darkTheme) {
        systemUiController.setSystemBarsColor(color = Color.Black)
        systemUiController.setStatusBarColor(color = colorScheme.background)
    } else {
        systemUiController.setSystemBarsColor(color = Color.White)
        systemUiController.setStatusBarColor(color = colorScheme.background)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
