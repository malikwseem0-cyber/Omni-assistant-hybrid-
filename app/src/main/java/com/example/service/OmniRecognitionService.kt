package com.example.service

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.util.Log

class OmniRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        Log.d(TAG, "onStartListening called")
        listener?.beginningOfSpeech()
        // Provide acknowledgment
        val bundle = Bundle().apply {
            putStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION,
                arrayListOf("Omni assistant listening")
            )
        }
        listener?.results(bundle)
    }

    override fun onStopListening(listener: Callback?) {
        Log.d(TAG, "onStopListening called")
        listener?.endOfSpeech()
    }

    override fun onCancel(listener: Callback?) {
        Log.d(TAG, "onCancel called")
    }

    companion object {
        private const val TAG = "OmniRecognitionService"
    }
}
