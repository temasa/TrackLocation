package com.kolee.tracklocation.debug

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Debug-only entry point (ADR-021). Declared in the debug manifest; its onCreate() hooks the app's
 * activity lifecycle so no main-source code needs to know about the recorder.
 */
class DiagnosticsInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        try {
            val app = context?.applicationContext as? Application ?: return false
            DiagnosticsController.install(app)
        } catch (t: Throwable) {
            Log.e(TAG, "install failed", t)
        }
        return false
    }

    override fun query(u: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?) = 0
}

/**
 * Counts started app activities (the consent activity excluded). First one up in a foreground session
 * resumes the live recording session if there is one, otherwise shows the consent screen (once per
 * foreground session; a denial or "Stop & keep" is not re-asked until the app was backgrounded).
 * When all activities are stopped (debounced for rotation/relaunch) a live session is paused, not ended.
 */
internal object DiagnosticsController : Application.ActivityLifecycleCallbacks {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var app: Application
    private var started = 0
    private var prompted = false      // consent already requested in this foreground session
    private var consentAlive = false  // consent activity currently exists

    private val onBackground = Runnable {
        if (started > 0 || consentAlive) return@Runnable
        prompted = false // next foreground may ask again if no session is live
        try {
            DiagnosticRecorderService.instance?.pause()
        } catch (t: Throwable) {
            Log.w(TAG, "pause failed", t)
        }
    }

    fun install(application: Application) {
        app = application
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity is DiagnosticConsentActivity) return
        started++
        handler.removeCallbacks(onBackground)
        if (!prompted && !consentAlive) {
            prompted = true
            val live = DiagnosticRecorderService.instance
            if (live != null) {
                // Session survived the background trip: resume silently, no dialog.
                try { live.resume() } catch (t: Throwable) { Log.w(TAG, "resume failed", t) }
                return
            }
            try {
                activity.startActivity(Intent(activity, DiagnosticConsentActivity::class.java))
            } catch (t: Throwable) {
                Log.w(TAG, "consent launch failed", t)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity is DiagnosticConsentActivity) return
        if (started > 0) started--
        if (started == 0) handler.postDelayed(onBackground, 1500)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is DiagnosticConsentActivity) consentAlive = true
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is DiagnosticConsentActivity) consentAlive = false
    }

    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}

internal const val TAG = "DiagRec"
