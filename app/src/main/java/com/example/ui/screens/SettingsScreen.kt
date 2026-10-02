package com.example.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ApiKeyManager
import com.example.data.model.VoicePersona
import com.example.engine.LlmBackendClient
import com.example.engine.SpeechManager
import com.example.service.OmniForegroundService
import com.example.service.OmniWallpaperService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    speechManager: SpeechManager,
    llmClient: LlmBackendClient,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentPersona by speechManager.currentPersona.collectAsState()
    val isFgRunning by OmniForegroundService.isRunning.collectAsState()

    // API Key & Model Settings State
    val storedApiKey by llmClient.apiKeyManager.apiKey.collectAsState()
    val storedBackendUrl by llmClient.apiKeyManager.backendUrl.collectAsState()
    val selectedModel by llmClient.apiKeyManager.selectedModel.collectAsState()

    var apiKeyInput by remember(storedApiKey) { mutableStateOf(storedApiKey) }
    var backendUrlInput by remember(storedBackendUrl) { mutableStateOf(storedBackendUrl) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var isTestingKey by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showSavedSnackbar by remember { mutableStateOf(false) }

    var gmailConnected by remember { mutableStateOf(false) }
    var calendarConnected by remember { mutableStateOf(false) }
    var driveConnected by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings & API Keys",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Gemini API, Personas, Wake-word & Privacy Center",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. GEMINI API KEY & LLM ENGINE CARD
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = CyanPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini API Key Setting",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                            }

                            val hasKey = llmClient.apiKeyManager.getEffectiveApiKey().isNotEmpty()
                            Surface(
                                color = if (hasKey) NeonGreen.copy(alpha = 0.2f) else WarningOrange.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (hasKey) "ACTIVE" else "OFFLINE",
                                    color = if (hasKey) NeonGreen else WarningOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your Google AI Studio Gemini API key to activate cloud conversational intelligence and reasoning.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // API Key Input Field
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                testResult = null
                            },
                            label = { Text("Gemini API Key (AIzaSy...)") },
                            placeholder = { Text("Paste your API key here", color = TextTertiary) },
                            singleLine = true,
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = CyanPrimary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model Selection
                        Text(
                            text = "Active Gemini Model:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ApiKeyManager.AVAILABLE_MODELS) { model ->
                                val isSelected = model == selectedModel
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { llmClient.apiKeyManager.saveSelectedModel(model) },
                                    label = { Text(model, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanPrimary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Save, Test, Clear
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    llmClient.apiKeyManager.saveApiKey(apiKeyInput)
                                    showSavedSnackbar = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Key", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    isTestingKey = true
                                    testResult = null
                                    scope.launch {
                                        val res = llmClient.testApiKey(apiKeyInput.ifBlank { storedApiKey }, selectedModel)
                                        testResult = res
                                        isTestingKey = false
                                    }
                                },
                                enabled = !isTestingKey && (apiKeyInput.isNotBlank() || storedApiKey.isNotBlank()),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isTestingKey) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyanPrimary)
                                } else {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Key", fontSize = 12.sp)
                                }
                            }

                            if (storedApiKey.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        llmClient.apiKeyManager.clearApiKey()
                                        apiKeyInput = ""
                                        testResult = null
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Test Result Banner
                        testResult?.let { (success, message) ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = if (success) NeonGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (success) NeonGreen else DangerRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = message,
                                        color = if (success) NeonGreen else DangerRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Optional custom backend server endpoint
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = backendUrlInput,
                            onValueChange = {
                                backendUrlInput = it
                                llmClient.apiKeyManager.saveBackendUrl(it)
                            },
                            label = { Text("Custom Server Endpoint (Optional)") },
                            placeholder = { Text("http://10.0.2.2:5000/api/assist", color = TextTertiary) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. TTS Voice Persona Selector
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = CyanPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TTS Voice Personas",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        speechManager.availablePersonas.forEach { persona ->
                            val isSelected = persona.id == currentPersona.id
                            Card(
                                onClick = { speechManager.selectPersona(persona) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) CyberSurfaceVariant else CyberSurface
                                ),
                                border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = persona.name,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) CyanPrimary else TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = persona.description,
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            speechManager.selectPersona(persona)
                                            speechManager.speak(persona.sampleText)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = "Preview voice",
                                            tint = if (isSelected) CyanPrimary else TextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Foreground Continuous Listening & Battery Optimization
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Continuous Wake Word",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Foreground service listening for 'Hey Omni'",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Switch(
                                checked = isFgRunning,
                                onCheckedChange = { active ->
                                    val intent = Intent(context, OmniForegroundService::class.java).apply {
                                        action = if (active) OmniForegroundService.ACTION_START else OmniForegroundService.ACTION_STOP
                                    }
                                    if (active) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            context.startService(intent)
                                        }
                                    } else {
                                        context.stopService(intent)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = CyanPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Battery optimization button
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                                    if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exempt From Battery Optimization")
                        }
                    }
                }
            }

            // 4. System Assistant & Live Wallpaper Setup
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "System Integration",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SettingsVoice, contentDescription = null, tint = CyanPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set as Default Assistant App", color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                    putExtra(
                                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                        ComponentName(context, OmniWallpaperService::class.java)
                                    )
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = null, tint = NeonPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set 3D Hologram Live Wallpaper", color = TextPrimary)
                        }
                    }
                }
            }

            // 5. OAuth Connectors (Google Services)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Cloud OAuth Connectors",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Connect Google Workspace accounts for email and calendar voice control",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ConnectorRow("Gmail Connector", "Read unread emails and draft replies", gmailConnected) {
                            gmailConnected = it
                        }
                        ConnectorRow("Google Calendar", "Create and check scheduled calendar events", calendarConnected) {
                            calendarConnected = it
                        }
                        ConnectorRow("Google Drive", "Search and access cloud documents", driveConnected) {
                            driveConnected = it
                        }
                    }
                }
            }

            // 6. Privacy & Consent Center
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Privacy & Consent Center",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "OmniAssist processes voiceprints and sensor data strictly on-device. You have granular control to disable any permission anytime.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        PrivacyPermissionItem("Microphone", "Used strictly for speech recognition and VAD.", true)
                        PrivacyPermissionItem("Camera", "Used strictly for Face Watchman and Barcode/OCR.", true)
                        PrivacyPermissionItem("Accessibility Service", "Used strictly for automating user macros on screen.", true)
                        PrivacyPermissionItem("Contacts & Telecom", "Used strictly when you ask to call or text someone.", true)
                        PrivacyPermissionItem("Notification Listener", "Used to read notifications aloud in Driving Mode.", true)

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage Android Permissions", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectorRow(
    title: String,
    desc: String,
    connected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
            Text(desc, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = connected,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CyanPrimary
            )
        )
    }
}

@Composable
fun PrivacyPermissionItem(
    name: String,
    explanation: String,
    granted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (granted) NeonGreen else DangerRed,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(name, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
            Text(explanation, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
