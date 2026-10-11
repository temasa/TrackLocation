package com.kolee.tracklocation.feature.obd

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ADR-027 Decision 5: process-lifetime holder of the RANGE efficiency, the average km/L over the
 * last [RANGE_WINDOW_KM] of MOVING driving. In memory only (null after a process restart, until
 * [MIN_WINDOW_KM] have been driven). Fed by ObdPollingService; read by FuelDockViewModel.
 * Idle/stationary polls never enter the window, so the last value is held while stopped.
 */
object RollingEfficiency {

    const val RANGE_WINDOW_KM = 1.0
    private const val MIN_WINDOW_KM = 0.2
    private const val MIN_WINDOW_LITRES = 0.01
    private const val MIN_MOVING_SPEED_KMH = 2.0

    private class Entry(val litres: Double, val coveredKm: Double)

    private val window = ArrayDeque<Entry>()
    private var windowLitres = 0.0
    private var windowKm = 0.0

    private val _avgKmL = MutableStateFlow<Double?>(null)
    val avgKmL: StateFlow<Double?> = _avgKmL.asStateFlow()

    /** Add one fuel-covered poll interval; only moving intervals with covered distance count. */
    @Synchronized
    fun onInterval(litres: Double, coveredKm: Double, gpsSpeedKmh: Double) {
        if (coveredKm <= 0.0 || gpsSpeedKmh < MIN_MOVING_SPEED_KMH) return
        window.addLast(Entry(litres, coveredKm))
        windowLitres += litres
        windowKm += coveredKm
        // Trim the oldest entries while the rest still spans at least the window length.
        while (window.size > 1 && windowKm - window.first().coveredKm >= RANGE_WINDOW_KM) {
            val old = window.removeFirst()
            windowLitres -= old.litres
            windowKm -= old.coveredKm
        }
        if (windowKm >= MIN_WINDOW_KM && windowLitres > MIN_WINDOW_LITRES) {
            _avgKmL.value = windowKm / windowLitres
        }
        // Otherwise hold the previously published value.
    }

    /** Drop the window on adapter (re)connect; the held value is kept. */
    @Synchronized
    fun clearWindow() {
        window.clear()
        windowLitres = 0.0
        windowKm = 0.0
    }
}
