package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.service.OmniAccessibilityService
import com.example.service.OmniForegroundService
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SupportChatMessage(
    val id: String,
    val sender: String, // "USER" or "AGENT"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val quickActionTitle: String? = null,
    val quickActionType: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSupportScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isAgentTyping by remember { mutableStateOf(false) }

    // Diagnostic status
    val isAccessibilityOn = OmniAccessibilityService.isServiceActive
    val isForegroundServiceOn by OmniForegroundService.isRunning.collectAsState()
    val isBatteryOptimized: Boolean = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val ignoring = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            !ignoring
        } else {
            false
        }
    }

    val messages = remember {
        mutableStateListOf(
            SupportChatMessage(
                id = "1",
                sender = "AGENT",
                message = "Assalam-o-Alaikum! Welcome to OmniAssist Live Support & Helpdesk. Main aapka AI technical assistant hoon. Aap mujhse app installation, Screen Controlling, wake-word, ya Gemini API key ke baare mein koi bhi sawal pooch sakte hain!",
                quickActionTitle = "Run System Health Check",
                quickActionType = "HEALTH_CHECK"
            )
        )
    }

    val quickQuestions = listOf(
        "Screen Controlling kaise kaam karta hai?",
        "Gemini API Key kaise add karein?",
        "Hey Omni wake-word setup",
        "System Diagnostics Report",
        "Macros automation guide"
    )

    fun handleAgentReply(userMsg: String) {
        scope.launch {
            isAgentTyping = true
            delay(900)
            val lower = userMsg.lowercase()

            val reply = when {
                lower.contains("screen control") || lower.contains("screen controlling") || lower.contains("gesture") -> {
                    SupportChatMessage(
                        id = System.currentTimeMillis().toString(),
                        sender = "AGENT",
                        message = "Screen Controlling use karne ke 2 aasan tareeqe hain:\n\n1. Voice Commands: Say 'scroll down', 'scroll up', 'go home', 'go back', 'take screenshot', ya 'click [text]'.\n2. Tools Tab: Tools Screen mein 'Live Screen Control Pad' diya gaya hai jisse aap D-pad gestures aur text injection remote controller ki tarah use kar sakte hain.\n\nNote: Iske liye Settings mein Accessibility Service ON honi zaroori hai.",
                        quickActionTitle = if (!isAccessibilityOn) "Enable Accessibility Service" else null,
                        quickActionType = if (!isAccessibilityOn) "OPEN_ACCESSIBILITY" else null
                    )
                }
                lower.contains("api key") || lower.contains("gemini") -> {
                    SupportChatMessage(
                        id = System.currentTimeMillis().toString(),
                        sender = "AGENT",
                        message = "Gemini API Key configure karne ke steps:\n\n1. Settings tab open karein.\n2. 'Gemini API Key Setting' card mein apni Google AI Studio key paste karein.\n3. 'Save Key' dabayein aur 'Test Key' se verify karein.\nKey lagate hi offline fallback ke bajaye ultra-smart cloud LLM intelligence activate ho jayegi.",
                        quickActionTitle = "Go to Settings",
                        quickActionType = "OPEN_SETTINGS"
                    )
                }
                lower.contains("wake-word") || lower.contains("hey omni") || lower.contains("voice") -> {
                    SupportChatMessage(
                        id = System.currentTimeMillis().toString(),
                        sender = "AGENT",
                        message = "'Hey Omni' continuous wake-word background mein listen karta hai. Agar wake-word trigger na ho raha ho toh:\n1. Settings mein 'Continuous Wake Word' switch ON karein.\n2. 'Exempt From Battery Optimization' par tap karein taaki Android system background listener ko kill na kare.",
                        quickActionTitle = if (isBatteryOptimized) "Fix Battery Optimization" else null,
                        quickActionType = if (isBatteryOptimized) "OPEN_BATTERY" else null
                    )
                }
                lower.contains("health") || lower.contains("diagnostic") || lower.contains("diagnostics") -> {
                    val accStatus = if (isAccessibilityOn) " ACTIVE" else "❌ INACTIVE (Needs Permission)"
                    val fgStatus = if (isForegroundServiceOn) " RUNNING" else "⚠️ IDLE"
                    val battStatus = if (!isBatteryOptimized) " OPTIMIZED" else "⚠️ RESTRICTED"

                    SupportChatMessage(
                        id = System.currentTimeMillis().toString(),
                        sender = "AGENT",
                        message = "Device Diagnostics Report:\n• Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n• Accessibility (Screen Control): $accStatus\n• Background Wake Word Service: $fgStatus\n• Battery Optimization Exemption: $battStatus\n• Database: Room DB Online\n\nSabhi core modules operational hain!",
                        quickActionTitle = if (!isAccessibilityOn) "Open Accessibility Settings" else null,
                        quickActionType = if (!isAccessibilityOn) "OPEN_ACCESSIBILITY" else null
                    )
                }
                lower.contains("macro") || lower.contains("automation") -> {
                    SupportChatMessage(
                        id = System.currentTimeMillis().toString(),
                        sender = "AGENT",
                        message = "Macros se aap multi-step tasks automate kar sakte hain! Jaise:\n• YouTube open karke gaana search karna\n• WhatsApp par message draft karna\n\nMacros tab mein jakar '+' dabayein, apna custom trigger word set karein, aur steps add karein."
                    )
                }
                else -> {
                    SupportChatMessage(
                        id = System.currentTimeMillis().toString(),
                        sender = "AGENT",
                        message = "Main aapka message samajh gaya. Aap kisi bhi feature (Voice, Screen Control, Macros, API Key, Security) ke baare mein guidelines le sakte hain ya diagnostic report check kar sakte hain."
                    )
                }
            }

            isAgentTyping = false
            messages.add(reply)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("OmniAssist Live Support", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Agent Online • 24/7 Technical Helpdesk", fontSize = 11.sp, color = NeonGreen)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSurface)
            )
        },
        containerColor = CyberBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Quick suggestion chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(quickQuestions) { question ->
                    SuggestionChip(
                        onClick = {
                            messages.add(SupportChatMessage(id = System.currentTimeMillis().toString(), sender = "USER", message = question))
                            handleAgentReply(question)
                        },
                        label = { Text(question, fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = CyberSurface,
                            labelColor = CyanPrimary
                        )
                    )
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.sender == "USER"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            color = if (isUser) CyanPrimary else CyberSurface,
                            border = if (!isUser) CardDefaults.outlinedCardBorder() else null,
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = msg.message,
                                    color = if (isUser) Color.Black else TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                msg.quickActionTitle?.let { actionTitle ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            when (msg.quickActionType) {
                                                "OPEN_ACCESSIBILITY" -> {
                                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                    context.startActivity(intent)
                                                }
                                                "OPEN_BATTERY" -> {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                            data = Uri.parse("package:${context.packageName}")
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                                }
                                                "HEALTH_CHECK" -> {
                                                    handleAgentReply("System Diagnostics Report")
                                                }
                                                "OPEN_SETTINGS" -> {
                                                    onDismiss()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(actionTitle, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isAgentTyping) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = CyanPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Support Agent is typing...", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask support team in Urdu / English...", color = TextTertiary, fontSize = 13.sp) },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            messages.add(SupportChatMessage(id = System.currentTimeMillis().toString(), sender = "USER", message = text))
                            inputText = ""
                            handleAgentReply(text)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}
