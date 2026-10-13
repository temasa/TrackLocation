package com.kolee.tracklocation.feature.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** True when "Display over other apps" is granted for this app. */
fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

/** Deep link to this app's "Display over other apps" system screen. */
fun overlaySettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        // Only a non-Activity context needs a new task; callers in the UI pass the Activity.
        .apply { if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
