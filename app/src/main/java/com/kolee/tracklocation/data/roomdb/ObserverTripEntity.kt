package com.kolee.tracklocation.data.roomdb

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ADR-013/014: device-only order extracted from the Gojek order card. No customer name/phone/rating.
@Entity(
    tableName = "observer_trip",
    indices = [Index("lastSeenAt")]
)
data class ObserverTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pickupName: String?,
    val pickupAddress: String?,
    val dropName: String?,
    val dropAddress: String?,
    val payment: String?,
    val earningsRp: Long?,
    val phase: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    // Set once the takeover (auto-stop of the active trip) has been processed for this order.
    @ColumnInfo(defaultValue = "0") val handled: Boolean = false
)
