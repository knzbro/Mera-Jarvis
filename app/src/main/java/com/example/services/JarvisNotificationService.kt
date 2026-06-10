package com.example.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class JarvisNotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {
    private val TAG = "JarvisNotification"
    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN") // Setting language to hindi/hinglish logic
        } else {
            Log.e(TAG, "Initialization of TTS failed")
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Jarvis Notification Listener Connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        Log.d(TAG, "Notification Received from: $packageName")
        
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: "Unknown"
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        
        if (text.isNotEmpty() && com.example.util.JarvisPreferences.getBoolean(this, "notification_read_enabled", true)) {
            val appName = packageName.substringAfterLast(".")
            val speechText = "Kashif Bhai Ye Notification Is Waqat $appName app per aaya hai. $title ne bheja hai, jisme likha hai: $text"
            speakOut(speechText)
        }
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        Log.d(TAG, "Notification Removed from: ${sbn.packageName}")
    }

    override fun onDestroy() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}
