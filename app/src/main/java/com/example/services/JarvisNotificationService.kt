package com.example.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class JarvisNotificationService : NotificationListenerService() {
    private val TAG = "JarvisNotification"

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Jarvis Notification Listener Connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        Log.d(TAG, "Notification Received from: $packageName")
        
        // This is a stub for notification intercept and reply logic.
        if (packageName == "com.whatsapp" || packageName.contains("messaging")) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title")
            val text = extras.getCharSequence("android.text")?.toString()
            Log.d(TAG, "Message from $title: $text")
            
            // Here Jarvis would evaluate the text, generate a reply with OpenRouter API,
            // and use remote input to send the reply back.
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        Log.d(TAG, "Notification Removed from: ${sbn.packageName}")
    }
}
