package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantDao {

    // --- Chat Sessions & Messages ---
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeChatSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    suspend fun getAllChatSessionsOnce(): List<ChatSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateChatSession(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET title = :newTitle, updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun renameChatSession(sessionId: Long, newTitle: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteChatSession(sessionId: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatMessages()

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllChatSessions()

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesOnce(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET memorySaved = 1 WHERE id = :messageId")
    suspend fun markMessageMemorySaved(messageId: Long)

    // --- Custom Commands ---
    @Query("SELECT * FROM custom_commands ORDER BY isBuiltIn DESC, title ASC")
    fun observeCustomCommands(): Flow<List<CustomCommandEntity>>

    @Query("SELECT * FROM custom_commands")
    suspend fun getAllCustomCommandsOnce(): List<CustomCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCommand(command: CustomCommandEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCommands(commands: List<CustomCommandEntity>)

    @Update
    suspend fun updateCustomCommand(command: CustomCommandEntity)

    @Delete
    suspend fun deleteCustomCommand(command: CustomCommandEntity)

    // --- Personal Memory Vault ---
    @Query("SELECT * FROM memory_items ORDER BY updatedAt DESC")
    fun observeMemoryItems(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items ORDER BY updatedAt DESC")
    suspend fun getAllMemoryItemsOnce(): List<MemoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryItem(item: MemoryItemEntity): Long

    @Update
    suspend fun updateMemoryItem(item: MemoryItemEntity)

    @Delete
    suspend fun deleteMemoryItem(item: MemoryItemEntity)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemoryItems()

    // --- Attendance & Salary ---
    @Query("SELECT * FROM attendance_records ORDER BY dateString DESC")
    fun observeAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE yearMonth = :yearMonth ORDER BY dateString DESC")
    fun observeAttendanceForMonth(yearMonth: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE dateString = :dateString LIMIT 1")
    suspend fun getAttendanceByDate(dateString: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records ORDER BY dateString DESC")
    suspend fun getAllAttendanceOnce(): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendanceRecord(record: AttendanceRecordEntity): Long

    @Delete
    suspend fun deleteAttendanceRecord(record: AttendanceRecordEntity)

    @Query("SELECT * FROM salary_adjustments ORDER BY timestamp DESC")
    fun observeAllSalaryAdjustments(): Flow<List<SalaryAdjustmentEntity>>

    @Query("SELECT * FROM salary_adjustments ORDER BY timestamp DESC")
    suspend fun getAllSalaryAdjustmentsOnce(): List<SalaryAdjustmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryAdjustment(adjustment: SalaryAdjustmentEntity): Long

    @Delete
    suspend fun deleteSalaryAdjustment(adjustment: SalaryAdjustmentEntity)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAllAttendance()

    @Query("DELETE FROM salary_adjustments")
    suspend fun clearAllSalaryAdjustments()
}
