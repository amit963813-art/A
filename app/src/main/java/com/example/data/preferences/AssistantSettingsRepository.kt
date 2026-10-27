package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_user_settings")

data class AssistantSettings(
    val assistantName: String = "Aarya",
    val hasCompletedOnboarding: Boolean = false,
    val projectSaveChoice: String = "UNSELECTED", // UNSELECTED, GITHUB, ZIP_DOWNLOAD, CONTINUE_LOCAL
    val githubRepoUrl: String = "",
    val wakePhrase: String = "Hey Aarya",
    val isWakePhraseCustom: Boolean = false,
    val isWakeListeningEnabled: Boolean = false,
    val isLockScreenServiceRunning: Boolean = false,
    val showNotificationOnLockScreen: Boolean = true,
    val languageMode: String = "HINGLISH", // HINGLISH, HINDI, ENGLISH
    val responseStyle: String = "FRIENDLY", // FRIENDLY, CONCISE, DETAILED, CODING_EXPERT
    val themePreset: String = "CYBER_INDIGO", // CYBER_INDIGO, SAFFRON_OBSIDIAN, DAYLIGHT_STUDIO
    val useDynamicColors: Boolean = false,
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val speakResponsesByDefault: Boolean = true,
    val aiProvider: String = "FIREBASE_GEMINI", // FIREBASE_GEMINI, OFFLINE_ONLY
    val aiModelName: String = "gemini-3.8-flash", // gemini-3.8-flash, gemini-3.5-flash-lite, gemini-4-argon
    val askBeforeSavingMemory: Boolean = true,
    val requireBiometricForMemory: Boolean = false,
    val requireBiometricForAttendance: Boolean = false,
    val requireBiometricForSettings: Boolean = false,
    val employeeName: String = "Primary Employee",
    val employeeRole: String = "Owner / Developer",
    val monthlyBaseSalary: Double = 24000.0,
    val standardWorkHoursPerDay: Double = 8.0,
    val hourlyOvertimeRate: Double = 150.0,
    val weeklyHolidayDayOfWeek: Int = 3 // 1=Mon, 2=Tue, 3=Wednesday (default per Step 10), ..., 7=Sun
)

class AssistantSettingsRepository(private val context: Context) {

    private object Keys {
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val PROJECT_SAVE_CHOICE = stringPreferencesKey("project_save_choice")
        val GITHUB_REPO_URL = stringPreferencesKey("github_repo_url")
        val WAKE_PHRASE = stringPreferencesKey("wake_phrase")
        val IS_WAKE_PHRASE_CUSTOM = booleanPreferencesKey("is_wake_phrase_custom")
        val IS_WAKE_LISTENING_ENABLED = booleanPreferencesKey("is_wake_listening_enabled")
        val IS_LOCK_SCREEN_SERVICE_RUNNING = booleanPreferencesKey("is_lock_screen_service_running")
        val SHOW_NOTIFICATION_ON_LOCK_SCREEN = booleanPreferencesKey("show_notification_on_lock_screen")
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val RESPONSE_STYLE = stringPreferencesKey("response_style")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val SPEAK_RESPONSES_BY_DEFAULT = booleanPreferencesKey("speak_responses_by_default")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL_NAME = stringPreferencesKey("ai_model_name")
        val ASK_BEFORE_SAVING_MEMORY = booleanPreferencesKey("ask_before_saving_memory")
        val REQUIRE_BIOMETRIC_MEMORY = booleanPreferencesKey("require_biometric_memory")
        val REQUIRE_BIOMETRIC_ATTENDANCE = booleanPreferencesKey("require_biometric_attendance")
        val REQUIRE_BIOMETRIC_SETTINGS = booleanPreferencesKey("require_biometric_settings")
        val EMPLOYEE_NAME = stringPreferencesKey("employee_name")
        val EMPLOYEE_ROLE = stringPreferencesKey("employee_role")
        val MONTHLY_BASE_SALARY = doublePreferencesKey("monthly_base_salary")
        val STANDARD_WORK_HOURS = doublePreferencesKey("standard_work_hours")
        val HOURLY_OVERTIME_RATE = doublePreferencesKey("hourly_overtime_rate")
        val WEEKLY_HOLIDAY_DAY = intPreferencesKey("weekly_holiday_day")
    }

    val settingsFlow: Flow<AssistantSettings> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.ASSISTANT_NAME]?.takeIf { it.isNotBlank() } ?: "Aarya"
        val isCustomWake = prefs[Keys.IS_WAKE_PHRASE_CUSTOM] ?: false
        val wake = if (isCustomWake) {
            prefs[Keys.WAKE_PHRASE]?.takeIf { it.isNotBlank() } ?: "Hey $name"
        } else {
            "Hey $name"
        }
        AssistantSettings(
            assistantName = name,
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            projectSaveChoice = prefs[Keys.PROJECT_SAVE_CHOICE] ?: "UNSELECTED",
            githubRepoUrl = prefs[Keys.GITHUB_REPO_URL] ?: "",
            wakePhrase = wake,
            isWakePhraseCustom = isCustomWake,
            isWakeListeningEnabled = prefs[Keys.IS_WAKE_LISTENING_ENABLED] ?: false,
            isLockScreenServiceRunning = prefs[Keys.IS_LOCK_SCREEN_SERVICE_RUNNING] ?: false,
            showNotificationOnLockScreen = prefs[Keys.SHOW_NOTIFICATION_ON_LOCK_SCREEN] ?: true,
            languageMode = prefs[Keys.LANGUAGE_MODE] ?: "HINGLISH",
            responseStyle = prefs[Keys.RESPONSE_STYLE] ?: "FRIENDLY",
            themePreset = prefs[Keys.THEME_PRESET] ?: "CYBER_INDIGO",
            useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: false,
            voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.0f,
            speakResponsesByDefault = prefs[Keys.SPEAK_RESPONSES_BY_DEFAULT] ?: true,
            aiProvider = prefs[Keys.AI_PROVIDER] ?: "FIREBASE_GEMINI",
            aiModelName = prefs[Keys.AI_MODEL_NAME] ?: "gemini-3.8-flash",
            askBeforeSavingMemory = prefs[Keys.ASK_BEFORE_SAVING_MEMORY] ?: true,
            requireBiometricForMemory = prefs[Keys.REQUIRE_BIOMETRIC_MEMORY] ?: false,
            requireBiometricForAttendance = prefs[Keys.REQUIRE_BIOMETRIC_ATTENDANCE] ?: false,
            requireBiometricForSettings = prefs[Keys.REQUIRE_BIOMETRIC_SETTINGS] ?: false,
            employeeName = prefs[Keys.EMPLOYEE_NAME] ?: "Primary Employee",
            employeeRole = prefs[Keys.EMPLOYEE_ROLE] ?: "Owner / Developer",
            monthlyBaseSalary = prefs[Keys.MONTHLY_BASE_SALARY] ?: 24000.0,
            standardWorkHoursPerDay = prefs[Keys.STANDARD_WORK_HOURS] ?: 8.0,
            hourlyOvertimeRate = prefs[Keys.HOURLY_OVERTIME_RATE] ?: 150.0,
            weeklyHolidayDayOfWeek = prefs[Keys.WEEKLY_HOLIDAY_DAY] ?: 3
        )
    }

    suspend fun updateAssistantName(newName: String) {
        val trimmed = newName.trim().ifBlank { "Aarya" }
        context.dataStore.edit { prefs ->
            prefs[Keys.ASSISTANT_NAME] = trimmed
            val isCustomWake = prefs[Keys.IS_WAKE_PHRASE_CUSTOM] ?: false
            if (!isCustomWake) {
                prefs[Keys.WAKE_PHRASE] = "Hey $trimmed"
            }
        }
    }

    suspend fun completeOnboarding(
        assistantName: String,
        projectSaveChoice: String,
        languageMode: String,
        themePreset: String
    ) {
        val trimmed = assistantName.trim().ifBlank { "Aarya" }
        context.dataStore.edit { prefs ->
            prefs[Keys.ASSISTANT_NAME] = trimmed
            prefs[Keys.WAKE_PHRASE] = "Hey $trimmed"
            prefs[Keys.IS_WAKE_PHRASE_CUSTOM] = false
            prefs[Keys.PROJECT_SAVE_CHOICE] = projectSaveChoice
            prefs[Keys.LANGUAGE_MODE] = languageMode
            prefs[Keys.THEME_PRESET] = themePreset
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = true
        }
    }

    suspend fun updateProjectSaveChoice(choice: String, repoUrl: String = "") {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROJECT_SAVE_CHOICE] = choice
            if (repoUrl.isNotBlank()) {
                prefs[Keys.GITHUB_REPO_URL] = repoUrl.trim()
            }
        }
    }

    suspend fun updateWakePhrase(phrase: String, isCustom: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WAKE_PHRASE] = phrase.trim()
            prefs[Keys.IS_WAKE_PHRASE_CUSTOM] = isCustom
        }
    }

    suspend fun setWakeListeningEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_WAKE_LISTENING_ENABLED] = enabled
        }
    }

    suspend fun setLockScreenServiceRunning(running: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOCK_SCREEN_SERVICE_RUNNING] = running
        }
    }

    suspend fun setShowNotificationOnLockScreen(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_NOTIFICATION_ON_LOCK_SCREEN] = show
        }
    }

    suspend fun updatePersonalization(
        languageMode: String? = null,
        responseStyle: String? = null,
        themePreset: String? = null,
        useDynamicColors: Boolean? = null,
        voiceSpeed: Float? = null,
        voicePitch: Float? = null,
        speakResponsesByDefault: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            languageMode?.let { prefs[Keys.LANGUAGE_MODE] = it }
            responseStyle?.let { prefs[Keys.RESPONSE_STYLE] = it }
            themePreset?.let { prefs[Keys.THEME_PRESET] = it }
            useDynamicColors?.let { prefs[Keys.USE_DYNAMIC_COLORS] = it }
            voiceSpeed?.let { prefs[Keys.VOICE_SPEED] = it.coerceIn(0.5f, 2.0f) }
            voicePitch?.let { prefs[Keys.VOICE_PITCH] = it.coerceIn(0.5f, 2.0f) }
            speakResponsesByDefault?.let { prefs[Keys.SPEAK_RESPONSES_BY_DEFAULT] = it }
        }
    }

    suspend fun updateAiConfig(provider: String, modelName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_PROVIDER] = provider
            prefs[Keys.AI_MODEL_NAME] = modelName
        }
    }

    suspend fun updatePrivacyAndBiometrics(
        askBeforeSavingMemory: Boolean? = null,
        requireBiometricMemory: Boolean? = null,
        requireBiometricAttendance: Boolean? = null,
        requireBiometricSettings: Boolean? = null
    ) {
        context.dataStore.edit { prefs ->
            askBeforeSavingMemory?.let { prefs[Keys.ASK_BEFORE_SAVING_MEMORY] = it }
            requireBiometricMemory?.let { prefs[Keys.REQUIRE_BIOMETRIC_MEMORY] = it }
            requireBiometricAttendance?.let { prefs[Keys.REQUIRE_BIOMETRIC_ATTENDANCE] = it }
            requireBiometricSettings?.let { prefs[Keys.REQUIRE_BIOMETRIC_SETTINGS] = it }
        }
    }

    suspend fun updateSalaryConfig(
        employeeName: String,
        employeeRole: String,
        monthlyBaseSalary: Double,
        standardWorkHoursPerDay: Double,
        hourlyOvertimeRate: Double,
        weeklyHolidayDayOfWeek: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EMPLOYEE_NAME] = employeeName.trim().ifBlank { "Primary Employee" }
            prefs[Keys.EMPLOYEE_ROLE] = employeeRole.trim().ifBlank { "Employee" }
            prefs[Keys.MONTHLY_BASE_SALARY] = monthlyBaseSalary.coerceAtLeast(0.0)
            prefs[Keys.STANDARD_WORK_HOURS] = standardWorkHoursPerDay.coerceIn(1.0, 24.0)
            prefs[Keys.HOURLY_OVERTIME_RATE] = hourlyOvertimeRate.coerceAtLeast(0.0)
            prefs[Keys.WEEKLY_HOLIDAY_DAY] = weeklyHolidayDayOfWeek.coerceIn(1, 7)
        }
    }
}
