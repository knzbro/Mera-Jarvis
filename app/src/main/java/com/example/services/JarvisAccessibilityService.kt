package com.example.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class JarvisAccessibilityService : AccessibilityService() {
    private val TAG = "JarvisAccessibility"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Jarvis Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // This is a stub for where scraping and blocking UI logic goes.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Log.d(TAG, "Window State Changed: $packageName")
            
            // If active app is NOT in ['Chrome', 'Google Maps', 'Supabase', 'KNZ Worker\'s'] during work hours
            val allowedApps = listOf(
                "com.android.chrome", 
                "com.google.android.apps.maps", 
                "io.supabase.app", 
                "com.knz.worker"
            )
            
            if (!allowedApps.contains(packageName)) {
                // Here we would deploy the System Overlay Window
                Log.d(TAG, "Unproductive app detected ($packageName). Overlay should be deployed here.")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Jarvis Accessibility Service Interrupted")
    }
}
