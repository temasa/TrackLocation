package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** ADR-027 Decision 13: access to the singleton `obd_ledger` row. */
@Dao
abstract class ObdLedgerDao {

    @Query("INSERT OR IGNORE INTO obd_ledger (id, fuelConsumedL) VALUES (1, 0.0)")
    abstract suspend fun seed()

    @Query("UPDATE obd_ledger SET fuelConsumedL = fuelConsumedL + :litres WHERE id = 1")
    abstract suspend fun increment(litres: Double)

    /** Adds [litres] to the ledger, creating the row first if it is missing. */
    @Transaction
    open suspend fun addFuel(litres: Double) {
        seed()
        increment(litres)
    }

    /** Cumulative OBD fuel (L); 0.0 when the row does not exist yet. */
    @Query("SELECT COALESCE((SELECT fuelConsumedL FROM obd_ledger WHERE id = 1), 0.0)")
    abstract fun totalFuelL(): Flow<Double>

    /** One-shot read of [totalFuelL], used when a Reset stores its baseline. */
    @Query("SELECT COALESCE((SELECT fuelConsumedL FROM obd_ledger WHERE id = 1), 0.0)")
    abstract suspend fun totalFuelLOnce(): Double
}
