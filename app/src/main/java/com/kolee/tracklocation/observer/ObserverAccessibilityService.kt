package com.kolee.tracklocation.observer

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kolee.tracklocation.data.roomdb.ObservedEventEntity
import org.json.JSONArray
import org.json.JSONObject
import com.kolee.tracklocation.data.roomdb.TrackDatabase
import com.kolee.tracklocation.feature.observer.data.ObserverPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ObserverAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var prefs: ObserverPreferencesDataStore
    private lateinit var db: TrackDatabase

    companion object {
        private const val MAX_TEXT_SUMMARY_LENGTH = 200
        private const val MAX_ROWS = 50_000
        private const val RETENTION_MS = 7L * 24 * 60 * 60 * 1000
        private const val MAX_TREE_NODES = 200
        private const val MAX_TREE_DEPTH = 10
        private const val MAX_NODE_TEXT = 300
        private const val MAX_SNAPSHOT_BYTES = 40_000
    }

    override fun onServiceConnected() {
        prefs = ObserverPreferencesDataStore(this)
        db = TrackDatabase.getDatabase(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val eventTypeName = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "TYPE_WINDOW_CONTENT_CHANGED"
            else -> return
        }

        val packageName = event.packageName?.toString() ?: return
        val activityName = event.className?.toString()
        val textSummary = buildTextSummary(event)
        val isContentChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        val now = System.currentTimeMillis()
        // Capture tree snapshot on the main thread before the coroutine — source node
        // becomes invalid once onAccessibilityEvent returns.
        val (snapshot, truncation) = captureTreeSnapshot(event)

        serviceScope.launch {
            if (!prefs.captureRunning.first()) return@launch

            if (!isAllowed(packageName)) return@launch

            // Content-changed collapse: if signature unchanged, bump timestamp + snapshot only
            if (isContentChanged) {
                val existing = db.observerEventDao.findExisting(packageName, eventTypeName, textSummary)
                if (existing != null) {
                    db.observerEventDao.update(
                        existing.copy(
                            lastSeenAt = now,
                            repeatCount = existing.repeatCount + 1,
                            treeSnapshot = snapshot,
                            truncationMetadata = truncation,
                        )
                    )
                    return@launch
                }
            }

            db.observerEventDao.insert(
                ObservedEventEntity(
                    packageName = packageName,
                    activityName = activityName,
                    eventType = eventTypeName,
                    firstSeenAt = now,
                    lastSeenAt = now,
                    textSummary = textSummary,
                    repeatCount = 1,
                    treeSnapshot = snapshot,
                    truncationMetadata = truncation,
                )
            )

            pruneIfNeeded(now)
        }
    }

    /**
     * DFS over the accessibility node tree rooted at event.source.
     * Returns (snapshotJson, truncationMetadataJson). Both null if root unavailable.
     * Recycles every node after use to avoid leaking the node cache.
     */
    private fun captureTreeSnapshot(event: AccessibilityEvent): Pair<String?, String?> {
        val root = event.source ?: return Pair(null, null)
        val nodes = JSONArray()
        // Stack entries: (node, depth). Node ownership is transferred — caller must recycle.
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(Pair(root, 0))

        var nodesCaptured = 0
        var truncationReason: String? = null

        while (stack.isNotEmpty() && truncationReason == null) {
            val (node, depth) = stack.removeLast()

            if (depth > MAX_TREE_DEPTH) {
                node.recycle()
                truncationReason = "depth_limit"
                break
            }

            val obj = JSONObject().apply {
                val textVal = node.text?.toString()?.take(MAX_NODE_TEXT)
                if (textVal != null) put("text", textVal)
                val cd = node.contentDescription?.toString()?.take(MAX_NODE_TEXT)
                if (cd != null) put("cd", cd)
                val cls = node.className?.toString()
                if (cls != null) put("cls", cls)
                put("click", node.isClickable)
                put("edit", node.isEditable)
                put("enabled", node.isEnabled)
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                put("bounds", bounds.toShortString())
            }
            nodes.put(obj)
            nodesCaptured++

            // Enqueue children (reversed so left-to-right DFS order is preserved)
            for (i in node.childCount - 1 downTo 0) {
                node.getChild(i)?.let { child -> stack.addLast(Pair(child, depth + 1)) }
            }
            node.recycle()

            if (nodesCaptured >= MAX_TREE_NODES) {
                truncationReason = "node_limit"
            }
        }

        // Recycle any nodes still on the stack if we broke early
        while (stack.isNotEmpty()) {
            stack.removeLast().first.recycle()
        }

        val snapshotJson = nodes.toString()
        if (snapshotJson.length > MAX_SNAPSHOT_BYTES) {
            // Snapshot exceeds size cap — discard and record reason
            val meta = JSONObject().apply {
                put("reason", "size_limit")
                put("nodesCaptured", nodesCaptured)
            }
            return Pair(null, meta.toString())
        }

        val truncationMeta = truncationReason?.let { reason ->
            JSONObject().apply {
                put("reason", reason)
                put("nodesCaptured", nodesCaptured)
            }.toString()
        }
        return Pair(snapshotJson, truncationMeta)
    }

    private fun buildTextSummary(event: AccessibilityEvent): String? {
        val texts = event.text
        val combined = texts.joinToString(" ").trim()
        return if (combined.isEmpty()) null else combined.take(MAX_TEXT_SUMMARY_LENGTH)
    }

    private suspend fun isAllowed(packageName: String): Boolean {
        val rules = db.allowlistRuleDao.getEnabledRules()
        if (rules.isEmpty()) return true
        return rules.any { rule ->
            when (rule.matchType) {
                "EXACT" -> rule.pattern == packageName
                "REGEX" -> try {
                    Regex(rule.pattern).containsMatchIn(packageName)
                } catch (_: Exception) {
                    false
                }
                else -> false
            }
        }
    }

    private suspend fun pruneIfNeeded(now: Long) {
        db.observerEventDao.deleteOlderThan(now - RETENTION_MS)
        val count = db.observerEventDao.count()
        if (count > MAX_ROWS) {
            db.observerEventDao.deleteOldest(count - MAX_ROWS)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
