package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.theme.*

@Composable
fun ScreenControlPad(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val service = OmniAccessibilityService.instance
    val isServiceActive = OmniAccessibilityService.isServiceActive

    var textToTap by remember { mutableStateOf("") }
    var textToType by remember { mutableStateOf("") }
    var actionStatus by remember { mutableStateOf("Ready for gesture dispatch") }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Screen Controller",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                }

                Surface(
                    color = if (isServiceActive) NeonGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isServiceActive) "CONNECTED" else "SERVICE OFF",
                        color = if (isServiceActive) NeonGreen else DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Control screen gestures, navigation keys, and automated text inputs remotely.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            if (!isServiceActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Accessibility, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enable Accessibility for Screen Control", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D-PAD & SWIPE GESTURE CONTROLLER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // UP Button (Scroll Up)
                    IconButton(
                        onClick = {
                            service?.scrollUp { ok ->
                                actionStatus = if (ok) "Scrolled Up" else "Scroll gesture failed"
                            }
                        },
                        enabled = isServiceActive,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isServiceActive) CyberSurfaceVariant else CyberCardBorder)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll Up", tint = CyanPrimary, modifier = Modifier.size(32.dp))
                    }

                    // Middle Row: LEFT, CENTER TAP, RIGHT
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                service?.swipeLeft { ok ->
                                    actionStatus = if (ok) "Swiped Left" else "Swipe left failed"
                                }
                            },
                            enabled = isServiceActive,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isServiceActive) CyberSurfaceVariant else CyberCardBorder)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Swipe Left", tint = CyanPrimary, modifier = Modifier.size(32.dp))
                        }

                        // Center Tap Button
                        Button(
                            onClick = {
                                service?.scrollDown { ok ->
                                    actionStatus = if (ok) "Center Tap / Action triggered" else "Gesture failed"
                                }
                            },
                            enabled = isServiceActive,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier.size(64.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("TAP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        IconButton(
                            onClick = {
                                service?.swipeRight { ok ->
                                    actionStatus = if (ok) "Swiped Right" else "Swipe right failed"
                                }
                            },
                            enabled = isServiceActive,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isServiceActive) CyberSurfaceVariant else CyberCardBorder)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Swipe Right", tint = CyanPrimary, modifier = Modifier.size(32.dp))
                        }
                    }

                    // DOWN Button (Scroll Down)
                    IconButton(
                        onClick = {
                            service?.scrollDown { ok ->
                                actionStatus = if (ok) "Scrolled Down" else "Scroll gesture failed"
                            }
                        },
                        enabled = isServiceActive,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isServiceActive) CyberSurfaceVariant else CyberCardBorder)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll Down", tint = CyanPrimary, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // GLOBAL ACTIONS TOOLBAR (Back, Home, Recents, Notifications, Screenshot, Lock)
            Text("System Navigation Keys:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedIconButton(
                    onClick = {
                        val ok = service?.pressBack() == true
                        actionStatus = if (ok) "Pressed Back" else "Back failed"
                    },
                    enabled = isServiceActive
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanPrimary)
                }

                OutlinedIconButton(
                    onClick = {
                        val ok = service?.pressHome() == true
                        actionStatus = if (ok) "Pressed Home" else "Home failed"
                    },
                    enabled = isServiceActive
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = CyanPrimary)
                }

                OutlinedIconButton(
                    onClick = {
                        val ok = service?.pressRecents() == true
                        actionStatus = if (ok) "Opened Recent Apps" else "Recents failed"
                    },
                    enabled = isServiceActive
                ) {
                    Icon(Icons.Default.Apps, contentDescription = "Recents", tint = CyanPrimary)
                }

                OutlinedIconButton(
                    onClick = {
                        val ok = service?.openNotifications() == true
                        actionStatus = if (ok) "Opened Notification Shade" else "Notifications failed"
                    },
                    enabled = isServiceActive
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = CyanPrimary)
                }

                OutlinedIconButton(
                    onClick = {
                        val ok = service?.takeScreenshot() == true
                        actionStatus = if (ok) "Screenshot Captured" else "Screenshot requires Android 9+"
                    },
                    enabled = isServiceActive
                ) {
                    Icon(Icons.Default.Screenshot, contentDescription = "Screenshot", tint = CyanPrimary)
                }

                OutlinedIconButton(
                    onClick = {
                        val ok = service?.lockScreen() == true
                        actionStatus = if (ok) "Screen Locked" else "Lock requires Android 9+"
                    },
                    enabled = isServiceActive
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock Screen", tint = WarningOrange)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CLICK ON-SCREEN TEXT INJECTOR
            Text("Tap Visible On-Screen Text:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textToTap,
                    onValueChange = { textToTap = it },
                    placeholder = { Text("e.g. Search, Subscribe, Submit", color = TextTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val text = textToTap.trim()
                        if (text.isNotEmpty()) {
                            val ok = service?.clickTextOnScreen(text) == true
                            actionStatus = if (ok) "Successfully clicked '$text'" else "Text '$text' not found on current screen"
                        }
                    },
                    enabled = isServiceActive && textToTap.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Click", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // REMOTE TEXT INJECTION / TYPING
            Text("Remote Type Into Active Field:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textToType,
                    onValueChange = { textToType = it },
                    placeholder = { Text("Type text to inject...", color = TextTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val text = textToType.trim()
                        if (text.isNotEmpty()) {
                            val ok = service?.typeTextIntoScreen("", text) == true
                            actionStatus = if (ok) "Injected '$text'" else "No focused editable text field found"
                        }
                    },
                    enabled = isServiceActive && textToType.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Type", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Action Feedback Bar
            Surface(
                color = CyberSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Status: $actionStatus",
                    color = CyanPrimary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
