package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ADR-008: fuel price as an effective-dated entity. Append-only log — the current price is the
 * row with the greatest [effectiveFromMs]; completed-trip cost uses the row effective at the
 * trip's start timestamp.
 */
@Entity(tableName = "fuel_price", indices = [Index("effectiveFromMs")])
data class FuelPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pricePerLiter: Double,
    val effectiveFromMs: Long
)
