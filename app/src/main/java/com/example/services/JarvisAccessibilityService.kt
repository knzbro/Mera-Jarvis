package com.example.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {
    private val TAG = "JarvisAccessibility"

    companion object {
        var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Jarvis Accessibility Service Connected")
        com.example.util.JarvisLogger.success("SYS_ACCESSIBILITY", "Jarvis Screen Control Core Engaged!")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun scrapeScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = java.lang.StringBuilder()
        try {
            traverseNodes(root, sb)
        } catch (e: Exception) {
            Log.e(TAG, "Error traversing accessibility tree", e)
        } finally {
            root.recycle()
        }
        return sb.toString().trim()
    }

    private fun traverseNodes(node: AccessibilityNodeInfo?, sb: java.lang.StringBuilder) {
        if (node == null) return
        
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val isClickable = node.isClickable
        val className = node.className?.toString()?.substringAfterLast('.') ?: "Element"

        if (!text.isNullOrEmpty() || !desc.isNullOrEmpty()) {
            val elementValue = if (!text.isNullOrEmpty()) text else desc
            sb.append("[")
            if (isClickable) sb.append("Clickable ")
            sb.append("$className: $elementValue]\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNodes(child, sb)
                child.recycle()
            }
        }
    }

    fun performClickOnNode(textQuery: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val found = findAndClickNode(root, textQuery.lowercase().trim())
        root.recycle()
        return found
    }

    private fun findAndClickNode(node: AccessibilityNodeInfo?, query: String): Boolean {
        if (node == null) return false
        
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (text.contains(query) || desc.contains(query)) {
            if (node.isClickable) {
                val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) {
                    Log.d(TAG, "Successfully clicked matching node: $query")
                    return true
                }
            }
            // Fallback: try clicking a clickable parent
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (success) {
                        Log.d(TAG, "Successfully clicked parent of matching node: $query")
                        parent.recycle()
                        return true
                    }
                }
                val nextParent = parent.parent
                parent.recycle()
                parent = nextParent
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val clicked = findAndClickNode(child, query)
                child.recycle()
                if (clicked) return true
            }
        }
        return false
    }

    fun performTypeOnNode(textQuery: String, inputVal: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val found = findAndTypeNode(root, textQuery.lowercase().trim(), inputVal)
        root.recycle()
        return found
    }

    private fun findAndTypeNode(node: AccessibilityNodeInfo?, query: String, inputVal: String): Boolean {
        if (node == null) return false
        
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val isEditText = node.className?.contains("EditText", ignoreCase = true) == true

        if (isEditText && (text.contains(query) || desc.contains(query) || query.isEmpty())) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, inputVal)
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (success) {
                Log.d(TAG, "Successfully typed value on element: $query")
                return true
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val typed = findAndTypeNode(child, query, inputVal)
                child.recycle()
                if (typed) return true
            }
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Log.d(TAG, "Window State Changed: $packageName")
            
            val allowedApps = com.example.util.JarvisPreferences.getAllowedApps(this)
            
            if (packageName != "com.android.systemui" && packageName != "com.google.android.apps.nexuslauncher" && packageName != applicationContext.packageName) {
                if (!allowedApps.contains(packageName)) {
                    Log.d(TAG, "Unproductive app detected ($packageName).")
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Jarvis Accessibility Service Interrupted")
    }
}
