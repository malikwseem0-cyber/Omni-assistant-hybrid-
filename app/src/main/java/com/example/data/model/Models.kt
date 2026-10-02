package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "USER" or "ASSISTANT" or "SYSTEM"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null,
    val actionPayload: String? = null
)

@Entity(tableName = "automation_macros")
data class AutomationMacro(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerPhrase: String,
    val stepsJson: String, // Serialized list of MacroStep
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun parseSteps(): List<MacroStep> {
        val list = mutableListOf<MacroStep>()
        try {
            val array = JSONArray(stepsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MacroStep(
                        actionType = obj.optString("actionType", MacroActionType.TAP_TEXT.name),
                        target = obj.optString("target", ""),
                        value = obj.optString("value", ""),
                        timeoutMs = obj.optLong("timeoutMs", 3000L)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}

enum class MacroActionType {
    TAP_TEXT,
    TAP_ID,
    INPUT_TEXT,
    WAIT_FOR_SCREEN_TEXT,
    LAUNCH_APP,
    SHELL_CMD,
    GLOBAL_BACK,
    GLOBAL_HOME,
    DELAY
}

data class MacroStep(
    val actionType: String,
    val target: String = "",
    val value: String = "",
    val timeoutMs: Long = 3000L
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("actionType", actionType)
        obj.put("target", target)
        obj.put("value", value)
        obj.put("timeoutMs", timeoutMs)
        return obj
    }
}

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timeEpochMs: Long,
    val isCompleted: Boolean = false,
    val isAlarm: Boolean = false
)

data class VoicePersona(
    val id: String,
    val name: String,
    val description: String,
    val pitch: Float,
    val speed: Float,
    val accentTag: String,
    val sampleText: String
)

data class VoiceprintProfile(
    val isEnrolled: Boolean = false,
    val userName: String = "Authorized User",
    val similarityThreshold: Float = 0.75f,
    val sampleCount: Int = 0,
    val enrolledFeatureVector: List<Float> = emptyList()
)

@Entity(tableName = "suspicious_access_logs")
data class SuspiciousAccessLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "UNKNOWN_FACE", "FAILED_VOICE_AUTH", "TAMPER_DETECTED"
    val description: String,
    val confidence: Float = 0f
)
