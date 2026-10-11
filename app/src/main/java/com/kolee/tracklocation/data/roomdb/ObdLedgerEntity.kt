package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ADR-027 Decision 13: session-independent running total of OBD-measured fuel (litres, idle
 * included). Singleton row with [id] = 1. No foreign keys.
 */
@Entity(tableName = "obd_ledger")
data class ObdLedgerEntity(
    @PrimaryKey val id: Int = 1,
    val fuelConsumedL: Double = 0.0
)
