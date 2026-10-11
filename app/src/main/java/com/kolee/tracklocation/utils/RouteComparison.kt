package com.kolee.tracklocation.utils

import com.google.android.gms.maps.model.LatLng
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ADR-026: planned-vs-actual numbers for a finished trip, derived at display time and never
 * persisted. Pure (no Android framework calls) so it stays unit-testable.
 */
data class RouteComparison(
    val plannedKm: Double,
    val actualKm: Double,
    /** Signed: actual - planned. */
    val extraKm: Double,
    /** Max over actual points of the distance to the planned polyline, metres. */
    val maxDeviationM: Double
) {
    companion object {
        private const val EARTH_RADIUS_M = 6_371_000.0
        private const val MAX_ACTUAL_SAMPLES = 500

        /**
         * [plannedDistanceM] (ORS summary) wins over the polyline length; [actualDistanceM] is
         * `track.distance` (metres). Returns null when either polyline has fewer than two points.
         */
        fun compare(
            planned: List<LatLng>,
            plannedDistanceM: Int?,
            actual: List<LatLng>,
            actualDistanceM: Int?
        ): RouteComparison? {
            if (planned.size < 2 || actual.size < 2) return null
            val plannedKm = (plannedDistanceM?.toDouble() ?: polylineLengthM(planned)) / 1000.0
            val actualKm = (actualDistanceM?.toDouble() ?: polylineLengthM(actual)) / 1000.0
            return RouteComparison(
                plannedKm = plannedKm,
                actualKm = actualKm,
                extraKm = actualKm - plannedKm,
                maxDeviationM = maxDeviationM(actual, planned)
            )
        }

        fun polylineLengthM(points: List<LatLng>): Double {
            var total = 0.0
            for (i in 1 until points.size) total += haversineM(points[i - 1], points[i])
            return total
        }

        /** Max distance (m) from any (downsampled to <= 500) actual point to the [planned] polyline. */
        fun maxDeviationM(actual: List<LatLng>, planned: List<LatLng>): Double {
            if (planned.size < 2 || actual.isEmpty()) return 0.0
            val step = max(1, (actual.size + MAX_ACTUAL_SAMPLES - 1) / MAX_ACTUAL_SAMPLES)
            var worst = 0.0
            var i = 0
            while (i < actual.size) {
                worst = max(worst, distanceToPolylineM(actual[i], planned))
                i += step
            }
            // Always include the last point (downsampling may skip it).
            worst = max(worst, distanceToPolylineM(actual.last(), planned))
            return worst
        }

        private fun distanceToPolylineM(p: LatLng, line: List<LatLng>): Double {
            var best = Double.MAX_VALUE
            for (i in 1 until line.size) best = min(best, distanceToSegmentM(p, line[i - 1], line[i]))
            return best
        }

        // Local equirectangular projection around p: accurate to well under a metre at trip scale.
        private fun distanceToSegmentM(p: LatLng, a: LatLng, b: LatLng): Double {
            val cosLat = cos(Math.toRadians(p.latitude))
            val mPerDeg = EARTH_RADIUS_M * PI / 180.0
            val ax = (a.longitude - p.longitude) * mPerDeg * cosLat
            val ay = (a.latitude - p.latitude) * mPerDeg
            val bx = (b.longitude - p.longitude) * mPerDeg * cosLat
            val by = (b.latitude - p.latitude) * mPerDeg
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else max(0.0, min(1.0, -(ax * dx + ay * dy) / len2))
            val cx = ax + t * dx
            val cy = ay + t * dy
            return sqrt(cx * cx + cy * cy)
        }

        private fun haversineM(a: LatLng, b: LatLng): Double {
            val dLat = Math.toRadians(b.latitude - a.latitude)
            val dLng = Math.toRadians(b.longitude - a.longitude)
            val h = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLng / 2) * sin(dLng / 2)
            return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
        }
    }
}
