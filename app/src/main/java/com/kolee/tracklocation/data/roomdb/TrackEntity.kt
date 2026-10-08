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
    val obdFuelConsumedL: Double = 0.0,
    // ADR-022: snapshot of the owning Gojek order (null for manual trips). No FK to observer_trip.
    val orderLabel: String? = null,
    val orderEarningsRp: Long? = null,
    // ADR-023: geo snapshot of the order (accepted = driver position at trip start, reverse-geocoded;
    // pickup/drop = order card names/addresses + cached geocode coordinates). All nullable.
    val acceptedName: String? = null,
    val acceptedAddress: String? = null,
    val acceptedLat: Double? = null,
    val acceptedLng: Double? = null,
    val pickupName: String? = null,
    val pickupAddress: String? = null,
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val dropName: String? = null,
    val dropAddress: String? = null,
    val dropLat: Double? = null,
    val dropLng: Double? = null
)
