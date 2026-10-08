package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM track ORDER BY timestamp DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(item: TrackEntity): Long

    /** ADR-023: late-fill the reverse-geocoded accepted place once the (already saved) trip resolves it. */
    @Query("UPDATE track SET acceptedName = :name, acceptedAddress = :address WHERE idx = :idx")
    suspend fun updateAcceptedPlace(idx: Int, name: String?, address: String?)

    @Query("SELECT * FROM track WHERE idx=:idx")
    fun getTrackById(idx: Int): Flow<TrackEntity>

    @Query("SELECT * FROM track WHERE idx=:idx")
    suspend fun getTrackByIdOnce(idx: Int): TrackEntity?

    /**
     * OBD Phase 2: atomically add a fuel increment to the trip's accumulator.
     * The domain "trip" is the `track` table; [tripIdx] is its `idx` primary key.
     */
    @Query("UPDATE track SET obdFuelConsumedL = obdFuelConsumedL + :fuelL WHERE idx = :tripIdx")
    suspend fun addObdFuel(tripIdx: Int, fuelL: Double)

    @Delete
    suspend fun deleteTrack(item: TrackEntity)
}
