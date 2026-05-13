package com.kolee.tracklocation.navigation

import androidx.annotation.StringRes
import com.kolee.tracklocation.R

sealed class Screen(
    @StringRes val title: Int,
    val iconId: Int,
    val route: String
) {
    object ListScreen: Screen(R.string.list_screen, R.drawable.baseline_list_24, "list_screen")
    object TrackScreen: Screen(R.string.track_screen, R.drawable.baseline_location_on_24, "track_screen")
    object SettingsScreen: Screen(R.string.settings_screen, R.drawable.baseline_settings_24, "settings_screen")
    object DetailScreen: Screen(R.string.details_screen, R.drawable.ic_location_pin, "details_screen")
}