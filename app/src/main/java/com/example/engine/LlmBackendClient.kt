package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.local.ApiKeyManager
import com.example.data.local.OmniDatabase
import com.example.data.model.AutomationMacro
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AssistantActionResponse(
    val intent: String,
    val spokenResponse: String,
    val macroToRun: AutomationMacro? = null,
    val shellCommand: String? = null,
    val telecomTarget: String? = null,
    val telecomMessage: String? = null,
    val reminderTitle: String? = null,
    val reminderDelayMinutes: Int = 0
)

class LlmBackendClient(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val db = OmniDatabase.getDatabase(context)
    val apiKeyManager = ApiKeyManager(context)

    /**
     * Verifies if an API key works by sending a lightweight test prompt to Gemini
     */
    suspend fun testApiKey(keyToTest: String, modelName: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val key = keyToTest.trim()
        if (key.isEmpty()) {
            return@withContext Pair(false, "API Key is empty")
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$key"
            val payload = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Respond with 'API Key is valid and working.' in 10 words."))
                        })
                    })
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val reply = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "Connection verified!") ?: "Connection verified!"
                Pair(true, reply.trim())
            } else {
                val json = try { JSONObject(body) } catch (_: Exception) { null }
                val errorMsg = json?.optJSONObject("error")?.optString("message")
                    ?: "HTTP ${response.code}: Verification failed"
                Pair(false, errorMsg)
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage}")
        }
    }

    suspend fun processUserCommand(
        userQuery: String,
        screenContextText: String? = null,
        history: List<ChatMessage> = emptyList(),
        backendServerUrl: String? = null
    ): AssistantActionResponse = withContext(Dispatchers.IO) {
        val queryLower = userQuery.lowercase().trim()

        // 1. Check local registered macros first for instant on-device execution
        val matchingMacro = db.macroDao().findByTrigger(queryLower)
            ?: db.macroDao().getEnabledMacros().firstOrNull {
                queryLower.contains(it.triggerPhrase.lowercase())
            }

        if (matchingMacro != null) {
            return@withContext AssistantActionResponse(
                intent = "RUN_MACRO",
                spokenResponse = "Executing macro: ${matchingMacro.title}",
                macroToRun = matchingMacro
            )
        }

        // 2. Direct Call / SMS intent parsing
        if (queryLower.startsWith("call ")) {
            val contactName = userQuery.substringAfter("call ").trim()
            return@withContext AssistantActionResponse(
                intent = "CALL_CONTACT",
                spokenResponse = "Initiating call to $contactName",
                telecomTarget = contactName
            )
        }

        if (queryLower.startsWith("send sms to ") || queryLower.startsWith("message ")) {
            val after = if (queryLower.startsWith("message ")) userQuery.substringAfter("message ") else userQuery.substringAfter("send sms to ")
            val parts = after.split(" that ", " saying ", limit = 2)
            val recipient = parts.firstOrNull()?.trim() ?: "Contact"
            val msgBody = if (parts.size > 1) parts[1].trim() else "Hello from OmniAssist"
            return@withContext AssistantActionResponse(
                intent = "SEND_SMS",
                spokenResponse = "Drafting SMS to $recipient: \"$msgBody\"",
                telecomTarget = recipient,
                telecomMessage = msgBody
            )
        }

        // 3. Shell / Terminal intent parsing
        if (queryLower.startsWith("run shell ") || queryLower.startsWith("exec ") || queryLower.startsWith("terminal ")) {
            val cmd = userQuery.replace(Regex("^(run shell|exec|terminal)\\s+"), "").trim()
            return@withContext AssistantActionResponse(
                intent = "RUN_SHELL",
                spokenResponse = "Executing terminal command: $cmd",
                shellCommand = cmd
            )
        }

        // 4. Reminders / Alarms intent parsing
        if (queryLower.startsWith("remind me to ") || queryLower.startsWith("set reminder ")) {
            val task = userQuery.replace(Regex("^(remind me to|set reminder)\\s+"), "").trim()
            return@withContext AssistantActionResponse(
                intent = "SET_REMINDER",
                spokenResponse = "Scheduled reminder: $task in 10 minutes",
                reminderTitle = task,
                reminderDelayMinutes = 10
            )
        }

        // 5. Screen Context Question ("What is on my screen?")
        if (queryLower.contains("screen") && (queryLower.contains("what") || queryLower.contains("read") || queryLower.contains("summarize"))) {
            val summary = if (!screenContextText.isNullOrBlank()) {
                "On your screen, I see:\n" + screenContextText.lines().take(5).joinToString("\n")
            } else {
                "I couldn't read active screen contents. Please ensure Accessibility Service is enabled in Settings."
            }
            return@withContext AssistantActionResponse(
                intent = "INSPECT_SCREEN",
                spokenResponse = summary
            )
        }

        // 5.5. Screen Controlling & Remote Gestures
        val accService = com.example.service.OmniAccessibilityService.instance
        if (queryLower == "scroll down" || queryLower == "swipe down") {
            accService?.scrollDown()
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = if (accService != null) "Scrolling down" else "Please enable Accessibility in Settings for screen control"
            )
        }
        if (queryLower == "scroll up" || queryLower == "swipe up") {
            accService?.scrollUp()
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = if (accService != null) "Scrolling up" else "Please enable Accessibility in Settings for screen control"
            )
        }
        if (queryLower == "swipe left") {
            accService?.swipeLeft()
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = "Swiping left"
            )
        }
        if (queryLower == "swipe right") {
            accService?.swipeRight()
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = "Swiping right"
            )
        }
        if (queryLower == "go home" || queryLower == "press home") {
            accService?.pressHome()
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = "Going to home screen"
            )
        }
        if (queryLower == "go back" || queryLower == "press back") {
            accService?.pressBack()
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = "Going back"
            )
        }
        if (queryLower == "recent apps" || queryLower == "show recents" || queryLower == "open recents") {
            accService?.pressRecents()
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = "Opening recent apps"
            )
        }
        if (queryLower == "take screenshot" || queryLower == "capture screen") {
            val ok = accService?.takeScreenshot() == true
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = if (ok) "Screenshot captured" else "Screenshot requires Android 9+ Accessibility"
            )
        }
        if (queryLower.startsWith("click ") || queryLower.startsWith("tap ")) {
            val target = userQuery.replace(Regex("^(click|tap)\\s+"), "").trim()
            val clicked = accService?.clickTextOnScreen(target) == true
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = if (clicked) "Tapped '$target'" else "Could not find '$target' on screen"
            )
        }
        if (queryLower.startsWith("type ") || queryLower.startsWith("input ")) {
            val textToType = userQuery.replace(Regex("^(type|input)\\s+"), "").trim()
            val typed = accService?.typeTextIntoScreen("", textToType) == true
            return@withContext AssistantActionResponse(
                intent = "SCREEN_CONTROL",
                spokenResponse = if (typed) "Typed '$textToType'" else "No active editable text field focused"
            )
        }

        // 6. Direct Gemini API call if API Key is configured
        val effectiveApiKey = apiKeyManager.getEffectiveApiKey()
        if (effectiveApiKey.isNotEmpty()) {
            val selectedModel = apiKeyManager.selectedModel.value
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent?key=$effectiveApiKey"
                val systemInstruction = "You are OmniAssist, a smart phone AI assistant with device automation, macro scripting, and voice control. Keep answers concise, natural, and friendly (1-2 sentences for speech synthesis)."

                val contentsArr = JSONArray()
                // Append recent conversation turns
                history.takeLast(4).forEach { msg ->
                    val role = if (msg.sender == "USER") "user" else "model"
                    contentsArr.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", msg.content))
                        })
                    })
                }

                // Append current user query + screen context if available
                val promptText = if (!screenContextText.isNullOrBlank()) {
                    "$userQuery\n\n[Active Screen Context:\n$screenContextText]"
                } else {
                    userQuery
                }

                contentsArr.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", promptText))
                    })
                })

                val requestJson = JSONObject().apply {
                    put("contents", contentsArr)
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemInstruction))
                        })
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val resObj = JSONObject(body)
                    val geminiReply = resObj.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")

                    if (!geminiReply.isNullOrBlank()) {
                        return@withContext AssistantActionResponse(
                            intent = "GENERAL_ANSWER",
                            spokenResponse = geminiReply.trim()
                        )
                    }
                } else {
                    Log.w(TAG, "Gemini API error ${response.code}: $body")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API call failed: ${e.localizedMessage}")
            }
        }

        // 7. External Backend Server call if configured
        val targetServer = backendServerUrl ?: apiKeyManager.backendUrl.value
        if (targetServer.isNotBlank()) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("query", userQuery)
                    put("screenContext", screenContextText ?: "")
                    val historyArr = JSONArray()
                    history.takeLast(6).forEach {
                        historyArr.put(JSONObject().apply {
                            put("sender", it.sender)
                            put("content", it.content)
                        })
                    }
                    put("history", historyArr)
                }

                val request = Request.Builder()
                    .url(targetServer)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val resObj = JSONObject(body)
                    val reply = resObj.optString("spokenResponse", resObj.optString("reply", "Understood."))
                    val intent = resObj.optString("intent", "GENERAL_ANSWER")
                    return@withContext AssistantActionResponse(
                        intent = intent,
                        spokenResponse = reply
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Backend server request failed: ${e.localizedMessage}")
            }
        }

        // 8. Intelligent conversational fallback
        val defaultSpoken = when {
            queryLower.contains("hello") || queryLower.contains("hi") ->
                "Hello! OmniAssist voice & automation engine is active. Try saying 'open youtube and search', 'remind me to call Mom', or 'check system health'."
            queryLower.contains("who are you") ->
                "I am OmniAssist, your personal on-device AI phone assistant. I can automate apps, monitor security, and run voice commands."
            queryLower.contains("driving") ->
                "Driving mode is armed. Speed telemetry is active and notifications will be read aloud."
            queryLower.contains("battery") ->
                "Battery optimization exemption is active to prevent background service termination."
            else ->
                "Processed command: \"$userQuery\". You can add a Gemini API Key in Settings for complete conversational intelligence."
        }

        return@withContext AssistantActionResponse(
            intent = "GENERAL_ANSWER",
            spokenResponse = defaultSpoken
        )
    }

    companion object {
        private const val TAG = "LlmBackendClient"
    }
}
