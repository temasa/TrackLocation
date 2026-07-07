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

    /**
     * Close only sessions whose last recorded activity (startedAt + durationMillis, refreshed on
     * every location point) is older than [staleBefore]. Used at app launch to reap genuinely
     * orphaned sessions WITHOUT racing the service's START_STICKY resume: a session being resumed
     * after a low-memory kill was updated seconds ago and is therefore not stale, so it survives
     * regardless of whether the Activity or the service wins the restart.
     */
    @Query("UPDATE recording_session SET isActive = 0, endedAt = :endedAt WHERE isActive = 1 AND endedAt IS NULL AND (startedAt + durationMillis) < :staleBefore")
    suspend fun closeStaleActiveSessions(endedAt: Long, staleBefore: Long)

    /**
     * Close ALL currently-open sessions (isActive = 1, endedAt IS NULL). Used to enforce the
     * single-active-session invariant when (re)starting always-recording: any duplicate/orphaned
     * open rows are reaped before the adopted or freshly-created session is written.
     */
    @Query("UPDATE recording_session SET isActive = 0, endedAt = :endedAt WHERE isActive = 1 AND endedAt IS NULL")
    suspend fun closeAllActiveSessions(endedAt: Long)

    /**
     * OBD Phase 2: atomically add a fuel/distance increment to the session's accumulators.
     * [sessionId] matches the String primary key of `recording_session`.
     */
    @Query("UPDATE recording_session SET obdFuelConsumedL = obdFuelConsumedL + :fuelL WHERE id = :sessionId")
    suspend fun addObdAccumulator(sessionId: String, fuelL: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(item: SessionEntity)

    @Update
    suspend fun updateSession(item: SessionEntity)
}
