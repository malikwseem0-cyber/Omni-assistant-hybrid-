package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiKeyManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("omni_api_settings", Context.MODE_PRIVATE)

    private val _apiKey = MutableStateFlow(getStoredApiKey())
    val apiKey = _apiKey.asStateFlow()

    private val _backendUrl = MutableStateFlow(getStoredBackendUrl())
    val backendUrl = _backendUrl.asStateFlow()

    private val _selectedModel = MutableStateFlow(getStoredModel())
    val selectedModel = _selectedModel.asStateFlow()

    fun getEffectiveApiKey(): String {
        val stored = _apiKey.value.trim()
        if (stored.isNotEmpty()) return stored

        // Fallback to BuildConfig if provided by secret manager
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun saveApiKey(newKey: String) {
        val cleaned = newKey.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, cleaned).apply()
        _apiKey.value = cleaned
    }

    fun saveBackendUrl(url: String) {
        val cleaned = url.trim()
        prefs.edit().putString(KEY_BACKEND_URL, cleaned).apply()
        _backendUrl.value = cleaned
    }

    fun saveSelectedModel(model: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, model).apply()
        _selectedModel.value = model
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
        _apiKey.value = ""
    }

    private fun getStoredApiKey(): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    private fun getStoredBackendUrl(): String {
        return prefs.getString(KEY_BACKEND_URL, "") ?: ""
    }

    private fun getStoredModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash"
    }

    companion object {
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
        private const val KEY_BACKEND_URL = "key_backend_url"
        private const val KEY_SELECTED_MODEL = "key_selected_model"

        val AVAILABLE_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-3.5-flash",
            "gemini-3.1-pro-preview"
        )
    }
}
