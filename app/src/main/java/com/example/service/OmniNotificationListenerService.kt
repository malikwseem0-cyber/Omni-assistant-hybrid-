package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReceivedNotification(
    val id: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

class OmniNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getString("android.title") ?: extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getString("android.text") ?: extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isNotEmpty() || text.isNotEmpty()) {
            val item = ReceivedNotification(
                id = "${sbn.id}_${sbn.postTime}",
                packageName = sbn.packageName,
                title = title,
                text = text,
                timestamp = sbn.postTime
            )
            _recentNotifications.value = (listOf(item) + _recentNotifications.value).take(30)
        }
    }

    companion object {
        private const val TAG = "OmniNotificationListener"
        var isConnected: Boolean = false
            private set

        private val _recentNotifications = MutableStateFlow<List<ReceivedNotification>>(emptyList())
        val recentNotifications = _recentNotifications.asStateFlow()
    }
}
