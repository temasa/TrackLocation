package com.kolee.tracklocation.feature.overlay.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kolee.tracklocation.R
import com.kolee.tracklocation.TrackApp
import com.kolee.tracklocation.feature.overlay.OverlayStateHolder
import com.kolee.tracklocation.feature.overlay.OverlayUiState
import com.kolee.tracklocation.feature.overlay.OverlayVisibility
import com.kolee.tracklocation.feature.obd.FillUpRecorder
import com.kolee.tracklocation.feature.overlay.ui.OverlayContent
import com.kolee.tracklocation.feature.overlay.ui.OverlayWindowHost
import com.kolee.tracklocation.utils.OVERLAY_CHANNEL_ID
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "OverlayService"

/**
 * ADR-029: foreground service hosting the session overlay banner: lifecycle, state holder and the window
 * ([OverlayWindowHost]). Visible only while a session is active AND the app is in the background AND the
 * overlay permission is granted (Decision 2/3); [evaluate] applies that rule.
 */
class OverlayService : Service() {

    // Same pattern as TrackingService: log instead of letting an uncaught exception kill the process.
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "serviceScope coroutine failed", throwable)
    }
    // Main dispatcher: the holder creates a ViewModel (viewModelScope is Main.immediate) and slice 5 adds a window.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + exceptionHandler)
    private var stateHolder: OverlayStateHolder? = null
    private var windowHost: OverlayWindowHost? = null
    // Read by OverlayContent inside the ComposeView, so assigning a new value recomposes the window content.
    private val uiState = mutableStateOf(OverlayUiState())
    // Slice 7: Fill-up form open. Not persisted; reset on hide/shrink/save/cancel.
    private val fillUpOpen = mutableStateOf(false)
    // Null until the session query has emitted once, so the stateIn default (sessionActive=false) never stops the service.
    private var sessionActive: Boolean? = null
    private var appInForeground = false

    // ProcessLifecycleOwner debounces ON_STOP by ~700 ms (rotation/activity switches), so brief transitions never flicker.
    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            appInForeground = true
            evaluate()
        }

        override fun onStop(owner: LifecycleOwner) {
            appInForeground = false
            evaluate()
        }
    }

    companion object {
        const val ACTION_START_OVERLAY = "START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "STOP_OVERLAY"
        // OBD service uses 2001; TrackingService's id is random.
        const val NOTIFICATION_ID = 2002

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).setAction(ACTION_START_OVERLAY)
            ContextCompat.startForegroundService(context, intent)
        }

        // stopService: never (re)creates the service just to stop it.
        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Promptly, to satisfy the Android 12+ 5s foreground-start rule.
        startForeground(NOTIFICATION_ID, createNotification())
        val holder = OverlayStateHolder(application as TrackApp, serviceScope)
        stateHolder = holder
        serviceScope.launch {
            holder.state.collect { state ->
                uiState.value = state
                if (state.collapsed) closeFillUp()
                windowHost?.applyState(state)
                // Also re-checks the permission, so a revocation while shown hides the banner on the next data tick.
                evaluate()
            }
        }
        serviceScope.launch {
            // Same source as the holder's sessionActive; the first emission is the real DB value.
            (application as TrackApp).sessionDao.getAllSessions()
                .map { sessions -> sessions.any { it.isActive } }
                .distinctUntilChanged()
                .collect {
                    sessionActive = it
                    evaluate()
                }
        }
        // onCreate runs on the main thread, as ProcessLifecycleOwner requires.
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        appInForeground = processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        processLifecycle.addObserver(processObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand, intent.action: ${intent?.action}")
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                hideOverlay()
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            // START_OVERLAY and the null intent of a START_STICKY redelivery both just (re)ensure the foreground
            // state; visibility (or stopSelf when no session is active) is decided by evaluate().
            else -> {
                startForeground(NOTIFICATION_ID, createNotification())
                evaluate()
            }
        }
        return START_STICKY
    }

    /** Decision 2/3 rule; idempotent. Does nothing until the session state is known. */
    private fun evaluate() {
        when (sessionActive) {
            null -> return
            false -> {
                hideOverlay()
                stopForeground(true)
                stopSelf()
            }
            true -> if (!appInForeground && OverlayVisibility.canDrawOverlays(this)) showOverlay() else hideOverlay()
        }
    }

    private fun showOverlay() {
        val host = windowHost ?: OverlayWindowHost(this, onDismissRequest = { closeFillUp() }) {
            OverlayContent(
                state = uiState.value,
                onShrink = { setCollapsed(true) },
                onExpand = { setCollapsed(false) },
                onFillUp = { onFillUpRequested() },
                onDrag = { dx, dy -> windowHost?.moveBy(dx, dy) },
                onDragEnd = { onCircleDragEnd() },
                fillUpOpen = fillUpOpen.value,
                onFillUpCancel = { closeFillUp() },
                onFillUpSave = { litres, price -> saveFillUp(litres, price) }
            )
        }.also { windowHost = it }
        host.show(uiState.value)
    }

    private fun hideOverlay() {
        // Visibility rule (session end / app foreground / permission revoke) closes the form; host.hide() also
        // drops focus and the keyboard.
        fillUpOpen.value = false
        windowHost?.hide()
    }

    private fun setCollapsed(collapsed: Boolean) {
        val holder = stateHolder ?: return
        // Shrinking with the form open cancels the form first.
        if (collapsed) closeFillUp()
        serviceScope.launch { holder.setCollapsed(collapsed) }
    }

    private fun onCircleDragEnd() {
        val host = windowHost ?: return
        val holder = stateHolder ?: return
        val (x, y) = host.endDrag()
        serviceScope.launch { holder.saveCirclePosition(x, y) }
    }

    private fun onFillUpRequested() {
        if (uiState.value.collapsed) return
        fillUpOpen.value = true
        windowHost?.setFocusable(true)
    }

    private fun closeFillUp() {
        if (!fillUpOpen.value) return
        fillUpOpen.value = false
        windowHost?.setFocusable(false)
    }

    private fun saveFillUp(litres: Double, price: Double?) {
        val dao = (application as TrackApp).fuelEventDao
        // FillUpRecorder decides whether the price changed; fuel left / range update through the state flow.
        serviceScope.launch { FillUpRecorder(dao).record(litres, price) }
        closeFillUp()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        windowHost?.onConfigurationChanged()
    }

    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, OVERLAY_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_location_on_24)
            .setContentTitle("GPS Tracker")
            .setContentText("Session totals overlay")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        windowHost?.destroy()
        windowHost = null
        serviceScope.cancel()
        stateHolder?.close()
        stateHolder = null
    }
}
