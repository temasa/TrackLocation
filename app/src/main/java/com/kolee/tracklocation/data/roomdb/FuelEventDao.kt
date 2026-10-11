package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * ADR-027: append-only access to `fuel_event`. No update or delete methods by design.
 */
@Dao
interface FuelEventDao {

    @Insert
    suspend fun insert(event: FuelEventEntity): Long

    /** Anchor for the fuel balance: the most recent RESET event, or null if none. */
    @Query("SELECT * FROM fuel_event WHERE kind = 'RESET' ORDER BY createdAtMs DESC, id DESC LIMIT 1")
    fun latestReset(): Flow<FuelEventEntity?>

    /** Sum of FILL_UP litres recorded strictly after [afterMs] (the latest Reset's createdAtMs). */
    @Query("SELECT COALESCE(SUM(litres), 0.0) FROM fuel_event WHERE kind = 'FILL_UP' AND createdAtMs > :afterMs")
    fun fillUpLitresAfter(afterMs: Long): Flow<Double>
}
