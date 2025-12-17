package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val idx: Int = 0,
    val timestamp: Long = 0L,
    val distance: Float = 0f,
    val duration: Long = 0L,
    val pathPoints: String = ""
)