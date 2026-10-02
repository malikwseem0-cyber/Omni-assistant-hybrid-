package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SuspiciousAccessLog
import com.example.engine.FaceWatchmanEngine
import com.example.engine.VoiceGuardianEngine
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityScreen(
    voiceGuardian: VoiceGuardianEngine,
    faceWatchman: FaceWatchmanEngine,
    accessLogs: List<SuspiciousAccessLog>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedSection by remember { mutableStateOf(0) } // 0: Voice Guardian, 1: Face Watchman

    val voiceProfile by voiceGuardian.profile.collectAsState()
    val modelStatus by voiceGuardian.modelStatus.collectAsState()
    val lastScore by voiceGuardian.lastVerificationScore.collectAsState()
    val lastResult by voiceGuardian.lastVerificationResult.collectAsState()

    val faceEnrolled by faceWatchman.isEnrolled.collectAsState()
    val watchmanActive by faceWatchman.watchmanActive.collectAsState()
    val faceInfo by faceWatchman.lastDetectedFaceInfo.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "AI Security & Biometrics",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Speaker-ID Voiceprint & Face Watchman Protection",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Section Tabs
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = CyberSurface,
            contentColor = CyanPrimary
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = { Text("Voice Guardian", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) }
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = { Text("Face Watchman", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Security, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedSection == 0) {
                // VOICE GUARDIAN SECTION
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
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (voiceProfile.isEnrolled) NeonGreen else WarningOrange
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (voiceProfile.isEnrolled) "Voiceprint Enrolled" else "Enrollment Pending",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 16.sp
                                    )
                                }
                                Surface(
                                    color = if (voiceProfile.isEnrolled) NeonGreen.copy(alpha = 0.2f) else WarningOrange.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (voiceProfile.isEnrolled) "ACTIVE" else "UNENROLLED",
                                        color = if (voiceProfile.isEnrolled) NeonGreen else WarningOrange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ensures the voice assistant only activates when your specific acoustic vocal tract profile is recognized.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Threshold slider
                            Text(
                                text = "Cosine Similarity Threshold: ${(voiceProfile.similarityThreshold * 100).toInt()}%",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Slider(
                                value = voiceProfile.similarityThreshold,
                                onValueChange = { voiceGuardian.setThreshold(it) },
                                valueRange = 0.5f..0.95f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanPrimary,
                                    activeTrackColor = CyanPrimary,
                                    inactiveTrackColor = CyberSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            // Synthesize voice sample feature vector
                                            val dummyAudio = ShortArray(2048) { (Math.random() * 20000 - 10000).toInt().toShort() }
                                            val vector = voiceGuardian.extractFeatureVector(dummyAudio)
                                            voiceGuardian.enrollVoiceSample("Owner", vector)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Enroll Voice", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val dummyIncoming = ShortArray(2048) { (Math.random() * 20000 - 10000).toInt().toShort() }
                                        val vector = voiceGuardian.extractFeatureVector(dummyIncoming)
                                        voiceGuardian.verifySpeaker(vector)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Test Match")
                                }
                            }

                            if (lastResult != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (lastResult == true)
                                        "Speaker Authenticated! Match: ${(lastScore * 100).toInt()}%"
                                    else
                                        "Access Rejected. Voice mismatch: ${(lastScore * 100).toInt()}%",
                                    color = if (lastResult == true) NeonGreen else DangerRed,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Model download & SHA-256 integrity
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyanPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "On-Device Neural Model Weights",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Status: $modelStatus",
                                color = CyanPrimary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        voiceGuardian.downloadAndVerifyModel("Omni-Silero-SpeakerV2")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = CyanPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download & Verify SHA-256", color = TextPrimary)
                            }
                        }
                    }
                }
            } else {
                // FACE WATCHMAN SECTION
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
                                        text = "Background Watchman",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Log unauthorized face attempts",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = watchmanActive,
                                    onCheckedChange = { faceWatchman.setWatchmanActive(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = CyanPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Camera scanner viewfinder frame
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(CyberSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Face Viewfinder",
                                        tint = if (faceEnrolled) NeonGreen else CyanPrimary,
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = faceInfo ?: "Camera Viewfinder Active",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { faceWatchman.enrollCurrentFace("Device Owner") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Enroll Face", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        // Simulate unauthorized face access
                                        faceWatchman.verifyFace(0.92f, isRecognizedUser = false)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Simulate Intrusion")
                                }
                            }
                        }
                    }
                }

                // Suspicious Access Logs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Suspicious Access Logs (${accessLogs.size})",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        if (accessLogs.isNotEmpty()) {
                            TextButton(onClick = onClearLogs) {
                                Text("Clear", color = DangerRed, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (accessLogs.isEmpty()) {
                    item {
                        Surface(
                            color = CyberSurface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No suspicious access attempts detected.",
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(accessLogs) { log ->
                        val dateStr = remember(log.timestamp) {
                            SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberSurface),
                            border = CardDefaults.outlinedCardBorder(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${log.eventType} • $dateStr",
                                        fontWeight = FontWeight.Bold,
                                        color = DangerRed,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = log.description,
                                        color = TextSecondary,
                                        fontSize = 12.sp
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
