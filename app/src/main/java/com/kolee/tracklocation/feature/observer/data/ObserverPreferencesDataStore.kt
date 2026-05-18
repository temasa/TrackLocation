package com.kolee.tracklocation.feature.observer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.observerDataStore: DataStore<Preferences> by preferencesDataStore(name = "observer_prefs")

class ObserverPreferencesDataStore(private val context: Context) {

    private val CAPTURE_RUNNING = booleanPreferencesKey("capture_running")

    val captureRunning: Flow<Boolean> = context.observerDataStore.data
        .map { prefs -> prefs[CAPTURE_RUNNING] ?: true }

    suspend fun setCaptureRunning(running: Boolean) {
        context.observerDataStore.edit { prefs ->
            prefs[CAPTURE_RUNNING] = running
        }
    }
}
