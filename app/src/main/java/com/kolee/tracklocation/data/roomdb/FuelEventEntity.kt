package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

/** ADR-027: kind of a fuel-dock event. */
const val FUEL_EVENT_RESET = "RESET"
const val FUEL_EVENT_FILL_UP = "FILL_UP"

/**
 * ADR-027: append-only fuel-dock log. [litres] is the starting level for a RESET and the litres
 * added for a FILL_UP. [resetBaselineL] is set only on RESET rows: the cumulative OBD fuel total
 * (obd_ledger.fuelConsumedL, ADR-027 Decision 13) at the moment of the Reset; NULL for FILL_UP.
 * [pricePerLiter] is set only when the user changed the price in the Fill up dialog. No foreign
 * keys (same convention as observer_trip and trip_planned_route).
 */
@Entity(tableName = "fuel_event")
data class FuelEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val kind: String,
    val litres: Double,
    val pricePerLiter: Double? = null,
    val createdAtMs: Long,
    val resetBaselineL: Double? = null
)
