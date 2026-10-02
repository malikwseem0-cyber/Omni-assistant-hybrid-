package com.jarvis.assistant.ui

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
import androidx.compose.ui.unit.dp

@Composable
fun MyJarvis3DDesign() {
    val infiniteTransition = rememberInfiniteTransition()

    // 3D Sphere Rotation angles
    val ringRotation1 by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)))
    val ringRotation2 by infiniteTransition.animateFloat(360f, 0f, infiniteRepeatable(tween(6000, easing = LinearEasing)))
    
    // AI Breathing / Pulsing effect
    val coreGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040914)), // Space Black background
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // 1. 3D Shadow/Glow (Background depth)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0EA5E9).copy(alpha = 0.3f), Color.Transparent),
                    radius = radius * 1.5f
                ),
                radius = radius * 1.5f,
                center = center
            )

            // 2. Outer Hologram Rings (Rotating)
            drawArc(
                color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                startAngle = ringRotation1, sweepAngle = 200f, useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            
            drawArc(
                color = Color(0xFF818CF8).copy(alpha = 0.5f),
                startAngle = ringRotation2, sweepAngle = 150f, useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. The 3D Sphere Core (Light and Depth)
            // Using Radial Gradient to give 3D lighting effect (light from top-left)
            val sphereBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = coreGlow), // Bright highlight
                    Color(0xFF38BDF8), // Cyan mid-tone
                    Color(0xFF1E3A8A), // Dark blue shadow
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
