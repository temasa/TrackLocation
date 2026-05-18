# Data Model Specification

Last updated: 2026-05-16 13:52:51 +07:00

## Purpose

This file describes the current accepted data model after accepted change requests are absorbed. It is not a changelog; for change history, see `../change-requests/`.

## Current GPS/location model after CR#1

### Canonical Location Log

`LocationEntity` is the append-only source of truth for GPS points.

Expected fields:

```kotlin
data class LocationEntity(
    val id: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val speedMetersPerSecond: Float?,
    val bearingDegrees: Float?,
    val altitudeMeters: Double?
)
```

Rules:

- IDs are incremental and used as range boundaries.
- Location rows are not deleted when a trip is deleted.
- Sessions and trips both resolve paths from this table.

### SessionEntity

Represents an always-recorded ON-to-OFF range.

Expected fields:

```kotlin
data class SessionEntity(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val startLocationId: Long?,
    val endLocationId: Long?,
    val distanceMeters: Double,
    val durationMillis: Long,
    val pointCount: Int,
    val isActive: Boolean
)
```

Rules:

- A session starts when always-recording turns ON.
- A session ends when always-recording turns OFF.
- An active session has `endedAt = null` and `isActive = true`.
- Session metrics may be stored as summaries for fast list rendering, but path reconstruction uses location ranges.

### TripEntity

Represents a user-declared trip range.

Expected direction:

```kotlin
data class TripEntity(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val startLocationId: Long?,
    val endLocationId: Long?,
    val distanceMeters: Double,
    val durationMillis: Long,
    val averageSpeed: Double?
)
```

Rules:

- Trips do not own serialized path data as the primary model.
- Trips reference the canonical location log using inclusive boundaries.
- Starting a trip uses the next inserted location row as `startLocationId`.
- Stopping a trip uses the latest existing location row as `endLocationId`.
- Empty trips are not saved.
- Deleting a trip keeps location rows.

## Observer data model

The Observer data model remains governed by `../product/accessibility-observer-prd.md`.

Core local entity:

```kotlin
data class ObservedEvent(
    val id: String,
    val timestamp: Long,
    val packageName: String,
    val activityName: String?,
    val eventType: Int,
    val uiSnapshotJson: String,
    val searchableText: String,
    val synced: Boolean = false
)
```

Observer events are device-level and are not associated with users.

## Migration requirement

Legacy trip path strings must be converted into canonical location rows. See `../implementation/migration-plan.md`.
