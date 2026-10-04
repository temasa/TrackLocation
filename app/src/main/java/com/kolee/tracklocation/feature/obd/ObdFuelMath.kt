package com.kolee.tracklocation.feature.obd

import com.kolee.tracklocation.data.roomdb.ObdSampleEntity

/**
 * Canonical fuel integral (ADR-007): integrate instantaneous fuel rate (L/h) over time into
 * total litres. For each consecutive ascending-by-timestamp sample pair, add
 * `fuelRateLph × dtHours` with the same `0 < dt < 60 s` guard used by the live per-poll
 * accumulation, so a long gap (adapter drop, backgrounding) does not inflate the total.
 */
object ObdFuelMath {
    fun integrateFuelLiters(samples: List<ObdSampleEntity>): Double {
        var liters = 0.0
        for (i in 1 until samples.size) {
            val rate = samples[i].fuelRateLph ?: continue
            if (rate !in 0.1..100.0) continue
            val dtSeconds = (samples[i].timestampMs - samples[i - 1].timestampMs) / 1000.0
            if (dtSeconds > 0 && dtSeconds < 60.0) {
                liters += rate * dtSeconds / 3600.0
            }
        }
        return liters
    }
}
