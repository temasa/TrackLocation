package com.kolee.tracklocation.feature.observer.trip

enum class OrderPhase { PICKUP, DROP, FINISHED }

/**
 * "Same order" / "active order" window: a stored row seen within this window and not FINISHED is
 * the order being served; older rows are stale, so a repeated address pair starts a new order.
 */
const val ORDER_ACTIVE_WINDOW_MS = 2L * 60 * 60 * 1000

/**
 * Device-only projection of a ride-hailing order card (ADR-013). Deliberately carries NO customer
 * name, rating or phone number.
 */
data class OrderCard(
    val phase: OrderPhase,
    val pickupName: String? = null,
    val pickupAddress: String? = null,
    val dropName: String? = null,
    val dropAddress: String? = null,
    val payment: String? = null,
    val earningsRp: Long? = null
) {
    /** All fields the takeover needs are known (pickup phase only). */
    fun isComplete(): Boolean =
        phase == OrderPhase.PICKUP &&
            pickupAddress != null && dropAddress != null && payment != null && earningsRp != null
}
