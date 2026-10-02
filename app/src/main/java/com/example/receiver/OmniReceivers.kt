package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.OmniApplication
import com.example.data.local.OmniDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class OmniAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Omni Reminder"
        val isAlarm = intent.getBooleanExtra(EXTRA_IS_ALARM, false)

        val notificationManager = NotificationManagerCompat.from(context)
        val notification = NotificationCompat.Builder(context, OmniApplication.CHANNEL_ALARMS)
            .setContentTitle(if (isAlarm) "🚨 Omni Alarm" else "⏰ Omni Reminder")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (_: SecurityException) {
            Log.w("OmniAlarmReceiver", "Notification permission missing")
        }

        if (reminderId != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
                OmniDatabase.getDatabase(context).reminderDao().markCompleted(reminderId)
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_IS_ALARM = "extra_is_alarm"

        fun scheduleReminder(
            context: Context,
            reminderId: Long,
            title: String,
            triggerTimeMs: Long,
            isAlarm: Boolean
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, OmniAlarmReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_IS_ALARM, isAlarm)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        }
    }
}

class OmniBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("OmniBootReceiver", "Device rebooted, restoring alarms and reminders")
            CoroutineScope(Dispatchers.IO).launch {
                val db = OmniDatabase.getDatabase(context)
                val pending = db.reminderDao().getPendingReminders().firstOrNull() ?: emptyList()
                val now = System.currentTimeMillis()
                for (reminder in pending) {
                    if (reminder.timeEpochMs > now) {
                        OmniAlarmReceiver.scheduleReminder(
                            context,
                            reminder.id,
                            reminder.title,
                            reminder.timeEpochMs,
                            reminder.isAlarm
                        )
                    }
                }
            }
        }
    }
}
