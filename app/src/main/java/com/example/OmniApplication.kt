package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.OmniDatabase

class OmniApplication : Application() {

    val database: OmniDatabase by lazy {
        OmniDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val fgChannel = NotificationChannel(
                CHANNEL_FOREGROUND,
                "OmniAssist Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps voice assistant and driving detection active"
            }

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS,
                "OmniAssist Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for scheduled tasks, alarms, and voice reminders"
                enableVibration(true)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "OmniAssist Security & Watchman",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Security alerts from Face Watchman and Voice Guardian"
            }

            notificationManager.createNotificationChannels(listOf(fgChannel, alarmChannel, alertChannel))
        }
    }

    companion object {
        const val CHANNEL_FOREGROUND = "omni_fg_channel"
        const val CHANNEL_ALARMS = "omni_alarm_channel"
        const val CHANNEL_ALERTS = "omni_alerts_channel"

        lateinit var instance: OmniApplication
            private set
    }
}
