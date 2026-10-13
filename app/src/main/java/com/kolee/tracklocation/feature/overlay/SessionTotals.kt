package com.kolee.tracklocation.feature.overlay

import com.kolee.tracklocation.data.roomdb.TrackEntity

// ADR-029 Decision 4: session cost / gross / net over the session's order trips. Pure Kotlin, no Android deps.

/** One order trip reduced to what the totals need; null/<=0 values mean "data missing". */
data class OrderInput(
    val status: String?,
    val earningsRp: Long?,
    val fuelL: Double?,
    val pricePerL: Double?
)

/** Null amounts mean no order was counted (UI shows "—"); [partial] means some order was left out for missing data. */
data class SessionTotals(
    val costRp: Long?,
    val grossRp: Long?,
    val netRp: Long?,
    val partial: Boolean,
    val countedOrders: Int
)

fun computeSessionTotals(orders: List<OrderInput>): SessionTotals {
    var gross = 0L
    var cost = 0L
    var counted = 0
    var partial = false
    for (o in orders) {
        // Cancelled orders have no earnings: excluded entirely and do not make the total partial.
        if (o.status == "CANCELLED") continue
        val earnings = o.earningsRp
        val fuelL = o.fuelL
        val price = o.pricePerL
        if (earnings == null || fuelL == null || fuelL <= 0.0 || price == null || price <= 0.0) {
            partial = true
            continue
        }
        gross += earnings
        cost += Math.round(fuelL * price) // same rounding as GojekOrderCard FR-16
        counted++
    }
    if (counted == 0) return SessionTotals(null, null, null, partial, 0)
    return SessionTotals(cost, gross, gross - cost, partial, counted)
}

/** Maps a trip row to [OrderInput]; [priceAtTimestamp] looks up the pump price in effect at the trip start. */
fun TrackEntity.toOrderInput(priceAtTimestamp: (Long) -> Double?): OrderInput =
    OrderInput(
        status = orderStatus,
        earningsRp = orderEarningsRp,
        fuelL = obdFuelConsumedL,
        pricePerL = priceAtTimestamp(timestamp)
    )
