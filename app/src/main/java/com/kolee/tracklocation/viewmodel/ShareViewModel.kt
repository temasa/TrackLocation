package com.kolee.tracklocation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.maps.model.LatLng
import com.kolee.tracklocation.data.roomdb.LocationDao
import com.kolee.tracklocation.data.roomdb.SessionDao
import com.kolee.tracklocation.data.roomdb.SessionEntity
import com.kolee.tracklocation.TrackApp
import com.kolee.tracklocation.data.roomdb.TrackDao
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.tracking.TrackingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ShareViewModel(
    private val databaseDao: TrackDao,
    private val locationDao: LocationDao,
    private val sessionDao: SessionDao
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
                    databaseDao = application.databaseDao,
                    locationDao = application.locationDao,
                    sessionDao = application.sessionDao
                )
            }
        }
    }
}
