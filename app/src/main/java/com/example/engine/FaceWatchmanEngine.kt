package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.OmniDatabase
import com.example.data.model.SuspiciousAccessLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaceWatchmanEngine(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("omni_face_watchman", Context.MODE_PRIVATE)

    private val _isEnrolled = MutableStateFlow(prefs.getBoolean(KEY_IS_ENROLLED, false))
    val isEnrolled = _isEnrolled.asStateFlow()

    private val _watchmanActive = MutableStateFlow(prefs.getBoolean(KEY_WATCHMAN_ACTIVE, false))
    val watchmanActive = _watchmanActive.asStateFlow()

    private val _lastDetectedFaceInfo = MutableStateFlow<String?>(null)
    val lastDetectedFaceInfo = _lastDetectedFaceInfo.asStateFlow()

    private val _verificationPassed = MutableStateFlow<Boolean?>(null)
    val verificationPassed = _verificationPassed.asStateFlow()

    fun enrollCurrentFace(faceLabel: String) {
        prefs.edit()
            .putBoolean(KEY_IS_ENROLLED, true)
            .putString(KEY_FACE_LABEL, faceLabel)
            .apply()
        _isEnrolled.value = true
        _verificationPassed.value = true
        _lastDetectedFaceInfo.value = "Face Enrolled: $faceLabel"
    }

    fun setWatchmanActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_WATCHMAN_ACTIVE, active).apply()
        _watchmanActive.value = active
    }

    fun verifyFace(confidence: Float, isRecognizedUser: Boolean) {
        _verificationPassed.value = isRecognizedUser
        if (isRecognizedUser) {
            _lastDetectedFaceInfo.value = "Verified Owner Face (Confidence: ${(confidence * 100).toInt()}%)"
        } else {
            _lastDetectedFaceInfo.value = "Unauthorized Face Detected (Confidence: ${(confidence * 100).toInt()}%)"
            // If watchman mode is active, log suspicious access
            if (_watchmanActive.value) {
                CoroutineScope(Dispatchers.IO).launch {
                    OmniDatabase.getDatabase(context).accessLogDao().insertLog(
                        SuspiciousAccessLog(
                            eventType = "UNKNOWN_FACE",
                            description = "Unidentified face attempted device/assistant access",
                            confidence = confidence
                        )
                    )
                }
            }
        }
    }

    fun resetEnrollment() {
        prefs.edit().clear().apply()
        _isEnrolled.value = false
        _verificationPassed.value = null
        _lastDetectedFaceInfo.value = null
    }

    companion object {
        private const val KEY_IS_ENROLLED = "key_face_enrolled"
        private const val KEY_FACE_LABEL = "key_face_label"
        private const val KEY_WATCHMAN_ACTIVE = "key_watchman_active"
    }
}
