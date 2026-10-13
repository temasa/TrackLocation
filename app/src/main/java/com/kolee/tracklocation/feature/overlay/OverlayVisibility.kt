package com.kolee.tracklocation.feature.overlay

import android.content.Context
import android.provider.Settings

/** ADR-029 Decision 3: the overlay permission can be revoked any time, so callers re-check it on every show/start. */
object OverlayVisibility {
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)
}
