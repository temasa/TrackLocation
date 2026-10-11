package com.kolee.tracklocation.observer

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

/**
 * True when this app's own [ObserverAccessibilityService] is enabled in system settings.
 * Shared by the Observer Feed banner and the floating status banner (ADR-028) so both
 * match package + class the same way.
 */
fun isObserverServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    return enabled.any {
        it.resolveInfo.serviceInfo.packageName == context.packageName &&
        it.resolveInfo.serviceInfo.name == ObserverAccessibilityService::class.java.name
    }
}
