package com.kolee.tracklocation.data.roomdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ObserverEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ObservedEventEntity): Long

    @Update
    suspend fun update(event: ObservedEventEntity)

    @Query("SELECT * FROM observer_event ORDER BY lastSeenAt DESC")
    fun getAllEvents(): Flow<List<ObservedEventEntity>>

    @Query("SELECT * FROM observer_event ORDER BY lastSeenAt DESC LIMIT :limit")
    suspend fun getEventsFirstPage(limit: Int): List<ObservedEventEntity>

    @Query("SELECT * FROM observer_event WHERE lastSeenAt < :beforeLastSeenAt ORDER BY lastSeenAt DESC LIMIT :limit")
    suspend fun getEventsNextPage(beforeLastSeenAt: Long, limit: Int): List<ObservedEventEntity>

    @Query("SELECT oe.* FROM observer_event oe JOIN observer_event_fts ON observer_event_fts.rowid = oe.id WHERE observer_event_fts MATCH :query ORDER BY oe.lastSeenAt DESC LIMIT :limit")
    suspend fun getFilteredEventsFirstPage(query: String, limit: Int): List<ObservedEventEntity>

    @Query("SELECT oe.* FROM observer_event oe JOIN observer_event_fts ON observer_event_fts.rowid = oe.id WHERE observer_event_fts MATCH :query AND oe.lastSeenAt < :beforeLastSeenAt ORDER BY oe.lastSeenAt DESC LIMIT :limit")
    suspend fun getFilteredEventsNextPage(query: String, beforeLastSeenAt: Long, limit: Int): List<ObservedEventEntity>

    @Query("SELECT * FROM observer_event WHERE lastSeenAt > :afterLastSeenAt ORDER BY lastSeenAt DESC")
    fun getEventsNewerThan(afterLastSeenAt: Long): Flow<List<ObservedEventEntity>>

    @Query("SELECT * FROM observer_event WHERE packageName = :packageName ORDER BY lastSeenAt DESC")
    fun getEventsByPackage(packageName: String): Flow<List<ObservedEventEntity>>

    @Query(
        "SELECT * FROM observer_event WHERE packageName = :packageName AND eventType = :eventType AND textSummary = :textSummary ORDER BY lastSeenAt DESC LIMIT 1"
    )
    suspend fun findExisting(packageName: String, eventType: String, textSummary: String?): ObservedEventEntity?

    @Query("DELETE FROM observer_event WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM observer_event WHERE lastSeenAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)

    @Query("SELECT COUNT(*) FROM observer_event")
    suspend fun count(): Int

    @Query(
        "DELETE FROM observer_event WHERE id IN (SELECT id FROM observer_event ORDER BY lastSeenAt ASC LIMIT :count)"
    )
    suspend fun deleteOldest(count: Int)
}
