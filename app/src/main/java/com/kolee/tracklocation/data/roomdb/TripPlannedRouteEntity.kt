package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

// ADR-026: device-only saved planned route of a trip. tripStartedAt == track.timestamp of the owning
// trip; no foreign key (same convention as observer_trip). Never ingested into the route store (BR-15).
@Entity(tableName = "trip_planned_route")
data class TripPlannedRouteEntity(
    @PrimaryKey(autoGenerate = false) val tripStartedAt: Long,
    val kind: String, // 'ORDER' | 'NAV' (NAV reserved for ADR-009)
    val geometry: String, // ORS encoded polyline (precision 5), exactly as returned
    val distanceM: Int?,
    val durationS: Int?,
    val originSource: String?, // 'ACCEPTED' | 'FALLBACK_LIVE'
    val fetchedAt: Long
)
