package com.kolee.tracklocation.feature.overlay

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.kolee.tracklocation.TrackApp
import com.kolee.tracklocation.feature.obd.FuelPriceController
import com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.feature.obd.service.ObdUiState
import com.kolee.tracklocation.feature.overlay.data.OverlayPreferencesDataStore
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.viewmodel.FuelDockViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private data class ObdOverlayValues(val avgKmL: Double?, val instantKmL: Double?)

private data class OverlayPrefs(val collapsed: Boolean, val circleX: Int, val circleY: Int)

/**
 * ADR-029 Decision 4: builds [state] for the overlay from the same sources the app screens use.
 * Owned by [com.kolee.tracklocation.feature.overlay.service.OverlayService]; [close] must be called from its onDestroy.
 *
 * FuelDockViewModel is created without an Activity: a private [ViewModelStore] plus the existing
 * [FuelDockViewModel.Factory], with the TrackApp supplied through APPLICATION_KEY in the creation extras.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OverlayStateHolder(app: TrackApp, scope: CoroutineScope) {

    private val viewModelStore = ViewModelStore()
    private val overlayPrefs = OverlayPreferencesDataStore(app)

    private val fuelDock: FuelDockViewModel

    init {
        // Idempotent; the Factory also attaches, this keeps it explicit for the service process path.
        FuelPriceController.attach(app.fuelPriceDao, ObdPreferencesDataStore(app))
        val extras = MutableCreationExtras().apply {
            set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, app)
        }
        fuelDock = ViewModelProvider(viewModelStore, FuelDockViewModel.Factory, extras)[FuelDockViewModel::class.java]
    }

    // Non-Connected => avg/instant null. Instant km/L uses the same gate as TrackScreen.
    private val obdValues: Flow<ObdOverlayValues> = combine(
        ObdPollingService.obdUiState,
        TrackingService.locationUiState
    ) { obd, loc ->
        val connected = obd as? ObdUiState.Connected
        ObdOverlayValues(
            avgKmL = connected?.tripAvgKmL,
            instantKmL = connected?.instantKmL
                ?.takeIf { loc.speedInKMH > 3f && loc.accuracyMeters <= 20f }
        )
    }.distinctUntilChanged()

    private val activeSession = app.sessionDao.getAllSessions()
        .map { sessions -> sessions.firstOrNull { it.isActive } }
        .distinctUntilChanged()

    // Order trips since the session start; prices are suspend lookups so they are pre-fetched per trip.
    private val totals: Flow<SessionTotals> = activeSession.flatMapLatest { session ->
        if (session == null) {
            flowOf(EmptySessionTotals)
        } else {
            app.databaseDao.getSessionOrderTracks(session.startedAt).map { tracks ->
                val prices = HashMap<Long, Double?>()
                for (t in tracks) prices[t.timestamp] = app.fuelPriceDao.priceEffectiveAt(t.timestamp)?.pricePerLiter
                computeSessionTotals(tracks.map { it.toOrderInput { ts -> prices[ts] } })
            }
        }
    }

    private val prefs: Flow<OverlayPrefs> = combine(
        overlayPrefs.collapsed,
        overlayPrefs.circleX,
        overlayPrefs.circleY
    ) { collapsed, x, y -> OverlayPrefs(collapsed, x, y) }

    // Eagerly: the holder lives exactly as long as the service scope, and the window host collects it continuously.
    val state: StateFlow<OverlayUiState> = combine(
        fuelDock.uiState,
        obdValues,
        activeSession.map { it != null },
        totals,
        prefs
    ) { dock, obd, active, totals, prefs ->
        OverlayUiState(
            sessionActive = active,
            fuelLeftL = dock.fuelLeftL,
            rangeKm = dock.rangeKm,
            avgKmL = obd.avgKmL,
            instantKmL = obd.instantKmL,
            totals = totals,
            collapsed = prefs.collapsed,
            circleX = prefs.circleX,
            circleY = prefs.circleY
        )
    }.stateIn(scope, SharingStarted.Eagerly, OverlayUiState())

    suspend fun setCollapsed(collapsed: Boolean) = overlayPrefs.setCollapsed(collapsed)

    suspend fun saveCirclePosition(x: Int, y: Int) = overlayPrefs.setCirclePosition(x, y)

    /** Clears the ViewModel (cancels its viewModelScope). */
    fun close() = viewModelStore.clear()
}
