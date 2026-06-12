package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private const val TAG = "GeminiHelper"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun triggerErrorBroadcast(context: Context, errorType: String, message: String) {
        JarvisLogger.error(errorType, message)
        val intent = Intent("com.example.JARVIS_API_ERROR").apply {
            putExtra("error_type", errorType)
            putExtra("error_message", message)
            `package` = context.packageName
        }
        context.sendBroadcast(intent)
    }

    suspend fun processCommand(context: Context, userCommand: String, screenTextContext: String = ""): JarvisActionResponse = withContext(Dispatchers.IO) {
        val configuredKey = JarvisPreferences.getString(context, "api_key", "").trim()
        val configuredModel = JarvisPreferences.getString(context, "model", "google/gemini-2.0-flash-exp:free").trim()

        val isPlaceholder = configuredKey.isEmpty() || 
                             configuredKey == "sk-or-v1-..." || 
                             configuredKey.startsWith("sk-or-v1-placeholder") ||
                             configuredKey == "MY_GEMINI_API_KEY"

        val activeKey = if (isPlaceholder) {
            BuildConfig.GEMINI_API_KEY
        } else {
            configuredKey
        }

        if (activeKey.isEmpty() || activeKey == "MY_GEMINI_API_KEY") {
            val msg = "Please enter a valid OpenRouter or Gemini API Key in Settings."
            Log.e(TAG, msg)
            triggerErrorBroadcast(context, "API_KEY_MISSING", msg)
            return@withContext fallbackResponse(userCommand)
        }

        // Determine if target API is OpenRouter or standard Google Gemini
        val isOpenRouter = activeKey.startsWith("sk-or-")

        val systemInstruction = """
            You are the voice and mind of "Jarvis v3", an advanced autonomous AI digital assistant built specifically for the owner Kashif Bhai.
            Your response must ALWAYS be a valid JSON object with EXACTLY three fields:
            1. "response" (String): The Urdu/Hindi spoken text in simple transliterated English (Roman Urdu/Hindi) so it can be spoken via TextToSpeech. Keep it highly polite, calling him "Kashif Bhai", and with high-tech energy.
            2. "action" (String): One of these mechanical tags if the user commands or requests an action:
               - "flashlight_on" (turn tool/torch/flashlight on)
               - "flashlight_off" (turn tool/torch/flashlight off)
               - "wifi_on" (enable or manage wifi)
               - "wifi_off" (disable wifi options)
               - "bluetooth_on" (enable bluetooth)
               - "bluetooth_off" (disable bluetooth)
               - "data_on" (open cellular network cellular options settings)
               - "show_recents" (open recents overlay panel)
               - "go_home" (go to main mobile portal screen)
               - "play_song" (Argument: song name or blank. Searches local storage matching the name and plays it)
               - "open_yt_music" (Argument: empty. Launches YT Music app or opens music web)
               - "accessibility_click" (Argument: EXACT text of the button, input, or label to click on Kashif Bhai's screen context)
               - "accessibility_type" (Argument: "targetEditTextLabel|valueToType", e.g., "username|kashifbhai")
               - "create_file" (write content or create file in Pro Level folder)
               - "copy_file" (make copy duplicate of files inside backup)
               - "move_file" (transfer file to backup folder)
               - "reply_notification" (reply notification text. Argument -> exact reply text)
               - "open_app" (launching any specific apps on the device, e.g., WhatsApp, Facebook, settings, etc. Argument -> app name)
               - "open_custom_folder" (open files application or Pro Level directory Explorer)
               - "create_note" (save digital memo or log a note on disk. Argument -> Note content text)
               - "set_timer" (activate quick alarm or scheduler countdown. Argument -> delay duration seconds, e.g., "60")
               - "get_time" (read current time on watch)
               - "get_date" (read today's date calendar)
               - "search_google" (lookup query on browser search engine. Argument -> query phrase)
               - "volume_up" (turn device speaker sound up)
               - "volume_down" (turn speaker sound down)
               - "mute" (make device profile totally mute)
               - "unmute" (enable sounds and profile high)
               - "open_youtube" (open search on YouTube. Argument -> query or app launch)
               - "open_google_maps" (open maps/directions target. Argument -> destination query)
               - "take_screenshot" (snap screenshot image)
               - "open_notifications" (pull down android alert notifications list drawer)
               - "open_quick_settings" (pull down top quick toggles)
               - "lock_screen" (lock android screen view)
               - "power_dialog" (display device restart/power-off menu overlay)
               - "open_browser" (open custom Web URL. Argument -> website link like "google.com")
               - "clear_logs" (wipe out jarvis diagnostic logs database)
               - "toggle_airplane_mode" (open setting flight mode panels)
               - "battery_status" (report current power percentage charge)
               - "none" (simple general chit chat, status report, conversation questions, date, time answers with no system changes)
            3. "arg" (String): An optional argument for the action as defined in the list above.

            SCREEN AWARENESS AUTO-PILOT INSTRUCTIONS:
            If screen-scraping context is provided below, analyze what elements are visible. If Kashif Bhai asks to click, confirm, enter, or select something on the screen, use "accessibility_click" with correct label, or "accessibility_type" with target input label.
            Always keep your spoken replies friendly, calling him "Kashif Bhai", in energetic Roman Urdu/Hindi.

            Example output for: "Jarvis torch on kardo"
            {
               "response": "Kashif Bhai, flashlight on kiye deta hoon.",
               "action": "flashlight_on",
               "arg": ""
            }

            Example output for: "Suno screen par login click karo"
            {
               "response": "Sure Kashif Bhai, main login button par tap kar raha hoon.",
               "action": "accessibility_click",
               "arg": "login"
            }

            Return ONLY the valid JSON block. ABSOLUTELY NO markdown formatting, NO ```json wrapping. Just the raw JSON object.
        """.trimIndent()

        val finalUserMsg = if (screenTextContext.isNotBlank()) {
            "[CURRENT_VISIBLE_SCREEN_CONTENT]\n$screenTextContext\n\n[USER_COMMAND]\n$userCommand"
        } else {
            userCommand
        }

        if (isOpenRouter) {
            // OpenRouter API Calling (OpenAI Specification)
            val url = "https://openrouter.ai/api/v1/chat/completions"
            val jsonRequest = JSONObject().apply {
                put("model", configuredModel)
                
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstruction)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", finalUserMsg)
                    })
                }
                put("messages", messagesArray)
                
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
                put("temperature", 0.6)
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $activeKey")
                .header("HTTP-Referer", "https://ai.studio")
                .header("X-Title", "Jarvis Pro Client")
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        val errMsg = "HTTP ${response.code}: $errBody"
                        Log.e(TAG, "OpenRouter Error: $errMsg")
                        triggerErrorBroadcast(context, "OPENROUTER_API_ERROR", errMsg)
                        return@withContext fallbackResponse(userCommand)
                    }

                    val responseStr = response.body?.string() ?: return@withContext fallbackResponse(userCommand)
                    Log.d(TAG, "OpenRouter Response: $responseStr")

                    val optCleanJson = parseOpenRouterResponse(responseStr)
                    if (optCleanJson != null) {
                        val parsed = parseActionResponseJson(context, optCleanJson)
                        if (parsed != null) return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                val errMsg = e.message ?: "Connection Timeout / Network Unreachable."
                Log.e(TAG, "OpenRouter call failed", e)
                triggerErrorBroadcast(context, "CONNECTION_FAILED", errMsg)
            }
        } else {
            // Google Gemini API Calling
            // Handle model sanitization (if user has configured an OpenRouter model but standard key, use fallback)
            val geminiModel = if (configuredModel.contains("/")) "gemini-1.5-flash" else configuredModel
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$geminiModel:generateContent?key=$activeKey"

            val jsonRequest = JSONObject().apply {
                val contentArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", finalUserMsg)
                            })
                        })
                    })
                }
                put("contents", contentArray)
                
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.6)
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        val errMsg = "HTTP ${response.code}: $errBody"
                        Log.e(TAG, "Gemini Direct Error: $errMsg")
                        triggerErrorBroadcast(context, "GEMINI_API_ERROR", errMsg)
                        return@withContext fallbackResponse(userCommand)
                    }

                    val bodyStr = response.body?.string() ?: return@withContext fallbackResponse(userCommand)
                    Log.d(TAG, "Gemini Response: $bodyStr")

                    val cleanJson = parseGeminiResponse(bodyStr)
                    if (cleanJson != null) {
                        val parsed = parseActionResponseJson(context, cleanJson)
                        if (parsed != null) return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                val errMsg = e.message ?: "Connection Timeout / Network Unreachable."
                Log.e(TAG, "Gemini call failed", e)
                triggerErrorBroadcast(context, "CONNECTION_FAILED", errMsg)
            }
        }

        return@withContext fallbackResponse(userCommand)
    }

    private fun parseOpenRouterResponse(responseBody: String): String? {
        return try {
            val root = JSONObject(responseBody)
            val choices = root.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            message?.optString("content")?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing OpenRouter JSON", e)
            null
        }
    }

    private fun parseGeminiResponse(responseBody: String): String? {
        return try {
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val part = parts?.optJSONObject(0)
            part?.optString("text")?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing Gemini response JSON", e)
            null
        }
    }

    private fun parseActionResponseJson(context: Context, jsonStr: String): JarvisActionResponse? {
        return try {
            val parsedObj = JSONObject(jsonStr)
            
            // Handle optional error_popup direct node
            if (parsedObj.has("error_popup")) {
                val errorPopup = parsedObj.optJSONObject("error_popup")
                if (errorPopup != null) {
                    val title = errorPopup.optString("title", "Jarvis Error")
                    val message = errorPopup.optString("message", "An unexpected runtime anomaly occurred.")
                    triggerErrorBroadcast(context, title, message)
                }
            }

            // Extract spoken response
            var respText = parsedObj.optString("response", "")
            if (respText.isEmpty()) {
                respText = parsedObj.optString("greet", "")
            }

            var action = "none"
            var arg = ""

            // Format 1: Standard direct fields
            if (parsedObj.has("action")) {
                action = parsedObj.optString("action", "none")
                arg = parsedObj.optString("arg", "")
            } 
            // Format 2: Actions array
            else if (parsedObj.has("actions")) {
                val actionsArray = parsedObj.optJSONArray("actions")
                if (actionsArray != null && actionsArray.length() > 0) {
                    val firstAction = actionsArray.optJSONObject(0)
                    if (firstAction != null) {
                        val cmd = firstAction.optString("command", "").uppercase()
                        val state = firstAction.optString("state", "").uppercase()
                        
                        // Map macro commands to our local execute action names
                        when (cmd) {
                            "TOGGLE_FLASHLIGHT" -> {
                                action = if (state == "ON" || state == "TRUE") "flashlight_on" else "flashlight_off"
                            }
                            "SET_VOLUME_PROFILE" -> {
                                action = if (state == "UP") "volume_up" else if (state == "DOWN") "volume_down" else if (state == "MUTE") "mute" else "unmute"
                            }
                            "GET_BATTERY_SYSTEM", "BATTERY_STATUS" -> {
                                action = "battery_status"
                            }
                            "TAKE_SCREENSHOT_HIDDEN", "TAKE_SCREENSHOT" -> {
                                action = "take_screenshot"
                            }
                            "LAUNCH_APP_PACKAGE", "OPEN_APP" -> {
                                action = "open_app"
                                arg = state
                                if (arg.isEmpty()) arg = firstAction.optString("package", "")
                                if (arg.isEmpty()) arg = firstAction.optString("app", "")
                            }
                            "VIEW_RECENT_APPS", "SHOW_RECENTS" -> {
                                action = "show_recents"
                            }
                            "GO_HOME" -> {
                                action = "go_home"
                            }
                            "PLAY_SONG" -> {
                                action = "play_song"
                            }
                            "CREATE_NOTE" -> {
                                action = "create_note"
                                arg = firstAction.optString("content", "")
                            }
                            "OPEN_CUSTOM_FOLDER" -> {
                                action = "open_custom_folder"
                            }
                            "SET_TIMER" -> {
                                action = "set_timer"
                                arg = firstAction.optString("duration", "60")
                            }
                            "GET_TIME" -> {
                                action = "get_time"
                            }
                            "GET_DATE" -> {
                                action = "get_date"
                            }
                            "SEARCH_GOOGLE" -> {
                                action = "search_google"
                                arg = firstAction.optString("query", "")
                            }
                            "OPEN_YOUTUBE" -> {
                                action = "open_youtube"
                                arg = firstAction.optString("query", "")
                            }
                            "OPEN_GOOGLE_MAPS" -> {
                                action = "open_google_maps"
                                arg = firstAction.optString("destination", "")
                            }
                            "OPEN_NOTIFICATIONS" -> {
                                action = "open_notifications"
                            }
                            "OPEN_QUICK_SETTINGS" -> {
                                action = "open_quick_settings"
                            }
                            "LOCK_SCREEN" -> {
                                action = "lock_screen"
                            }
                            "POWER_DIALOG" -> {
                                action = "power_dialog"
                            }
                            "OPEN_BROWSER" -> {
                                action = "open_browser"
                                arg = firstAction.optString("url", "")
                            }
                            "CLEAR_LOGS" -> {
                                action = "clear_logs"
                            }
                            "TOGGLE_AIRPLANE_MODE" -> {
                                action = "toggle_airplane_mode"
                            }
                            "WIFI_ON" -> {
                                action = "wifi_on"
                            }
                            "WIFI_OFF" -> {
                                action = "wifi_off"
                            }
                            "BLUETOOTH_ON" -> {
                                action = "bluetooth_on"
                            }
                            "BLUETOOTH_OFF" -> {
                                action = "bluetooth_off"
                            }
                        }
                    }
                }
            }

            if (respText.isEmpty() && action == "none") {
                return null
            }

            JarvisActionResponse(respText, action, arg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing Jarvis action blocks from text: $jsonStr", e)
            null
        }
    }

    private fun fallbackResponse(cmd: String): JarvisActionResponse {
        val lower = cmd.lowercase()
        return when {
            lower.contains("flashlight on") || lower.contains("torch on") || lower.contains("flash light on") || lower.contains("torch jalao") -> 
                JarvisActionResponse("Flashlight on kiye deta hoon kashif Bhai.", "flashlight_on", "")
            lower.contains("flashlight off") || lower.contains("torch off") || lower.contains("flash light off") || lower.contains("torch bujhao") -> 
                JarvisActionResponse("Flashlight off kiye deta hoon kashif Bhai.", "flashlight_off", "")
            lower.contains("wifi on") || lower.contains("wifi kholo") -> 
                JarvisActionResponse("Wifi settings open kar raha hoon Bhai.", "wifi_on", "")
            lower.contains("wifi off") || lower.contains("wifi band karo") -> 
                JarvisActionResponse("Wifi settings check kijiye.", "wifi_off", "")
            lower.contains("bluetooth on") || lower.contains("bluetooth chalao") -> 
                JarvisActionResponse("Bluetooth activate kar raha hoon.", "bluetooth_on", "")
            lower.contains("bluetooth off") || lower.contains("bluetooth band karo") -> 
                JarvisActionResponse("Bluetooth off kar raha hoon.", "bluetooth_off", "")
            lower.contains("home") || lower.contains("home screen") || lower.contains("peeche jao") -> 
                JarvisActionResponse("Home screen par jate hain.", "go_home", "")
            lower.contains("recent") || lower.contains("recent apps") || lower.contains("saari apps dikhao") -> 
                JarvisActionResponse("Recently used apps khol raha hoon.", "show_recents", "")
            lower.contains("song") || lower.contains("music") || lower.contains("gaana") || lower.contains("gaana chalao") -> 
                JarvisActionResponse("Song track play kar raha hoon Kashif Bhai.", "play_song", "")
            lower.contains("create") || lower.contains("file banao") || lower.contains("fayl banao") -> 
                JarvisActionResponse("File generate kar raha hoon.", "create_file", "")
            else -> 
                JarvisActionResponse("Ji Kashif Bhai, main aapka kaam internet key absence me handle kar raha hoon.", "none", "")
        }
    }
}

data class JarvisActionResponse(
    val response: String,
    val action: String,
    val arg: String
)
