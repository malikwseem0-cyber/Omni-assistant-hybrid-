package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AutomationMacro
import com.example.data.model.ChatMessage
import com.example.data.model.ReminderItem
import com.example.data.model.SuspiciousAccessLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Database(
    entities = [ChatMessage::class, AutomationMacro::class, ReminderItem::class, SuspiciousAccessLog::class],
    version = 1,
    exportSchema = false
)
abstract class OmniDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao
    abstract fun macroDao(): MacroDao
    abstract fun reminderDao(): ReminderDao
    abstract fun accessLogDao(): AccessLogDao

    companion object {
        @Volatile
        private var INSTANCE: OmniDatabase? = null

        fun getDatabase(context: Context): OmniDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmniDatabase::class.java,
                    "omni_assist_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            populateInitialMacros(getDatabase(context).macroDao())
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialMacros(macroDao: MacroDao) {
            val macro1Steps = JSONArray().apply {
                put(JSONObject().apply {
                    put("actionType", "LAUNCH_APP")
                    put("target", "com.google.android.youtube")
                    put("value", "")
                    put("timeoutMs", 2000L)
                })
                put(JSONObject().apply {
                    put("actionType", "WAIT_FOR_SCREEN_TEXT")
                    put("target", "Search")
                    put("value", "")
                    put("timeoutMs", 3000L)
                })
                put(JSONObject().apply {
                    put("actionType", "TAP_TEXT")
                    put("target", "Search")
                    put("value", "")
                    put("timeoutMs", 1000L)
                })
                put(JSONObject().apply {
                    put("actionType", "INPUT_TEXT")
                    put("target", "Search")
                    put("value", "AI Android Tutorial")
                    put("timeoutMs", 1000L)
                })
            }

            val macro2Steps = JSONArray().apply {
                put(JSONObject().apply {
                    put("actionType", "SHELL_CMD")
                    put("target", "uptime")
                    put("value", "")
                    put("timeoutMs", 2000L)
                })
            }

            macroDao.insertMacro(
                AutomationMacro(
                    title = "YouTube Search Assistant",
                    triggerPhrase = "open youtube and search",
                    stepsJson = macro1Steps.toString(),
                    isEnabled = true
                )
            )

            macroDao.insertMacro(
                AutomationMacro(
                    title = "System Uptime Diagnostics",
                    triggerPhrase = "check system health",
                    stepsJson = macro2Steps.toString(),
                    isEnabled = true
                )
            )
        }
    }
}
