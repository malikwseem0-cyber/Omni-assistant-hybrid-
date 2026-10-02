package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.MacroActionType
import com.example.data.model.MacroStep
import kotlinx.coroutines.*

class OmniAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "OmniAccessibilityService connected and ready for screen control")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events monitoring for screen updates
    }

    override fun onInterrupt() {
        Log.w(TAG, "OmniAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        serviceScope.cancel()
    }

    // =========================================================================
    // SCREEN CONTROLLING & GESTURE INJECTION APIS
    // =========================================================================

    /**
     * Injects a tap gesture at specific screen coordinates (x, y)
     */
    fun tapAt(x: Float, y: Float, onResult: ((Boolean) -> Unit)? = null) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onResult?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onResult?.invoke(false)
            }
        }, null)
    }

    /**
     * Injects a swipe gesture from start coordinates to end coordinates
     */
    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onResult?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onResult?.invoke(false)
            }
        }, null)
    }

    /**
     * Automatic scroll down (swipes up from lower screen to upper screen)
     */
    fun scrollDown(onResult: ((Boolean) -> Unit)? = null) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        val centerX = width / 2
        swipe(centerX, height * 0.72f, centerX, height * 0.28f, durationMs = 300, onResult = onResult)
    }

    /**
     * Automatic scroll up (swipes down from upper screen to lower screen)
     */
    fun scrollUp(onResult: ((Boolean) -> Unit)? = null) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        val centerX = width / 2
        swipe(centerX, height * 0.28f, centerX, height * 0.72f, durationMs = 300, onResult = onResult)
    }

    /**
     * Automatic swipe left
     */
    fun swipeLeft(onResult: ((Boolean) -> Unit)? = null) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        val centerY = height / 2
        swipe(width * 0.85f, centerY, width * 0.15f, centerY, durationMs = 280, onResult = onResult)
    }

    /**
     * Automatic swipe right
     */
    fun swipeRight(onResult: ((Boolean) -> Unit)? = null) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        val centerY = height / 2
        swipe(width * 0.15f, centerY, width * 0.85f, centerY, durationMs = 280, onResult = onResult)
    }

    // Global Key Actions
    fun pressHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun pressBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    /**
     * Clicks on the first visible node on screen that contains the specified text
     */
    fun clickTextOnScreen(text: String): Boolean {
        return clickNodeWithText(text)
    }

    /**
     * Inputs text into currently focused editable field or node with matching hint/text
     */
    fun typeTextIntoScreen(targetTextOrEmptyForFocused: String, textToType: String): Boolean {
        val root = rootInActiveWindow ?: return false
        if (targetTextOrEmptyForFocused.isNotBlank()) {
            return inputNodeText(targetTextOrEmptyForFocused, textToType)
        }

        // Find currently focused editable node
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && focusedNode.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    // =========================================================================
    // MACRO EXECUTION
    // =========================================================================

    /**
     * Executes an automation macro asynchronously
     */
    fun runMacro(
        steps: List<MacroStep>,
        onProgress: (stepIndex: Int, stepDesc: String) -> Unit,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        serviceScope.launch {
            for ((index, step) in steps.withIndex()) {
                val stepType = try {
                    MacroActionType.valueOf(step.actionType)
                } catch (_: Exception) {
                    MacroActionType.TAP_TEXT
                }

                withContext(Dispatchers.Main) {
                    onProgress(index, "Executing step ${index + 1}: ${step.actionType} (${step.target})")
                }

                val success = when (stepType) {
                    MacroActionType.TAP_TEXT -> clickNodeWithText(step.target)
                    MacroActionType.TAP_ID -> clickNodeWithId(step.target)
                    MacroActionType.INPUT_TEXT -> inputNodeText(step.target, step.value)
                    MacroActionType.WAIT_FOR_SCREEN_TEXT -> waitForText(step.target, step.timeoutMs)
                    MacroActionType.LAUNCH_APP -> launchApp(step.target)
                    MacroActionType.GLOBAL_BACK -> {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        true
                    }
                    MacroActionType.GLOBAL_HOME -> {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        true
                    }
                    MacroActionType.DELAY -> {
                        delay(step.timeoutMs.coerceAtLeast(500L))
                        true
                    }
                    MacroActionType.SHELL_CMD -> true // Handled by shell engine
                }

                if (!success && stepType == MacroActionType.WAIT_FOR_SCREEN_TEXT) {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Timeout waiting for text: '${step.target}'")
                    }
                    return@launch
                }

                delay(600) // Inter-step stabilization delay
            }

            withContext(Dispatchers.Main) {
                onComplete(true, "Macro completed successfully (${steps.size} steps)")
            }
        }
    }

    private fun clickNodeWithText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (nodes.isNullOrEmpty()) return false

        for (node in nodes) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isClickable) {
                val clicked = clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }
        }
        return false
    }

    private fun clickNodeWithId(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        if (nodes.isNullOrEmpty()) return false

        for (node in nodes) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isClickable) {
                val clicked = clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }
        }
        return false
    }

    private fun inputNodeText(targetTextOrId: String, textToSet: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(targetTextOrId).ifEmpty {
            root.findAccessibilityNodeInfosByViewId(targetTextOrId)
        }
        if (nodes.isEmpty()) return false

        for (node in nodes) {
            var editableNode: AccessibilityNodeInfo? = node
            while (editableNode != null && !editableNode.isEditable) {
                editableNode = editableNode.parent
            }
            if (editableNode != null) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToSet)
                }
                return editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }
        }
        return false
    }

    private suspend fun waitForText(targetText: String, timeoutMs: Long): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val root = rootInActiveWindow
            if (root != null) {
                val nodes = root.findAccessibilityNodeInfosByText(targetText)
                if (!nodes.isNullOrEmpty()) {
                    return true
                }
            }
            delay(250)
        }
        return false
    }

    private fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts text from the active screen hierarchy for AI context analysis
     */
    fun extractScreenHierarchyText(): String {
        val root = rootInActiveWindow ?: return "Screen content unavailable or locked."
        val builder = StringBuilder()
        traverseNodes(root, builder, 0)
        return builder.toString()
    }

    private fun traverseNodes(node: AccessibilityNodeInfo?, builder: StringBuilder, depth: Int) {
        if (node == null || depth > 8) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            builder.append("• ").append(text).append("\n")
        } else if (!desc.isNullOrEmpty()) {
            builder.append("• [Desc] ").append(desc).append("\n")
        }

        for (i in 0 until node.childCount) {
            traverseNodes(node.getChild(i), builder, depth + 1)
        }
    }

    companion object {
        private const val TAG = "OmniAccessibility"
        var instance: OmniAccessibilityService? = null
            private set

        val isServiceActive: Boolean
            get() = instance != null
    }
}
