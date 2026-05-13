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
    fun pathPointsToString(list: List<LatLng>): String {
        val sb = StringBuilder()

        list.forEach {
            sb.append("${it.latitude},${it.longitude}/")
        }

        return sb.toString()
    }

    fun stringToPathPoints(geoPoints: String): List<LatLng> {
        val geoPointsList = mutableListOf<LatLng>()
        val tempList = geoPoints.split("/")

        tempList.forEach {
            if (it.isEmpty()) return@forEach
            val points = it.split(",")
            geoPointsList.add(
                LatLng(
                    points[0].toDouble(),
                    points[1].toDouble()
                )
            )
        }

        return geoPointsList
    }

}

