package com.kolee.tracklocation.feature.observer.trip

import org.json.JSONArray

/** Extracts the non-blank `text` values of an `observer_event.treeSnapshot` JSON array, in order. */
fun extractTexts(treeSnapshotJson: String): List<String> {
    val arr = JSONArray(treeSnapshotJson)
    val out = ArrayList<String>(arr.length())
    for (i in 0 until arr.length()) {
        val text = arr.optJSONObject(i)?.optString("text", "")?.trim().orEmpty()
        if (text.isNotEmpty()) out.add(text)
    }
    return out
}
