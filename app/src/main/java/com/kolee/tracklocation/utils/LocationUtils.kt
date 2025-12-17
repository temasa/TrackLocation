package com.kolee.tracklocation.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

object LocationUtils {
    fun getDistanceBetweenPathPoints(
        pathPoints1: LatLng,
        pathPoints2: LatLng
    ): Int {
        val result = FloatArray(1)
        Location.distanceBetween(
            pathPoints1.latitude,
            pathPoints1.longitude,
            pathPoints2.latitude,
            pathPoints2.longitude,
            result
        )

        return result[0].roundToInt()
    }
}