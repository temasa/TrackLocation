package com.kolee.tracklocation.feature.obd

import com.kolee.tracklocation.data.roomdb.FuelPriceDao
import com.kolee.tracklocation.data.roomdb.FuelPriceEntity
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
 * Fuel Cost (ADR-008) — process-lifetime singleton owning the shared current fuel price
 * (IDR per litre) plus an in-memory multi-step undo/redo history.
 *
 * Persistence is the `fuel_price` table (effective-dated, append-only): every Save/Undo/Redo
 * appends a new row with `effectiveFromMs = now`, so completed-trip costs (priced at the trip's
 * start time) never change retroactively. The in-memory stack still drives which value is
 * "current" for the active session/trip. On first [attach], a one-time seed migrates the legacy
 * DataStore scalar into the table if it is still empty.
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

    private var fuelPriceDao: FuelPriceDao? = null
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
        val dao = fuelPriceDao ?: return
        val price = currentPrice()
        scope.launch {
            dao.insert(FuelPriceEntity(pricePerLiter = price, effectiveFromMs = System.currentTimeMillis()))
        }
    }

    /**
     * Idempotent. On the first call, captures [fuelPriceDao] and seeds the history from the
     * current row in the `fuel_price` table (seeding the table from the legacy DataStore scalar
     * first, if the table is still empty). Safe to call from every screen's LaunchedEffect(Unit).
     */
    fun attach(fuelPriceDao: FuelPriceDao, prefs: ObdPreferencesDataStore) {
        if (attached) return
        attached = true
        this.fuelPriceDao = fuelPriceDao
        scope.launch {
            if (fuelPriceDao.count() == 0) {
                val scalar = prefs.obdFuelPricePerLiter.first()
                if (scalar > 0.0) {
                    fuelPriceDao.insert(FuelPriceEntity(pricePerLiter = scalar, effectiveFromMs = 0L))
                }
            }
            val cur = fuelPriceDao.currentPriceOnce()?.pricePerLiter ?: 0.0
            history.clear()
            history.add(cur)
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
