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
import com.kolee.tracklocation.data.roomdb.ObserverTripDao
import com.kolee.tracklocation.data.roomdb.ObserverTripEntity
import com.kolee.tracklocation.data.roomdb.SessionDao
import com.kolee.tracklocation.data.roomdb.SessionEntity
import com.kolee.tracklocation.TrackApp
import com.kolee.tracklocation.data.roomdb.TrackDao
import com.kolee.tracklocation.data.roomdb.TrackEntity
import com.kolee.tracklocation.feature.observer.trip.ORDER_ACTIVE_WINDOW_MS
import com.kolee.tracklocation.feature.observer.trip.OrderCard
import com.kolee.tracklocation.feature.observer.trip.OrderPhase
import com.kolee.tracklocation.feature.observer.trip.route.OpenRouteServiceClient
import com.kolee.tracklocation.feature.observer.trip.route.OrderRouteController
import com.kolee.tracklocation.feature.observer.trip.route.OrderRouteState
import com.kolee.tracklocation.feature.observer.trip.route.ReverseGeocodeResult
import com.kolee.tracklocation.tracking.Actions
import com.kolee.tracklocation.tracking.LocationUiState
import com.kolee.tracklocation.tracking.TrackingService
import com.kolee.tracklocation.utils.LocationUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ShareViewModel(
    private val appContext: Context,
    private val databaseDao: TrackDao,
    private val locationDao: LocationDao,
    private val sessionDao: SessionDao,
    private val obdSampleDao: ObdSampleDao,
    private val observerTripDao: ObserverTripDao
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

    // ADR-015: true while the running trip was started or adopted by an order. In-memory only.
    private var orderOwnsTrip = false
    // ADR-022: id of the observer_trip row that owns the running trip, read at stop for the label.
    private var orderOwnedId: Long? = null

    // ADR-014 (provisional): the Gojek order currently being served, or null. An order is "active"
    // while its latest row is not FINISHED and was seen within ORDER_ACTIVE_WINDOW_MS. The ticker
    // re-evaluates staleness even when the table doesn't change.
    val activeOrder: StateFlow<OrderCard?> = combine(
        observerTripDao.latestOrderFlow(),
        flow { while (true) { emit(System.currentTimeMillis()); delay(ORDER_STALENESS_TICK_MS) } }
    ) { row, now ->
        row?.takeIf {
            it.phase != OrderPhase.FINISHED.name && now - it.lastSeenAt <= ORDER_ACTIVE_WINDOW_MS
        }?.toOrderCard()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val routeProvider = OpenRouteServiceClient(appContext)

    // ADR-016 (provisional): planned + runtime route overlay for the active order (display only).
    private val orderRouteController = OrderRouteController(
        scope = viewModelScope,
        client = routeProvider,
        activeOrder = activeOrder,
        location = locationUiState
    )
    val orderRoute: StateFlow<OrderRouteState> = orderRouteController.state


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
     * ADR-007: delegates to the shared canonical integral in ObdFuelMath.
     */
    private fun integrateFuelLiters(samples: List<ObdSampleEntity>): Double =
        com.kolee.tracklocation.feature.obd.ObdFuelMath.integrateFuelLiters(samples)

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
        // ADR-015 start: a fresh, unfinished order starts a trip (or adopts one already running),
        // then is marked handled. A stale or already-finished order never starts a trip.
        viewModelScope.launch {
            observerTripDao.unhandledReadyFlow().collect { row ->
                if (row == null) return@collect
                if (row.phase != OrderPhase.FINISHED.name &&
                    System.currentTimeMillis() - row.lastSeenAt <= ORDER_ACTIVE_WINDOW_MS
                ) {
                    val current = locationUiState.value
                    if (!current.isTracking) sendServiceCommand(Actions.START_TRIP)
                    orderOwnsTrip = true
                    orderOwnedId = row.id
                }
                observerTripDao.markHandled(row.id)
            }
        }
        // ADR-015 end: the order reaching FINISHED (Drop off, Cancelled, Cleared or dismissed) ends
        // the trip only if the order owns it; a trip already stopped manually just clears the flag.
        viewModelScope.launch {
            observerTripDao.latestOrderFlow().collect { row ->
                if (row?.phase == OrderPhase.FINISHED.name && orderOwnsTrip) {
                    val current = locationUiState.value
                    if (current.isTracking && !current.isPaused) {
                        stopActiveTrip(current)
                    }
                    orderOwnsTrip = false
                    orderOwnedId = null
                }
            }
        }
    }

    /** Dismisses the active order card (e.g. a cancelled order that never reaches "Selesai"). */
    fun dismissActiveOrder() {
        viewModelScope.launch {
            observerTripDao.findLatestOpen()?.let { observerTripDao.dismiss(it.id) }
        }
    }

    fun onTripCtaTap() {
        val current = locationUiState.value
        viewModelScope.launch {
            if (current.isTracking && !current.isPaused) {
                stopActiveTrip(current)
            } else if (!current.isTracking) {
                sendServiceCommand(Actions.START_TRIP)
            }
            // PAUSED → LIVE resumption requires a RESUME_TRIP service action (future phase)
        }
    }

    /**
     * Persists the active trip, then asks the service to stop it. Shared by the CTA and the
     * ADR-015 order-end collector. Any stop (manual or order-driven) releases order ownership.
     */
    private suspend fun stopActiveTrip(current: LocationUiState) {
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
        // ADR-022: snapshot the owning order's label + earnings BEFORE ownership is released.
        val ownedId = orderOwnedId
        val orderRow = if (orderOwnsTrip && ownedId != null) observerTripDao.findById(ownedId) else null
        val orderLabel = orderRow?.let {
            "Gojek: ${it.pickupName ?: it.pickupAddress ?: "?"} → ${it.dropName ?: it.dropAddress ?: "?"}"
        }
        // ADR-023: pickup/drop coordinates from the route controller's cache (no new geocode call);
        // accepted point = the trip's start fix (location row, else first recorded path point).
        val cachedGeo = orderRow?.let { orderRouteController.cachedGeo(it.toOrderCard()) }
        val acceptedPoint = if (orderRow != null) {
            val startFix = if (hasValidRange && startId != null) {
                locationDao.getLocationByIdOnce(startId)?.let { LatLng(it.latitude, it.longitude) }
            } else null
            startFix ?: current.pathPoints.firstOrNull()
        } else null
        val tripIdx = databaseDao.insertTrack(
            TrackEntity(
                timestamp = current.tripStartedAt,
                distance = current.distanceInMeters,
                duration = current.durationTimer,
                pathPoints = LocationUtils.pathPointsToString(current.pathPoints),
                startLocationId = if (hasValidRange) startId else null,
                endLocationId = if (hasValidRange) endId else null,
                obdFuelConsumedL = tripFuelConsumedL,
                orderLabel = orderLabel,
                orderEarningsRp = orderRow?.earningsRp,
                acceptedLat = acceptedPoint?.latitude,
                acceptedLng = acceptedPoint?.longitude,
                pickupName = orderRow?.pickupName,
                pickupAddress = orderRow?.pickupAddress,
                pickupLat = cachedGeo?.first?.latitude,
                pickupLng = cachedGeo?.first?.longitude,
                dropName = orderRow?.dropName,
                dropAddress = orderRow?.dropAddress,
                dropLat = cachedGeo?.second?.latitude,
                dropLng = cachedGeo?.second?.longitude
            )
        )
        sendServiceCommand(Actions.STOP_TRIP)
        orderOwnsTrip = false
        orderOwnedId = null
        // ADR-023: the trip is already saved and STOP_TRIP sent; the accepted place is resolved
        // afterwards (off this flow, so the order collector is never blocked) and late-filled.
        if (acceptedPoint != null) {
            viewModelScope.launch { resolveAcceptedPlace(tripIdx.toInt(), acceptedPoint) }
        }
    }

    /** ADR-023: one reverse geocode (+ one retry), each bounded by a timeout; failure leaves NULLs. */
    private suspend fun resolveAcceptedPlace(tripIdx: Int, point: LatLng) {
        var result: ReverseGeocodeResult? = null
        for (attempt in 1..2) {
            result = withTimeoutOrNull(REVERSE_GEOCODE_TIMEOUT_MS) {
                routeProvider.reverseGeocode(point.latitude, point.longitude)
            }
            if (result != null) break
        }
        val place = result ?: return
        databaseDao.updateAcceptedPlace(tripIdx, place.name, place.address)
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

    private fun ObserverTripEntity.toOrderCard() = OrderCard(
        phase = runCatching { OrderPhase.valueOf(phase) }.getOrDefault(OrderPhase.PICKUP),
        pickupName = pickupName,
        pickupAddress = pickupAddress,
        dropName = dropName,
        dropAddress = dropAddress,
        payment = payment,
        earningsRp = earningsRp
    )

    companion object {
        private const val ORDER_STALENESS_TICK_MS = 60_000L
        private const val REVERSE_GEOCODE_TIMEOUT_MS = 8_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrackApp)
                ShareViewModel(
                    appContext = application.applicationContext,
                    databaseDao = application.databaseDao,
                    locationDao = application.locationDao,
                    sessionDao = application.sessionDao,
                    obdSampleDao = application.obdSampleDao,
                    observerTripDao = application.observerTripDao
                )
            }
        }
    }
}
