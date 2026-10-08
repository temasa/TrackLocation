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

    /** ADR-023 amendment: persist the accepted point (backfill fallback from the stored path). */
    @Query("UPDATE track SET acceptedLat = :lat, acceptedLng = :lng WHERE idx = :idx")
    suspend fun updateAcceptedPoint(idx: Int, lat: Double, lng: Double)

    /** ADR-023 amendment: order trips whose accepted place was never resolved, newest first. */
    @Query(
        "SELECT * FROM track WHERE orderLabel IS NOT NULL AND acceptedName IS NULL " +
            "AND acceptedAddress IS NULL ORDER BY idx DESC LIMIT :limit"
    )
    suspend fun getOrderTripsMissingAcceptedPlace(limit: Int): List<TrackEntity>

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
