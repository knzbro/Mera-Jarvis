package com.example.util

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
    private const val MODEL = "gemini-3.5-flash"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun processCommand(userCommand: String): JarvisActionResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Empty Gemini API Key")
            return@withContext fallbackResponse(userCommand)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
        
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
                put("temperature", 0.7)
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
                    Log.e(TAG, "Request failed: ${response.code} ${response.message}")
                    return@withContext fallbackResponse(userCommand)
                }

                val bodyStr = response.body?.string() ?: return@withContext fallbackResponse(userCommand)
                Log.d(TAG, "Raw Response: $bodyStr")

                val cleanJson = parseResponseText(bodyStr)
                if (cleanJson != null) {
                    try {
                        val parsedObj = JSONObject(cleanJson)
                        val respText = parsedObj.optString("response", "")
                        val action = parsedObj.optString("action", "none")
                        val arg = parsedObj.optString("arg", "")
                        return@withContext JarvisActionResponse(respText, action, arg)
                    } catch (e: Exception) {
                        Log.e(TAG, "Parsing JSON object failed: $e. CleanJson: $cleanJson")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command via Gemini API", e)
        }

        return@withContext fallbackResponse(userCommand)
    }

    private fun parseResponseText(responseBody: String): String? {
        return try {
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val part = parts?.optJSONObject(0)
            part?.optString("text")?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate response JSON", e)
            null
        }
    }

    private fun fallbackResponse(cmd: String): JarvisActionResponse {
        val lower = cmd.lowercase()
        return when {
            lower.contains("flashlight on") || lower.contains("torch on") || lower.contains("flash light on") -> 
                JarvisActionResponse("Flashlight on kiye deta hoon kashif Bhai.", "flashlight_on", "")
            lower.contains("flashlight off") || lower.contains("torch off") || lower.contains("flash light off") -> 
                JarvisActionResponse("Flashlight off kiye deta hoon kashif Bhai.", "flashlight_off", "")
            lower.contains("wifi on") -> 
                JarvisActionResponse("Wifi settings open kar raha hoon Bhai.", "wifi_on", "")
            lower.contains("wifi off") -> 
                JarvisActionResponse("Wifi settings check kijiye.", "wifi_off", "")
            lower.contains("bluetooth on") -> 
                JarvisActionResponse("Bluetooth activate kar raha hoon.", "bluetooth_on", "")
            lower.contains("bluetooth off") -> 
                JarvisActionResponse("Bluetooth off kar raha hoon.", "bluetooth_off", "")
            lower.contains("home") -> 
                JarvisActionResponse("Home screen par jate hain.", "go_home", "")
            lower.contains("recent") -> 
                JarvisActionResponse("Recently used apps khol raha hoon.", "show_recents", "")
            lower.contains("song") || lower.contains("music") || lower.contains("gaana") -> 
                JarvisActionResponse("Song track play kar raha hoon Kashif Bhai.", "play_song", "")
            lower.contains("create") || lower.contains("file banao") -> 
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
