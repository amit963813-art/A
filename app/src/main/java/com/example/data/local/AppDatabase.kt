package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        CustomCommandEntity::class,
        MemoryItemEntity::class,
        AttendanceRecordEntity::class,
        SalaryAdjustmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun assistantDao(): AssistantDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_ai_assistant.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun defaultBuiltInCommands(): List<CustomCommandEntity> = listOf(
            CustomCommandEntity(
                title = "Open YouTube (YouTube kholo)",
                triggerPhrases = "youtube kholo,open youtube,यूट्यूब खोलो,youtube chalu karo",
                actionType = "OPEN_APP",
                targetPackageOrAction = "com.google.android.youtube",
                customReplyText = "YouTube khol raha hoon.",
                requiresConfirmation = false,
                isEnabled = true,
                isBuiltIn = true
            ),
            CustomCommandEntity(
                title = "Open Camera (Camera kholo)",
                triggerPhrases = "camera kholo,open camera,कैमरा खोलो,photo khicho",
                actionType = "SYSTEM_INTENT",
                targetPackageOrAction = "android.media.action.STILL_IMAGE_CAMERA",
                customReplyText = "Camera open kar raha hoon.",
                requiresConfirmation = false,
                isEnabled = true,
                isBuiltIn = true
            ),
            CustomCommandEntity(
                title = "Open Calculator (Calculator kholo)",
                triggerPhrases = "calculator kholo,open calculator,कैलकुलेटर खोलो,hisab kholo",
                actionType = "SYSTEM_INTENT",
                targetPackageOrAction = "ANDROID_CALCULATOR",
                customReplyText = "Calculator khol raha hoon.",
                requiresConfirmation = false,
                isEnabled = true,
                isBuiltIn = true
            ),
            CustomCommandEntity(
                title = "Open Phone Settings (Settings kholo)",
                triggerPhrases = "settings kholo,open settings,सेटिंग्स खोलो,phone settings",
                actionType = "SYSTEM_INTENT",
                targetPackageOrAction = "android.settings.SETTINGS",
                customReplyText = "Phone Settings open kar raha hoon.",
                requiresConfirmation = false,
                isEnabled = true,
                isBuiltIn = true
            ),
            CustomCommandEntity(
                title = "Tell Current Time (Time batao)",
                triggerPhrases = "time batao,samay kya hua hai,what time is it,abhi kitne baje hain,टाइम बताओ",
                actionType = "SPEAK_TIME",
                targetPackageOrAction = "INTERNAL_TIME",
                customReplyText = "",
                requiresConfirmation = false,
                isEnabled = true,
                isBuiltIn = true
            ),
            CustomCommandEntity(
                title = "Open My Assistant (Mera assistant kholo)",
                triggerPhrases = "mera assistant kholo,open my assistant,assistant chalu karo,मेरा असिस्टेंट खोलो",
                actionType = "OPEN_ASSISTANT",
                targetPackageOrAction = "INTERNAL_ASSISTANT",
                customReplyText = "Main yahan hoon! Bataiye main aapki kya madad karoon?",
                requiresConfirmation = false,
                isEnabled = true,
                isBuiltIn = true
            ),
            CustomCommandEntity(
                title = "Enable Spoken Replies (Mujhe jawab bolkar do)",
                triggerPhrases = "mujhe jawab bolkar do,speak responses,bol kar batao,आवाज़ में जवाब दो,bolo",
                actionType = "TOGGLE_TTS",
                targetPackageOrAction = "ENABLE_TTS",
                customReplyText = "Bilkul! Ab se main aapko har jawab bolkar sunaunga.",
                requiresConfirmation = false,
                isEnabled = true,
                isBuiltIn = true
            ),
            CustomCommandEntity(
                title = "Check Today's Attendance (Aaj ki attendance batao)",
                triggerPhrases = "aaj ki attendance batao,check attendance,meri hajiri batao,आज की अटेंडेंस बताओ,attendance status",
                actionType = "CHECK_ATTENDANCE",
                targetPackageOrAction = "INTERNAL_ATTENDANCE",
                customReplyText = "",
                requiresConfirmation = false,
                isEnabled = true,
                isBuiltIn = true
            )
        )
    }
}
