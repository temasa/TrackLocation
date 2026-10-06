package com.kolee.tracklocation.feature.observer.trip.route

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.kolee.tracklocation.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * ADR-016: thin Google Geocoding + Directions client (HttpURLConnection + org.json, no new
 * dependency). Every failure (network, timeout, non-OK status, bad JSON) yields null; nothing
 * throws. The Maps key comes from the `google_maps_key` resource and is never logged.
 */
class GoogleRouteClient(context: Context) {

    private val apiKey: String = context.applicationContext.getString(R.string.google_maps_key)

    /** First geocoding result for [address] (biased to Indonesia), or null. */
    suspend fun geocode(address: String): LatLng? {
        if (address.isBlank() || apiKey.isBlank()) return null
        val url = "$GEOCODE_URL?address=${URLEncoder.encode(address, "UTF-8")}&region=id&key=$apiKey"
        val json = getJson(url, "geocode") ?: return null
        return try {
            if (json.optString("status") != "OK") {
                Log.w(TAG, "geocode status=${json.optString("status")}")
                return null
            }
            val loc = json.getJSONArray("results").getJSONObject(0)
                .getJSONObject("geometry").getJSONObject("location")
            LatLng(loc.getDouble("lat"), loc.getDouble("lng"))
        } catch (e: Exception) {
            Log.w(TAG, "geocode parse failed: ${e.javaClass.simpleName}")
            null
        }
    }

    /** Driving route polyline origin -> [waypoint]? -> destination, or null. */
    suspend fun route(origin: LatLng, destination: LatLng, waypoint: LatLng? = null): List<LatLng>? {
        if (apiKey.isBlank()) return null
        val sb = StringBuilder(DIRECTIONS_URL)
            .append("?origin=").append(origin.latitude).append(',').append(origin.longitude)
            .append("&destination=").append(destination.latitude).append(',').append(destination.longitude)
        if (waypoint != null) {
            sb.append("&waypoints=").append(waypoint.latitude).append(',').append(waypoint.longitude)
        }
        sb.append("&mode=driving&key=").append(apiKey)
        val json = getJson(sb.toString(), "route") ?: return null
        return try {
            if (json.optString("status") != "OK") {
                Log.w(TAG, "route status=${json.optString("status")}")
                return null
            }
            val points = json.getJSONArray("routes").getJSONObject(0)
                .getJSONObject("overview_polyline").getString("points")
            PolyUtil.decode(points).takeIf { it.size > 1 }
        } catch (e: Exception) {
            Log.w(TAG, "route parse failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun getJson(url: String, what: String): JSONObject? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "$what http=${conn.responseCode}")
                null
            } else {
                JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Do not log the exception message: it can contain the request URL (and key).
            Log.w(TAG, "$what failed: ${e.javaClass.simpleName}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private companion object {
        const val TAG = "GoogleRouteClient"
        const val TIMEOUT_MS = 10_000
        const val GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json"
        const val DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json"
    }
}
