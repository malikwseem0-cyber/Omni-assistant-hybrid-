package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.AutomationMacro
import com.example.data.model.MacroStep
import com.example.service.OmniAccessibilityService
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun MacrosScreen(
    macros: List<AutomationMacro>,
    onAddMacro: (AutomationMacro) -> Unit,
    onDeleteMacro: (AutomationMacro) -> Unit,
    onToggleMacro: (AutomationMacro) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRunningMacro by remember { mutableStateOf(false) }
    var currentStepDesc by remember { mutableStateOf("") }
    var executionResult by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var jsonImportText by remember { mutableStateOf("") }

    val accessibilityEnabled = OmniAccessibilityService.isServiceActive

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Accessibility Macros",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "JSON-based UI automation & app scripting",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showJsonDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "JSON Backup/Import",
                        tint = CyanPrimary
                    )
                }

                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Macro",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Accessibility Permission Notice if disabled
        if (!accessibilityEnabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = WarningOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Accessibility Service Inactive",
                            fontWeight = FontWeight.SemiBold,
                            color = WarningOrange,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Required to inspect screen trees and tap buttons.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                    ) {
                        Text("Enable", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Live execution feedback card
        AnimatedVisibility(visible = isRunningMacro || executionResult != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunningMacro) CyberSurfaceVariant else CyberSurface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRunningMacro) {
                        CircularProgressIndicator(
                            color = CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = NeonGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRunningMacro) "Executing Automation..." else "Execution Finished",
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isRunningMacro) currentStepDesc else (executionResult ?: ""),
                            color = if (isRunningMacro) CyanPrimary else NeonGreen,
                            fontSize = 12.sp
                        )
                    }
                    if (!isRunningMacro) {
                        IconButton(onClick = { executionResult = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextTertiary)
                        }
                    }
                }
            }
        }

        // List of Macros
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(macros) { macro ->
                val steps = remember(macro.stepsJson) { macro.parseSteps() }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = macro.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Trigger: \"${macro.triggerPhrase}\"",
                                    fontSize = 13.sp,
                                    color = CyanPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Switch(
                                checked = macro.isEnabled,
                                onCheckedChange = { onToggleMacro(macro) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = CyanPrimary,
                                    uncheckedTrackColor = CyberSurfaceVariant
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Steps flow preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            steps.take(4).forEach { step ->
                                Surface(
                                    color = CyberSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${step.actionType}: ${step.target.take(10)}",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (steps.size > 4) {
                                Text(
                                    text = "+${steps.size - 4} more",
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${steps.size} automation steps",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onDeleteMacro(macro) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        isRunningMacro = true
                                        executionResult = null
                                        val service = OmniAccessibilityService.instance
                                        if (service == null) {
                                            isRunningMacro = false
                                            executionResult = "Error: Accessibility Service is not active. Enable it in Settings."
                                        } else {
                                            service.runMacro(
                                                steps = steps,
                                                onProgress = { _, desc -> currentStepDesc = desc },
                                                onComplete = { success, msg ->
                                                    isRunningMacro = false
                                                    executionResult = if (success) "Completed: $msg" else "Failed: $msg"
                                                }
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Run Macro", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Macro Creation Dialog
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newTrigger by remember { mutableStateOf("") }
        var appPackage by remember { mutableStateOf("com.google.android.youtube") }
        var waitText by remember { mutableStateOf("Search") }
        var inputTextVal by remember { mutableStateOf("Trending Tech") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Voice Macro", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Macro Title (e.g. YouTube Play)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTrigger,
                        onValueChange = { newTrigger = it },
                        label = { Text("Spoken Voice Trigger") },
                        placeholder = { Text("e.g. play trending videos") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = appPackage,
                        onValueChange = { appPackage = it },
                        label = { Text("App Package To Launch") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = waitText,
                        onValueChange = { waitText = it },
                        label = { Text("Screen Text To Wait For") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputTextVal,
                        onValueChange = { inputTextVal = it },
                        label = { Text("Text To Auto-Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newTrigger.isNotBlank()) {
                            val stepsArray = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("actionType", "LAUNCH_APP")
                                    put("target", appPackage)
                                    put("value", "")
                                    put("timeoutMs", 2000L)
                                })
                                put(JSONObject().apply {
                                    put("actionType", "WAIT_FOR_SCREEN_TEXT")
                                    put("target", waitText)
                                    put("value", "")
                                    put("timeoutMs", 3000L)
                                })
                                put(JSONObject().apply {
                                    put("actionType", "TAP_TEXT")
                                    put("target", waitText)
                                    put("value", "")
                                    put("timeoutMs", 1000L)
                                })
                                put(JSONObject().apply {
                                    put("actionType", "INPUT_TEXT")
                                    put("target", waitText)
                                    put("value", inputTextVal)
                                    put("timeoutMs", 1000L)
                                })
                            }
                            onAddMacro(
                                AutomationMacro(
                                    title = newTitle,
                                    triggerPhrase = newTrigger,
                                    stepsJson = stepsArray.toString(),
                                    isEnabled = true
                                )
                            )
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Save Macro", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // JSON Import/Export Dialog
    if (showJsonDialog) {
        val currentJson = remember(macros) {
            val arr = JSONArray()
            for (m in macros) {
                arr.put(JSONObject().apply {
                    put("title", m.title)
                    put("triggerPhrase", m.triggerPhrase)
                    put("stepsJson", m.stepsJson)
                })
            }
            arr.toString(2)
        }

        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("Macros JSON Backup & Import", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        text = "Paste macros JSON to import or copy below for backup:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (jsonImportText.isEmpty()) currentJson else jsonImportText,
                        onValueChange = { jsonImportText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonImportText.isNotBlank()) {
                            try {
                                val arr = JSONArray(jsonImportText)
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    onAddMacro(
                                        AutomationMacro(
                                            title = obj.getString("title"),
                                            triggerPhrase = obj.getString("triggerPhrase"),
                                            stepsJson = obj.optString("stepsJson", "[]"),
                                            isEnabled = true
                                        )
                                    )
                                }
                            } catch (_: Exception) {}
                        }
                        showJsonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Import JSON", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}
