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
        duration = 2000
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

        // 1. Dynamic multi-colored futuristic cyber gradient flowing horizontally
        val colors = intArrayOf(
            Color.TRANSPARENT,
            Color.parseColor("#00E5FF"), // Neon Cyan
            Color.parseColor("#0066FF"), // Electric Blue
            Color.parseColor("#D500F9"), // Cyber Magenta
            Color.parseColor("#00E5FF"), // Neon Cyan
            Color.TRANSPARENT
        )

        val x0 = -w + (progress * w * 2)
        val x1 = x0 + w

        val horizontalGradient = LinearGradient(
            x0, 0f, x1, 0f,
            colors, null,
            Shader.TileMode.REPEAT
        )

        paint.reset()
        paint.isAntiAlias = true
        paint.shader = horizontalGradient
        paint.style = Paint.Style.FILL

        // Soft ambient underglow that fits comfortably around navigation keys
        val pulsingAlpha = (90 + (35 * progress)).toInt().coerceIn(0, 255)
        paint.alpha = pulsingAlpha
        canvas.drawRect(0f, 0f, w, h, paint)

        // 2. High-brightness top accent neon laser line (Always shines above system bottom navigation bar)
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#00E5FF") // Neon Cyan Core
        val laserProgressAlpha = (180 + (75 * progress)).toInt().coerceIn(0, 255)
        paint.alpha = laserProgressAlpha
        
        // Render thin intense cyan laser line at the top boundary of the overlay height (10px deep)
        val density = resources.displayMetrics.density
        val laserHeightPx = 3f * density
        canvas.drawRect(0f, 0f, w, laserHeightPx, paint)
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

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating bottom glow overlay...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Cannot show overlay: Draw overlay permission is missing.")
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        glowView = JarvisGlowView(this)

        val heightInDp = 35
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
            Log.d(TAG, "Glow overlay successfully added to user screen.")
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
