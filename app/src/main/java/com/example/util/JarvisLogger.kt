package com.example.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object JarvisLogger {
    private const val MAX_LOGS_LIMIT = 80
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    enum class LogLevel {
        INFO, WARN, ERROR, SUCCESS
    }

    data class LogEntry(
        val id: String = UUID.randomUUID().toString(),
        val timestamp: String,
        val tag: String,
        val message: String,
        val level: LogLevel
    )

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(timestamp = sdf.format(Date()), tag = "SYSTEM", message = "Jarvis Kernel Initialized successfully.", level = LogLevel.SUCCESS),
            LogEntry(timestamp = sdf.format(Date()), tag = "ACCESSIBILITY", message = "Standby mode enabled. Waiting for trigger.", level = LogLevel.INFO)
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun info(tag: String, message: String) = log(tag, message, LogLevel.INFO)
    fun success(tag: String, message: String) = log(tag, message, LogLevel.SUCCESS)
    fun warn(tag: String, message: String) = log(tag, message, LogLevel.WARN)
    fun error(tag: String, message: String) = log(tag, message, LogLevel.ERROR)

    @Synchronized
    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val timestamp = sdf.format(Date())
        val entry = LogEntry(timestamp = timestamp, tag = tag, message = message, level = level)

        // Android logcat integration
        when (level) {
            LogLevel.INFO -> Log.i("Jarvis_$tag", message)
            LogLevel.SUCCESS -> Log.i("Jarvis_$tag", "✔ $message")
            LogLevel.WARN -> Log.w("Jarvis_$tag", "⚠ $message")
            LogLevel.ERROR -> Log.e("Jarvis_$tag", "❌ $message")
        }

        // Update active flow
        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry) // Insert at top for reverse chronological order

        if (currentList.size > MAX_LOGS_LIMIT) {
            _logs.value = currentList.subList(0, MAX_LOGS_LIMIT)
        } else {
            _logs.value = currentList
        }
    }

    fun clear() {
        _logs.value = listOf(
            LogEntry(timestamp = sdf.format(Date()), tag = "SYSTEM", message = "Logs cleared by console command.", level = LogLevel.INFO)
        )
    }
}
