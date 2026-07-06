package com.kolee.tracklocation.feature.observer.data.repository

import android.content.Context
import com.kolee.tracklocation.data.roomdb.TrackDatabase
import com.kolee.tracklocation.feature.observer.domain.model.ObservedEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface EventRepository {
    fun getEvents(): Flow<List<ObservedEvent>>
    suspend fun getFirstPage(limit: Int): List<ObservedEvent>
    suspend fun getNextPage(beforeLastSeenAt: Long, limit: Int): List<ObservedEvent>
    fun getEventsNewerThan(afterLastSeenAt: Long): Flow<List<ObservedEvent>>
    suspend fun getFilteredFirstPage(query: String, limit: Int): List<ObservedEvent>
    suspend fun getFilteredNextPage(query: String, beforeLastSeenAt: Long, limit: Int): List<ObservedEvent>
}

class EventRepositoryImpl(context: Context) : EventRepository {

    private val dao = TrackDatabase.getDatabase(context).observerEventDao

    override fun getEvents(): Flow<List<ObservedEvent>> =
        dao.getAllEvents().map { entities ->
            // DB returns newest-first; reverse to newest-last for the feed list
            entities.reversed().map { it.toDomain() }
        }

    override suspend fun getFirstPage(limit: Int): List<ObservedEvent> =
        dao.getEventsFirstPage(limit).reversed().map { it.toDomain() }

    override suspend fun getNextPage(beforeLastSeenAt: Long, limit: Int): List<ObservedEvent> =
        dao.getEventsNextPage(beforeLastSeenAt, limit).reversed().map { it.toDomain() }

    override suspend fun getFilteredFirstPage(query: String, limit: Int): List<ObservedEvent> =
        dao.getFilteredEventsFirstPage(query, limit).reversed().map { it.toDomain() }

    override suspend fun getFilteredNextPage(query: String, beforeLastSeenAt: Long, limit: Int): List<ObservedEvent> =
        dao.getFilteredEventsNextPage(query, beforeLastSeenAt, limit).reversed().map { it.toDomain() }

    override fun getEventsNewerThan(afterLastSeenAt: Long): Flow<List<ObservedEvent>> =
        dao.getEventsNewerThan(afterLastSeenAt).map { entities ->
            entities.reversed().map { it.toDomain() }
        }

    private fun com.kolee.tracklocation.data.roomdb.ObservedEventEntity.toDomain() = ObservedEvent(
        id = id,
        packageName = packageName,
        activityName = activityName,
        eventType = eventType,
        firstSeenMs = firstSeenAt,
        timestampMs = lastSeenAt,
        repeatCount = repeatCount,
        textSummary = textSummary,
        treeSnapshot = treeSnapshot,
        truncationMetadata = truncationMetadata,
    )
}

// Returns a static set of fake events; used as the fallback when no real events exist yet.
class FakeEventRepository : EventRepository {
    override fun getEvents(): Flow<List<ObservedEvent>> = flow {
        val now = System.currentTimeMillis()
        emit(
            listOf(
                ObservedEvent(1, "com.android.chrome", ".browser.ChromeTabbedActivity", "TYPE_VIEW_CLICKED", now - 12_000, now - 10_000, 1, "Search results", null, null),
                ObservedEvent(2, "com.android.settings", ".Settings", "TYPE_WINDOW_STATE_CHANGED", now - 9_000, now - 8_000, 1, null, null, null),
                ObservedEvent(3, "com.google.android.gms", ".auth.GoogleSignInActivity", "TYPE_VIEW_FOCUSED", now - 7_000, now - 6_000, 1, "Sign in with Google", null, null),
                ObservedEvent(4, "com.android.systemui", ".statusbar.NotificationPanel", "TYPE_WINDOW_CONTENT_CHANGED", now - 5_000, now - 4_000, 1, null, null, null),
                ObservedEvent(5, "com.kolee.tracklocation", ".MainActivity", "TYPE_VIEW_SCROLLED", now - 3_000, now - 2_000, 1, null, null, null),
            )
        )
    }

    override suspend fun getFirstPage(limit: Int): List<ObservedEvent> = emptyList()
    override suspend fun getNextPage(beforeLastSeenAt: Long, limit: Int): List<ObservedEvent> = emptyList()
    override fun getEventsNewerThan(afterLastSeenAt: Long): Flow<List<ObservedEvent>> = flowOf(emptyList())
    override suspend fun getFilteredFirstPage(query: String, limit: Int): List<ObservedEvent> = emptyList()
    override suspend fun getFilteredNextPage(query: String, beforeLastSeenAt: Long, limit: Int): List<ObservedEvent> = emptyList()
}
