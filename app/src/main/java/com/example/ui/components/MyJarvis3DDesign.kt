package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MyJarvis3DDesign(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 240.dp,
    audioAmplitude: Float = 0f,
    state: OrbState = OrbState.IDLE
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Jarvis3DTransition")

    // 3D Sphere Rotation angles
    val ringRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == OrbState.LISTENING) 2500 else 4000, easing = LinearEasing)
        ),
        label = "RingRotation1"
    )

    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == OrbState.THINKING) 3000 else 6000, easing = LinearEasing)
        ),
        label = "RingRotation2"
    )

    // AI Breathing / Pulsing effect
    val coreGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == OrbState.SPEAKING) 500 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CoreGlow"
    )

    Box(
        modifier = modifier
            .size(sizeDp)
            .background(Color(0xFF040914).copy(alpha = 0.85f)), // Space Black background
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val dynamicBoost = if (state == OrbState.LISTENING || state == OrbState.SPEAKING) audioAmplitude * 0.25f else 0f
            val radius = (size.width / 2) * (1f + dynamicBoost)

            // 1. 3D Shadow/Glow (Background depth)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        when (state) {
                            OrbState.THINKING -> Color(0xFF9333EA).copy(alpha = 0.35f)
                            OrbState.SPEAKING -> Color(0xFF10B981).copy(alpha = 0.35f)
                            else -> Color(0xFF0EA5E9).copy(alpha = 0.3f)
                        },
                        Color.Transparent
                    ),
                    radius = radius * 1.5f
                ),
                radius = radius * 1.5f,
                center = center
            )

            // 2. Outer Hologram Rings (Rotating)
            drawArc(
                color = when (state) {
                    OrbState.THINKING -> Color(0xFFA855F7).copy(alpha = 0.7f)
                    OrbState.SPEAKING -> Color(0xFF34D399).copy(alpha = 0.7f)
                    else -> Color(0xFF38BDF8).copy(alpha = 0.5f)
                },
                startAngle = ringRotation1,
                sweepAngle = 200f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            drawArc(
                color = when (state) {
                    OrbState.THINKING -> Color(0xFFC084FC).copy(alpha = 0.7f)
                    OrbState.SPEAKING -> Color(0xFF6EE7B7).copy(alpha = 0.7f)
                    else -> Color(0xFF818CF8).copy(alpha = 0.5f)
                },
                startAngle = ringRotation2,
                sweepAngle = 150f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. The 3D Sphere Core (Light and Depth)
            // Using Radial Gradient to give 3D lighting effect (light from top-left)
            val sphereBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (coreGlow + dynamicBoost).coerceIn(0.6f, 1f)), // Bright highlight
                    when (state) {
                        OrbState.THINKING -> Color(0xFFA855F7)
                        OrbState.SPEAKING -> Color(0xFF10B981)
                        else -> Color(0xFF38BDF8) // Cyan mid-tone
                    },
                    when (state) {
                        OrbState.THINKING -> Color(0xFF581C87)
                        OrbState.SPEAKING -> Color(0xFF064E3B)
                        else -> Color(0xFF1E3A8A) // Dark blue shadow
                    },
                    Color.Transparent
                ),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f), // Offsetting light source for 3D look
                radius = radius * 0.8f
            )

            drawCircle(
                brush = sphereBrush,
                radius = radius * 0.6f,
                center = center
            )
        }
    }
}
