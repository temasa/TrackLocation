package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val idx: Int = 0,
    val timestamp: Long = 0L,
    val distance: Int = 0,
    val duration: Long = 0L,
    val pathPoints: String = "",
    val startLocationId: Long? = null,
    val endLocationId: Long? = null,
    val obdFuelConsumedL: Double = 0.0
)
