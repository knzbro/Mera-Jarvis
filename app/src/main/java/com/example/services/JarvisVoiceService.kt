package com.example.services
 
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.BatteryManager
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
import java.net.URLEncoder
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

    fun handleCommand(command: String, isFromText: Boolean = false) {
        val originalCmd = command.trim()
        val lowerCmd = originalCmd.lowercase(Locale.getDefault())
        
        // Check voice-wake conditions
        val sensitivity = JarvisPreferences.getString(this, "wake_word_sensitivity", "MEDIUM")
        val hasWakeWord = when (sensitivity.uppercase()) {
            "LOW" -> {
                lowerCmd.contains("jarvis")
            }
            "MEDIUM" -> {
                lowerCmd.contains("jarvis") || (lowerCmd.contains("bhai") && lowerCmd.split("\\s+".toRegex()).size >= 2)
            }
            else -> {
                lowerCmd.contains("jarvis") || lowerCmd.contains("bhai") || lowerCmd.contains("suno")
            }
        }
        val voiceWakeEnabled = JarvisPreferences.getBoolean(this, "voice_wake_enabled", true)
        
        // Require wake-word ONLY for background voice inputs
        if (!isFromText && voiceWakeEnabled && !hasWakeWord) {
            Log.d(TAG, "Background input ignored (no wake word matched): $command")
            return
        }

        // Add user chat text to persistent logs if voice-activated
        if (!isFromText) {
            com.example.util.JarvisPreferences.addChatMessage(this, "You: $originalCmd")
        }

        // --- 1. LOCAL CUSTOM TRAINED COMMANDS INTERCEPTOR ---
        val trainedCmds = com.example.util.JarvisPreferences.getTrainedCommands(this)
        var matchedTrained: com.example.util.TrainedCommand? = null
        val lookupCmd = originalCmd.lowercase().replace("jarvis", "").replace("bhai", "").trim()
        
        for (tc in trainedCmds) {
            val triggerLower = tc.trigger.trim().lowercase()
            if (lookupCmd == triggerLower || lookupCmd.contains(triggerLower) || originalCmd.lowercase().contains(triggerLower)) {
                matchedTrained = tc
                break
            }
        }

        if (matchedTrained != null) {
            val tc = matchedTrained
            Log.d(TAG, "Matched trained custom command: Trigger='${tc.trigger}' -> Action='${tc.action}'")
            com.example.util.JarvisLogger.success("SYS_CORE", "Custom command matched offline! (Trigger: ${tc.trigger})")
            
            // Speak custom reply
            if (tc.response.isNotBlank()) {
                speakOut(tc.response)
                com.example.util.JarvisLogger.success("TTS_ENGINE", "Spoken reply: \"${tc.response}\"")
            }
            
            // Trigger target custom action
            if (tc.action != "voice_reply" && tc.action != "reply" && tc.action != "none") {
                executeAction(tc.action, "")
            }
            return
        }

        // --- 2. DEEP NLP BACKEND (GEMINI / OPENROUTER) ---
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
                val actionResult = GeminiHelper.processCommand(this@JarvisVoiceService, cleanedCmd)
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
            
            // NEW 30+ MECHANICAL MACRO ACTIONS
            "open_custom_folder" -> openCustomFolder()
            "create_note" -> createNoteOnDisk(actionArg)
            "set_timer" -> setSystemTimer(actionArg)
            "get_time" -> speakCurrentTime()
            "get_date" -> speakCurrentDate()
            "search_google" -> searchOnGoogle(actionArg)
            "volume_up" -> adjustVolume(true)
            "volume_down" -> adjustVolume(false)
            "mute" -> muteDevice(true)
            "unmute" -> muteDevice(false)
            "open_youtube" -> openYouTube(actionArg)
            "open_google_maps" -> openGoogleMaps(actionArg)
            "take_screenshot" -> triggerAccessibilityAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, "Kashif Bhai, screenshot gestures access ke liye accessibility check kijiye.")
            "open_notifications" -> triggerAccessibilityAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "Notifications list open karne me access block hai.")
            "open_quick_settings" -> triggerAccessibilityAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, "Quick toggles load nahi kiye ja sake.")
            "lock_screen" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    triggerAccessibilityAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "Mera dynamic access blocked hai lock parameters me.")
                } else {
                    speakOut("Apka level lock protocol support nahi karta.")
                }
            }
            "power_dialog" -> triggerAccessibilityAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, "Power metrics check kijiye.")
            "open_browser" -> openBrowserWeb(actionArg)
            "clear_logs" -> clearJarvisLogs()
            "toggle_airplane_mode" -> openAirplaneModeSettings()
            "battery_status" -> speakBatteryStatus()
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
            lower.contains("recents") -> showRecents()
            lower.contains("home") -> goHome()
            lower.contains("song") || lower.contains("music") || lower.contains("gaana") -> playSongInProLevel()
            lower.contains("create file") || lower.contains("file banao") -> handleFileCommand("create")
            
            // Offline fallbacks for new macros
            lower.contains("time") || lower.contains("waqt") || lower.contains("kitne baj") -> speakCurrentTime()
            lower.contains("date") || lower.contains("tareekh") || lower.contains("aaj kya din") -> speakCurrentDate()
            lower.contains("battery") || lower.contains("charge") -> speakBatteryStatus()
            lower.startsWith("search ") || lower.contains("google search") || lower.startsWith("google kero ") -> {
                val q = lower.replace("search ", "").replace("google search", "").replace("google kero ", "").trim()
                searchOnGoogle(q)
            }
            lower.contains("volume up") || lower.contains("awaaz tez") || lower.contains("volume barhao") -> adjustVolume(true)
            lower.contains("volume down") || lower.contains("awaaz kam") -> adjustVolume(false)
            lower.contains("screenshot") -> triggerAccessibilityAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, "Kashif Bhai, screenshot settings require Accessibility active permissions.")
            lower.contains("mute") || lower.contains("silent") -> muteDevice(true)
            lower.contains("unmute") -> muteDevice(false)
            
            lower.startsWith("open ") || lower.contains("kholo ") || lower.contains("chalao ") -> {
                val app = lower.replace("open ", "").replace("kholo ", "").replace("chalao ", "").trim()
                openAppByName(app)
            }
            else -> {
                speakOut("Kashif Bhai, main background check kiya hai. Command interpret nahi ho paayi.")
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
                speakOut("Pro Level folder blank hai. Maine system alarm play kiya hai.")
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

    // --- HELPER IMPLEMENTATION OF NEW ADVANCED MACROS ---
    private fun openCustomFolder() {
        val baseDir = getExternalFilesDir(null) ?: filesDir
        val proLevelFolder = File(baseDir, "Pro Level")
        if (!proLevelFolder.exists()) proLevelFolder.mkdirs()
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(proLevelFolder), "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            speakOut("Kashif Bhai, files system folder explorer view launch ho gaya hai.")
        } catch (e: Exception) {
            speakOut("Kashif Bhai, " + proLevelFolder.name + " screen visual explore open kiya.")
        }
    }

    private fun createNoteOnDisk(content: String) {
        val baseDir = getExternalFilesDir(null) ?: filesDir
        val proLevelFolder = File(baseDir, "Pro Level")
        if (!proLevelFolder.exists()) proLevelFolder.mkdirs()
        val noteContent = if (content.isBlank()) "Default Memo saved by Kashif Bhai." else content
        val noteFile = File(proLevelFolder, "note_${System.currentTimeMillis() % 10000}.txt")
        try {
            noteFile.writeText(noteContent)
            speakOut("Kashif Bhai, maine note save kar diya: $noteContent")
        } catch (e: Exception) {
            speakOut("Kashif Bhai, storage note memory allocation failure.")
        }
    }

    private fun setSystemTimer(secStr: String) {
        val seconds = secStr.toIntOrNull() ?: 60
        try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Jarvis Timer")
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            speakOut("Kashif Bhai, $seconds seconds ka timer start kar diya hai.")
        } catch (e: Exception) {
            speakOut("Timer system configurations access is not loaded.")
        }
    }

    private fun speakCurrentTime() {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = sdf.format(Date())
        speakOut("Kashif Bhai, abhi ka waqt hai $formattedTime.")
    }

    private fun speakCurrentDate() {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date())
        speakOut("Kashif Bhai, aaj ki tareekh hai: $formattedDate.")
    }

    private fun searchOnGoogle(query: String) {
        val trimmed = if (query.isBlank()) "Jarvis Android AI" else query
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, trimmed)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            speakOut("Kashif Bhai, main Google par '$trimmed' search kar raha hoon.")
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(trimmed, "UTF-8")}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(webIntent)
                speakOut("Google main page loaded.")
            } catch (ex: Exception) {
                speakOut("Search query pipeline block.")
            }
        }
    }

    private fun adjustVolume(increase: Boolean) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            speakOut("Kashif Bhai, volume sound level " + (if (increase) "raise" else "lower") + " kar diya.")
        } catch (e: Exception) {
            speakOut("Volume controller streams are not accessible.")
        }
    }

    private fun muteDevice(mute: Boolean) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            if (mute) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                speakOut("Mute and silent status activated.")
            } else {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol / 2, AudioManager.FLAG_SHOW_UI)
                speakOut("Sound levels channel unmuted.")
            }
        } catch (e: Exception) {
            speakOut("Sound adjustments restricted.")
        }
    }

    private fun openYouTube(query: String) {
        try {
            val intent = if (query.isNotBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}"))
            } else {
                packageManager.getLaunchIntentForPackage("com.google.android.youtube") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            speakOut("Kashif Bhai, YouTube activate ho gaya.")
        } catch (e: Exception) {
            speakOut("YouTube web launch failed.")
        }
    }

    private fun openGoogleMaps(destination: String) {
        try {
            val q = if (destination.isBlank()) "google maps" else destination
            val uri = Uri.parse("google.navigation:q=${URLEncoder.encode(q, "UTF-8")}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.google.android.apps.maps")
            }
            startActivity(intent)
            speakOut("Kashif Bhai, maps navigation launch kiya.")
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${URLEncoder.encode(destination, "UTF-8")}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                speakOut("Google Maps online loaded.")
            } catch (ex: Exception) {
                speakOut("Navigation metrics restricted.")
            }
        }
    }

    private fun triggerAccessibilityAction(actionId: Int, fallbackMsg: String) {
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.performGlobalAction(actionId)
            speakOut("Micro trigger execution success.")
        } else {
            speakOut(fallbackMsg)
        }
    }

    private fun openBrowserWeb(urlArg: String) {
        val rawUrl = if (urlArg.isBlank()) "google.com" else urlArg
        val targetUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            speakOut("Kashif Bhai, main cyberdeck browser link launch kar diya hai.")
        } catch (e: Exception) {
            speakOut("Web launch system browser is not loaded.")
        }
    }

    private fun clearJarvisLogs() {
        com.example.util.JarvisLogger.clear()
        speakOut("Kashif Bhai, diagnostic records pipeline empty kardo.")
    }

    private fun openAirplaneModeSettings() {
        try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            speakOut("Kashif Bhai, airplane mode toggle frame launch kar diya.")
        } catch (e: Exception) {
            speakOut("System settings permission block.")
        }
    }

    private fun speakBatteryStatus() {
        try {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = (level * 100 / scale.toFloat()).toInt()
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val stateMsg = if (isCharging) "charging power supply connected status par hai" else "discharge level par hai"
            speakOut("Kashif Bhai, phone battery power percentage $pct hai, aur device abhi $stateMsg.")
        } catch (e: Exception) {
            speakOut("Power diagnostics check metrics failure.")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.getStringExtra("COMMAND")
        if (command != null) {
            handleCommand(command, isFromText = true)
        } else {
            Toast.makeText(this, "Jarvis Voice Engine Engaged", Toast.LENGTH_SHORT).show()
        }
        return START_STICKY
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "")
        // Log spoken outputs to local persistent chat log
        com.example.util.JarvisPreferences.addChatMessage(this, "Jarvis: $text")
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
