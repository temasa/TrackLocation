package com.kolee.tracklocation.feature.overlay.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.overlayDataStore: DataStore<Preferences> by preferencesDataStore(name = "overlay_prefs")

class OverlayPreferencesDataStore(private val context: Context) {

    private val OVERLAY_COLLAPSED = booleanPreferencesKey("overlay_collapsed")
    private val OVERLAY_CIRCLE_X = intPreferencesKey("overlay_circle_x")
    private val OVERLAY_CIRCLE_Y = intPreferencesKey("overlay_circle_y")
    private val OVERLAY_PROMPT_SHOWN = booleanPreferencesKey("overlay_prompt_shown")

    // Shrunk-to-circle state. false = full strip.
    val collapsed: Flow<Boolean> = context.overlayDataStore.data
        .map { prefs -> prefs[OVERLAY_COLLAPSED] ?: false }

    // Circle position in px. -1 = unset (use the default corner).
    val circleX: Flow<Int> = context.overlayDataStore.data
        .map { prefs -> prefs[OVERLAY_CIRCLE_X] ?: -1 }

    val circleY: Flow<Int> = context.overlayDataStore.data
        .map { prefs -> prefs[OVERLAY_CIRCLE_Y] ?: -1 }

    // One-time "Display over other apps" prompt has been shown.
    val promptShown: Flow<Boolean> = context.overlayDataStore.data
        .map { prefs -> prefs[OVERLAY_PROMPT_SHOWN] ?: false }

    suspend fun setCollapsed(collapsed: Boolean) {
        context.overlayDataStore.edit { prefs ->
            prefs[OVERLAY_COLLAPSED] = collapsed
        }
    }

    // Writes both coordinates in one edit so they are never saved half-updated.
    suspend fun setCirclePosition(x: Int, y: Int) {
        context.overlayDataStore.edit { prefs ->
            prefs[OVERLAY_CIRCLE_X] = x
            prefs[OVERLAY_CIRCLE_Y] = y
        }
    }

    suspend fun setPromptShown(shown: Boolean) {
        context.overlayDataStore.edit { prefs ->
            prefs[OVERLAY_PROMPT_SHOWN] = shown
        }
    }
}
