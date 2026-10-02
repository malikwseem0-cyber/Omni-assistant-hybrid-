package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.engine.LlmBackendClient
import com.example.engine.SpeechManager
import com.example.engine.TelecomEngine
import com.example.service.OmniAccessibilityService
import com.example.ui.components.GlowingOrb
import com.example.ui.components.OrbState
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AssistantChatScreen(
    speechManager: SpeechManager,
    llmClient: LlmBackendClient,
    telecomEngine: TelecomEngine,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var orbState by remember { mutableStateOf(OrbState.IDLE) }
    var avatarStyle by remember { mutableStateOf(0) } // 0: Jarvis 3D, 1: Glowing Orb, 2: SceneView
    val isListening by speechManager.isListening.collectAsState()
    val speechAmplitude by speechManager.speechAmplitude.collectAsState()
    val partialText by speechManager.partialText.collectAsState()
    val speechState by speechManager.speechState.collectAsState()

    LaunchedEffect(isListening) {
        orbState = if (isListening) OrbState.LISTENING else OrbState.IDLE
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickChips = listOf(
        "Open Screen Controller",
        "Scroll down",
        "Live Chat Support",
        "Open YouTube and search",
        "What is on my screen?",
        "Check system health",
        "Take screenshot"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Top Avatar & Visualizer Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (avatarStyle) {
                    0 -> {
                        com.example.ui.components.MyJarvis3DDesign(
                            state = orbState,
                            audioAmplitude = speechAmplitude,
                            sizeDp = 140.dp
                        )
                    }
                    1 -> {
                        GlowingOrb(
                            state = orbState,
                            audioAmplitude = speechAmplitude,
                            sizeDp = 130.dp
                        )
                    }
                    else -> {
                        com.example.ui.components.SceneView3DAvatar(
                            state = orbState,
                            audioAmplitude = speechAmplitude,
                            sizeDp = 130.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                WaveformVisualizer(
                    isListening = isListening,
                    amplitude = speechAmplitude,
                    height = 28.dp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (orbState) {
                            OrbState.LISTENING -> "Listening to your voice..."
                            OrbState.THINKING -> "Analyzing command & macro routing..."
                            OrbState.SPEAKING -> "OmniAssist replying..."
                            OrbState.IDLE -> "Tap orb or mic to speak"
                        },
                        color = if (isListening) CyanPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = avatarStyle == 0,
                            onClick = { avatarStyle = 0 },
                            label = { Text("Jarvis 3D", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = avatarStyle == 1,
                            onClick = { avatarStyle = 1 },
                            label = { Text("Orb", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = avatarStyle == 2,
                            onClick = { avatarStyle = 2 },
                            label = { Text("SceneView", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // Live Real-Time Speech Recognition Streaming Banner
        AnimatedVisibility(visible = isListening || partialText.isNotBlank()) {
            Surface(
                color = CyberSurfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isListening) DangerRed else CyanPrimary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isListening) "REAL-TIME SPEECHRECOGNIZER STREAM" else "COMMAND RECOGNIZED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isListening) CyanPrimary else NeonGreen
                        )
                        Text(
                            text = if (partialText.isNotBlank()) "\"$partialText\"" else "Listening... speak now",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickChips) { chip ->
                SuggestionChip(
                    onClick = {
                        onSendMessage(chip)
                        orbState = OrbState.THINKING
                    },
                    label = { Text(chip, fontSize = 12.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = CyberSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        // Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Say \"Hey Omni\" or type a prompt.\nI can control your apps, read your screen,\nmake calls, or execute automation macros.",
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            items(messages) { msg ->
                val isUser = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 300.dp),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) NeonPurple else CyberSurfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "You" else "OmniAssist",
                                fontSize = 11.sp,
                                color = if (isUser) Color.White.copy(alpha = 0.8f) else CyanPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.content,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Mic Toggle
            IconButton(
                onClick = {
                    if (isListening) {
                        speechManager.stopListening()
                    } else {
                        speechManager.startListening()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isListening) DangerRed else CyanPrimary)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Command or ask anything...", color = TextTertiary, fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = CyberCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) CyanPrimary else CyberSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) Color.Black else TextTertiary
                )
            }
        }
    }
}
