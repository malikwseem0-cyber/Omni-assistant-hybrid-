package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BarcodeOcrEngine
import com.example.engine.ScanResult
import com.example.engine.ShellExecutor
import com.example.engine.ShellResult
import com.example.engine.TelecomEngine
import com.example.service.OmniAccessibilityService
import com.example.service.OmniForegroundService
import com.example.service.OmniNotificationListenerService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    telecomEngine: TelecomEngine,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedToolIndex by remember { mutableStateOf(0) }

    // Driving telemetry
    val speedKmh by OmniForegroundService.currentSpeedKmh.collectAsState()
    val isDrivingMode by OmniForegroundService.isDrivingModeActive.collectAsState()
    val notifications by OmniNotificationListenerService.recentNotifications.collectAsState()

    // Shell state
    var shellInput by remember { mutableStateOf("uptime") }
    var shellOutput by remember { mutableStateOf<ShellResult?>(null) }
    var isExecutingShell by remember { mutableStateOf(false) }

    // Barcode & OCR state
    var scanSampleCount by remember { mutableStateOf(0) }
    var currentScanResult by remember { mutableStateOf<ScanResult?>(null) }

    // Screen context state
    var screenHierarchyText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "AI Tools & Automation",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Driving HUD, Shell Terminal, Barcode/OCR & Screen Context",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tool Selection Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedToolIndex,
            containerColor = CyberSurface,
            contentColor = CyanPrimary,
            edgePadding = 0.dp
        ) {
            Tab(
                selected = selectedToolIndex == 0,
                onClick = { selectedToolIndex = 0 },
                text = { Text("Screen Control") },
                icon = { Icon(Icons.Default.TouchApp, contentDescription = null) }
            )
            Tab(
                selected = selectedToolIndex == 1,
                onClick = { selectedToolIndex = 1 },
                text = { Text("Driving HUD") },
                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) }
            )
            Tab(
                selected = selectedToolIndex == 2,
                onClick = { selectedToolIndex = 2 },
                text = { Text("Shell/Termux") },
                icon = { Icon(Icons.Default.Terminal, contentDescription = null) }
            )
            Tab(
                selected = selectedToolIndex == 3,
                onClick = { selectedToolIndex = 3 },
                text = { Text("Barcode/OCR") },
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
            )
            Tab(
                selected = selectedToolIndex == 4,
                onClick = { selectedToolIndex = 4 },
                text = { Text("Screen Inspector") },
                icon = { Icon(Icons.Default.Screenshot, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedToolIndex) {
                0 -> {
                    // SCREEN CONTROLLER PAD
                    item {
                        com.example.ui.components.ScreenControlPad()
                    }
                }
                1 -> {
                    // DRIVING MODE HUD
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberSurface),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Speed Telemetry",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    FilterChip(
                                        selected = isDrivingMode,
                                        onClick = {
                                            OmniForegroundService.setManualDrivingMode(!isDrivingMode)
                                        },
                                        label = {
                                            Text(if (isDrivingMode) "DRIVING ACTIVE" else "PARKED")
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonGreen,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Big Speedometer Display
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(130.dp)
                                        .clip(CircleShape)
                                        .background(CyberSurfaceVariant)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "%.0f".format(speedKmh),
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Black,
                                            color = CyanPrimary
                                        )
                                        Text(
                                            text = "KM / H",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Hands-free Large Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val latest = notifications.firstOrNull()
                                            if (latest != null) {
                                                onSpeak("Notification from ${latest.packageName}: ${latest.title}. ${latest.text}")
                                            } else {
                                                onSpeak("No unread notifications while driving.")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Read Notifs", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            telecomEngine.makeCall("112", directCall = false)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Emergency", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Driving Notifications readout list
                    item {
                        Text(
                            text = "Recent Read-Aloud Notifications (${notifications.size})",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    }

                    if (notifications.isEmpty()) {
                        item {
                            Surface(
                                color = CyberSurface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No captured notifications. Enable Notification Listener in Settings.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(notifications) { notif ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                                border = CardDefaults.outlinedCardBorder(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${notif.title} (${notif.packageName.substringAfterLast('.')})",
                                        fontWeight = FontWeight.Bold,
                                        color = CyanPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = notif.text,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // SHELL / TERMUX RUNNER
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberSurface),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Termux / Shell Command Console",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Execute local system commands and inspect Android runtime.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick command chips
                                val presets = listOf("uptime", "uname -a", "df -h", "cat /proc/meminfo", "date")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(presets) { p ->
                                        SuggestionChip(
                                            onClick = { shellInput = p },
                                            label = { Text(p, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = shellInput,
                                        onValueChange = { shellInput = it },
                                        label = { Text("Command") },
                                        modifier = Modifier.weight(1f),
                                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            isExecutingShell = true
                                            scope.launch {
                                                val res = ShellExecutor.execute(shellInput)
                                                shellOutput = res
                                                isExecutingShell = false
                                            }
                                        },
                                        enabled = !isExecutingShell && shellInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                                    ) {
                                        if (isExecutingShell) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                                        } else {
                                            Text("Run", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Terminal CRT Black Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF030712))
                                        .padding(12.dp)
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = shellOutput?.let {
                                            "> ${it.command}\n" +
                                            (if (it.stdout.isNotEmpty()) it.stdout else "") +
                                            (if (it.stderr.isNotEmpty()) "\n[stderr]: ${it.stderr}" else "") +
                                            "\n\n[exit code: ${it.exitCode} in ${it.executionTimeMs}ms]"
                                        } ?: "$ Ready for execution. Type command and tap Run.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = if (shellOutput?.exitCode == 0 || shellOutput == null) NeonGreen else DangerRed
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // BARCODE & OCR SCANNER
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberSurface),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "On-Device ML Vision Scanner",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Barcode, QR Code, and Text OCR recognition engine",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Viewfinder box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(CyberSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scanner",
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        scanSampleCount++
                                        currentScanResult = BarcodeOcrEngine.processSimulatedScan(scanSampleCount)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Capture & Analyze Frame", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                currentScanResult?.let { res ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = res.title,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanPrimary,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = res.rawValue,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Scan Result", res.rawValue))
                                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurface)
                                                ) {
                                                    Text("Copy Text")
                                                }
                                                Button(
                                                    onClick = {
                                                        onSpeak("Executing action: ${res.suggestedAction}")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                                                ) {
                                                    Text(res.suggestedAction, color = Color.Black)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // SCREEN INSPECTOR
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberSurface),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Accessibility Screen Context",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Pulls active window node hierarchy for LLM multimodal reasoning.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val text = OmniAccessibilityService.instance?.extractScreenHierarchyText()
                                        screenHierarchyText = text ?: "Accessibility Service not running or screen locked."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Inspect Active Screen Hierarchy", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CyberSurfaceVariant)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = screenHierarchyText ?: "Tap button above to fetch active window accessibility nodes.",
                                        fontSize = 12.sp,
                                        color = if (screenHierarchyText != null) TextPrimary else TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
