package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NeonGreen
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    isListening: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    barCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnimation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveformPhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = (canvasWidth / (barCount * 1.5f)).coerceAtLeast(3f)
        val spacing = barWidth * 0.5f
        val centerY = canvasHeight / 2f

        val totalWidth = barCount * (barWidth + spacing) - spacing
        val startX = (canvasWidth - totalWidth) / 2f

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount.toFloat()
            val waveHeight = if (isListening) {
                val harmonic = (sin(phase + progress * 6f) + 1f) / 2f
                val dynamicHeight = (amplitude * canvasHeight * 0.8f).coerceAtLeast(6f)
                (dynamicHeight * harmonic).coerceIn(4f, canvasHeight * 0.95f)
            } else {
                (canvasHeight * 0.08f)
            }

            val x = startX + i * (barWidth + spacing)
            val y = centerY - (waveHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(CyanPrimary, NeonGreen)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, waveHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
