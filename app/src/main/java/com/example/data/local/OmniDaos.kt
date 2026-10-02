package com.example.data.local

import androidx.room.*
import com.example.data.model.AutomationMacro
import com.example.data.model.ChatMessage
import com.example.data.model.ReminderItem
import com.example.data.model.SuspiciousAccessLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface MacroDao {
    @Query("SELECT * FROM automation_macros ORDER BY createdAt DESC")
    fun getAllMacros(): Flow<List<AutomationMacro>>

    @Query("SELECT * FROM automation_macros WHERE isEnabled = 1")
    suspend fun getEnabledMacros(): List<AutomationMacro>

    @Query("SELECT * FROM automation_macros WHERE LOWER(triggerPhrase) = LOWER(:phrase) LIMIT 1")
    suspend fun findByTrigger(phrase: String): AutomationMacro?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: AutomationMacro): Long

    @Update
    suspend fun updateMacro(macro: AutomationMacro)

    @Delete
    suspend fun deleteMacro(macro: AutomationMacro)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY timeEpochMs ASC")
    fun getPendingReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders ORDER BY timeEpochMs DESC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(item: ReminderItem): Long

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Delete
    suspend fun deleteReminder(item: ReminderItem)
}

@Dao
interface AccessLogDao {
    @Query("SELECT * FROM suspicious_access_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<SuspiciousAccessLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SuspiciousAccessLog): Long

    @Query("DELETE FROM suspicious_access_logs")
    suspend fun clearLogs()
}
