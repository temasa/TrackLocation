package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_log")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = 0L,
    val dwellStartTimestamp: Long = 0L,
    val collapsedCount: Int = 1,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Float? = null,
    val speedMetersPerSecond: Float? = null,
    val bearingDegrees: Float? = null,
    val altitudeMeters: Double? = null
)
