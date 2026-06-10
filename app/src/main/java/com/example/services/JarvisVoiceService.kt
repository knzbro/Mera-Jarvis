package com.example.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import android.widget.Toast

class JarvisVoiceService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private val TAG = "JarvisVoiceService"

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
            speakOut("Assalamualaikum Kashif Bhai Me Apki Kasy Madad Ker skta Ho")
        } else {
            Log.e(TAG, "TTS Init failed")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Here we would configure background speech recognizer for wake word "Jarvis"
        Toast.makeText(this, "Jarvis Voice Module Activated", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
