package com.kolee.tracklocation.feature.observer.domain.model

data class ObservedEvent(
    val id: Long,
    val packageName: String,
    val activityName: String?,
    val eventType: String,
    val timestampMs: Long,
    val textSummary: String?
)
