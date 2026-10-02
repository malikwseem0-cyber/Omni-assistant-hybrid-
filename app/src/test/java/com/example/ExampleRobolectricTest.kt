package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ApiKeyManager
import com.example.data.model.AutomationMacro
import com.example.data.model.MacroActionType
import com.example.data.model.MacroStep
import com.example.engine.LlmBackendClient
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OmniAssist", appName)
    }

    @Test
    fun `macro parsing works correctly with Android JSON`() {
        val steps = JSONArray().apply {
            put(MacroStep(actionType = MacroActionType.LAUNCH_APP.name, target = "com.google.android.youtube").toJson())
            put(MacroStep(actionType = MacroActionType.WAIT_FOR_SCREEN_TEXT.name, target = "Search").toJson())
            put(MacroStep(actionType = MacroActionType.TAP_TEXT.name, target = "Search").toJson())
        }

        val macro = AutomationMacro(
            title = "Test Macro",
            triggerPhrase = "test trigger",
            stepsJson = steps.toString()
        )

        val parsed = macro.parseSteps()
        assertEquals(3, parsed.size)
        assertEquals("com.google.android.youtube", parsed[0].target)
        assertEquals("WAIT_FOR_SCREEN_TEXT", parsed[1].actionType)
        assertEquals("Search", parsed[2].target)
    }

    @Test
    fun `api key manager stores and retrieves key correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keyManager = ApiKeyManager(context)

        val testKey = "AIzaSyTestKey123456789"
        keyManager.saveApiKey(testKey)
        assertEquals(testKey, keyManager.getEffectiveApiKey())

        keyManager.clearApiKey()
        assertTrue(keyManager.apiKey.value.isEmpty())
    }

    @Test
    fun `screen control voice intents are recognized properly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = LlmBackendClient(context)

        val scrollDownRes = client.processUserCommand("scroll down")
        assertEquals("SCREEN_CONTROL", scrollDownRes.intent)

        val goHomeRes = client.processUserCommand("go home")
        assertEquals("SCREEN_CONTROL", goHomeRes.intent)

        val screenshotRes = client.processUserCommand("take screenshot")
        assertEquals("SCREEN_CONTROL", screenshotRes.intent)

        val clickRes = client.processUserCommand("click Search")
        assertEquals("SCREEN_CONTROL", clickRes.intent)
    }
}
