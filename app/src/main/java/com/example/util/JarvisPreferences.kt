package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

object JarvisPreferences {
    private const val PREFS_NAME = "JarvisSettings"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAllowedApps(context: Context): Set<String> {
        val defaultApps = setOf("com.android.chrome", "com.google.android.apps.maps", "io.supabase.app", "com.knz.worker")
        return getPrefs(context).getStringSet("allowed_apps", defaultApps) ?: defaultApps
    }

    fun setAppAllowed(context: Context, packageName: String, allowed: Boolean) {
        // Use a mutable copy of the set as required by SharedPreferences
        val current = getAllowedApps(context).toMutableSet()
        if (allowed) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        getPrefs(context).edit().putStringSet("allowed_apps", current).apply()
    }

    fun saveString(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, default: String): String {
        return getPrefs(context).getString(key, default) ?: default
    }

    // --- CUSTOM TRAINED COMMANDS MANAGEMENT ---
    fun getTrainedCommands(context: Context): List<TrainedCommand> {
        val jsonStr = getString(context, "trained_commands", "[]")
        val list = mutableListOf<TrainedCommand>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TrainedCommand(
                        trigger = obj.getString("trigger"),
                        action = obj.getString("action"),
                        response = obj.getString("response")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveTrainedCommands(context: Context, commands: List<TrainedCommand>) {
        try {
            val array = org.json.JSONArray()
            for (cmd in commands) {
                val obj = org.json.JSONObject().apply {
                    put("trigger", cmd.trigger)
                    put("action", cmd.action)
                    put("response", cmd.response)
                }
                array.put(obj)
            }
            saveString(context, "trained_commands", array.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addTrainedCommand(context: Context, cmd: TrainedCommand) {
        val current = getTrainedCommands(context).toMutableList()
        // Remove existing if trigger matches (case insensitive check)
        current.removeAll { it.trigger.equals(cmd.trigger, ignoreCase = true) }
        current.add(cmd)
        saveTrainedCommands(context, current)
    }

    fun removeTrainedCommand(context: Context, trigger: String) {
        val current = getTrainedCommands(context).toMutableList()
        current.removeAll { it.trigger.equals(trigger, ignoreCase = true) }
        saveTrainedCommands(context, current)
    }

    fun saveBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, default: Boolean): Boolean {
        return getPrefs(context).getBoolean(key, default)
    }

    fun setJarvisActive(context: Context, active: Boolean) {
        saveBoolean(context, "jarvis_active", active)
    }

    fun isJarvisActive(context: Context): Boolean {
        return getBoolean(context, "jarvis_active", false)
    }

    // --- PERSISTENT CHAT HISTORY CONFIGURATION ---
    fun getChatHistory(context: Context): List<String> {
        val jsonStr = getString(context, "chat_history", "[]")
        val list = mutableListOf<String>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (list.isEmpty()) {
            list.add("Jarvis: I am online. How can I help you, Kashif Bhai?")
        }
        return list
    }

    fun saveChatHistory(context: Context, history: List<String>) {
        try {
            val array = org.json.JSONArray()
            for (msg in history) {
                array.put(msg)
            }
            saveString(context, "chat_history", array.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addChatMessage(context: Context, msg: String) {
        val current = getChatHistory(context).toMutableList()
        current.add(msg)
        if (current.size > 200) {
            current.removeAt(0)
        }
        saveChatHistory(context, current)
        
        // Notify chat observers
        val intent = Intent("com.example.JARVIS_CHAT_UPDATED").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    fun clearChatHistory(context: Context) {
        saveChatHistory(context, listOf("Jarvis: Chat history cleared. Ready for your command, Kashif Bhai!"))
        val intent = Intent("com.example.JARVIS_CHAT_UPDATED").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
}

data class TrainedCommand(
    val trigger: String,
    val action: String,
    val response: String
)

