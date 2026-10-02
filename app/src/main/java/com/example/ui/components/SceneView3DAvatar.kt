package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes

/**
 * 3D SceneView Composable for Jetpack Compose
 * Powered by io.github.sceneview:sceneview
 */
@Composable
fun SceneView3DAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 200.dp,
    state: OrbState = OrbState.IDLE,
    audioAmplitude: Float = 0f
) {
    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine = engine)
        val childNodes = rememberNodes()

        // 3D Scene rendering container
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            childNodes = childNodes
        )

        // Overlay holographic energy ring and waveform animation
        GlowingOrb(
            state = state,
            audioAmplitude = audioAmplitude,
            sizeDp = sizeDp
        )
    }
}
