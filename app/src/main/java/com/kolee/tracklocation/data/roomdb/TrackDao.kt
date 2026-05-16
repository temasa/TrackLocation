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
    suspend fun insertTrack(item: TrackEntity)

    @Query("SELECT * FROM track WHERE idx=:idx")
    fun getTrackById(idx: Int): Flow<TrackEntity>

    @Query("SELECT * FROM track WHERE idx=:idx")
    suspend fun getTrackByIdOnce(idx: Int): TrackEntity?

    @Delete
    suspend fun deleteTrack(item: TrackEntity)
}
