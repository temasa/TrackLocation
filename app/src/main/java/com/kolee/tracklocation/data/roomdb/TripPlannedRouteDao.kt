package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TripPlannedRouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: TripPlannedRouteEntity)

    @Query("SELECT * FROM trip_planned_route WHERE tripStartedAt = :tripStartedAt LIMIT 1")
    suspend fun getByTripStartedAt(tripStartedAt: Long): TripPlannedRouteEntity?

    @Query("DELETE FROM trip_planned_route WHERE tripStartedAt = :tripStartedAt")
    suspend fun deleteByTripStartedAt(tripStartedAt: Long)

    // ADR-026: an in-progress trip has no track row yet, hence the fetchedAt age guard.
    @Query("DELETE FROM trip_planned_route WHERE fetchedAt < :cutoff AND tripStartedAt NOT IN (SELECT timestamp FROM track)")
    suspend fun deleteOrphansOlderThan(cutoff: Long)
}
