package com.kolee.tracklocation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kolee.tracklocation.TrackApp
import com.kolee.tracklocation.data.roomdb.FUEL_EVENT_FILL_UP
import com.kolee.tracklocation.data.roomdb.FUEL_EVENT_RESET
import com.kolee.tracklocation.data.roomdb.FuelEventDao
import com.kolee.tracklocation.data.roomdb.FuelEventEntity
import com.kolee.tracklocation.data.roomdb.SessionDao
import com.kolee.tracklocation.feature.obd.FuelPriceController
import com.kolee.tracklocation.feature.obd.data.ObdPreferencesDataStore
import com.kolee.tracklocation.utils.FuelBalance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ADR-027: values shown by the fuel dock. Null means "not known yet" (rendered as an em dash). */
data class FuelDockUiState(
    val fuelLeftL: Double? = null,
    val rangeKm: Double? = null,
    val price: Double = 0.0,
    val level: FuelBalance.FuelLevel = FuelBalance.FuelLevel.Normal,
    val lowConfidence: Boolean = false,
)

private data class ResetAndFills(
    val resetLitres: Double?,
    val resetBaselineL: Double?,
    val fillUpLitres: Double,
)

private data class ActiveSessionStats(
    val fuelL: Double,
    val coveredKm: Double,
    val distanceKm: Double,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FuelDockViewModel(
    private val fuelEventDao: FuelEventDao,
    private val sessionDao: SessionDao,
) : ViewModel() {

    val uiState: StateFlow<FuelDockUiState> = combine(
        fuelEventDao.latestReset().flatMapLatest { reset ->
            if (reset == null) {
                flowOf(ResetAndFills(null, null, 0.0))
            } else {
                fuelEventDao.fillUpLitresAfter(reset.createdAtMs).map { fills ->
                    ResetAndFills(reset.litres, reset.resetBaselineL, fills)
                }
            }
        },
        sessionDao.totalObdFuelConsumedL(),
        sessionDao.getAllSessions().map { sessions ->
            val active = sessions.firstOrNull { it.isActive }
            ActiveSessionStats(
                fuelL = active?.obdFuelConsumedL ?: 0.0,
                coveredKm = active?.obdCoveredDistanceKm ?: 0.0,
                distanceKm = (active?.distanceMeters ?: 0.0) / 1000.0,
            )
        },
        FuelPriceController.state.map { it.currentPrice },
    ) { resetAndFills, cumulativeNow, active, price ->
        val fuelLeft = FuelBalance.fuelLeft(
            resetAndFills.resetLitres,
            resetAndFills.resetBaselineL,
            resetAndFills.fillUpLitres,
            cumulativeNow,
        )
        val avg = FuelBalance.avgKmL(active.coveredKm, active.fuelL)
        FuelDockUiState(
            fuelLeftL = fuelLeft,
            rangeKm = FuelBalance.rangeKm(fuelLeft, avg),
            price = price,
            level = FuelBalance.level(fuelLeft),
            lowConfidence = FuelBalance.isLowConfidence(active.distanceKm, active.coveredKm),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelDockUiState())

    /** ADR-027: store the starting level and the current cumulative OBD total as the baseline. */
    fun reset(litres: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val baseline = sessionDao.totalObdFuelConsumedLOnce()
            fuelEventDao.insert(
                FuelEventEntity(
                    kind = FUEL_EVENT_RESET,
                    litres = litres,
                    createdAtMs = System.currentTimeMillis(),
                    resetBaselineL = baseline,
                )
            )
        }
    }

    /** ADR-027: add litres; a changed price is saved as a new effective-now price (ADR-008). */
    fun fillUp(litres: Double, price: Double?) {
        val changedPrice = price?.takeIf { it > 0.0 && it != FuelPriceController.state.value.currentPrice }
        if (changedPrice != null) FuelPriceController.set(changedPrice)
        viewModelScope.launch(Dispatchers.IO) {
            fuelEventDao.insert(
                FuelEventEntity(
                    kind = FUEL_EVENT_FILL_UP,
                    litres = litres,
                    pricePerLiter = changedPrice,
                    createdAtMs = System.currentTimeMillis(),
                )
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrackApp
                // Idempotent; other screens call it too.
                FuelPriceController.attach(app.fuelPriceDao, ObdPreferencesDataStore(app))
                FuelDockViewModel(app.fuelEventDao, app.sessionDao)
            }
        }
    }
}
