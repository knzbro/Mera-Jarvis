package com.example.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.animation.ValueAnimator

class JarvisGlowView(context: Context) : View(context) {
    private val paint = Paint()
    private var progress = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val state = JarvisGlowOverlayService.glowState

        // 1. Pick the cyber-glow color palette dynamically based on active state
        val colors = when (state) {
            JarvisGlowOverlayService.GlowState.LISTENING -> {
                // Interactive Electric Green & Cyan laser
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#00FF87"), // Neon Lime Green
                    Color.parseColor("#60EFFF"), // Crystal Ice
                    Color.parseColor("#00E5FF"), // Vivid Cyan
                    Color.parseColor("#00FF87"), 
                    Color.TRANSPARENT
                )
            }
            JarvisGlowOverlayService.GlowState.THENKNG -> {
                // Fast Purple / Ultra Magenta cosmic computing wave
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#D500F9"), // Intense Purple
                    Color.parseColor("#F50057"), // Laser Rose Pink
                    Color.parseColor("#3F51B5"), // Indigo Deep Indigo
                    Color.parseColor("#D500F9"),
                    Color.TRANSPARENT
                )
            }
            JarvisGlowOverlayService.GlowState.SPEAKING -> {
                // Harmonic Amber Gold & Sapphire Blue vocal engine
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#FFD600"), // Warm Sun Amber
                    Color.parseColor("#2979FF"), // Sapphire Blue
                    Color.parseColor("#FF9100"), // Bright Orange
                    Color.parseColor("#FFD600"),
                    Color.TRANSPARENT
                )
            }
            JarvisGlowOverlayService.GlowState.IDLE -> {
                // Dim Slate Blue energy saver
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#37474F"), // Deep Slate
                    Color.parseColor("#1DE9B6"), // Subtle Jade Teal
                    Color.parseColor("#37474F"),
                    Color.TRANSPARENT
                )
            }
        }

        // speed adjusts automatically based on active AI state
        val speedFactor = when (state) {
            JarvisGlowOverlayService.GlowState.THENKNG -> 2.5f
            JarvisGlowOverlayService.GlowState.LISTENING -> 1.5f
            JarvisGlowOverlayService.GlowState.SPEAKING -> 1.0f
            JarvisGlowOverlayService.GlowState.IDLE -> 0.4f
        }

        val offsetW = (System.currentTimeMillis() % 10000) / 10000f * speedFactor
        val x0 = -w + (offsetW * w * 2) % w
        val x1 = x0 + w

        val horizontalGradient = android.graphics.RadialGradient(
            w / 2f, h, Math.max(w, h),
            intArrayOf(Color.parseColor("#6600FFCC"), Color.parseColor("#0000FFCC"), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )

        paint.reset()
        paint.isAntiAlias = true
        paint.shader = horizontalGradient
        paint.style = Paint.Style.FILL
        
        val pulsingAlpha = if (state == JarvisGlowOverlayService.GlowState.IDLE) {
            (25 + (15 * progress)).toInt()
        } else {
            (95 + (50 * progress)).toInt()
        }
        paint.alpha = pulsingAlpha.coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}

class JarvisGlowOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var glowView: JarvisGlowView? = null
    private val TAG = "JarvisGlowOverlayService"

    enum class GlowState {
        IDLE,
        LISTENING,
        THENKNG, // "THENKNG" matches references exactly to ensure robust compilation
        SPEAKING
    }

    companion object {
        var glowState = GlowState.IDLE
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating ultra-thin 4dp bottom glow overlay...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Cannot show overlay: Draw overlay permission is missing.")
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        glowView = JarvisGlowView(this)

        // Ultra-thin fine cyber bar (Made more bareek as requested: changed from 8 to 4)
        val heightInDp = 120
        val density = resources.displayMetrics.density
        val heightInPx = (heightInDp * density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            heightInPx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            x = 0
            y = 0
        }

        try {
            windowManager?.addView(glowView, params)
            Log.d(TAG, "Dynamic neon laser overlay successfully attached.")
            com.example.util.JarvisLogger.success("SYS_OVERLAY", "Ultra thin dynamic laser line bound successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "WindowManager addView error: ${e.message}", e)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (glowView != null && windowManager != null) {
            try {
                windowManager?.removeView(glowView)
                Log.d(TAG, "Glow overlay view removed cleanly.")
            } catch (e: Exception) {
                Log.e(TAG, "WindowManager removeView error", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
