package com.kolee.tracklocation.observer

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.kolee.tracklocation.data.roomdb.ObservedEventEntity
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

        serviceScope.launch {
            if (!prefs.captureRunning.first()) return@launch

            if (!isAllowed(packageName)) return@launch

            // Content-changed collapse: if signature unchanged, bump timestamp only
            if (isContentChanged) {
                val existing = db.observerEventDao.findExisting(packageName, eventTypeName, textSummary)
                if (existing != null) {
                    db.observerEventDao.update(
                        existing.copy(lastSeenAt = now, repeatCount = existing.repeatCount + 1)
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
                    treeSnapshot = null,
                    truncationMetadata = null,
                )
            )

            pruneIfNeeded(now)
        }
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
