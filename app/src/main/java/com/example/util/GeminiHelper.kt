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

    suspend fun processCommand(context: Context, userCommand: String): JarvisActionResponse = withContext(Dispatchers.IO) {
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
            You are the voice and mind of "Jarvis", an advanced autonomous AI assistant built specifically for the owner Kashif Bhai.
            Your response must ALWAYS be a valid JSON object with EXACTLY three fields:
            1. "response" (String): The Urdu/Hindi spoken text in simple transliterated English (Roman Urdu/Hindi) so it can be spoken via TextToSpeech. Keep it highly polite, calling him "Kashif Bhai", and with high-tech energy.
            2. "action" (String): One of these mechanical tags if the user commands an action:
               - "flashlight_on" (when asking to turn flashlight/torch on)
               - "flashlight_off" (when asking to turn flashlight/torch off)
               - "wifi_on" (turn wifi on)
               - "wifi_off" (turn wifi off)
               - "bluetooth_on" (turn bluetooth on)
               - "bluetooth_off" (turn bluetooth off)
               - "data_on" (turn data/roaming screen on)
               - "show_recents" (open recent apps on screen)
               - "go_home" (navigate to home screen)
               - "play_song" (playable song in Pro Level folder or generic ringtone play)
               - "create_file" (create a file in Pro Level folder)
               - "copy_file" (copy file)
               - "move_file" (move file)
               - "reply_notification" (when asking to reply or answer a WhatsApp or active contact's notification. Example: "reply kashif busy hoon" -> Action: reply_notification)
               - "open_app" (launching any app. Example: "open WhatsApp" -> Action: open_app)
               - "none" (if the command is simple Q&A like date, time, status, mood, general chit chat, etc.)
            3. "arg" (String): An optional argument for the action. For "reply_notification", parse the specific text to reply with. For "open_app", specify the name of the app to launch (e.g. "whatsapp", "chrome", "maps", etc.). For others, set to empty string "".

            Example output for: "Jarvis torch on kardo"
            {
               "response": "Kashif Bhai, flashlight on kiye deta hoon.",
               "action": "flashlight_on",
               "arg": ""
            }

            Example output for: "Suno WhatsApp par reply bhej do main raste me hu"
            {
               "response": "Kashif Bhai, main WhatsApp ka reply 'main raste me hu' bhej raha hoon.",
               "action": "reply_notification",
               "arg": "main raste me hu"
            }

            Return ONLY the valid JSON block. ABSOLUTELY NO markdown formatting, NO ```json wrapping. Just the raw JSON object.
        """.trimIndent()

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
                        put("content", userCommand)
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
                        val parsed = parseActionResponseJson(optCleanJson)
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
                                put("text", userCommand)
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
                        val parsed = parseActionResponseJson(cleanJson)
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

    private fun parseActionResponseJson(jsonStr: String): JarvisActionResponse? {
        return try {
            val parsedObj = JSONObject(jsonStr)
            val respText = parsedObj.optString("response", "")
            val action = parsedObj.optString("action", "none")
            val arg = parsedObj.optString("arg", "")
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
