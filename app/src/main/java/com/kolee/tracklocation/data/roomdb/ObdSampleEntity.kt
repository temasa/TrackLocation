package com.kolee.tracklocation.data.roomdb

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "obd_sample",
    indices = [Index(value = ["timestampMs"])]
)
data class ObdSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMs: Long,
    val rpm: Int?,
    val obdSpeedKmh: Int?,
    val fuelRateLph: Double?,
    val mafGramsPerSecond: Double?,
    val fuelRateSource: String,
    val adapterElapsedMs: Long?
)
