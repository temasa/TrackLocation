package com.kolee.tracklocation.feature.obd.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.obdDataStore: DataStore<Preferences> by preferencesDataStore(name = "obd_prefs")

class ObdPreferencesDataStore(private val context: Context) {

    private val OBD_SERVICE_ENABLED = booleanPreferencesKey("obd_service_enabled")
    private val OBD_DEVICE_MAC = stringPreferencesKey("obd_device_mac")
    private val OBD_POLL_HZ = intPreferencesKey("obd_poll_hz")
    private val OBD_RETENTION_DAYS = intPreferencesKey("obd_retention_days")
    private val OBD_RETRY_MAX_SECONDS = intPreferencesKey("obd_retry_max_seconds")
    private val OBD_LAST_STATE = stringPreferencesKey("obd_last_state")
    private val OBD_LAST_ERROR = stringPreferencesKey("obd_last_error")
    private val OBD_LAST_SAMPLE_TS = longPreferencesKey("obd_last_sample_ts")
    private val OBD_ENGINE_DISPLACEMENT_CC = intPreferencesKey("obd_engine_displacement_cc")

    val obdServiceEnabled: Flow<Boolean> = context.obdDataStore.data
        .map { prefs -> prefs[OBD_SERVICE_ENABLED] ?: false }

    val obdDeviceMac: Flow<String> = context.obdDataStore.data
        .map { prefs -> prefs[OBD_DEVICE_MAC] ?: "" }

    val obdPollHz: Flow<Int> = context.obdDataStore.data
        .map { prefs -> prefs[OBD_POLL_HZ] ?: 2 }

    val obdRetentionDays: Flow<Int> = context.obdDataStore.data
        .map { prefs -> prefs[OBD_RETENTION_DAYS] ?: 7 }

    val obdRetryMaxSeconds: Flow<Int> = context.obdDataStore.data
        .map { prefs -> prefs[OBD_RETRY_MAX_SECONDS] ?: 120 }

    val obdLastState: Flow<String> = context.obdDataStore.data
        .map { prefs -> prefs[OBD_LAST_STATE] ?: "Idle" }

    val obdLastError: Flow<String> = context.obdDataStore.data
        .map { prefs -> prefs[OBD_LAST_ERROR] ?: "" }

    val obdLastSampleTs: Flow<Long> = context.obdDataStore.data
        .map { prefs -> prefs[OBD_LAST_SAMPLE_TS] ?: 0L }

    // Engine displacement in cc, used for the indirect speed-density fuel estimate on vehicles
    // that expose neither direct fuel rate (015E) nor MAF (0110). 0 disables the estimate.
    val obdEngineDisplacementCc: Flow<Int> = context.obdDataStore.data
        .map { prefs -> prefs[OBD_ENGINE_DISPLACEMENT_CC] ?: 1193 }

    suspend fun setObdServiceEnabled(enabled: Boolean) {
        context.obdDataStore.edit { prefs ->
            prefs[OBD_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setObdDeviceMac(mac: String) {
        context.obdDataStore.edit { prefs ->
            prefs[OBD_DEVICE_MAC] = mac
        }
    }

    suspend fun setObdPollHz(hz: Int) {
        context.obdDataStore.edit { prefs ->
            prefs[OBD_POLL_HZ] = hz
        }
    }

    suspend fun setObdRetentionDays(days: Int) {
        context.obdDataStore.edit { prefs ->
            prefs[OBD_RETENTION_DAYS] = days
        }
    }

    suspend fun setObdRetryMaxSeconds(seconds: Int) {
        context.obdDataStore.edit { prefs ->
            prefs[OBD_RETRY_MAX_SECONDS] = seconds
        }
    }

    suspend fun setObdLastState(state: String) {
        context.obdDataStore.edit { prefs ->
            prefs[OBD_LAST_STATE] = state
        }
    }

    suspend fun setObdLastError(error: String) {
        context.obdDataStore.edit { prefs ->
            prefs[OBD_LAST_ERROR] = error
        }
    }

    suspend fun setObdLastSampleTs(ts: Long) {
        context.obdDataStore.edit { prefs ->
            prefs[OBD_LAST_SAMPLE_TS] = ts
        }
    }

    suspend fun setObdEngineDisplacementCc(cc: Int) {
        context.obdDataStore.edit { prefs ->
            prefs[OBD_ENGINE_DISPLACEMENT_CC] = cc
        }
    }
}
