package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "observer_event",
    indices = [
        Index("packageName"),
        Index("lastSeenAt")
    ]
)
data class ObservedEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val activityName: String?,
    val eventType: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val textSummary: String?,
    val repeatCount: Int = 1,
    val treeSnapshot: String?,
    val truncationMetadata: String?
)
