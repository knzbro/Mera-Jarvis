package com.example.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class JarvisAccessibilityService : AccessibilityService() {
    private val TAG = "JarvisAccessibility"

    companion object {
        var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Jarvis Accessibility Service Connected")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // This is a stub for where scraping and blocking UI logic goes.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Log.d(TAG, "Window State Changed: $packageName")
            
            // If active app is NOT in ['Chrome', 'Google Maps', 'Supabase', 'KNZ Worker\'s'] during work hours
            val allowedApps = com.example.util.JarvisPreferences.getAllowedApps(this)
            
            // Ignore system UI and launcher to prevent lockouts
            if (packageName != "com.android.systemui" && packageName != "com.google.android.apps.nexuslauncher" && packageName != applicationContext.packageName) {
                if (!allowedApps.contains(packageName)) {
                    Log.d(TAG, "Unproductive app detected ($packageName).")
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Jarvis Accessibility Service Interrupted")
    }
}
