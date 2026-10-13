package com.kolee.tracklocation.feature.overlay.ui

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kolee.tracklocation.feature.overlay.OverlayUiState

private const val TAG = "OverlayWindowHost"

private const val STRIP_MAX_WIDTH_DP = 382
private const val STRIP_SIDE_GUTTER_DP = 4
private const val STRIP_TOP_GAP_DP = 8
const val OVERLAY_CIRCLE_SIZE_DP = 48
private const val CIRCLE_MARGIN_DP = 8
private const val CIRCLE_DEFAULT_Y_FRACTION = 0.25f

/** Window root: ComposeView child plus BACK / outside-touch interception, used only while focusable. */
private class OverlayRootView(
    context: Context,
    val compose: ComposeView,
    private val dismissWhenFocusable: () -> Unit
) : FrameLayout(context) {
    init { addView(compose) }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) dismissWhenFocusable()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_OUTSIDE) {
            dismissWhenFocusable()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}

/**
 * ADR-029 / UI-SPEC 5d: owns the WindowManager window of the session overlay (strip or circle).
 * [show]/[hide]/[destroy] are idempotent. Main thread only. Visibility policy lives elsewhere (slice 6).
 *
 * Flags: NOT_FOCUSABLE (dropped while the Fill-up form is open, then WATCH_OUTSIDE_TOUCH is added) keeps the keyboard/back with the app below, NOT_TOUCH_MODAL lets touches outside the window
 * reach it, LAYOUT_IN_SCREEN makes x/y screen-absolute (the status bar offset is added explicitly).
 */
class OverlayWindowHost(
    private val context: Context,
    // Slice 7: called on BACK or an outside touch while the window is focusable (the Fill-up form is open).
    private val onDismissRequest: () -> Unit = {},
    private val content: @Composable () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val lifecycleOwner = OverlayLifecycleOwner()

    private var composeView: OverlayRootView? = null
    private var params: WindowManager.LayoutParams? = null

    private var collapsed = false
    private var savedCircleX = -1
    private var savedCircleY = -1
    // Live circle position in px while/after dragging; -1 until first computed.
    private var circleX = -1
    private var circleY = -1
    private var dragging = false
    private var focusable = false

    val isShowing: Boolean get() = composeView != null

    /** Adds the window; returns false when the permission is missing or the system refuses it. */
    fun show(state: OverlayUiState): Boolean {
        if (composeView != null) {
            applyState(state)
            return true
        }
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "show skipped: overlay permission not granted")
            return false
        }
        readState(state)
        val compose = ComposeView(context).apply {
            ViewTreeLifecycleOwner.set(this, lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            ViewTreeViewModelStoreOwner.set(this, lifecycleOwner)
            setContent { content() }
        }
        val view = OverlayRootView(context, compose) { if (focusable) onDismissRequest() }
        // Owners must be on the window root view too: Compose looks them up from the root (ERR-012).
        ViewTreeLifecycleOwner.set(view, lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        ViewTreeViewModelStoreOwner.set(view, lifecycleOwner)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags(),
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        fillLayout(lp)
        return try {
            windowManager.addView(view, lp)
            composeView = view
            params = lp
            true
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "addView failed", e)
            view.compose.disposeComposition()
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "addView failed", e)
            view.compose.disposeComposition()
            false
        }
    }

    fun hide() {
        val view = composeView ?: return
        composeView = null
        params = null
        dragging = false
        focusable = false
        hideIme(view)
        try {
            windowManager.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "removeView: view was not attached", e)
        }
        view.compose.disposeComposition()
    }

    fun destroy() {
        hide()
        lifecycleOwner.destroy()
    }

    /** Re-applies mode (strip/circle) and the saved circle position; a live drag keeps its own position. */
    fun applyState(state: OverlayUiState) {
        readState(state)
        relayout()
    }

    /** Rotation / density / screen-size change: positions are recomputed from the current display metrics. */
    fun onConfigurationChanged() = relayout()

    /** Moves the circle by a px delta (clamped to the screen). No-op in strip mode. */
    fun moveBy(dxPx: Float, dyPx: Float) {
        if (!collapsed || composeView == null) return
        dragging = true
        ensureCircleDefault()
        circleX += dxPx.toInt()
        circleY += dyPx.toInt()
        relayout()
    }

    /** Ends a drag; returns the clamped circle position to persist. */
    fun endDrag(): Pair<Int, Int> {
        dragging = false
        return circleX to circleY
    }

    /**
     * Toggles FLAG_NOT_FOCUSABLE (slice 7: the Fill-up form needs input focus, keyboard, BACK and outside touch).
     * Focusable: IME shown via softInputMode, ADJUST_PAN (panel sits at the top, so it is never covered).
     * Not focusable: IME hidden and the window is pass-through again.
     */
    fun setFocusable(value: Boolean) {
        focusable = value
        val lp = params ?: return
        val view = composeView ?: return
        if (!value) hideIme(view)
        lp.flags = flags()
        lp.softInputMode = if (value) {
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        } else {
            WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
        }
        fillLayout(lp)
        try {
            windowManager.updateViewLayout(view, lp)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "updateViewLayout: view was not attached", e)
        }
    }

    private fun hideIme(view: android.view.View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun readState(state: OverlayUiState) {
        collapsed = state.collapsed
        if (state.circleX != savedCircleX || state.circleY != savedCircleY) {
            savedCircleX = state.circleX
            savedCircleY = state.circleY
            if (!dragging) {
                circleX = state.circleX
                circleY = state.circleY
            }
        }
    }

    private fun relayout() {
        val view = composeView ?: return
        val lp = params ?: return
        fillLayout(lp)
        try {
            windowManager.updateViewLayout(view, lp)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "updateViewLayout: view was not attached", e)
        }
    }

    private fun flags(): Int {
        var f = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        f = if (focusable) f or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        else f or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        return f
    }

    @Suppress("DEPRECATION")
    private fun realMetrics(): DisplayMetrics =
        DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }

    private fun statusBarHeightPx(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    private fun navBarHeightPx(): Int {
        val id = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    private fun ensureCircleDefault() {
        if (circleX >= 0 && circleY >= 0) return
        val dm = realMetrics()
        val d = dm.density
        circleX = dm.widthPixels - (OVERLAY_CIRCLE_SIZE_DP * d).toInt() - (CIRCLE_MARGIN_DP * d).toInt()
        circleY = (dm.heightPixels * CIRCLE_DEFAULT_Y_FRACTION).toInt()
    }

    private fun fillLayout(lp: WindowManager.LayoutParams) {
        val dm = realMetrics()
        val d = dm.density
        if (!collapsed) {
            val widthDp = minOf(dm.widthPixels / d - 2 * STRIP_SIDE_GUTTER_DP, STRIP_MAX_WIDTH_DP.toFloat())
            lp.width = (widthDp * d).toInt()
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            lp.x = 0
            lp.y = statusBarHeightPx() + (STRIP_TOP_GAP_DP * d).toInt()
        } else {
            val size = (OVERLAY_CIRCLE_SIZE_DP * d).toInt()
            val margin = (CIRCLE_MARGIN_DP * d).toInt()
            ensureCircleDefault()
            circleX = circleX.coerceIn(margin, maxOf(margin, dm.widthPixels - size - margin))
            circleY = circleY.coerceIn(
                statusBarHeightPx() + margin,
                maxOf(statusBarHeightPx() + margin, dm.heightPixels - navBarHeightPx() - size - margin)
            )
            lp.width = size
            lp.height = size
            lp.gravity = Gravity.TOP or Gravity.START
            lp.x = circleX
            lp.y = circleY
        }
    }
}
