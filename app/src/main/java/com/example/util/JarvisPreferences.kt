package com.example.util

import android.content.Context
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

    fun saveBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, default: Boolean): Boolean {
        return getPrefs(context).getBoolean(key, default)
    }
}
