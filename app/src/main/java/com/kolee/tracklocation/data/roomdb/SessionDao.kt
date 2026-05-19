package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM recording_session ORDER BY isActive DESC, startedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM recording_session WHERE isActive = 1 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Query("UPDATE recording_session SET isActive = 0, endedAt = :endedAt WHERE isActive = 1 AND endedAt IS NULL")
    suspend fun closeAllActiveSessions(endedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(item: SessionEntity)

    @Update
    suspend fun updateSession(item: SessionEntity)
}
