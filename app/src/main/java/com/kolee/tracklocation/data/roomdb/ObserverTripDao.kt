package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ObserverTripDao {

    @Insert
    suspend fun insert(trip: ObserverTripEntity): Long

    @Update
    suspend fun update(trip: ObserverTripEntity)

    @Query("SELECT * FROM observer_trip WHERE pickupAddress = :pickupAddress AND dropAddress = :dropAddress ORDER BY lastSeenAt DESC LIMIT 1")
    suspend fun findByAddresses(pickupAddress: String, dropAddress: String): ObserverTripEntity?

    @Query("SELECT * FROM observer_trip WHERE dropAddress = :dropAddress AND phase != 'FINISHED' ORDER BY lastSeenAt DESC LIMIT 1")
    suspend fun findLatestOpenByDropAddress(dropAddress: String): ObserverTripEntity?

    @Query("SELECT * FROM observer_trip WHERE phase != 'FINISHED' ORDER BY lastSeenAt DESC LIMIT 1")
    suspend fun findLatestOpen(): ObserverTripEntity?

    @Query("SELECT * FROM observer_trip ORDER BY lastSeenAt DESC LIMIT 1")
    fun latestOrderFlow(): Flow<ObserverTripEntity?>

    @Query(
        "SELECT * FROM observer_trip WHERE handled = 0 AND pickupAddress IS NOT NULL AND dropAddress IS NOT NULL " +
            "AND payment IS NOT NULL AND earningsRp IS NOT NULL ORDER BY lastSeenAt DESC LIMIT 1"
    )
    fun unhandledReadyFlow(): Flow<ObserverTripEntity?>

    @Query("UPDATE observer_trip SET handled = 1 WHERE id = :id")
    suspend fun markHandled(id: Long)

    @Query("UPDATE observer_trip SET phase = 'FINISHED', handled = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("DELETE FROM observer_trip WHERE lastSeenAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)

    @Query("SELECT COUNT(*) FROM observer_trip")
    suspend fun count(): Int

    @Query("DELETE FROM observer_trip WHERE id IN (SELECT id FROM observer_trip ORDER BY lastSeenAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
