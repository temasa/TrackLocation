package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "observer_event_fts")
@Fts4(contentEntity = ObservedEventEntity::class)
data class ObservedEventFtsEntity(
    val packageName: String,
    val activityName: String?,
    val textSummary: String?
)
