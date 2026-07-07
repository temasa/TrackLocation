package com.kolee.tracklocation.feature.obd

import com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fuel Cost (FR-12) — process-lifetime singleton owning the shared current fuel price
 * (IDR per litre) plus an in-memory multi-step undo/redo history.
 *
 * Only the current price is persisted (via [ObdPreferencesDataStore.setObdFuelPricePerLiter]);
 * the undo/redo history is intentionally in-memory only and resets on process restart.
 * On first [attach] the history is seeded from the persisted current price so cost display and
 * undo/redo start from the last saved value.
 */
object FuelPriceController {

    data class FuelPriceState(
        val currentPrice: Double,
        val canUndo: Boolean,
        val canRedo: Boolean,
    )

    // Internal coroutine scope for persistence writes only.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory price history; currentPrice = history[index] (0.0 when empty).
    private val history: MutableList<Double> = mutableListOf()
    private var index: Int = -1

    private var prefs: ObdPreferencesDataStore? = null
    private var attached = false

    private val _state = MutableStateFlow(FuelPriceState(currentPrice = 0.0, canUndo = false, canRedo = false))
    val state: StateFlow<FuelPriceState> = _state.asStateFlow()

    private fun currentPrice(): Double = if (index in history.indices) history[index] else 0.0

    private fun canUndo(): Boolean = index > 0

    private fun canRedo(): Boolean = index >= 0 && index < history.lastIndex

    private fun emitState() {
        _state.value = FuelPriceState(
            currentPrice = currentPrice(),
            canUndo = canUndo(),
            canRedo = canRedo(),
        )
    }

    private fun persistCurrent() {
        val p = prefs ?: return
        val price = currentPrice()
        scope.launch { p.setObdFuelPricePerLiter(price) }
    }

    /**
     * Idempotent. On the first call, captures [prefs] and seeds the history from the FIRST
     * persisted price value. Safe to call from every screen's LaunchedEffect(Unit).
     */
    fun attach(prefs: ObdPreferencesDataStore) {
        if (attached) return
        attached = true
        this.prefs = prefs
        scope.launch {
            val seed = prefs.obdFuelPricePerLiter.first()
            history.clear()
            history.add(seed)
            index = 0
            emitState()
        }
    }

    /** Set a new current price. No-op if equal to the current price. Truncates any redo tail. */
    fun set(newPrice: Double) {
        if (index in history.indices && history[index] == newPrice) return
        if (index < history.lastIndex) {
            // Drop the redo tail before appending the new value.
            while (history.lastIndex > index) history.removeAt(history.lastIndex)
        }
        history.add(newPrice)
        index = history.lastIndex
        persistCurrent()
        emitState()
    }

    fun undo() {
        if (index > 0) {
            index--
            persistCurrent()
            emitState()
        }
    }

    fun redo() {
        if (index < history.lastIndex) {
            index++
            persistCurrent()
            emitState()
        }
    }
}
