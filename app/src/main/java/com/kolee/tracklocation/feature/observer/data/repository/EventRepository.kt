package com.kolee.tracklocation.feature.observer.data.repository

import android.content.Context
import com.kolee.tracklocation.data.roomdb.TrackDatabase
import com.kolee.tracklocation.feature.observer.domain.model.ObservedEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface EventRepository {
    fun getEvents(): Flow<List<ObservedEvent>>
}

class EventRepositoryImpl(context: Context) : EventRepository {

    private val dao = TrackDatabase.getDatabase(context).observerEventDao

    override fun getEvents(): Flow<List<ObservedEvent>> =
        dao.getAllEvents().map { entities ->
            // DB returns newest-first; reverse to newest-last for the feed list
            entities.reversed().map { e ->
                ObservedEvent(
                    id = e.id,
                    packageName = e.packageName,
                    activityName = e.activityName,
                    eventType = e.eventType,
                    timestampMs = e.lastSeenAt,
                    textSummary = e.textSummary,
                )
            }
        }
}

// Returns a static set of fake events; used as the fallback when no real events exist yet.
class FakeEventRepository : EventRepository {
    override fun getEvents(): Flow<List<ObservedEvent>> = flow {
        val now = System.currentTimeMillis()
        emit(
            listOf(
                ObservedEvent(1, "com.android.chrome", ".browser.ChromeTabbedActivity", "TYPE_VIEW_CLICKED", now - 10_000, "Search results"),
                ObservedEvent(2, "com.android.settings", ".Settings", "TYPE_WINDOW_STATE_CHANGED", now - 8_000, null),
                ObservedEvent(3, "com.google.android.gms", ".auth.GoogleSignInActivity", "TYPE_VIEW_FOCUSED", now - 6_000, "Sign in with Google"),
                ObservedEvent(4, "com.android.systemui", ".statusbar.NotificationPanel", "TYPE_WINDOW_CONTENT_CHANGED", now - 4_000, null),
                ObservedEvent(5, "com.kolee.tracklocation", ".MainActivity", "TYPE_VIEW_SCROLLED", now - 2_000, null),
            )
        )
    }
}
