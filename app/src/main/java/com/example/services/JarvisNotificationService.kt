package com.example.services

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

class JarvisNotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {
    private val TAG = "JarvisNotification"
    private var tts: TextToSpeech? = null

    companion object {
        var lastNotificationAction: Notification.Action? = null
        var lastSbn: StatusBarNotification? = null
        private val replyActionsMap = java.util.concurrent.ConcurrentHashMap<String, Pair<Notification.Action, StatusBarNotification>>()

        fun replyToLastNotification(context: Context, replyText: String): Boolean {
            val action = lastNotificationAction ?: return false
            val sbn = lastSbn ?: return false
            val remoteInputs = action.remoteInputs ?: return false
            if (remoteInputs.isEmpty()) return false

            try {
                val intent = Intent()
                val bundle = Bundle()
                for (input in remoteInputs) {
                    bundle.putCharSequence(input.resultKey, replyText)
                }
                RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
                action.actionIntent.send(context, 0, intent)
                Log.d("JarvisNotification", "Draft reply sent successfully: $replyText")
                return true
            } catch (e: Exception) {
                Log.e("JarvisNotification", "Failed to send auto reply", e)
                return false
            }
        }

        fun replyToSender(context: Context, senderKey: String, replyText: String): Boolean {
            val queryKey = senderKey.lowercase().trim()
            if (queryKey.isEmpty()) return false
            
            // Try to match sender name or phoneNumber in our map
            val matchedEntry = replyActionsMap.entries.firstOrNull { entry ->
                val cachedKey = entry.key.lowercase().trim()
                cachedKey.contains(queryKey) || queryKey.contains(cachedKey)
            }
            
            if (matchedEntry == null) {
                Log.d("JarvisNotification", "No active WhatsApp or messenger session found for target: $senderKey")
                return false
            }

            val (action, sbn) = matchedEntry.value
            val remoteInputs = action.remoteInputs ?: return false
            if (remoteInputs.isEmpty()) return false

            try {
                val intent = Intent()
                val bundle = Bundle()
                for (input in remoteInputs) {
                    bundle.putCharSequence(input.resultKey, replyText)
                }
                RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
                action.actionIntent.send(context, 0, intent)
                Log.d("JarvisNotification", "Targeted reply sent to ${matchedEntry.key}: $replyText")
                return true
            } catch (e: Exception) {
                Log.e("JarvisNotification", "Failed to send auto targeted reply to $senderKey", e)
                return false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
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
        
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString("android.title") ?: "Unknown"
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        val actions = notification.actions
        var capturedAction: Notification.Action? = null
        if (actions != null) {
            for (action in actions) {
                if (action.remoteInputs != null && action.remoteInputs.isNotEmpty()) {
                    lastNotificationAction = action
                    lastSbn = sbn
                    capturedAction = action
                    if (title.isNotEmpty() && title != "Unknown") {
                        replyActionsMap[title.lowercase().trim()] = Pair(action, sbn)
                    }
                    Log.d(TAG, "Captured WhatsApp/message remote input action successfully for: $title")
                    break
                }
            }
        }
        
        if (packageName == "com.whatsapp" && text.isNotEmpty() && capturedAction != null) {
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            // Focus Mode active between 12:00 and 13:00
            val isBusy = hour in 12..13
            if (isBusy) {
                Log.d(TAG, "Global state is BUSY (Focus Mode). Generating auto-reply via AI for WhatsApp...")
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val prompt = "Kashif Bhai is currently busy in Focus Mode. You received a WhatsApp message from '$title'. Message: '$text'. Write a short, polite Urdu/English auto-reply response on behalf of Kashif Bhai explaining he is busy and will reply later."
                        val response = com.example.util.GeminiHelper.processCommand(this@JarvisNotificationService, prompt)
                        val aiReply = response.response + "\n\nThis message generated by Our Ai Assiant"
                        
                        // Send the auto-reply
                        val intent = Intent()
                        val bundle = Bundle()
                        for (input in capturedAction.remoteInputs!!) {
                            bundle.putCharSequence(input.resultKey, aiReply)
                        }
                        RemoteInput.addResultsToIntent(capturedAction.remoteInputs, intent, bundle)
                        capturedAction.actionIntent.send(this@JarvisNotificationService, 0, intent)
                        Log.d(TAG, "Auto-reply sent successfully to $title")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error generating or sending auto-reply", e)
                    }
                }
                return // Skip reading it out loud if we auto-replied
            }
        }

        if (text.isNotEmpty() && com.example.util.JarvisPreferences.getBoolean(this, "notification_read_enabled", true)) {
            val appName = packageName.substringAfterLast(".")
            val speechText = "Kashif Bhai, ye notification is waqt $appName application par aaya hai. $title ne bheja hai, jisme likha hai: $text"
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
