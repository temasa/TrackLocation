package com.kolee.tracklocation.feature.observer.trip

import android.util.Log
import com.kolee.tracklocation.data.roomdb.ObserverTripDao
import com.kolee.tracklocation.data.roomdb.ObserverTripEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Parses an Observer snapshot for a ride-hailing order card and upserts `observer_trip`
 * (ADR-013). Fires [onBecameComplete] once per order, on the transition to "complete" (ADR-014).
 * Must be called from a coroutine (IO). Never throws; never logs extracted text.
 */
class OrderTripRecorder(
    private val dao: ObserverTripDao,
    private val onBecameComplete: () -> Unit
) {
    private val mutex = Mutex()
    private var lastPruneAt = 0L

    suspend fun record(packageName: String, treeSnapshotJson: String, now: Long) {
        try {
            val rules = GojekRules.forPackage(packageName) ?: return
            val card = OrderCardParser.parse(extractTexts(treeSnapshotJson), rules) ?: return
            val becameComplete = mutex.withLock {
                val result = upsert(card, now)
                prune(now)
                result
            }
            if (becameComplete) {
                try {
                    onBecameComplete()
                } catch (e: Exception) {
                    Log.w(TAG, "takeover launch failed: ${e.javaClass.simpleName}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "order record failed: ${e.javaClass.simpleName}")
        }
    }

    /** Returns true when the stored row just became complete and has not been handled. */
    private suspend fun upsert(card: OrderCard, now: Long): Boolean {
        when (card.phase) {
            OrderPhase.PICKUP -> {
                val pickup = card.pickupAddress ?: return false
                val drop = card.dropAddress ?: return false
                val existing = dao.findByAddresses(pickup, drop)
                if (existing == null || !existing.isSameActiveOrder(now)) {
                    dao.insert(
                        ObserverTripEntity(
                            pickupName = card.pickupName,
                            pickupAddress = pickup,
                            dropName = card.dropName,
                            dropAddress = drop,
                            payment = card.payment,
                            earningsRp = card.earningsRp,
                            phase = card.phase.name,
                            firstSeenAt = now,
                            lastSeenAt = now
                        )
                    )
                    return card.isComplete()
                }
                val updated = existing.copy(
                    pickupName = card.pickupName ?: existing.pickupName,
                    dropName = card.dropName ?: existing.dropName,
                    payment = card.payment ?: existing.payment,
                    earningsRp = card.earningsRp ?: existing.earningsRp,
                    phase = card.phase.name,
                    lastSeenAt = now
                )
                dao.update(updated)
                return !existing.isComplete() && updated.isComplete() && !existing.handled
            }
            OrderPhase.DROP -> {
                val drop = card.dropAddress ?: return false
                val existing = dao.findLatestOpenByDropAddress(drop)
                if (existing == null || !existing.isSameActiveOrder(now)) {
                    dao.insert(
                        ObserverTripEntity(
                            pickupName = null,
                            pickupAddress = null,
                            dropName = card.dropName,
                            dropAddress = drop,
                            payment = card.payment,
                            earningsRp = card.earningsRp,
                            phase = card.phase.name,
                            firstSeenAt = now,
                            lastSeenAt = now
                        )
                    )
                } else {
                    dao.update(
                        existing.copy(
                            dropName = card.dropName ?: existing.dropName,
                            payment = card.payment ?: existing.payment,
                            earningsRp = card.earningsRp ?: existing.earningsRp,
                            phase = card.phase.name,
                            lastSeenAt = now
                        )
                    )
                }
                return false
            }
            OrderPhase.FINISHED -> {
                val existing = dao.findLatestOpen() ?: return false
                if (now - existing.lastSeenAt <= FINISHED_WINDOW_MS) {
                    dao.update(existing.copy(phase = card.phase.name, lastSeenAt = now))
                }
                return false
            }
        }
    }

    // A matching row is the same order only while unfinished and seen within the active window.
    private fun ObserverTripEntity.isSameActiveOrder(now: Long): Boolean =
        phase != OrderPhase.FINISHED.name && now - lastSeenAt <= ORDER_ACTIVE_WINDOW_MS

    private fun ObserverTripEntity.isComplete(): Boolean =
        pickupAddress != null && dropAddress != null && payment != null && earningsRp != null

    // Throttled: retention is 90 days OR 5,000 rows; no need to run on every event.
    private suspend fun prune(now: Long) {
        if (now - lastPruneAt < PRUNE_INTERVAL_MS) return
        lastPruneAt = now
        dao.deleteOlderThan(now - RETENTION_MS)
        val count = dao.count()
        if (count > MAX_ROWS) dao.deleteOldest(count - MAX_ROWS)
    }

    private companion object {
        const val TAG = "OrderTripRecorder"
        const val FINISHED_WINDOW_MS = 6L * 60 * 60 * 1000
        const val RETENTION_MS = 90L * 24 * 60 * 60 * 1000
        const val MAX_ROWS = 5_000
        const val PRUNE_INTERVAL_MS = 10L * 60 * 1000
    }
}
