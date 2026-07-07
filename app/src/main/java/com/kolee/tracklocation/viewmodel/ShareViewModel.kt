package com.kolee.tracklocation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.maps.model.LatLng
import com.kolee.tracklocation.data.roomdb.LocationDao
import com.kolee.tracklocation.data.roomdb.ObdSampleDao
import com.kolee.tracklocation.data.roomdb.ObdSampleEntity
import com.kolee.tracklocation.data.roomdb.SessionDao
import com.kolee.tracklocation.data.roomdb.SessionEntity
import com.kolee.tracklocation.TrackApp
import com.kolee.tracklocation.data.roomdb.TrackDao
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.tracking.Actions
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.utils.LocationUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ShareViewModel(
    private val appContext: Context,
    private val databaseDao: TrackDao,
    private val locationDao: LocationDao,
    private val sessionDao: SessionDao,
    private val obdSampleDao: ObdSampleDao
): ViewModel() {

    var locationUiState = TrackingService.locationUiState
    var responseState by mutableStateOf<Response>(Response.Loading)
        private set
    var sessionsState by mutableStateOf<List<SessionEntity>>(emptyList())
        private set
    var selectedTrackState by mutableStateOf(TrackEntity())
        private set
    var selectedTrackPathPoints by mutableStateOf<List<LatLng>>(emptyList())
        private set
    private var job: Job? = null


    fun insertTrack(item: TrackEntity) {
        viewModelScope.launch {
            databaseDao.insertTrack(item)
        }
    }

    /**
     * OBD Phase 2 Slice 4: total litres consumed since [startMs] (trip start) up to now, integrated
     * from the recorded `obd_sample` rows. Used to compute a live trip-average km/L on the Track
     * screen (there is no live trip row/id — see IMPLEMENTATION-ISSUES #1). 0.0 if no OBD samples.
     */
    suspend fun tripFuelLitersSince(startMs: Long): Double =
        integrateFuelLiters(obdSampleDao.samplesBetweenOnce(startMs, System.currentTimeMillis()))

    /**
     * Integrate instantaneous fuel rate (L/h) over time into total litres, mirroring the
     * per-poll accumulation in ObdPollingService: for each consecutive sample pair, add
     * `fuelRateLph × dtHours` using the same 0 < dt < 60 s guard so a long gap (adapter drop,
     * backgrounding) doesn't inflate the total. [samples] must be ascending by timestamp.
     */
    private fun integrateFuelLiters(samples: List<ObdSampleEntity>): Double {
        var liters = 0.0
        for (i in 1 until samples.size) {
            val rate = samples[i].fuelRateLph ?: continue
            val dtSeconds = (samples[i].timestampMs - samples[i - 1].timestampMs) / 1000.0
            if (dtSeconds > 0 && dtSeconds < 60.0) {
                liters += rate * dtSeconds / 3600.0
            }
        }
        return liters
    }

    init {
        viewModelScope.launch {
            databaseDao.getAllTracks().distinctUntilChanged().collect { allTracks ->
                responseState = Response.Success(data = allTracks)
            }
        }
        viewModelScope.launch {
            sessionDao.getAllSessions().distinctUntilChanged().collect { sessions ->
                sessionsState = sessions
            }
        }
    }

    fun onTripCtaTap() {
        val current = locationUiState.value
        viewModelScope.launch {
            if (current.isTracking && !current.isPaused) {
                val startId = current.activeTripStartLocationId
                val endId = current.activeTripEndLocationId
                // A trip's start/end location IDs are only set once the first GPS fix arrives
                // (up to LOCATION_UPDATE_INTERVAL later). Persist the trip regardless so a short
                // trip started+stopped before any fix (or with no signal) still shows in the list —
                // keep the IDs only when the range is valid, else null so getPathPointsForTrack
                // falls back to the stored pathPoints string.
                val hasValidRange = startId != null && endId != null && endId >= startId
                // OBD Phase 2: derive the trip's fuel total from the obd_sample rows recorded
                // during the trip window (there is no live trip row/id to accumulate into —
                // see IMPLEMENTATION-ISSUES #1). 0.0 when OBD was not connected (no samples).
                val tripFuelConsumedL = integrateFuelLiters(
                    obdSampleDao.samplesBetweenOnce(current.tripStartedAt, System.currentTimeMillis())
                )
                insertTrack(
                    TrackEntity(
                        timestamp = current.tripStartedAt,
                        distance = current.distanceInMeters,
                        duration = current.durationTimer,
                        pathPoints = LocationUtils.pathPointsToString(current.pathPoints),
                        startLocationId = if (hasValidRange) startId else null,
                        endLocationId = if (hasValidRange) endId else null,
                        obdFuelConsumedL = tripFuelConsumedL
                    )
                )
                sendServiceCommand(Actions.STOP_TRIP)
            } else if (!current.isTracking) {
                sendServiceCommand(Actions.START_TRIP)
            }
            // PAUSED → LIVE resumption requires a RESUME_TRIP service action (future phase)
        }
    }

    private fun sendServiceCommand(action: Actions) {
        Intent(appContext, TrackingService::class.java).also {
            it.action = action.name
            ContextCompat.startForegroundService(appContext, it)
        }
    }

    fun deleteTrack(item: TrackEntity) {
        viewModelScope.launch {
            databaseDao.deleteTrack(item)
        }
    }

    fun getTrack(idx: Int) {
        job?.cancel()
        job = viewModelScope.launch {
            databaseDao.getTrackById(idx).distinctUntilChanged().collect { track ->
                selectedTrackState = track
                selectedTrackPathPoints = getPathPointsForTrack(track)
            }
        }
    }

    private suspend fun getPathPointsForTrack(track: TrackEntity): List<LatLng> {
        val startLocationId = track.startLocationId
        val endLocationId = track.endLocationId
        if (startLocationId != null && endLocationId != null) {
            return locationDao.getLocationsByRangeOnce(startLocationId, endLocationId)
                .map { LatLng(it.latitude, it.longitude) }
        }

        return com.kolee.tracklocation.utils.LocationUtils.stringToPathPoints(track.pathPoints)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrackApp)
                ShareViewModel(
                    appContext = application.applicationContext,
                    databaseDao = application.databaseDao,
                    locationDao = application.locationDao,
                    sessionDao = application.sessionDao,
                    obdSampleDao = application.obdSampleDao
                )
            }
        }
    }
}
