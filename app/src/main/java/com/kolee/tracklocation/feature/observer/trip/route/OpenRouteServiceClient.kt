package com.kolee.tracklocation.feature.observer.trip.route

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.kolee.tracklocation.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * ADR-017: OpenRouteService geocoding (Pelias) + directions client (HttpURLConnection + org.json,
 * no new dependency). Every failure (network, timeout, non-200, bad JSON) yields null; nothing
 * throws. The key comes from the `ors_api_key` resource, is sent only in the Authorization header
 * and is never logged.
 */
class OpenRouteServiceClient(context: Context) : RouteProvider {

    private val apiKey: String = context.applicationContext.getString(R.string.ors_api_key)

    /**
     * ADR-017 fallback ladder: name(+area hint) -> full address -> trimmed address -> street only.
     * Returns the first accepted (non-coarse, confident enough) point, or null.
     */
    override suspend fun geocode(name: String?, address: String, focus: LatLng?): LatLng? {
        if (apiKey.isBlank()) return null
        val candidates = buildCandidates(name, address)
        for (text in candidates) {
            val point = geocodeOne(text, focus)
            if (point != null) return point
        }
        return null
    }

    /** ADR-023: Pelias reverse geocode of one point (size=1); null on any failure/no result. */
    override suspend fun reverseGeocode(lat: Double, lng: Double): ReverseGeocodeResult? {
        if (apiKey.isBlank()) return null
        val url = "$REVERSE_URL?point.lon=$lng&point.lat=$lat&size=1&boundary.circle.radius=$REVERSE_RADIUS_KM"
        val json = request(url, null, "reverse") ?: return null
        return try {
            val features = json.optJSONArray("features") ?: return null
            if (features.length() == 0) return null
            val props = features.getJSONObject(0).optJSONObject("properties") ?: return null
            val label = props.optString("label").takeIf { it.isNotBlank() }
            val name = props.optString("name").takeIf { it.isNotBlank() }
                ?: props.optString("street").takeIf { it.isNotBlank() }
                ?: label
            if (name == null && label == null) null else ReverseGeocodeResult(name, label)
        } catch (e: Exception) {
            Log.w(TAG, "reverse parse failed: ${e.javaClass.simpleName}")
            null
        }
    }

    /** Driving route polyline origin -> [waypoint]? -> destination, or null. */
    override suspend fun route(origin: LatLng, destination: LatLng, waypoint: LatLng?): List<LatLng>? {
        if (apiKey.isBlank()) return null
        val points = listOfNotNull(origin, waypoint, destination)
        val body = try {
            val coords = JSONArray()
            val radiuses = JSONArray()
            for (p in points) {
                // ORS wants [lon, lat].
                coords.put(JSONArray().put(p.longitude).put(p.latitude))
                radiuses.put(-1)
            }
            JSONObject().put("coordinates", coords).put("radiuses", radiuses).toString()
        } catch (e: Exception) {
            Log.w(TAG, "route body failed: ${e.javaClass.simpleName}")
            return null
        }
        val json = request(DIRECTIONS_URL, body, "route") ?: return null
        return try {
            val encoded = json.getJSONArray("routes").getJSONObject(0).getString("geometry")
            PolyUtil.decode(encoded).takeIf { it.size > 1 }
        } catch (e: Exception) {
            Log.w(TAG, "route parse failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun geocodeOne(text: String, focus: LatLng?): LatLng? {
        val sb = StringBuilder(GEOCODE_URL)
            .append("?text=").append(URLEncoder.encode(text, "UTF-8"))
            .append("&boundary.country=ID&size=1")
        if (focus != null) {
            sb.append("&focus.point.lat=").append(focus.latitude)
                .append("&focus.point.lon=").append(focus.longitude)
        }
        val json = request(sb.toString(), null, "geocode") ?: return null
        return try {
            val features = json.optJSONArray("features") ?: return null
            if (features.length() == 0) return null
            val feature = features.getJSONObject(0)
            val props = feature.optJSONObject("properties") ?: return null
            val layer = props.optString("layer")
            val confidence = props.optDouble("confidence", 0.0)
            if (layer in COARSE_LAYERS) return null
            if (!(confidence >= MIN_CONFIDENCE || layer in PRECISE_LAYERS)) return null
            val c = feature.getJSONObject("geometry").getJSONArray("coordinates")
            LatLng(c.getDouble(1), c.getDouble(0)) // coordinates are [lon, lat]
        } catch (e: Exception) {
            Log.w(TAG, "geocode parse failed: ${e.javaClass.simpleName}")
            null
        }
    }

    /** Candidate query strings in ladder order, de-duplicated and capped at [MAX_QUERIES]. */
    private fun buildCandidates(name: String?, address: String): List<String> {
        val out = ArrayList<String>()
        val segments = address.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val trimmedName = name?.trim().orEmpty()

        // 1. Place name (+ city/area hint = last 1-2 address segments), then the bare name.
        if (trimmedName.isNotEmpty()) {
            if (segments.size > 1) {
                out.add(trimmedName + ", " + segments.takeLast(2).joinToString(", "))
            }
            out.add(trimmedName)
        }

        // 2. Full address with Jl. -> Jalan.
        if (segments.isNotEmpty()) out.add(normalise(segments.joinToString(", ")))

        // 3. Progressively trimmed: house numbers dropped, then trailing segments, keep street + city.
        val cleaned = segments
            .map { stripHouseNumber(it) }
            .filter { it.isNotEmpty() }
        if (cleaned.isNotEmpty()) {
            out.add(normalise(cleaned.joinToString(", ")))
            if (cleaned.size > 2) {
                out.add(normalise(cleaned.dropLast(1).joinToString(", ")))
                out.add(normalise(cleaned.first() + ", " + cleaned.last()))
            }
        }

        // 4. Street only (first segment). Added last, but reserved a slot under the cap.
        val street = cleaned.firstOrNull()?.let { normalise(it) }

        val unique = out.filter { it.isNotBlank() }.distinct().filter { it != street }
        val head = unique.take(if (street != null) MAX_QUERIES - 1 else MAX_QUERIES)
        return if (street != null) head + street else head
    }

    private fun normalise(s: String): String =
        s.replace(JL_REGEX, "Jalan ").replace(Regex("\\s+"), " ").trim()

    private fun stripHouseNumber(segment: String): String =
        segment.replace(HOUSE_NO_REGEX, "").replace(Regex("\\s+"), " ").trim()

    /** POST when [body] != null, else GET. Returns the parsed JSON for HTTP 200, otherwise null. */
    private suspend fun request(url: String, body: String?, what: String): JSONObject? =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = if (body != null) "POST" else "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("Authorization", apiKey)
                    setRequestProperty("Accept", "application/json")
                    if (body != null) {
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        doOutput = true
                    }
                }
                if (body != null) {
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
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
                // Do not log the exception message: it can contain the request URL.
                Log.w(TAG, "$what failed: ${e.javaClass.simpleName}")
                null
            } finally {
                conn?.disconnect()
            }
        }

    private companion object {
        const val TAG = "OpenRouteServiceClient"
        const val TIMEOUT_MS = 10_000
        const val MAX_QUERIES = 6
        const val MIN_CONFIDENCE = 0.8
        const val GEOCODE_URL = "https://api.openrouteservice.org/geocode/search"
        const val REVERSE_URL = "https://api.openrouteservice.org/geocode/reverse"
        const val REVERSE_RADIUS_KM = 1 // nearest hit within 1 km is plenty for "where was I"
        const val DIRECTIONS_URL = "https://api.openrouteservice.org/v2/directions/driving-car"

        // Coarse (city/region-level) pins are worse than none.
        val COARSE_LAYERS = setOf(
            "locality", "localadmin", "county", "region", "macroregion", "country",
            "borough", "neighbourhood"
        )
        val PRECISE_LAYERS = setOf("venue", "street", "address")

        val JL_REGEX = Regex("\\bJl(\\.\\s*|\\s+)", RegexOption.IGNORE_CASE)
        val HOUSE_NO_REGEX = Regex("\\b(No|Nomor)\\.?\\s*\\d+\\w*", RegexOption.IGNORE_CASE)
    }
}
