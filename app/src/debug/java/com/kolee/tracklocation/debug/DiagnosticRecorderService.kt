package com.kolee.tracklocation.debug

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug-only rolling screen recorder (ADR-021). Video only, 2-minute MP4 segments in
 * Movies/TrackLocation-Diagnostics, last [KEEP] kept.
 *
 * Rollover: MediaRecorder.setMaxDuration(2 min) -> on MAX_DURATION_REACHED a NEW MediaRecorder is
 * prepared, the VirtualDisplay is re-pointed at its surface and it is started, then the old one is
 * stopped and its MediaStore entry finalized (sub-second gap; setNextOutputFile is size-triggered).
 */
class DiagnosticRecorderService : Service() {

    private class Segment(
        val recorder: MediaRecorder,
        val pfd: ParcelFileDescriptor,
        val uri: Uri?,
        val file: File?,
        val width: Int,
        val height: Int,
        val dpi: Int,
    )

    private val handler = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var seg: Segment? = null
    private var segCount = 0
    private var destroyed = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            handler.post {
                Log.w(TAG, "projection stopped by system")
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START -> start(intent)
                ACTION_KEEP -> keep()
                else -> stopSelf()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onStartCommand failed", t)
            alert("Recorder error: ${t.javaClass.simpleName}")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(intent: Intent) {
        startInForeground(notification("Recording screen (last $KEEP x 2 min)", withAction = true))
        if (projection != null) return // already recording
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
        if (data == null) {
            stopSelf()
            return
        }
        if (Build.VERSION.SDK_INT < 29 && !hasLegacyStorage()) {
            alert("Recorder needs storage permission on Android 9 (grant WRITE_EXTERNAL_STORAGE in app settings)")
            stopSelf()
            return
        }
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = mpm.getMediaProjection(intent.getIntExtra(EXTRA_CODE, 0), data)
        if (p == null) {
            stopSelf()
            return
        }
        projection = p
        p.registerCallback(projectionCallback, handler) // must precede createVirtualDisplay
        beginSegment()
    }

    /** "Stop & keep": freeze retention, finalize the current segment and stop; all clips stay. */
    private fun keep() {
        frozen = true
        finishCurrent()
        alert("Recording stopped. Existing clips kept (retention frozen until app restart).")
        stopSelf()
    }

    // region segments

    /** Opens + prepares a new segment, points the display at it, starts it, then retires the old one. */
    private fun beginSegment() {
        val next = openSegment()
        try {
            val d = display
            if (d == null) {
                display = projection!!.createVirtualDisplay(
                    "DiagRec", next.width, next.height, next.dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, next.recorder.surface, null, handler
                )
            } else {
                d.resize(next.width, next.height, next.dpi)
                d.surface = next.recorder.surface
            }
            next.recorder.start()
        } catch (t: Throwable) {
            discard(next)
            throw t
        }
        val old = seg
        seg = next
        old?.let { finish(it) }
        segCount++
        enforceRetention()
        notifyState("Recording screen, segment $segCount (last $KEEP x 2 min)")
    }

    private fun openSegment(): Segment {
        val name = "diag_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4"
        var uri: Uri? = null
        var file: File? = null
        val pfd: ParcelFileDescriptor
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, REL_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert returned null")
            pfd = try {
                contentResolver.openFileDescriptor(uri, "w") ?: error("openFileDescriptor returned null")
            } catch (t: Throwable) {
                contentResolver.delete(uri, null, null)
                throw t
            }
        } else {
            val dir = legacyDir().apply { mkdirs() }
            file = File(dir, name)
            pfd = ParcelFileDescriptor.open(
                file, ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
            )
        }
        var rec: MediaRecorder? = null
        try {
            val (w, h, dpi) = captureSize()
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            rec = r
            r.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            r.setVideoSize(w, h)
            r.setVideoFrameRate(30)
            r.setVideoEncodingBitRate(4_000_000)
            r.setMaxDuration(SEGMENT_MS)
            r.setOutputFile(pfd.fileDescriptor)
            r.setOnInfoListener { mr, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED && mr === seg?.recorder) rollOver()
            }
            r.prepare()
            return Segment(r, pfd, uri, file, w, h, dpi)
        } catch (t: Throwable) {
            try { rec?.release() } catch (_: Throwable) {}
            try { pfd.close() } catch (_: Throwable) {}
            removeEntry(uri, file)
            throw t
        }
    }

    private fun rollOver() {
        if (destroyed) return
        try {
            beginSegment()
        } catch (t: Throwable) {
            Log.e(TAG, "rollover failed", t)
            alert("Recording stopped: segment rollover failed (${t.javaClass.simpleName})")
            stopSelf()
        }
    }

    private fun finishCurrent() {
        val s = seg ?: return
        seg = null
        finish(s)
    }

    /** Stops the recorder and publishes the clip; empty/failed clips are deleted. */
    private fun finish(s: Segment) {
        var ok = true
        try { s.recorder.stop() } catch (t: Throwable) { ok = false; Log.w(TAG, "recorder.stop failed", t) }
        try { s.recorder.release() } catch (_: Throwable) {}
        val empty = try { s.pfd.statSize == 0L } catch (_: Throwable) { false }
        try { s.pfd.close() } catch (_: Throwable) {}
        if (!ok || empty) {
            removeEntry(s.uri, s.file)
            return
        }
        try {
            if (s.uri != null) {
                contentResolver.update(
                    s.uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null
                )
            } else if (s.file != null) {
                MediaScannerConnection.scanFile(this, arrayOf(s.file.absolutePath), arrayOf("video/mp4"), null)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "finalize failed", t)
        }
    }

    private fun discard(s: Segment) {
        try { s.recorder.release() } catch (_: Throwable) {}
        try { s.pfd.close() } catch (_: Throwable) {}
        removeEntry(s.uri, s.file)
    }

    private fun removeEntry(uri: Uri?, file: File?) {
        try {
            if (uri != null) contentResolver.delete(uri, null, null)
            file?.delete()
        } catch (t: Throwable) {
            Log.w(TAG, "removeEntry failed", t)
        }
    }

    // endregion

    // region retention

    /** Deletes finalized clips beyond the newest [KEEP] (skipping any we do not own). No-op once frozen. */
    private fun enforceRetention() {
        if (frozen) return
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val uris = ArrayList<Uri>()
                contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                    arrayOf("$REL_PATH%", "diag_%"),
                    "${MediaStore.MediaColumns.DISPLAY_NAME} DESC"
                )?.use { c ->
                    while (c.moveToNext()) {
                        uris += Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, c.getLong(0).toString())
                    }
                }
                uris.drop(KEEP).forEach {
                    try { contentResolver.delete(it, null, null) } catch (e: SecurityException) { Log.i(TAG, "not owned, skipped $it") }
                }
            } else {
                legacyDir().listFiles { f -> f.name.startsWith("diag_") && f.name.endsWith(".mp4") }
                    ?.sortedByDescending { it.name }?.drop(KEEP)?.forEach { f ->
                        f.delete()
                        MediaScannerConnection.scanFile(this, arrayOf(f.absolutePath), null, null)
                    }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "retention failed", t)
        }
    }

    // endregion

    private fun captureSize(): Triple<Int, Int, Int> {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(dm)
        val scale = minOf(1f, 1280f / maxOf(dm.widthPixels, dm.heightPixels))
        val w = (dm.widthPixels * scale).toInt() and 1.inv()
        val h = (dm.heightPixels * scale).toInt() and 1.inv()
        return Triple(w, h, dm.densityDpi)
    }

    private fun hasLegacyStorage() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun legacyDir() =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), DIR_NAME)

    // region notification

    private fun notification(text: String, withAction: Boolean): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Diagnostic recorder", NotificationManager.IMPORTANCE_LOW)
        )
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("[TrackLocation] Diagnostic recorder")
            .setContentText(text)
            .setOngoing(withAction)
            .setAutoCancel(!withAction)
        if (withAction) {
            val pi = PendingIntent.getService(
                this, 0,
                Intent(this, DiagnosticRecorderService::class.java).setAction(ACTION_KEEP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            b.addAction(0, "Stop & keep", pi)
        }
        return b.build()
    }

    private fun startInForeground(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun notifyState(text: String) {
        try {
            getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notification(text, true))
        } catch (t: Throwable) {
            Log.w(TAG, "notify failed", t)
        }
    }

    /** Separate (dismissable) notification for terminal states such as "kept" or errors. */
    private fun alert(text: String) {
        Log.i(TAG, text)
        try {
            getSystemService(NotificationManager::class.java).notify(NOTIF_ALERT_ID, notification(text, false))
        } catch (t: Throwable) {
            Log.w(TAG, "alert failed", t)
        }
    }

    // endregion

    override fun onDestroy() {
        destroyed = true
        try { finishCurrent() } catch (t: Throwable) { Log.w(TAG, "finish on destroy failed", t) }
        try { display?.release() } catch (_: Throwable) {}
        display = null
        try {
            projection?.unregisterCallback(projectionCallback)
            projection?.stop()
        } catch (_: Throwable) {}
        projection = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.kolee.tracklocation.debug.START"
        const val ACTION_KEEP = "com.kolee.tracklocation.debug.KEEP"
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"

        private const val DIR_NAME = "TrackLocation-Diagnostics"
        private const val REL_PATH = "Movies/$DIR_NAME/"
        private const val SEGMENT_MS = 120_000
        private const val KEEP = 5
        private const val CHANNEL = "diag_recorder"
        private const val NOTIF_ID = 9021
        private const val NOTIF_ALERT_ID = 9022

        /** Set by "Stop & keep"; lives until the process dies. While set, no clip is ever deleted. */
        @Volatile private var frozen = false
    }
}
