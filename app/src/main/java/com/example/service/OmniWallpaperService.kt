package com.example.service

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.sin

class OmniWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return HologramOrbEngine()
    }

    inner class HologramOrbEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var phase = 0f

        private val backgroundPaint = Paint().apply {
            color = Color.parseColor("#060A17")
        }

        private val orbCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#4DEEEA")
        }

        private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#74EE15")
        }

        private val drawRunnable = Runnable { drawFrame() }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val width = canvas.width.toFloat()
                    val height = canvas.height.toFloat()
                    val cx = width / 2f
                    val cy = height / 2f
                    val baseRadius = (width.coerceAtMost(height) * 0.22f)

                    // Draw deep dark cyber background
                    canvas.drawRect(0f, 0f, width, height, backgroundPaint)

                    // Radial glow for core orb
                    val pulse = 1f + 0.08f * sin(phase)
                    val currentRadius = baseRadius * pulse

                    val shader = RadialGradient(
                        cx, cy, currentRadius * 1.5f,
                        intArrayOf(
                            Color.parseColor("#00F0FF"),
                            Color.parseColor("#7000FF"),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0.0f, 0.6f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                    orbCorePaint.shader = shader
                    canvas.drawCircle(cx, cy, currentRadius * 1.5f, orbCorePaint)

                    // Animated digital rings
                    for (i in 0..2) {
                        val ringRad = currentRadius * (1.1f + i * 0.28f)
                        val angle = phase * (i + 1) * 0.7f
                        val oval = RectF(
                            cx - ringRad,
                            cy - ringRad * (0.6f + 0.2f * sin(phase * 0.5f + i)),
                            cx + ringRad,
                            cy + ringRad * (0.6f + 0.2f * sin(phase * 0.5f + i))
                        )
                        canvas.save()
                        canvas.rotate(angle * 15f, cx, cy)
                        ringPaint.alpha = (180 - i * 40).coerceIn(40, 255)
                        canvas.drawOval(oval, ringPaint)
                        canvas.restore()
                    }

                    // Orbital particles
                    for (p in 0..7) {
                        val orbitAngle = phase + (p * Math.PI.toFloat() * 2f / 8f)
                        val orbitR = currentRadius * 1.4f
                        val px = cx + orbitR * cos(orbitAngle)
                        val py = cy + (orbitR * 0.5f) * sin(orbitAngle)
                        particlePaint.alpha = (150 + 100 * sin(phase + p)).toInt().coerceIn(60, 255)
                        canvas.drawCircle(px, py, 6f, particlePaint)
                    }

                    phase += 0.04f
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {}
                }
            }

            if (visible) {
                handler.postDelayed(drawRunnable, 33) // ~30 FPS smooth rendering
            }
        }
    }
}
