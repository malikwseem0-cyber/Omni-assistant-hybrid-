package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.OmniApplication
import com.example.data.model.ChatMessage
import com.example.engine.LlmBackendClient
import com.example.engine.TelecomEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class OmniForegroundService : Service(), LocationListener, RecognitionListener, TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var locationManager: LocationManager? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var llmClient: LlmBackendClient
    private lateinit var telecomEngine: TelecomEngine

    override fun onCreate() {
        super.onCreate()
        instance = this
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        llmClient = LlmBackendClient(this)
        telecomEngine = TelecomEngine(this)

        tts = TextToSpeech(applicationContext, this)

        mainHandler.post {
            initSpeechRecognizer()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsReady = true
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(this@OmniForegroundService)
                }
                Log.d(TAG, "SpeechRecognizer created in OmniForegroundService")
            } else {
                Log.w(TAG, "SpeechRecognizer not available on device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init SpeechRecognizer in service", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopListeningLoop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification(
            "OmniAssist Voice Engine",
            "Real-time voice listener active ('Hey Omni')"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startListeningLoop()
        startSpeedMonitoring()

        _isRunning.value = true
        return START_STICKY
    }

    private fun startListeningLoop() {
        mainHandler.post {
            startServiceListening()
        }
    }

    private fun startServiceListening() {
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer?.startListening(intent)
            _isVoiceListening.value = true
            Log.d(TAG, "Foreground service started real-time SpeechRecognizer")
        } catch (e: Exception) {
            Log.w(TAG, "Error starting SpeechRecognizer in service: ${e.localizedMessage}")
            _isVoiceListening.value = false
            reArmListening(1500)
        }
    }

    private fun stopListeningLoop() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            _isVoiceListening.value = false
        }
    }

    private fun reArmListening(delayMs: Long = 800) {
        if (!_isRunning.value) return
        mainHandler.postDelayed({
            if (_isRunning.value) {
                startServiceListening()
            }
        }, delayMs)
    }

    // =========================================================================
    // SPEECH RECOGNITION LISTENER (Background Continuous Voice Processing)
    // =========================================================================

    override fun onReadyForSpeech(params: Bundle?) {
        _isVoiceListening.value = true
    }

    override fun onBeginningOfSpeech() {
        _isVoiceListening.value = true
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _voiceAmplitude.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isVoiceListening.value = false
        _voiceAmplitude.value = 0f
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _latestPartialVoiceText.value = text
            Log.d(TAG, "Foreground service partial voice: $text")
        }
    }

    override fun onResults(results: Bundle?) {
        _isVoiceListening.value = false
        _voiceAmplitude.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spokenText = matches?.firstOrNull() ?: ""

        if (spokenText.isNotBlank()) {
            _latestPartialVoiceText.value = ""
            Log.d(TAG, "Foreground service recognized command: $spokenText")
            processVoiceCommand(spokenText)
        } else {
            reArmListening(800)
        }
    }

    override fun onError(error: Int) {
        _isVoiceListening.value = false
        _voiceAmplitude.value = 0f
        Log.d(TAG, "Foreground service SpeechRecognizer error: $error (re-arming)")
        reArmListening(1200)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    /**
     * Real-time voice command processing dispatcher
     */
    private fun processVoiceCommand(command: String) {
        serviceScope.launch {
            val db = (application as OmniApplication).database

            // Record command in database
            db.chatDao().insertMessage(ChatMessage(sender = "USER", content = command))

            // Update Notification
            updateNotification("OmniAssist Processing", "Command: \"$command\"")

            // Extract active screen context if Accessibility Service is bound
            val screenContext = OmniAccessibilityService.instance?.extractScreenHierarchyText()

            // Run through LLM & Intent dispatcher
            val response = llmClient.processUserCommand(
                userQuery = command,
                screenContextText = screenContext
            )

            // Speak reply
            if (isTtsReady && response.spokenResponse.isNotBlank()) {
                tts?.speak(response.spokenResponse, TextToSpeech.QUEUE_FLUSH, null, "fg_speech_${System.currentTimeMillis()}")
            }

            // Save assistant reply
            db.chatDao().insertMessage(
                ChatMessage(
                    sender = "ASSISTANT",
                    content = response.spokenResponse,
                    actionType = response.intent
                )
            )

            // Execute Intent Actions
            when (response.intent) {
                "RUN_MACRO" -> {
                    response.macroToRun?.let { macro ->
                        val steps = macro.parseSteps()
                        OmniAccessibilityService.instance?.runMacro(steps, { _, _ -> }, { _, _ -> })
                    }
                }
                "CALL_CONTACT" -> {
                    response.telecomTarget?.let { target ->
                        val contacts = telecomEngine.searchContact(target)
                        val phone = contacts.firstOrNull()?.phoneNumber ?: target
                        telecomEngine.makeCall(phone, directCall = false)
                    }
                }
                "SEND_SMS" -> {
                    if (response.telecomTarget != null && response.telecomMessage != null) {
                        val contacts = telecomEngine.searchContact(response.telecomTarget)
                        val phone = contacts.firstOrNull()?.phoneNumber ?: response.telecomTarget
                        telecomEngine.sendSms(phone, response.telecomMessage)
                    }
                }
            }

            // Reset notification text & resume listening
            delay(1500)
            updateNotification("OmniAssist Active", "Listening for 'Hey Omni' & voice commands")
            reArmListening(500)
        }
    }

    private fun updateNotification(title: String, text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildForegroundNotification(title, text))
    }

    private fun buildForegroundNotification(title: String, text: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, OmniApplication.CHANNEL_FOREGROUND)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    // =========================================================================
    // DRIVING SPEED TELEMETRY
    // =========================================================================

    private fun startSpeedMonitoring() {
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                5f,
                this
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission not yet granted for driving detector")
        } catch (_: Exception) {}
    }

    override fun onLocationChanged(location: Location) {
        val speedKmh = (location.speed * 3.6f)
        _currentSpeedKmh.value = speedKmh
        val isDriving = speedKmh > 20f
        if (_isDrivingModeActive.value != isDriving) {
            _isDrivingModeActive.value = isDriving
            Log.d(TAG, "Driving mode transitioned to: $isDriving at speed: $speedKmh km/h")
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(this)
        stopListeningLoop()
        tts?.stop()
        tts?.shutdown()
        serviceScope.cancel()
        _isRunning.value = false
        if (instance === this) {
            instance = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "OmniForegroundService"
        const val ACTION_START = "com.example.action.START_FOREGROUND"
        const val ACTION_STOP = "com.example.action.STOP_FOREGROUND"
        const val NOTIFICATION_ID = 1001

        var instance: OmniForegroundService? = null
            private set

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _isVoiceListening = MutableStateFlow(false)
        val isVoiceListening = _isVoiceListening.asStateFlow()

        private val _latestPartialVoiceText = MutableStateFlow("")
        val latestPartialVoiceText = _latestPartialVoiceText.asStateFlow()

        private val _voiceAmplitude = MutableStateFlow(0f)
        val voiceAmplitude = _voiceAmplitude.asStateFlow()

        private val _currentSpeedKmh = MutableStateFlow(0f)
        val currentSpeedKmh = _currentSpeedKmh.asStateFlow()

        private val _isDrivingModeActive = MutableStateFlow(false)
        val isDrivingModeActive = _isDrivingModeActive.asStateFlow()

        fun setManualDrivingMode(active: Boolean) {
            _isDrivingModeActive.value = active
        }
    }
}
