package com.kolee.tracklocation.feature.overlay

// ADR-029 Decision 2/4: everything the overlay strip/circle renders, already resolved to plain values.
// Null fuel/efficiency values mean "not known / OBD not Connected" and are shown as "—".
data class OverlayUiState(
    val sessionActive: Boolean = false,
    val fuelLeftL: Double? = null,
    val rangeKm: Double? = null,
    val avgKmL: Double? = null,
    val instantKmL: Double? = null,
    val totals: SessionTotals = EmptySessionTotals,
    val collapsed: Boolean = false,
    // Circle position in px; -1 = unset (host picks the default corner).
    val circleX: Int = -1,
    val circleY: Int = -1
)

// Same value computeSessionTotals returns for an empty order list.
val EmptySessionTotals = SessionTotals(null, null, null, partial = false, countedOrders = 0)
