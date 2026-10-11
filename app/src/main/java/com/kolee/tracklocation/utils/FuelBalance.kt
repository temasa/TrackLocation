package com.kolee.tracklocation.utils

import kotlin.math.max

/**
 * ADR-027: pure fuel-dock calculations. No Android dependencies.
 */
object FuelBalance {

    /**
     * ADR-027 Decision 2: fuel left = max(0, anchor + fill-ups since the Reset − consumption since
     * the Reset). [resetLitres] and [resetBaselineL] come from the latest RESET event; consumption
     * is the growth of [cumulativeNowL] (the obd_ledger total) since that baseline. Returns null when there is no Reset.
     */
    fun fuelLeft(
        resetLitres: Double?,
        resetBaselineL: Double?,
        fillUpLitresSinceReset: Double,
        cumulativeNowL: Double
    ): Double? {
        if (resetLitres == null) return null
        val consumedSinceReset = max(0.0, cumulativeNowL - (resetBaselineL ?: cumulativeNowL))
        return max(0.0, resetLitres + fillUpLitresSinceReset - consumedSinceReset)
    }

    /**
     * ADR-024: session average (no longer used by the dock RANGE) km/L = covered distance ÷ OBD fuel.
     * Null when the session has no meaningful average yet (fuel ≤ 0.01 L or covered distance ≤ 0).
     */
    fun avgKmL(sessionCoveredKm: Double, sessionFuelL: Double): Double? {
        if (sessionFuelL <= 0.01 || sessionCoveredKm <= 0.0) return null
        return sessionCoveredKm / sessionFuelL
    }

    /**
     * ADR-027 Decision 5: RANGE = fuel left × rolling avg km/L (last 1 km of driving, RollingEfficiency). Null if either input is null.
     */
    fun rangeKm(fuelLeftL: Double?, avgKmL: Double?): Double? {
        if (fuelLeftL == null || avgKmL == null) return null
        return fuelLeftL * avgKmL
    }

    /**
     * ADR-027 Decision 9: low confidence when more than 10% of the session distance was not
     * covered by OBD. Drives the `~` prefix on the dock values.
     */
    fun isLowConfidence(sessionDistanceKm: Double, sessionCoveredKm: Double): Boolean {
        if (sessionDistanceKm <= 0.0) return false
        return (sessionDistanceKm - sessionCoveredKm) / sessionDistanceKm > 0.10
    }

    /** ADR-027 Decision 9: dock state for the fuel-left value. */
    enum class FuelLevel { Normal, Low, Critical }

    /**
     * ADR-027 Decision 9: Critical below 3.0 L, Low below 6.0 L, otherwise Normal.
     * A null value (no Reset yet) is Normal; the UI shows `—` in that case.
     */
    fun level(fuelLeftL: Double?): FuelLevel {
        return when {
            fuelLeftL == null -> FuelLevel.Normal
            fuelLeftL < 3.0 -> FuelLevel.Critical
            fuelLeftL < 6.0 -> FuelLevel.Low
            else -> FuelLevel.Normal
        }
    }
}
