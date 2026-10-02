package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AutomationMacro
import com.example.data.model.ChatMessage
import com.example.engine.*
import com.example.receiver.OmniAlarmReceiver
import com.example.service.OmniAccessibilityService
import com.example.ui.screens.*
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.launch

enum class MainTab(val title: String, val icon: ImageVector) {
    ASSISTANT("Assistant", Icons.Default.ChatBubble),
    MACROS("Macros", Icons.Default.AltRoute),
    SECURITY("Security", Icons.Default.Shield),
    TOOLS("Tools", Icons.Default.Build),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var speechManager: SpeechManager
    private lateinit var llmClient: LlmBackendClient
    private lateinit var voiceGuardian: VoiceGuardianEngine
    private lateinit var faceWatchman: FaceWatchmanEngine
    private lateinit var telecomEngine: TelecomEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        speechManager = SpeechManager(this)
        llmClient = LlmBackendClient(this)
        voiceGuardian = VoiceGuardianEngine(this)
        faceWatchman = FaceWatchmanEngine(this)
        telecomEngine = TelecomEngine(this)

        val db = (application as OmniApplication).database

        setContent {
            MyApplicationTheme {
                val scope = rememberCoroutineScope()
                var currentTab by remember { mutableStateOf(MainTab.ASSISTANT) }
                var showLiveSupport by remember { mutableStateOf(false) }

                // Runtime Permissions Launcher
                val permissionsToRequest = remember {
                    val list = mutableListOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_CONTACTS
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    list.toTypedArray()
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* permissions evaluated */ }

                LaunchedEffect(Unit) {
                    val needsPrompt = permissionsToRequest.any {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (needsPrompt) {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                }

                // Data flows from Room DB
                val messages by db.chatDao().getAllMessages().collectAsState(initial = emptyList())
                val macros by db.macroDao().getAllMacros().collectAsState(initial = emptyList())
                val accessLogs by db.accessLogDao().getRecentLogs().collectAsState(initial = emptyList())

                // Process message function
                val handleUserMessage: (String) -> Unit = { query ->
                    scope.launch {
                        // Insert user message
                        db.chatDao().insertMessage(ChatMessage(sender = "USER", content = query))

                        // Extract screen context if available
                        val screenText = OmniAccessibilityService.instance?.extractScreenHierarchyText()

                        // Process query with LLM dispatcher
                        val response = llmClient.processUserCommand(
                            userQuery = query,
                            screenContextText = screenText,
                            history = messages
                        )

                        // Insert assistant reply
                        db.chatDao().insertMessage(
                            ChatMessage(
                                sender = "ASSISTANT",
                                content = response.spokenResponse,
                                actionType = response.intent
                            )
                        )

                        // Speak response
                        speechManager.speak(response.spokenResponse)

                        // Execute intent actions
                        when (response.intent) {
                            "RUN_MACRO" -> {
                                response.macroToRun?.let { macro ->
                                    OmniAccessibilityService.instance?.runMacro(
                                        macro.parseSteps(),
                                        onProgress = { _, _ -> },
                                        onComplete = { _, _ -> }
                                    )
                                }
                            }
                            "CALL_CONTACT" -> {
                                response.telecomTarget?.let { target ->
                                    val contacts = telecomEngine.searchContact(target)
                                    val phone = contacts.firstOrNull()?.phoneNumber ?: target
                                    telecomEngine.makeCall(phone, directCall = false)
                                }
                            }
                            "SEND_SMS" -> {
                                if (response.telecomTarget != null && response.telecomMessage != null) {
                                    val contacts = telecomEngine.searchContact(response.telecomTarget)
                                    val phone = contacts.firstOrNull()?.phoneNumber ?: response.telecomTarget
                                    telecomEngine.sendSms(phone, response.telecomMessage)
                                }
                            }
                            "RUN_SHELL" -> {
                                response.shellCommand?.let { cmd ->
                                    val shellRes = ShellExecutor.execute(cmd)
                                    db.chatDao().insertMessage(
                                        ChatMessage(
                                            sender = "ASSISTANT",
                                            content = "Command output:\n${shellRes.stdout.ifEmpty { shellRes.stderr }}"
                                        )
                                    )
                                }
                            }
                            "SET_REMINDER" -> {
                                response.reminderTitle?.let { title ->
                                    val trigger = System.currentTimeMillis() + (response.reminderDelayMinutes * 60 * 1000L)
                                    OmniAlarmReceiver.scheduleReminder(
                                        context = this@MainActivity,
                                        reminderId = System.currentTimeMillis(),
                                        title = title,
                                        triggerTimeMs = trigger,
                                        isAlarm = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Wire speech recognized callback to handler
                DisposableEffect(speechManager) {
                    speechManager.onSpeechRecognizedCallback = { spokenText ->
                        handleUserMessage(spokenText)
                    }
                    onDispose {
                        speechManager.onSpeechRecognizedCallback = null
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBackground),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        if (!showLiveSupport) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyberBackground)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "OmniAssist",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = CyanPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = NeonGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "LIVE AI",
                                            color = NeonGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Screen Controller direct button
                                    IconButton(onClick = { currentTab = MainTab.TOOLS }) {
                                        Icon(Icons.Default.TouchApp, contentDescription = "Screen Controller", tint = CyanPrimary)
                                    }

                                    // Live Support Chat direct button with BADGE
                                    IconButton(onClick = { showLiveSupport = true }) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = NeonGreen) {
                                                    Text("24/7", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.SupportAgent, contentDescription = "Live Chat Support", tint = CyanPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!showLiveSupport) {
                            NavigationBar(
                                containerColor = CyberSurface,
                                contentColor = CyanPrimary,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                MainTab.values().forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = if (selected) CyanPrimary else Color.Gray
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                color = if (selected) CyanPrimary else Color.Gray
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = CyberSurface.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (showLiveSupport) {
                            BackHandler {
                                showLiveSupport = false
                            }
                            LiveSupportScreen(
                                onDismiss = { showLiveSupport = false }
                            )
                        } else {
                            when (currentTab) {
                                MainTab.ASSISTANT -> AssistantChatScreen(
                                    speechManager = speechManager,
                                    llmClient = llmClient,
                                    telecomEngine = telecomEngine,
                                    messages = messages,
                                    onSendMessage = handleUserMessage
                                )
                            MainTab.MACROS -> MacrosScreen(
                                macros = macros,
                                onAddMacro = { macro ->
                                    scope.launch { db.macroDao().insertMacro(macro) }
                                },
                                onDeleteMacro = { macro ->
                                    scope.launch { db.macroDao().deleteMacro(macro) }
                                },
                                onToggleMacro = { macro ->
                                    scope.launch { db.macroDao().updateMacro(macro.copy(isEnabled = !macro.isEnabled)) }
                                }
                            )
                            MainTab.SECURITY -> SecurityScreen(
                                voiceGuardian = voiceGuardian,
                                faceWatchman = faceWatchman,
                                accessLogs = accessLogs,
                                onClearLogs = {
                                    scope.launch { db.accessLogDao().clearLogs() }
                                }
                            )
                            MainTab.TOOLS -> ToolsScreen(
                                telecomEngine = telecomEngine,
                                onSpeak = { text -> speechManager.speak(text) }
                            )
                            MainTab.SETTINGS -> SettingsScreen(
                                speechManager = speechManager,
                                llmClient = llmClient
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
