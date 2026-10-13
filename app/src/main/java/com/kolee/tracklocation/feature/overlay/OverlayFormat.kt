package com.kolee.tracklocation.feature.overlay

import com.kolee.tracklocation.ui.components.MetricEmpty
import com.kolee.tracklocation.ui.components.formatCompactAmount
import com.kolee.tracklocation.ui.components.formatMetricDecimal
import com.kolee.tracklocation.ui.components.formatMetricPair
import com.kolee.tracklocation.utils.FuelBalance
import java.util.Locale

// ADR-029 / UI-SPEC 5d display formatting. Pure functions; every missing value renders as "—".

/** FUEL cell / circle: litres with 1 decimal, e.g. "12.4". */
fun formatOverlayFuelLeft(fuelLeftL: Double?): String = formatMetricDecimal(fuelLeftL)

/** The circle has no tank capacity to compute a percent from (none exists in the app), so it shows litres plus this band. */
fun overlayFuelLevel(fuelLeftL: Double?): FuelBalance.FuelLevel = FuelBalance.level(fuelLeftL)

/** RANGE cell: whole km, e.g. "310". */
fun formatOverlayRange(rangeKm: Double?): String =
    if (rangeKm == null || rangeKm.isNaN() || rangeKm.isInfinite()) MetricEmpty
    else String.format(Locale.US, "%.0f", rangeKm)

/** FUEL CONS cell: "avg/instant", e.g. "12.3/8.1"; a missing half is "—". */
fun formatOverlayFuelCons(avgKmL: Double?, instantKmL: Double?): String =
    formatMetricPair(
        avgKmL?.let { formatMetricDecimal(it) },
        instantKmL?.let { formatMetricDecimal(it) }
    )

/** COST cell: compact rupiah, e.g. "1.2jt". */
fun formatOverlayCost(totals: SessionTotals): String =
    totals.costRp?.let { formatCompactAmount(it) } ?: MetricEmpty

/** INCOME cell: "gross/net", e.g. "8.4k/6.0k" or "15.0k/−3.5k"; a single "—" when no order was counted. */
fun formatOverlayIncome(totals: SessionTotals): String {
    val gross = totals.grossRp ?: return MetricEmpty
    val net = totals.netRp ?: return MetricEmpty
    return "${formatCompactAmount(gross)}/${formatCompactAmount(net)}"
}
