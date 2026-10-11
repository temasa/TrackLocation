package com.kolee.tracklocation.feature.observer.trip.route

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.kolee.tracklocation.feature.observer.trip.OrderCard
import com.kolee.tracklocation.feature.observer.trip.OrderPhase
import com.kolee.tracklocation.feature.observer.trip.isTerminal
import com.kolee.tracklocation.tracking.LocationUiState
import kotlinx.coroutines.CancellationException
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

/** ADR-026: a saved planned route as read back for hydration. */
data class StoredPlan(val points: List<LatLng>, val distanceM: Int?)

/** ADR-026: persistence port for the frozen planned route (device-only, keyed by trip start). */
interface PlannedRouteStore {
    suspend fun save(tripStartedAt: Long, kind: String, route: RouteResult, originSource: String)
    suspend fun load(tripStartedAt: Long): StoredPlan?
}

/**
 * ADR-016: geocodes the active order's pickup/drop, fetches a frozen planned route
 * (accepted point -> pickup -> drop) and a runtime route (current -> next stop) that is re-fetched on
 * phase change or when the driver leaves the polyline (throttled). Only the planned route is
 * persisted (ADR-026, write-at-fetch, hydrated after a restart); the runtime route never is. State
 * is cleared when the order ends. Failures leave the state empty and the trip unaffected.
 */
class OrderRouteController(
    scope: CoroutineScope,
    private val client: RouteProvider,
    activeOrder: StateFlow<OrderCard?>,
    private val location: StateFlow<LocationUiState>,
    /**
     * ADR-017 amendment: the point where the order was accepted (trip-start fix, else first path
     * point), used as the planned-route origin. Null falls back to the live fix.
     */
    private val acceptedOrigin: suspend () -> LatLng?,
    /** ADR-026: where the frozen planned route is saved / hydrated from. */
    private val plannedRouteStore: PlannedRouteStore,
    /** ADR-026: `LocationUiState.tripStartedAt` of the running trip (0 = none recorded). */
    private val tripStartedAt: () -> Long
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
    private var hydrateTried = false
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
        hydrateTried = false
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
            Log.d(TAG, "order active phase=${order.phase}")
            hydratePlanned() // ADR-026: show the saved plan before geocode completes
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

    /**
     * ADR-026: restore the frozen plan saved for the running trip, if any, and do not re-plan.
     * Needs a recorded trip start; retried later (from [step]) until one exists. Fail-soft.
     */
    private suspend fun hydratePlanned() {
        if (hydrateTried || plannedDone) return
        val startedAt = tripStartedAt()
        if (startedAt <= 0L) return
        val stored = try {
            plannedRouteStore.load(startedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "planned hydrate failed: ${e.javaClass.simpleName}")
            null
        }
        hydrateTried = true
        if (stored != null && stored.points.size > 1) {
            Log.d(TAG, "planned route hydrated points=${stored.points.size}")
            plannedDone = true
            _state.value = _state.value.copy(plannedRoute = stored.points)
        }
    }

    private suspend fun step(order: OrderCard, key: String, pickupAddr: String, dropAddr: String, loc: LatLng) {
        val now = System.currentTimeMillis()
        hydratePlanned() // ADR-026: no-op once tried or a plan exists

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
                Log.w(TAG, "geocode failed attempt=${geocodeAttempts[key]}")
                return
            }
            geo = Pair(p, d)
            geocodeCache[key] = geo
            Log.d(TAG, "geocode ok attempt=${geocodeAttempts[key]}")
            if (distanceM(p, d) < ARRIVED_METERS) {
                Log.w(TAG, "pickup and drop resolve to the same point")
            }
        }
        val (pickup, drop) = geo
        if (plannedDone && _state.value.pickup == null) {
            // ADR-026: hydrated plan; markers follow the (re)geocode.
            _state.value = _state.value.copy(pickup = pickup, drop = drop)
        }

        // 2. Planned route: once per order, then frozen.
        if (!plannedDone && now - plannedFailedAt >= RETRY_MIN_MS) {
            val accepted = acceptedOrigin()
            val origin = accepted ?: loc
            val planned = if (order.phase == OrderPhase.DROP) {
                client.route(origin, drop)
            } else {
                client.route(origin, drop, waypoint = pickup)
            }
            coroutineContext.ensureActive()
            val originSrc = if (accepted != null) "accepted" else "fallback-live"
            if (planned != null) {
                Log.d(TAG, "planned route ok points=${planned.points.size} origin=$originSrc")
                plannedDone = true
                _state.value = _state.value.copy(plannedRoute = planned.points, pickup = pickup, drop = drop)
                persistPlanned(planned, if (accepted != null) "ACCEPTED" else "FALLBACK_LIVE")
            } else {
                Log.w(TAG, "planned route failed origin=$originSrc")
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
            Log.d(TAG, "runtime route ok points=${route.points.size}")
            _state.value = _state.value.copy(runtimeRoute = route.points) // ADR-026: never persisted
        } else {
            Log.w(TAG, "runtime route failed")
        }
    }

    /** ADR-026: save the frozen plan keyed by the trip start; failures never break the overlay. */
    private suspend fun persistPlanned(route: RouteResult, originSource: String) {
        val startedAt = tripStartedAt()
        if (startedAt <= 0L) {
            Log.w(TAG, "planned route not persisted: tripStartedAt=0")
            return
        }
        try {
            plannedRouteStore.save(startedAt, "ORDER", route, originSource)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "planned route persist failed: ${e.javaClass.simpleName}")
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
        const val TAG = "OrderRouteController"
        const val LOCATION_SAMPLE_MS = 5_000L
        const val RETRY_MIN_MS = 60_000L
        const val RUNTIME_MIN_INTERVAL_MS = 30_000L
        const val RUNTIME_MIN_MOVE_M = 100f
        const val OFF_ROUTE_METERS = 40.0
        const val ARRIVED_METERS = 30f
        const val MAX_GEOCODE_ATTEMPTS = 3
    }
}
