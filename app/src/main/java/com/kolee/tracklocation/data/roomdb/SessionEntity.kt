package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_session")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val startLocationId: Long? = null,
    val endLocationId: Long? = null,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0L,
    val pointCount: Int = 0,
    val isActive: Boolean = false,
    val obdFuelConsumedL: Double = 0.0,
    val obdGpsDistanceKm: Double = 0.0
)
