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
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.util.GeminiHelper
import com.example.util.JarvisPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisVoiceService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private val TAG = "JarvisVoice"
    private val CHANNEL_ID = "jarvis_voice_channel"
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var mediaPlayer: android.media.MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis Voice Activated")
            .setContentText("Kashif Bhai, main background me command sun raha hoon...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
        tts = TextToSpeech(this, this)

        // Launch global high-tech glowing bottom bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            val glowIntent = Intent(this, JarvisGlowOverlayService::class.java)
            try {
                startService(glowIntent)
                Log.d(TAG, "Glow overlay successfully started at service startup.")
                com.example.util.JarvisLogger.success("SYS_OVERLAY", "Cyber bottom animation overlay started.")
            } catch (e: Exception) {
                Log.e(TAG, "Glow overlay start failed: ${e.message}")
            }
        }
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
            com.example.util.JarvisLogger.success("TTS_ENGINE", "Hindustani/Indian English voice engine loaded.")
            speakOut("Kashif Bhai, Jarvis activate ho gya hai. Ab me kya kaam karu?")
            startSpeechRecognizer()
        } else {
            Log.e(TAG, "TTS Init failed")
        }
    }

    private fun startSpeechRecognizer() {
        mainHandler.post {
            try {
                if (!JarvisPreferences.isJarvisActive(this)) {
                    Log.d(TAG, "Jarvis is not active in preferences. Stopping SpeechRecognizer.")
                    return@post
                }

                // Check microphone permission physically before launching listen block
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "SpeechRecognizer: RECORD_AUDIO permission not granted! Retrying with slow delay.")
                    speakOut("Kashif Bhai, audio compiler ke liye system microphone permission is waqt disabled hai.")
                    restartListeningWithDelay(12000)
                    return@post
                }

                if (speechRecognizer != null) {
                    try {
                        speechRecognizer?.destroy()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(TAG, "SpeechRecognizer: Ready")
                            com.example.util.JarvisLogger.info("SPEECH_REC", "Continuous microphone stream listening...")
                            isListening = true
                        }
                        override fun onBeginningOfSpeech() {
                            Log.d(TAG, "SpeechRecognizer: Beginning")
                        }
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            Log.d(TAG, "SpeechRecognizer: End Of Speech")
                            isListening = false
                        }
                        override fun onError(error: Int) {
                            Log.e(TAG, "SpeechRecognizer Error value: $error")
                            isListening = false
                            
                            val delayMs = when (error) {
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 3000L
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 12000L
                                else -> 1200L
                            }
                            // Restart continuous recognition with safe loop delay
                            restartListeningWithDelay(delayMs)
                        }
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val text = matches[0]
                                Log.d(TAG, "Recognized text: $text")
                                com.example.util.JarvisLogger.info("SPEECH_REC", "Captured raw: \"$text\"")
                                handleCommand(text)
                            }
                            restartListeningWithDelay(500)
                        }
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                if (recognizerIntent == null) {
                    recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-IN")
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }
                }

                speechRecognizer?.startListening(recognizerIntent)
                Log.d(TAG, "Listening loop launched successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Initialization of SpeechRecognizer failed", e)
                restartListeningWithDelay(3000)
            }
        }
    }

    private fun restartListeningWithDelay(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (JarvisPreferences.isJarvisActive(this@JarvisVoiceService)) {
                startSpeechRecognizer()
            }
        }, delayMs)
    }

    fun handleCommand(command: String) {
        val originalCmd = command.trim()
        val lowerCmd = originalCmd.lowercase(Locale.getDefault())
        
        // Check voice-wake conditions
        val sensitivity = JarvisPreferences.getString(this, "wake_word_sensitivity", "MEDIUM")
        val hasWakeWord = when (sensitivity.uppercase()) {
            "LOW" -> {
                // Strict: must contain "jarvis" keyword. Ignores "bhai"/"suno" completely to prevent false triggers.
                lowerCmd.contains("jarvis")
            }
            "MEDIUM" -> {
                // Balanced: must contain "jarvis", or contain "bhai" only if spoken with additional words
                lowerCmd.contains("jarvis") || (lowerCmd.contains("bhai") && lowerCmd.split("\\s+".toRegex()).size >= 2)
            }
            else -> {
                // High (highly responsive): match jarvis, bhai, or suno anywhere
                lowerCmd.contains("jarvis") || lowerCmd.contains("bhai") || lowerCmd.contains("suno")
            }
        }
        val voiceWakeEnabled = JarvisPreferences.getBoolean(this, "voice_wake_enabled", true)
        
        // Require wake-word for background speech inputs
        if (voiceWakeEnabled && !hasWakeWord) {
            Log.d(TAG, "Background input ignored (no wake word matched): $command")
            return
        }

        // Strip wake terms to extract the pristine command for Gemini
        val cleanedCmd = originalCmd
            .replace("jarvis", "", ignoreCase = true)
            .replace("bhai", "", ignoreCase = true)
            .replace("suno", "", ignoreCase = true)
            .replace("please", "", ignoreCase = true)
            .trim()

        Log.d(TAG, "Sending processed voice command to Gemini AI: $cleanedCmd")
        com.example.util.JarvisLogger.info("GEMINI_API", "Dispatched NLP prompt: \"$cleanedCmd\"")
        
        // Query Gemini API
        serviceScope.launch {
            try {
                val actionResult = GeminiHelper.processCommand(cleanedCmd)
                Log.d(TAG, "Gemini Resolved Response: Speech='${actionResult.response}', Action='${actionResult.action}', Arg='${actionResult.arg}'")
                com.example.util.JarvisLogger.success("GEMINI_API", "AI Reply: \"${actionResult.response}\" (Action -> ${actionResult.action})")
                
                // Read aloud the friendly AI response
                speakOut(actionResult.response)
                
                // Dispatch execution action
                executeAction(actionResult.action, actionResult.arg)
            } catch (e: Exception) {
                Log.e(TAG, "Coroutine execution exception reading Gemini result, falling back locally", e)
                localFallbackCommand(cleanedCmd)
            }
        }
    }

    private fun executeAction(actionName: String, actionArg: String) {
        Log.d(TAG, "Executing Action: $actionName with Argument: $actionArg")
        com.example.util.JarvisLogger.success("SYS_CORE", "Executing: $actionName ($actionArg)")
        when (actionName) {
            "flashlight_on" -> toggleFlashlight(true)
            "flashlight_off" -> toggleFlashlight(false)
            "wifi_on" -> toggleWifi(true)
            "wifi_off" -> toggleWifi(false)
            "bluetooth_on" -> toggleBluetooth(true)
            "bluetooth_off" -> toggleBluetooth(false)
            "data_on" -> toggleMobileData()
            "show_recents" -> showRecents()
            "go_home" -> goHome()
            "play_song" -> playSongInProLevel()
            "create_file" -> handleFileCommand("create file")
            "copy_file" -> handleFileCommand("copy")
            "move_file" -> handleFileCommand("move")
            "reply_notification" -> {
                val success = JarvisNotificationService.replyToSender(this, actionArg, actionArg) ||
                              JarvisNotificationService.replyToLastNotification(this, actionArg)
                if (success) {
                    Toast.makeText(this, "Reply Draft Sent: $actionArg", Toast.LENGTH_SHORT).show()
                } else {
                    speakOut("Bhai, reply bhejney ke liye active notifications ya WhatsApp content access nahi mil pa raha.")
                }
            }
            "open_app" -> openAppByName(actionArg)
        }
    }

    private fun localFallbackCommand(cleanedCmd: String) {
        val lower = cleanedCmd.lowercase()
        when {
            lower.contains("flashlight on") || lower.contains("flash light on") || lower.contains("torch on") -> {
                toggleFlashlight(true)
                speakOut("Kashif Bhai, flashlight turned on.")
            }
            lower.contains("flashlight off") || lower.contains("flash light off") || lower.contains("torch off") -> {
                toggleFlashlight(false)
                speakOut("Kashif Bhai, flashlight turned off.")
            }
            lower.contains("wifi on") -> toggleWifi(true)
            lower.contains("wifi off") -> toggleWifi(false)
            lower.contains("bluetooth on") -> toggleBluetooth(true)
            lower.contains("bluetooth off") -> toggleBluetooth(false)
            lower.contains("data on") || lower.contains("data off") || lower.contains("internet") -> toggleMobileData()
            lower.startsWith("open ") || lower.contains("kholo ") || lower.contains("chalao ") -> {
                val app = lower.replace("open ", "").replace("kholo ", "").replace("chalao ", "").trim()
                openAppByName(app)
            }
            lower.contains("recents") -> showRecents()
            lower.contains("home") -> goHome()
            lower.contains("song") || lower.contains("music") -> playSongInProLevel()
            lower.contains("create") || lower.contains("file") -> handleFileCommand(lower)
            else -> {
                speakOut("Kashif Bhai, ye command online process nahi ho saki ya internet disconnected hai.")
            }
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

    private fun toggleWifi(enable: Boolean) {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = enable
                speakOut("Kashif Bhai, WiFi " + (if (enable) "on" else "off") + " kar diya hai.")
            } else {
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                speakOut("Kashif Bhai, WiFi panel khol diya hai.")
            }
        } catch (e: Exception) {
            speakOut("WiFi control settings access limited.")
        }
    }

    private fun toggleBluetooth(enable: Boolean) {
        try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                speakOut("Bluetooth support missing.")
                return
            }
            if (enable) {
                if (!bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.enable()
                }
                speakOut("Bluetooth is now activated.")
            } else {
                if (bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.disable()
                }
                speakOut("Bluetooth has been turned off.")
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            speakOut("Kashif Bhai, Bluetooth settings panel open kar diya hai.")
        }
    }

    private fun toggleMobileData() {
        try {
            val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            speakOut("Kashif Bhai, cellular network options frame launch kar diya hai.")
        } catch (e: Exception) {
            speakOut("Data settings window redirect limited.")
        }
    }

    private fun openAppByName(appNameArg: String) {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedInfos = pm.queryIntentActivities(mainIntent, 0)
        
        var bestMatchPkg: String? = null
        var bestMatchLabel: String? = null
        
        val target = appNameArg.trim().lowercase(Locale.ROOT)
        for (info in resolvedInfos) {
            val label = info.loadLabel(pm).toString().lowercase(Locale.ROOT)
            val pkgName = info.activityInfo.packageName
            
            if (label == target || pkgName.lowercase(Locale.ROOT).contains(target)) {
                bestMatchPkg = pkgName
                bestMatchLabel = info.loadLabel(pm).toString()
                break
            } else if (label.contains(target) || target.contains(label)) {
                bestMatchPkg = pkgName
                bestMatchLabel = info.loadLabel(pm).toString()
            }
        }
        
        if (bestMatchPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(bestMatchPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                speakOut("Kashif Bhai, maine $bestMatchLabel application select karke launch kar diya hai.")
            } else {
                speakOut("Kashif Bhai, software launch process not found.")
            }
        } else {
            speakOut("Kashif Bhai, mujhe device par $appNameArg naam ka app nahi mila.")
        }
    }

    private fun showRecents() {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
            speakOut("Recently active applications panel khol diye hain.")
        } else {
            speakOut("Bhai accessibility permission check kijiye.")
        }
    }

    private fun goHome() {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            speakOut("Home par bhej diya.")
        } else {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            speakOut("Home screen active ho gayi hai.")
        }
    }

    private fun playSongInProLevel() {
        val baseDir = getExternalFilesDir(null) ?: filesDir
        val proLevelFolder = File(baseDir, "Pro Level")
        if (!proLevelFolder.exists()) {
            proLevelFolder.mkdirs()
        }

        val mp3Files = proLevelFolder.listFiles { _, name -> name.lowercase(Locale.ROOT).endsWith(".mp3") }
        if (!mp3Files.isNullOrEmpty()) {
            val song = mp3Files[0]
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(song.absolutePath)
                    prepare()
                    start()
                }
                speakOut("Pro Level directory se custom sound " + song.name + " play ho raha hai.")
            } catch (e: Exception) {
                speakOut("Audio streams system playback error.")
                e.printStackTrace()
            }
        } else {
            try {
                val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = android.media.MediaPlayer.create(this, notificationUri).apply {
                    start()
                }
                speakOut("Pro Level folder blank hai. Maine system alarm notify play kiya hai.")
            } catch (ex: Exception) {
                speakOut("Playback folder details holds no media files.")
            }
        }
    }

    private fun handleFileCommand(lowerCmd: String) {
        val baseDir = getExternalFilesDir(null) ?: filesDir
        val proLevelFolder = File(baseDir, "Pro Level")
        if (!proLevelFolder.exists()) {
            proLevelFolder.mkdirs()
        }

        when {
            lowerCmd.contains("create") || lowerCmd.contains("create file") || lowerCmd.contains("file banao") -> {
                val fileName = "test_data_" + (System.currentTimeMillis() % 1000) + ".txt"
                val file = File(proLevelFolder, fileName)
                file.writeText("Kashif Bhai ke liye is file ko system ne generate kiya hai.")
                speakOut("Kashif Bhai, 'Pro Level' folder ke andarr file " + file.name + " create ho gayi hai.")
            }
            lowerCmd.contains("move") || lowerCmd.contains("copy") || lowerCmd.contains("paste") -> {
                val files = proLevelFolder.listFiles()
                if (!files.isNullOrEmpty()) {
                    val targetFile = files[0]
                    val backupFolder = File(baseDir, "Backup Folder")
                    if (!backupFolder.exists()) backupFolder.mkdirs()
                    val destFile = File(backupFolder, targetFile.name)
                    try {
                        targetFile.copyTo(destFile, overwrite = true)
                        if (lowerCmd.contains("move")) {
                            targetFile.delete()
                            speakOut("Kashif Bhai, file backup folder me move kar di.")
                        } else {
                            speakOut("Kashif Bhai, file backup path par copy paste ho gayi hai.")
                        }
                    } catch (e: Exception) {
                        speakOut("File task operations failure.")
                    }
                } else {
                    speakOut("Source Pro Level folder khali hai.")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.getStringExtra("COMMAND")
        if (command != null) {
            handleCommand(command)
        } else {
            Toast.makeText(this, "Jarvis Voice Engine Engaged", Toast.LENGTH_SHORT).show()
        }
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
        mediaPlayer?.release()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Stop global high-tech glowing bottom bar
        val glowIntent = Intent(this, JarvisGlowOverlayService::class.java)
        try {
            stopService(glowIntent)
            Log.d(TAG, "Glow overlay service successfully stopped.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}
