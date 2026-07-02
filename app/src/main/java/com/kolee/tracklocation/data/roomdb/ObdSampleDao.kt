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

    /**
     * One-shot (non-Flow) variant for integrating fuel over a finished window, e.g. computing a
     * trip's total litres at trip stop. Ascending by time so consecutive dt gaps are positive.
     */
    @Query("SELECT * FROM obd_sample WHERE timestampMs BETWEEN :startMs AND :endMs ORDER BY timestampMs ASC")
    suspend fun samplesBetweenOnce(startMs: Long, endMs: Long): List<ObdSampleEntity>

    @Query("DELETE FROM obd_sample WHERE timestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
