package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.Locale

class JarvisVoiceService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private val TAG = "JarvisVoice"
    private val CHANNEL_ID = "jarvis_voice_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis Active")
            .setContentText("Awaiting orders, Kashif Bhai...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Default icon, change later
            .build()

        startForeground(1, notification)
        tts = TextToSpeech(this, this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Voice Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager: NotificationManager? = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
            speakOut("Assalamualaikum Kashif Bhai Me Activate Hogya Ho Ab Me Kia Kaam Kero")
            // Here you initialize SpeechRecognizer and start listening
            startListening()
        } else {
            Log.e(TAG, "TTS Init failed")
        }
    }

    private fun startListening() {
        // Pseudo code for actual continuous voice recognition. 
        // In reality, background continuous listening requires a Wakeword Engine (like Porcupine)
        // or a looping SpeechRecognizer which consumes heavy battery.
        // For now, this is where it's mocked to intercept commands.
        Log.d(TAG, "Listening for 'Jarvis' or 'Bhai'...")
    }

    fun handleCommand(command: String) {
        val lowerCmd = command.lowercase(Locale.getDefault())
        when {
            lowerCmd.contains("flashlight on") -> {
                toggleFlashlight(true)
                speakOut("Flashlight on kar di hai")
            }
            lowerCmd.contains("flashlight off") -> {
                toggleFlashlight(false)
                speakOut("Flashlight off kar di hai")
            }
            lowerCmd.contains("wifi on") -> {
                speakOut("Wifi on karne ki request receive hui")
                // toggleWifi(true)
            }
            lowerCmd.contains("play song") -> {
                speakOut("Pro Level folder se song play kar raha hu")
                // playMedia()
            }
            else -> speakOut("Samajh nahi aaya Bhai, dobara boliye.")
        }
    }

    private fun toggleFlashlight(status: Boolean) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, status)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        speakOut("Jarvis Deactivated")
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
