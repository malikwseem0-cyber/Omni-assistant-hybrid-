package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.model.VoicePersona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

enum class SpeechState {
    IDLE,
    READY,
    LISTENING,
    PROCESSING,
    ERROR
}

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener, RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var speechRecognizer: SpeechRecognizer? = null

    val availablePersonas = listOf(
        VoicePersona(
            id = "aria",
            name = "Aria (Empathetic AI)",
            description = "Warm, balanced, natural conversational tone",
            pitch = 1.05f,
            speed = 1.0f,
            accentTag = "en_US",
            sampleText = "Hello! I am Aria, your personal phone assistant. How can I help you today?"
        ),
        VoicePersona(
            id = "jarvis",
            name = "Jarvis (Tactical & Concise)",
            description = "Crisp, analytical, fast response style",
            pitch = 0.85f,
            speed = 1.15f,
            accentTag = "en_GB",
            sampleText = "Systems online. All automation protocols ready for execution, sir."
        ),
        VoicePersona(
            id = "kavya",
            name = "Kavya (Hinglish / Bilingual)",
            description = "Friendly Indian English / Hindi conversational assistant",
            pitch = 1.1f,
            speed = 1.0f,
            accentTag = "en_IN",
            sampleText = "Namaste! Main aapki phone assistant hoon. Aaj kya task automate karna hai?"
        ),
        VoicePersona(
            id = "titan",
            name = "Titan (Authoritative)",
            description = "Deep baritone, commanding voice for alerts and security",
            pitch = 0.7f,
            speed = 0.95f,
            accentTag = "en_US",
            sampleText = "Voice Guardian and Watchman security layers are fully operational."
        )
    )

    private val _currentPersona = MutableStateFlow(availablePersonas[0])
    val currentPersona = _currentPersona.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _speechState = MutableStateFlow(SpeechState.IDLE)
    val speechState = _speechState.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText = _partialText.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText = _recognizedText.asStateFlow()

    private val _speechAmplitude = MutableStateFlow(0f)
    val speechAmplitude = _speechAmplitude.asStateFlow()

    var onSpeechRecognizedCallback: ((String) -> Unit)? = null
    var onPartialSpeechCallback: ((String) -> Unit)? = null
    var isContinuousMode: Boolean = false

    private var retryCount = 0
    private val maxRetries = 3

    init {
        tts = TextToSpeech(context.applicationContext, this)
        mainHandler.post {
            initRecognizer()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsInitialized = true
            applyPersona(_currentPersona.value)
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    private fun initRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@SpeechManager)
                }
                Log.d(TAG, "SpeechRecognizer initialized successfully on main thread")
            } else {
                Log.w(TAG, "SpeechRecognizer is not available on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer: ${e.localizedMessage}")
        }
    }

    fun selectPersona(persona: VoicePersona) {
        _currentPersona.value = persona
        applyPersona(persona)
    }

    private fun applyPersona(persona: VoicePersona) {
        tts?.let { engine ->
            engine.setPitch(persona.pitch)
            engine.setSpeechRate(persona.speed)
            val locale = when (persona.accentTag) {
                "en_GB" -> Locale.UK
                "en_IN" -> Locale("en", "IN")
                else -> Locale.US
            }
            try {
                engine.language = locale
            } catch (_: Exception) {}
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isTtsInitialized) return
        tts?.speak(text, queueMode, null, "omni_tts_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun startListening() {
        mainHandler.post {
            if (speechRecognizer == null) {
                initRecognizer()
            }

            _partialText.value = ""
            _speechState.value = SpeechState.READY

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }

            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _speechState.value = SpeechState.LISTENING
                Log.d(TAG, "SpeechRecognizer started listening")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech listening", e)
                _isListening.value = false
                _speechState.value = SpeechState.ERROR
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
            _speechState.value = SpeechState.IDLE
            _speechAmplitude.value = 0f
        }
    }

    fun cancelListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isListening.value = false
            _speechState.value = SpeechState.IDLE
            _speechAmplitude.value = 0f
            _partialText.value = ""
        }
    }

    // RecognitionListener Implementation
    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
        _speechState.value = SpeechState.LISTENING
        retryCount = 0
        Log.d(TAG, "SpeechRecognizer ready for speech")
    }

    override fun onBeginningOfSpeech() {
        _isListening.value = true
        _speechState.value = SpeechState.LISTENING
        Log.d(TAG, "User started speaking")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Map dB (-2 to 10 typical) to 0.0 - 1.0 range
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _speechAmplitude.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _speechState.value = SpeechState.PROCESSING
        _speechAmplitude.value = 0f
        Log.d(TAG, "User ended speech, processing recognition")
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _partialText.value = text
            onPartialSpeechCallback?.invoke(text)
            Log.d(TAG, "Partial speech: $text")
        }
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        _speechState.value = SpeechState.IDLE
        _speechAmplitude.value = 0f

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""

        if (text.isNotBlank()) {
            _recognizedText.value = text
            _partialText.value = ""
            Log.d(TAG, "Final speech recognized: $text")
            onSpeechRecognizedCallback?.invoke(text)
        }

        // In continuous listening mode, re-arm listening after a short pause
        if (isContinuousMode) {
            mainHandler.postDelayed({
                startListening()
            }, 800)
        }
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _speechState.value = SpeechState.ERROR
        _speechAmplitude.value = 0f

        val errorDescription = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input timeout"
            else -> "SpeechRecognizer error code: $error"
        }
        Log.w(TAG, "SpeechRecognizer error: $errorDescription ($error)")

        // For recoverable errors in continuous mode (like timeout or no match), re-listen
        if (isContinuousMode && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
            if (retryCount < maxRetries) {
                retryCount++
                mainHandler.postDelayed({
                    startListening()
                }, 1000)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        mainHandler.post {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d(TAG, "SpeechManager destroyed cleanly")
        }
    }

    companion object {
        private const val TAG = "SpeechManager"
    }
}
