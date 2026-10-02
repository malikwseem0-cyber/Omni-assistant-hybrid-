package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun GlowingOrb(
    state: OrbState = OrbState.IDLE,
    audioAmplitude: Float = 0f,
    sizeDp: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulseTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.LISTENING -> 800
                    OrbState.THINKING -> 500
                    OrbState.SPEAKING -> 650
                    OrbState.IDLE -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == OrbState.THINKING) 3000 else 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val innerRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerRotation"
    )

    val coreColors = when (state) {
        OrbState.LISTENING -> listOf(CyanPrimary, NeonGreen, Color.Transparent)
        OrbState.THINKING -> listOf(NeonPurple, CyanPrimary, Color.Transparent)
        OrbState.SPEAKING -> listOf(NeonGreen, CyanPrimary, Color.Transparent)
        OrbState.IDLE -> listOf(CyanPrimary, NeonPurple, Color.Transparent)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(sizeDp)
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val dynamicBoost = if (state == OrbState.LISTENING || state == OrbState.SPEAKING) audioAmplitude * 0.35f else 0f
            val baseRadius = (this.size.minDimension / 2f) * 0.52f * (pulseScale + dynamicBoost)

            // Outer Aura Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColors[0].copy(alpha = 0.45f),
                        coreColors[1].copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.6f
                ),
                radius = baseRadius * 1.6f,
                center = center
            )

            // Core Energy Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        coreColors[0],
                        coreColors[1],
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // Orbiting digital ring 1 (Rotated)
            rotate(rotationAngle, pivot = center) {
                val ringRadius = baseRadius * 1.25f
                drawOval(
                    brush = Brush.sweepGradient(
                        listOf(coreColors[0], coreColors[1], coreColors[0])
                    ),
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius * 0.55f),
                    size = Size(ringRadius * 2f, ringRadius * 1.1f),
                    style = Stroke(width = 3.5f)
                )
            }

            // Orbiting digital ring 2 (Counter-rotated & Tilted)
            rotate(innerRotationAngle, pivot = center) {
                val ringRadius = baseRadius * 1.45f
                drawOval(
                    color = coreColors[1].copy(alpha = 0.75f),
                    topLeft = Offset(center.x - ringRadius * 0.7f, center.y - ringRadius),
                    size = Size(ringRadius * 1.4f, ringRadius * 2f),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                )
            }

            // Orbital particle nodes
            val particleCount = 6
            for (i in 0 until particleCount) {
                val angleRad = Math.toRadians((rotationAngle + (i * 360f / particleCount)).toDouble())
                val orbitRadius = baseRadius * 1.35f
                val px = center.x + (orbitRadius * cos(angleRad)).toFloat()
                val py = center.y + (orbitRadius * 0.6f * sin(angleRad)).toFloat()

                drawCircle(
                    color = if (i % 2 == 0) coreColors[0] else coreColors[1],
                    radius = 4.5f,
                    center = Offset(px, py)
                )
            }
        }
    }
}
