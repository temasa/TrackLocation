package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(item: LocationEntity): Long

    @Query("UPDATE location_log SET timestamp = :timestamp, collapsedCount = collapsedCount + 1 WHERE id = :id")
    suspend fun updateDwellAnchor(id: Long, timestamp: Long)

    @Query("SELECT * FROM location_log ORDER BY id DESC LIMIT 1")
    suspend fun getLatestLocation(): LocationEntity?

    @Query("SELECT * FROM location_log WHERE id = :id")
    suspend fun getLocationByIdOnce(id: Long): LocationEntity?

    @Query("SELECT * FROM location_log WHERE id BETWEEN :startId AND :endId ORDER BY id ASC")
    fun getLocationsByRange(startId: Long, endId: Long): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location_log WHERE id BETWEEN :startId AND :endId ORDER BY id ASC")
    suspend fun getLocationsByRangeOnce(startId: Long, endId: Long): List<LocationEntity>
}
