package com.kolee.tracklocation.feature.obd

import com.kolee.tracklocation.data.roomdb.FUEL_EVENT_FILL_UP
import com.kolee.tracklocation.data.roomdb.FuelEventDao
import com.kolee.tracklocation.data.roomdb.FuelEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ADR-027 / ADR-029: the fill-up write, shared by the in-app Fuel Dock and the overlay service.
 * A changed price is saved as a new effective-now price (ADR-008); the litres go in as a
 * FILL_UP fuel event. Safe to call from any coroutine.
 */
class FillUpRecorder(private val fuelEventDao: FuelEventDao) {

    suspend fun record(litres: Double, price: Double?) {
        // Price update stays on the main thread, as the Fuel Dock always did. `immediate` runs inline
        // when already on main (viewModelScope), so there is no extra dispatch hop.
        val changedPrice = withContext(Dispatchers.Main.immediate) {
            price?.takeIf { it > 0.0 && it != FuelPriceController.state.value.currentPrice }
                ?.also { FuelPriceController.set(it) }
        }
        withContext(Dispatchers.IO) {
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
        /** Same rules as FillUpDialog's litres field: digits and one '.', comma read as '.', > 0. */
        fun parseLitres(text: String): Double? {
            var seenDot = false
            val sb = StringBuilder()
            for (c in text) {
                when {
                    c.isDigit() -> sb.append(c)
                    (c == '.' || c == ',') && !seenDot -> {
                        seenDot = true
                        sb.append('.')
                    }
                }
            }
            return sb.toString().toDoubleOrNull()?.takeIf { it > 0.0 }
        }

        /** Same rules as FillUpDialog's price field: digits only, > 0. */
        fun parsePrice(text: String): Double? =
            text.filter { it.isDigit() }.toDoubleOrNull()?.takeIf { it > 0.0 }
    }
}
