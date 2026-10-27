package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val sender: String, // "USER", "ASSISTANT", "SYSTEM"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOfflineCommand: Boolean = false,
    val proposedMemoryTitle: String? = null,
    val proposedMemoryContent: String? = null,
    val memorySaved: Boolean = false
)

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val triggerPhrases: String, // Comma-separated phrases in Hindi, Hinglish, English
    val actionType: String, // OPEN_APP, SYSTEM_INTENT, SPEAK_TIME, CHECK_ATTENDANCE, TOGGLE_TTS, OPEN_ASSISTANT, CUSTOM_REPLY, MAKE_CALL, SEND_SMS
    val targetPackageOrAction: String = "",
    val customReplyText: String = "",
    val requiresConfirmation: Boolean = false,
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_items")
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "FACT", // FACT, PREFERENCE, NOTE
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["dateString"], unique = true)]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // YYYY-MM-DD
    val yearMonth: String, // YYYY-MM
    val checkInTimeMillis: Long? = null,
    val checkOutTimeMillis: Long? = null,
    val workedMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val status: String = "PRESENT", // PRESENT, HALF_DAY, ABSENT, WEEKLY_HOLIDAY, PAID_LEAVE
    val notes: String = "",
    val biometricVerified: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "salary_adjustments",
    indices = [Index(value = ["yearMonth"])]
)
data class SalaryAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // YYYY-MM-DD
    val yearMonth: String, // YYYY-MM
    val type: String, // ADVANCE, DEDUCTION, BONUS, OVERTIME_MANUAL
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
