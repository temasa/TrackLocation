package com.kolee.tracklocation.feature.observer.domain.model

data class ObservedEvent(
    val id: Long,
    val packageName: String,
    val activityName: String?,
    val eventType: String,
    val firstSeenMs: Long,
    val timestampMs: Long,
    val repeatCount: Int,
    val textSummary: String?,
    val treeSnapshot: String?,
    val truncationMetadata: String?,
)
