package com.kolee.tracklocation.feature.observer.trip

enum class OrderPhase { PICKUP, DROP, FINISHED }

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
