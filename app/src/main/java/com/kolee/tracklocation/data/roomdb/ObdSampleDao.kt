package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ObdSampleDao {

    @Insert
    suspend fun insert(sample: ObdSampleEntity)

    @Query("SELECT * FROM obd_sample ORDER BY timestampMs DESC LIMIT 1")
    fun latestSample(): Flow<ObdSampleEntity?>

    @Query("SELECT * FROM obd_sample WHERE timestampMs BETWEEN :startMs AND :endMs ORDER BY timestampMs ASC")
    fun samplesBetween(startMs: Long, endMs: Long): Flow<List<ObdSampleEntity>>

    @Query("DELETE FROM obd_sample WHERE timestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
