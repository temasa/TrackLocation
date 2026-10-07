package com.kolee.tracklocation.debug

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat

/** Transparent host for the system MediaProjection consent dialog (ADR-021). Debug builds only. */
class DiagnosticConsentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return // result is still pending from before recreation
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            @Suppress("DEPRECATION")
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ)
        } catch (t: Throwable) {
            Log.e(TAG, "consent request failed", t)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    ContextCompat.startForegroundService(
                        this,
                        Intent(this, DiagnosticRecorderService::class.java)
                            .setAction(DiagnosticRecorderService.ACTION_START)
                            .putExtra(DiagnosticRecorderService.EXTRA_CODE, resultCode)
                            .putExtra(DiagnosticRecorderService.EXTRA_DATA, data)
                    )
                } catch (t: Throwable) {
                    Log.e(TAG, "service start failed", t)
                }
            } else {
                Log.i(TAG, "consent denied; no recording this foreground session")
            }
        }
        finish()
    }

    private companion object {
        const val REQ = 4021
    }
}
