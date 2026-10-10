package com.kolee.tracklocation.feature.observer.trip.route

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.kolee.tracklocation.feature.observer.trip.OrderCard
import com.kolee.tracklocation.feature.observer.trip.OrderPhase
import com.kolee.tracklocation.feature.observer.trip.isTerminal
import com.kolee.tracklocation.tracking.LocationUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/** ADR-016 (provisional): display-only route overlay state for the active Gojek order. */
data class OrderRouteState(
    val plannedRoute: List<LatLng> = emptyList(),
    val runtimeRoute: List<LatLng> = emptyList(),
    val pickup: LatLng? = null,
    val drop: LatLng? = null
)

/**
 * ADR-016: geocodes the active order's pickup/drop, fetches a frozen planned route
 * (current -> pickup -> drop) and a runtime route (current -> next stop) that is re-fetched on
 * phase change or when the driver leaves the polyline (throttled). Never persisted; cleared when
 * the order ends. Failures leave the state empty and the trip unaffected.
 */
class OrderRouteController(
    scope: CoroutineScope,
    private val client: RouteProvider,
    activeOrder: StateFlow<OrderCard?>,
    private val location: StateFlow<LocationUiState>
) {
    private val _state = MutableStateFlow(OrderRouteState())
    val state: StateFlow<OrderRouteState> = _state.asStateFlow()

    // Per-order bookkeeping. Only touched from the single collectLatest block (cancelAndJoin
    // semantics guarantee one handler at a time).
    private val geocodeCache = ConcurrentHashMap<String, Pair<LatLng, LatLng>>() // read by ShareViewModel at trip stop (ADR-023)
    private val geocodeFailedAt = HashMap<String, Long>()
    private val geocodeAttempts = HashMap<String, Int>()
    private var activeKey: String? = null
    private var lastPhase: OrderPhase? = null
    private var plannedDone = false
    private var plannedFailedAt = 0L
    private var lastRuntimeAt = 0L
    private var runtimeOrigin: LatLng? = null
    private var forceRuntime = false

    init {
        scope.launch {
            activeOrder
                .map { o ->
                    o?.takeIf {
                        !it.phase.isTerminal() &&
                            !it.pickupAddress.isNullOrBlank() && !it.dropAddress.isNullOrBlank()
                    }
                }
                .distinctUntilChanged()
                .collectLatest { order ->
                    if (order == null) {
                        reset(null)
                    } else {
                        handle(order)
                    }
                }
        }
    }

    /** ADR-023: the cached (pickup, drop) geocode of [order], or null if never resolved. No new calls. */
    fun cachedGeo(order: OrderCard): Pair<LatLng, LatLng>? {
        val pickupAddr = order.pickupAddress ?: return null
        val dropAddr = order.dropAddress ?: return null
        return geocodeCache[cacheKey(pickupAddr, dropAddr)]
    }

    private fun cacheKey(pickupAddr: String, dropAddr: String) = pickupAddr + "\u0000" + dropAddr

    private fun reset(key: String?) {
        _state.value = OrderRouteState()
        activeKey = key
        lastPhase = null
        plannedDone = false
        plannedFailedAt = 0L
        lastRuntimeAt = 0L
        runtimeOrigin = null
        forceRuntime = false
    }

    private suspend fun handle(order: OrderCard) {
        val pickupAddr = order.pickupAddress ?: return
        val dropAddr = order.dropAddress ?: return
        val key = cacheKey(pickupAddr, dropAddr)
        if (key != activeKey) {
            reset(key)
        } else if (order.phase != lastPhase) {
            // Phase change: retarget immediately, ignoring the throttle.
            forceRuntime = true
        }
        lastPhase = order.phase

        location
            .map { s -> s.takeIf { isRealFix(it) }?.currentLocation }
            .filterNotNull()
            .collect { loc ->
                step(order, key, pickupAddr, dropAddr, loc)
                delay(LOCATION_SAMPLE_MS) // cheap sampling: StateFlow keeps only the latest fix
            }
    }

    private suspend fun step(order: OrderCard, key: String, pickupAddr: String, dropAddr: String, loc: LatLng) {
        val now = System.currentTimeMillis()

        // 1. Geocode (cached per order key; failures retried at most once per 60 s).
        var geo = geocodeCache[key]
        if (geo == null) {
            if ((geocodeAttempts[key] ?: 0) >= MAX_GEOCODE_ATTEMPTS) return // ADR-017: quota guard
            if (now - (geocodeFailedAt[key] ?: 0L) < RETRY_MIN_MS) return
            geocodeAttempts[key] = (geocodeAttempts[key] ?: 0) + 1
            val p = client.geocode(order.pickupName, pickupAddr, loc)
            val d = if (p != null) client.geocode(order.dropName, dropAddr, loc) else null
            coroutineContext.ensureActive()
            if (p == null || d == null) {
                geocodeFailedAt[key] = System.currentTimeMillis()
                return
            }
            geo = Pair(p, d)
            geocodeCache[key] = geo
        }
        val (pickup, drop) = geo

        // 2. Planned route: once per order, then frozen.
        if (!plannedDone && now - plannedFailedAt >= RETRY_MIN_MS) {
            val planned = if (order.phase == OrderPhase.DROP) {
                client.route(loc, drop)
            } else {
                client.route(loc, drop, waypoint = pickup)
            }
            coroutineContext.ensureActive()
            if (planned != null) {
                plannedDone = true
                _state.value = _state.value.copy(plannedRoute = planned, pickup = pickup, drop = drop)
            } else {
                plannedFailedAt = System.currentTimeMillis()
            }
        }

        // 3. Runtime route to the next stop.
        val target = if (order.phase == OrderPhase.PICKUP) pickup else drop
        if (order.phase == OrderPhase.PICKUP && distanceM(loc, target) < ARRIVED_METERS) return

        val force = forceRuntime
        val current = _state.value.runtimeRoute
        if (force) {
            // The old polyline leads to the previous target; drop it right away.
            _state.value = _state.value.copy(runtimeRoute = emptyList())
        }
        val shouldFetch = when {
            force -> true
            current.isEmpty() -> now - lastRuntimeAt >= RUNTIME_MIN_INTERVAL_MS
            else -> {
                val off = !PolyUtil.isLocationOnPath(loc, current, true, OFF_ROUTE_METERS)
                val moved = runtimeOrigin?.let { distanceM(loc, it) >= RUNTIME_MIN_MOVE_M } ?: true
                off && now - lastRuntimeAt >= RUNTIME_MIN_INTERVAL_MS && moved
            }
        }
        if (!shouldFetch) return

        forceRuntime = false
        lastRuntimeAt = now
        runtimeOrigin = loc
        val route = client.route(loc, target)
        coroutineContext.ensureActive()
        if (route != null) {
            _state.value = _state.value.copy(runtimeRoute = route)
        }
    }

    /** The service seeds a placeholder location (Seoul) until the first fix; ignore it. */
    private fun isRealFix(s: LocationUiState): Boolean =
        s.currentLocation != LocationUiState().currentLocation &&
            !(s.currentLocation.latitude == 0.0 && s.currentLocation.longitude == 0.0)

    private fun distanceM(a: LatLng, b: LatLng): Float {
        val out = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out)
        return out[0]
    }

    private companion object {
        const val LOCATION_SAMPLE_MS = 5_000L
        const val RETRY_MIN_MS = 60_000L
        const val RUNTIME_MIN_INTERVAL_MS = 30_000L
        const val RUNTIME_MIN_MOVE_M = 100f
        const val OFF_ROUTE_METERS = 40.0
        const val ARRIVED_METERS = 30f
        const val MAX_GEOCODE_ATTEMPTS = 3
    }
}
