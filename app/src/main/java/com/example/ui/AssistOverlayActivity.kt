package com.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LlmBackendClient
import com.example.engine.SpeechManager
import com.example.service.OmniAccessibilityService
import com.example.ui.components.GlowingOrb
import com.example.ui.components.OrbState
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class AssistOverlayActivity : ComponentActivity() {

    private lateinit var speechManager: SpeechManager
    private lateinit var llmClient: LlmBackendClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechManager = SpeechManager(this)
        llmClient = LlmBackendClient(this)

        setContent {
            MyApplicationTheme {
                val scope = rememberCoroutineScope()
                var promptText by remember { mutableStateOf("Listening for command...") }
                var assistantReply by remember { mutableStateOf("") }
                var orbState by remember { mutableStateOf(OrbState.LISTENING) }
                val amplitude by speechManager.speechAmplitude.collectAsState()
                val isListening by speechManager.isListening.collectAsState()

                LaunchedEffect(Unit) {
                    speechManager.onSpeechRecognizedCallback = { spokenText ->
                        promptText = spokenText
                        orbState = OrbState.THINKING
                        scope.launch {
                            val screenCtx = OmniAccessibilityService.instance?.extractScreenHierarchyText()
                            val response = llmClient.processUserCommand(spokenText, screenCtx)
                            assistantReply = response.spokenResponse
                            orbState = OrbState.SPEAKING
                            speechManager.speak(response.spokenResponse)
                        }
                    }
                    speechManager.startListening()
                }

                // Semi-transparent overlay backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            finish()
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.95f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OmniAssist Voice Overlay",
                                    color = CyanPrimary,
                                    fontSize = 16.sp,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close overlay",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            GlowingOrb(
                                state = orbState,
                                audioAmplitude = amplitude,
                                sizeDp = 130.dp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            WaveformVisualizer(
                                isListening = isListening,
                                amplitude = amplitude,
                                height = 36.dp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (assistantReply.isNotEmpty()) assistantReply else promptText,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Quick actions row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = {
                                        promptText = "What is on my screen?"
                                        orbState = OrbState.THINKING
                                        scope.launch {
                                            val screenCtx = OmniAccessibilityService.instance?.extractScreenHierarchyText()
                                            val res = llmClient.processUserCommand("What is on my screen?", screenCtx)
                                            assistantReply = res.spokenResponse
                                            orbState = OrbState.SPEAKING
                                            speechManager.speak(res.spokenResponse)
                                        }
                                    },
                                    label = { Text("Read Screen") }
                                )

                                AssistChip(
                                    onClick = {
                                        promptText = "Open YouTube and search"
                                        orbState = OrbState.THINKING
                                        scope.launch {
                                            val res = llmClient.processUserCommand("open youtube and search")
                                            assistantReply = res.spokenResponse
                                            orbState = OrbState.SPEAKING
                                            speechManager.speak(res.spokenResponse)
                                            res.macroToRun?.let { macro ->
                                                OmniAccessibilityService.instance?.runMacro(
                                                    macro.parseSteps(),
                                                    onProgress = { _, _ -> },
                                                    onComplete = { _, _ -> }
                                                )
                                            }
                                        }
                                    },
                                    label = { Text("Run Macro") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.destroy()
    }
}
