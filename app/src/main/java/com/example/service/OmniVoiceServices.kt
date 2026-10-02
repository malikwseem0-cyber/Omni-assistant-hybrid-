package com.example.service

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ui.AssistOverlayActivity

class OmniVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        Log.d(TAG, "OmniVoiceInteractionService is ready as system assistant")
        instance = this
    }

    override fun onShutdown() {
        super.onShutdown()
        if (instance === this) {
            instance = null
        }
    }

    companion object {
        private const val TAG = "OmniVoiceService"
        var instance: OmniVoiceInteractionService? = null
            private set
    }
}

class OmniVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return OmniVoiceSession(this)
    }
}

class OmniVoiceSession(context: Context) : VoiceInteractionSession(context) {

    private var assistantDialogView: View? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateContentView(): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#CC0B132B"))
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val titleView = TextView(context).apply {
            text = "OmniAssist Active"
            textSize = 20f
            setTextColor(Color.parseColor("#4DEEEA"))
            gravity = Gravity.CENTER
        }

        val subtitleView = TextView(context).apply {
            text = "Listening for your voice or inspecting screen context..."
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }

        container.addView(titleView)
        container.addView(subtitleView)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        root.addView(container, params)
        assistantDialogView = root

        root.setOnClickListener {
            // Launch full overlay activity
            val intent = Intent(context, AssistOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            hide()
        }

        return root
    }

    override fun onHandleAssist(
        data: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?
    ) {
        super.onHandleAssist(data, structure, content)
        Log.d("OmniVoiceSession", "onHandleAssist received screen context data")
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        Log.d("OmniVoiceSession", "onHandleScreenshot captured bitmap: ${screenshot != null}")
        lastScreenCapture = screenshot
    }

    companion object {
        var lastScreenCapture: Bitmap? = null
    }
}
