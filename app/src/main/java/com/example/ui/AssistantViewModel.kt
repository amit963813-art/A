package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.CustomCommandEntity
import com.example.data.local.MemoryItemEntity
import com.example.data.local.SalaryAdjustmentEntity
import com.example.data.preferences.AssistantSettings
import com.example.data.preferences.AssistantSettingsRepository
import com.example.data.repository.AssistantRepository
import com.example.data.repository.MonthlySalarySummary
import com.example.data.repository.VerifiedZipInfo
import com.example.domain.CommandExecutionOutcome
import com.example.domain.InstalledAppInfo
import com.example.domain.LocalCommandProcessor
import com.example.service.AssistantForegroundService
import com.example.voice.VoiceAndWakeManager
import com.example.voice.VoiceUiState
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AssistantViewModel"

sealed interface AiConnectionStatus {
    data object Ready : AiConnectionStatus
    data object Generating : AiConnectionStatus
    data object OfflineCommandModeOnly : AiConnectionStatus
    data class Error(val message: String) : AiConnectionStatus
}

data class PendingSensitiveCommand(
    val title: String,
    val warningMessage: String,
    val pendingIntent: Intent,
    val replyAfterConfirm: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsRepo = AssistantSettingsRepository(appContext)
    private val database = AppDatabase.getInstance(appContext)
    private val repository = AssistantRepository(database.assistantDao())
    val localCommandProcessor = LocalCommandProcessor(appContext)

    // Lazy cache of Firebase AI Logic generative models (never evaluated eagerly at construction)
    private val modelCache by lazy { mutableMapOf<String, GenerativeModel>() }

    private fun getGenerativeModel(modelName: String): GenerativeModel {
        val validModel = when (modelName) {
            "gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon" -> modelName
            else -> "gemini-3.8-flash"
        }
        return modelCache.getOrPut(validModel) {
            Firebase.ai.generativeModel(validModel)
        }
    }

    val settings: StateFlow<AssistantSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AssistantSettings())

    val chatSessions: StateFlow<List<ChatSessionEntity>> = repository.chatSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Long>(0L)
    val currentSessionId: StateFlow<Long> = _currentSessionId.asStateFlow()

    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { id ->
            if (id <= 0L) MutableStateFlow(emptyList()) else repository.observeMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCommands: StateFlow<List<CustomCommandEntity>> = repository.customCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoryItems: StateFlow<List<MemoryItemEntity>> = repository.memoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceRecordEntity>> = repository.allAttendanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salaryAdjustments: StateFlow<List<SalaryAdjustmentEntity>> = repository.allSalaryAdjustments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedYearMonth = MutableStateFlow(repository.currentYearMonthString())
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    val monthlySalarySummary: StateFlow<MonthlySalarySummary> = combine(
        _selectedYearMonth,
        attendanceRecords,
        salaryAdjustments,
        settings
    ) { ym, records, adjustments, currentSettings ->
        repository.calculateMonthlySalary(ym, records, adjustments, currentSettings)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.calculateMonthlySalary(
            repository.currentYearMonthString(),
            emptyList(),
            emptyList(),
            AssistantSettings()
        )
    )

    private val _streamingPartialResponse = MutableStateFlow<String?>(null)
    val streamingPartialResponse: StateFlow<String?> = _streamingPartialResponse.asStateFlow()

    private val _aiConnectionStatus = MutableStateFlow<AiConnectionStatus>(AiConnectionStatus.Ready)
    val aiConnectionStatus: StateFlow<AiConnectionStatus> = _aiConnectionStatus.asStateFlow()

    private val _pendingSensitiveCommand = MutableStateFlow<PendingSensitiveCommand?>(null)
    val pendingSensitiveCommand: StateFlow<PendingSensitiveCommand?> = _pendingSensitiveCommand.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _verifiedZipInfo = MutableStateFlow<VerifiedZipInfo?>(null)
    val verifiedZipInfo: StateFlow<VerifiedZipInfo?> = _verifiedZipInfo.asStateFlow()

    private val _githubConnectionState = MutableStateFlow<com.example.domain.GitHubConnectionState>(
        com.example.domain.GitHubConnectionState.NotConnected
    )
    val githubConnectionState: StateFlow<com.example.domain.GitHubConnectionState> =
        _githubConnectionState.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _memoryVaultUnlockedInSession = MutableStateFlow(false)
    val memoryVaultUnlockedInSession: StateFlow<Boolean> = _memoryVaultUnlockedInSession.asStateFlow()

    private val _settingsUnlockedInSession = MutableStateFlow(false)
    val settingsUnlockedInSession: StateFlow<Boolean> = _settingsUnlockedInSession.asStateFlow()

    val voiceManager = VoiceAndWakeManager(
        context = appContext,
        onFinalSpeechResult = { transcript, wasWakeTriggered ->
            if (transcript.isNotBlank()) {
                val prefixNote = if (wasWakeTriggered) "[Wake Triggered] " else ""
                sendMessageOrCommand(transcript, fromVoiceInput = true, statusNote = prefixNote)
            }
        }
    )
    val voiceState: StateFlow<VoiceUiState> = voiceManager.state

    init {
        viewModelScope.launch {
            val initialId = repository.ensureSeededAndInitialSession()
            _currentSessionId.value = initialId
            _installedApps.value = localCommandProcessor.queryLaunchableApps()
            // Pre-generate and verify the source ZIP so it is always ready
            val zip = repository.generateAndVerifyProjectZip(appContext, settings.value)
            _verifiedZipInfo.value = zip
        }
        viewModelScope.launch {
            settings.collect { s ->
                voiceManager.updateVoicePreferences(
                    speed = s.voiceSpeed,
                    pitch = s.voicePitch,
                    languageMode = s.languageMode,
                    wakePhrase = s.wakePhrase,
                    assistantName = s.assistantName
                )
                if (s.aiProvider == "OFFLINE_ONLY") {
                    _aiConnectionStatus.value = AiConnectionStatus.OfflineCommandModeOnly
                } else if (_aiConnectionStatus.value is AiConnectionStatus.OfflineCommandModeOnly) {
                    _aiConnectionStatus.value = AiConnectionStatus.Ready
                }
            }
        }
    }

    fun showStatusBanner(msg: String?) {
        _statusBannerMessage.value = msg
    }

    fun fetchGitHubReposForUser(username: String) {
        viewModelScope.launch {
            _githubConnectionState.value = com.example.domain.GitHubConnectionState.Checking
            val result = com.example.domain.GitHubSyncManager.fetchUserRepositories(username)
            _githubConnectionState.value = result
        }
    }

    fun verifyAndConnectGitHubRepo(repoFullName: String, preferPrivate: Boolean = true) {
        viewModelScope.launch {
            _githubConnectionState.value = com.example.domain.GitHubConnectionState.Checking
            val result = com.example.domain.GitHubSyncManager.verifyRepositoryStatus(repoFullName, preferPrivate)
            _githubConnectionState.value = result
            if (result is com.example.domain.GitHubConnectionState.RepoVerified ||
                result is com.example.domain.GitHubConnectionState.PrivateRepoSelected
            ) {
                settingsRepo.updateProjectSaveChoice("GITHUB", repoFullName.trim())
            }
        }
    }

    fun markMemoryVaultUnlocked() {
        _memoryVaultUnlockedInSession.value = true
    }

    fun markSettingsUnlocked() {
        _settingsUnlockedInSession.value = true
    }

    // --- Step 1 & Step 2 Onboarding & Project Saving ---
    fun completeOnboarding(
        assistantName: String,
        projectSaveChoice: String,
        languageMode: String,
        themePreset: String
    ) {
        viewModelScope.launch {
            val cleanName = assistantName.trim().ifBlank { "Aarya" }
            settingsRepo.completeOnboarding(cleanName, projectSaveChoice, languageMode, themePreset)
            val sid = _currentSessionId.value
            if (sid > 0L) {
                repository.addMessage(
                    sessionId = sid,
                    sender = "ASSISTANT",
                    content = "Namaste! Mera naam **$cleanName** set ho gaya hai. Aap mujhe *\"Hey $cleanName\"* keh kar bula sakte hain ya Settings > Personalization mein kabhi bhi mera naam badal sakte hain."
                )
            }
            prepareVerifiedProjectZip()
        }
    }

    fun updateProjectSaveOption(choice: String, repoUrl: String = "") {
        viewModelScope.launch {
            settingsRepo.updateProjectSaveChoice(choice, repoUrl)
            showStatusBanner("Project saving mode updated to: $choice")
        }
    }

    fun prepareVerifiedProjectZip(onReady: ((VerifiedZipInfo) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val info = repository.generateAndVerifyProjectZip(appContext, settings.value)
                _verifiedZipInfo.value = info
                if (info.isValid) {
                    showStatusBanner("Verified Project ZIP ready (${info.sizeBytes / 1024} KB, ${info.entryCount} files).")
                    onReady?.invoke(info)
                } else {
                    showStatusBanner("ZIP verification failed.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate project ZIP", e)
                showStatusBanner("Could not generate ZIP: ${e.localizedMessage}")
            }
        }
    }

    // --- Assistant Identity & Personalization ---
    fun updateAssistantName(newName: String) {
        viewModelScope.launch {
            val trimmed = newName.trim().ifBlank { "Aarya" }
            settingsRepo.updateAssistantName(trimmed)
            if (settings.value.isLockScreenServiceRunning) {
                AssistantForegroundService.startService(
                    context = appContext,
                    assistantName = trimmed,
                    wakePhrase = "Hey $trimmed",
                    showOnLockScreen = settings.value.showNotificationOnLockScreen
                )
            }
            showStatusBanner("Assistant name updated to '$trimmed'. Chat history & attendance preserved.")
        }
    }

    fun updatePersonalization(
        languageMode: String? = null,
        responseStyle: String? = null,
        themePreset: String? = null,
        useDynamicColors: Boolean? = null,
        voiceSpeed: Float? = null,
        voicePitch: Float? = null,
        speakResponsesByDefault: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsRepo.updatePersonalization(
                languageMode = languageMode,
                responseStyle = responseStyle,
                themePreset = themePreset,
                useDynamicColors = useDynamicColors,
                voiceSpeed = voiceSpeed,
                voicePitch = voicePitch,
                speakResponsesByDefault = speakResponsesByDefault
            )
        }
    }

    fun updateWakePhrase(phrase: String, isCustom: Boolean = true) {
        viewModelScope.launch {
            settingsRepo.updateWakePhrase(phrase, isCustom)
            showStatusBanner("Wake phrase updated to \"$phrase\"")
        }
    }

    fun toggleWakeListening(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setWakeListeningEnabled(enabled)
            if (enabled) {
                voiceManager.startListening(isWakeCheckMode = true)
            } else {
                voiceManager.stopListening()
            }
        }
    }

    fun startOrStopLockScreenService(enable: Boolean) {
        viewModelScope.launch {
            val s = settings.value
            settingsRepo.setLockScreenServiceRunning(enable)
            if (enable) {
                AssistantForegroundService.startService(
                    context = appContext,
                    assistantName = s.assistantName,
                    wakePhrase = s.wakePhrase,
                    showOnLockScreen = s.showNotificationOnLockScreen
                )
                showStatusBanner("Lock-screen assistant notification started for ${s.assistantName}.")
            } else {
                AssistantForegroundService.stopService(appContext)
                showStatusBanner("Lock-screen assistant service stopped.")
            }
        }
    }

    fun setLockScreenNotificationVisibility(showOnLockScreen: Boolean) {
        viewModelScope.launch {
            settingsRepo.setShowNotificationOnLockScreen(showOnLockScreen)
            val s = settings.value
            if (s.isLockScreenServiceRunning) {
                AssistantForegroundService.startService(
                    context = appContext,
                    assistantName = s.assistantName,
                    wakePhrase = s.wakePhrase,
                    showOnLockScreen = showOnLockScreen
                )
            }
        }
    }

    fun updateAiConfig(provider: String, modelName: String) {
        viewModelScope.launch {
            settingsRepo.updateAiConfig(provider, modelName)
            showStatusBanner("AI configuration updated ($provider • $modelName).")
        }
    }

    fun updatePrivacyAndBiometrics(
        askBeforeSavingMemory: Boolean? = null,
        requireBiometricMemory: Boolean? = null,
        requireBiometricAttendance: Boolean? = null,
        requireBiometricSettings: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsRepo.updatePrivacyAndBiometrics(
                askBeforeSavingMemory = askBeforeSavingMemory,
                requireBiometricMemory = requireBiometricMemory,
                requireBiometricAttendance = requireBiometricAttendance,
                requireBiometricSettings = requireBiometricSettings
            )
        }
    }

    // --- Chat Sessions & Messaging ---
    fun selectChatSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun createNewChatSession(title: String = "New Conversation") {
        viewModelScope.launch {
            val newId = repository.createNewSession(title)
            _currentSessionId.value = newId
            val name = settings.value.assistantName
            repository.addMessage(
                sessionId = newId,
                sender = "ASSISTANT",
                content = "Namaste! Main **$name** hoon. Nayi baat-cheet shuru karte hain—bataiye main aapki kya madad karoon?"
            )
        }
    }

    fun renameChatSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameSession(sessionId, newTitle)
        }
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            val remaining = repository.ensureSeededAndInitialSession()
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = remaining
            }
        }
    }

    fun clearAllChatHistorySeparately() {
        viewModelScope.launch {
            repository.clearAllChatHistoryOnly()
            val newId = repository.ensureSeededAndInitialSession()
            _currentSessionId.value = newId
            showStatusBanner("Chat history cleared. Personal Memory Vault and Attendance data remain untouched.")
        }
    }

    fun sendMessageOrCommand(
        rawText: String,
        fromVoiceInput: Boolean = false,
        statusNote: String = ""
    ) {
        val text = rawText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            val sessionId = if (_currentSessionId.value <= 0L) {
                repository.ensureSeededAndInitialSession().also { _currentSessionId.value = it }
            } else {
                _currentSessionId.value
            }

            val currentSettings = settings.value
            repository.addMessage(
                sessionId = sessionId,
                sender = "USER",
                content = "$statusNote$text"
            )

            // Detect if user shared personal information that could be saved to Memory Vault
            val candidateMemory = detectMemoryCandidate(text)

            // Step 1: Check deterministic Local Command Processor (works 100% offline)
            val todayAtt = repository.getTodayAttendance()
            val memories = repository.getAllMemoriesOnce()
            val cmdOutcome = localCommandProcessor.processInput(
                userInput = text,
                assistantName = currentSettings.assistantName,
                customCommands = customCommands.value,
                todayAttendance = todayAtt,
                memories = memories
            )

            when (cmdOutcome) {
                is CommandExecutionOutcome.ExecutedOffline -> {
                    if (cmdOutcome.enableTtsRequested) {
                        settingsRepo.updatePersonalization(speakResponsesByDefault = true)
                    }
                    repository.addMessage(
                        sessionId = sessionId,
                        sender = "ASSISTANT",
                        content = cmdOutcome.spokenAndChatReply,
                        isOfflineCommand = true,
                        proposedMemoryTitle = candidateMemory?.first,
                        proposedMemoryContent = candidateMemory?.second
                    )
                    if (currentSettings.speakResponsesByDefault || fromVoiceInput || cmdOutcome.enableTtsRequested) {
                        voiceManager.speak(cmdOutcome.spokenAndChatReply)
                    }
                    return@launch
                }

                is CommandExecutionOutcome.ConfirmationRequired -> {
                    _pendingSensitiveCommand.value = PendingSensitiveCommand(
                        title = cmdOutcome.commandTitle,
                        warningMessage = cmdOutcome.warningMessage,
                        pendingIntent = cmdOutcome.pendingIntent,
                        replyAfterConfirm = cmdOutcome.replyAfterConfirm
                    )
                    repository.addMessage(
                        sessionId = sessionId,
                        sender = "ASSISTANT",
                        content = "Confirmation Required: ${cmdOutcome.warningMessage}",
                        isOfflineCommand = true
                    )
                    return@launch
                }

                CommandExecutionOutcome.NotMatched -> {
                    // Proceed to AI generation if online and enabled
                }
            }

            // Step 2: Check if Offline-Only mode is selected or device has no internet
            if (currentSettings.aiProvider == "OFFLINE_ONLY") {
                val offlineReply = "${currentSettings.assistantName} abhi **Offline Local Command Mode** mein hai. Aap local commands chala sakte hain jaise *\"Time batao\"*, *\"Aaj ki attendance batao\"*, *\"YouTube kholo\"*, *\"Calculator kholo\"*, ya hisab jaise *\"450 * 12\"*. General AI सवालों ke liye Settings mein Firebase Gemini AI enable karein."
                repository.addMessage(
                    sessionId = sessionId,
                    sender = "ASSISTANT",
                    content = offlineReply,
                    isOfflineCommand = true,
                    proposedMemoryTitle = candidateMemory?.first,
                    proposedMemoryContent = candidateMemory?.second
                )
                if (currentSettings.speakResponsesByDefault || fromVoiceInput) {
                    voiceManager.speak(offlineReply)
                }
                return@launch
            }

            if (!isNetworkConnected()) {
                val netErr = "Internet connection uplabdh nahi hai. Main abhi sirf offline phone commands (*\"Time batao\"*, *\"Aaj ki attendance batao\"*, *\"Camera kholo\"*, math calculations) chala sakta hoon."
                _aiConnectionStatus.value = AiConnectionStatus.Error("No internet connection")
                repository.addMessage(
                    sessionId = sessionId,
                    sender = "ASSISTANT",
                    content = netErr,
                    isOfflineCommand = true
                )
                if (currentSettings.speakResponsesByDefault || fromVoiceInput) {
                    voiceManager.speak(netErr)
                }
                return@launch
            }

            // Step 3: Call Firebase AI Logic with streaming and 60s timeout
            _aiConnectionStatus.value = AiConnectionStatus.Generating
            _streamingPartialResponse.value = ""
            val accumulated = StringBuilder()

            val memoryContext = if (memories.isNotEmpty()) {
                "User's Saved Personal Memory Vault:\n" +
                    memories.take(8).joinToString("\n") { "- ${it.title}: ${it.content}" }
            } else {
                "No saved memory facts yet."
            }

            val languageInstruction = when (currentSettings.languageMode) {
                "HINDI" -> "Respond in natural Hindi (Devanagari or easy Hindi)."
                "ENGLISH" -> "Respond in clear, helpful English."
                else -> "Respond in natural Hinglish (conversational Hindi + English mix) or match the user's language."
            }

            val styleInstruction = when (currentSettings.responseStyle) {
                "CONCISE" -> "Keep your answer concise, direct, and easy to listen to."
                "DETAILED" -> "Provide a thorough, step-by-step explanation with examples."
                "CODING_EXPERT" -> "Focus on clean code, technical accuracy, and Android/Kotlin best practices."
                else -> "Be warm, respectful, and helpful."
            }

            val fullPrompt = """
                You are ${currentSettings.assistantName}, the user's personal native Android AI assistant.
                Never call yourself KAYRA unless the user explicitly named you KAYRA. Your current name is ${currentSettings.assistantName}.
                $languageInstruction
                $styleInstruction
                $memoryContext
                
                User message: $text
            """.trimIndent()

            try {
                val model = getGenerativeModel(currentSettings.aiModelName)
                withTimeout(60_000L) {
                    model.generateContentStream(fullPrompt).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        accumulated.append(part)
                        _streamingPartialResponse.value = accumulated.toString()
                    }
                }
                val finalAnswer = accumulated.toString().trim()
                _streamingPartialResponse.value = null

                if (finalAnswer.isNotBlank()) {
                    _aiConnectionStatus.value = AiConnectionStatus.Ready
                    // If user disabled "Ask before saving memory", auto-save detected memory
                    val shouldAskMemory = candidateMemory != null && currentSettings.askBeforeSavingMemory
                    if (candidateMemory != null && !currentSettings.askBeforeSavingMemory) {
                        repository.saveMemoryItem(candidateMemory.first, candidateMemory.second, "FACT")
                    }
                    repository.addMessage(
                        sessionId = sessionId,
                        sender = "ASSISTANT",
                        content = finalAnswer,
                        isOfflineCommand = false,
                        proposedMemoryTitle = if (shouldAskMemory) candidateMemory?.first else null,
                        proposedMemoryContent = if (shouldAskMemory) candidateMemory?.second else null
                    )
                    if (currentSettings.speakResponsesByDefault || fromVoiceInput) {
                        voiceManager.speak(finalAnswer)
                    }
                } else {
                    _aiConnectionStatus.value = AiConnectionStatus.Error("Model returned empty response.")
                    repository.addMessage(
                        sessionId = sessionId,
                        sender = "ASSISTANT",
                        content = "AI model ne khaali jawab bheja. Kripya dobara koshish karein."
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                _streamingPartialResponse.value = null
                val msg = "AI response timed out after 60 seconds. Check your internet connection or switch to gemini-3.5-flash-lite in Settings."
                _aiConnectionStatus.value = AiConnectionStatus.Error(msg)
                repository.addMessage(
                    sessionId = sessionId,
                    sender = "ASSISTANT",
                    content = msg
                )
            } catch (e: CancellationException) {
                _streamingPartialResponse.value = null
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI Logic generation failed", e)
                _streamingPartialResponse.value = null
                val errText = e.localizedMessage ?: "AI provider unavailable."
                _aiConnectionStatus.value = AiConnectionStatus.Error(errText)
                repository.addMessage(
                    sessionId = sessionId,
                    sender = "ASSISTANT",
                    content = "AI Provider Error: $errText\n\n(Note: Offline commands jaise *\"Time batao\"*, *\"Aaj ki attendance batao\"*, *\"YouTube kholo\"* abhi bhi kaam kar rahe hain.)"
                )
            }
        }
    }

    private fun detectMemoryCandidate(userText: String): Pair<String, String>? {
        val lower = userText.lowercase()
        return when {
            lower.startsWith("yaad rakhna ") || lower.startsWith("remember that ") || lower.startsWith("note kar lo ") -> {
                "Personal Note" to userText.trim()
            }
            lower.contains("mera naam ") || lower.contains("my name is ") || lower.contains("i like ") || lower.contains("mujhe ") && lower.contains(" pasand hai") -> {
                "User Preference / Fact" to userText.trim()
            }
            else -> null
        }
    }

    fun confirmPendingSensitiveCommand() {
        val pending = _pendingSensitiveCommand.value ?: return
        _pendingSensitiveCommand.value = null
        viewModelScope.launch {
            try {
                appContext.startActivity(pending.pendingIntent)
                val sid = _currentSessionId.value
                if (sid > 0L) {
                    repository.addMessage(
                        sessionId = sid,
                        sender = "ASSISTANT",
                        content = pending.replyAfterConfirm,
                        isOfflineCommand = true
                    )
                }
            } catch (e: Exception) {
                showStatusBanner("Could not launch action: ${e.localizedMessage}")
            }
        }
    }

    fun dismissPendingSensitiveCommand() {
        _pendingSensitiveCommand.value = null
    }

    fun approveMemoryFromChat(message: ChatMessageEntity) {
        viewModelScope.launch {
            repository.approveProposedMemory(message)
            showStatusBanner("Saved to Personal Memory Vault!")
        }
    }

    // --- Custom Commands CRUD ---
    fun saveCustomCommand(command: CustomCommandEntity) {
        viewModelScope.launch {
            repository.saveCustomCommand(command)
            showStatusBanner("Custom command '${command.title}' saved.")
        }
    }

    fun toggleCustomCommand(command: CustomCommandEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleCustomCommand(command, enabled)
        }
    }

    fun deleteCustomCommand(command: CustomCommandEntity) {
        viewModelScope.launch {
            repository.deleteCustomCommand(command)
            showStatusBanner("Deleted command '${command.title}'.")
        }
    }

    // --- Memory Vault CRUD ---
    fun saveMemoryItem(title: String, content: String, category: String, id: Long = 0L) {
        viewModelScope.launch {
            repository.saveMemoryItem(title, content, category, id)
            showStatusBanner("Memory saved locally.")
        }
    }

    fun deleteMemoryItem(item: MemoryItemEntity) {
        viewModelScope.launch {
            repository.deleteMemoryItem(item)
            showStatusBanner("Memory deleted.")
        }
    }

    // --- PagarBook Attendance & Salary Operations ---
    fun selectYearMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
    }

    fun clockInToday(biometricVerified: Boolean, notes: String = "") {
        viewModelScope.launch {
            val rec = repository.clockInToday(biometricVerified, notes)
            val badge = if (biometricVerified) " (Biometric Verified)" else ""
            showStatusBanner("Clocked IN for ${rec.dateString}$badge")
        }
    }

    fun clockOutToday(biometricVerified: Boolean) {
        viewModelScope.launch {
            val rec = repository.clockOutToday(
                standardWorkHoursPerDay = settings.value.standardWorkHoursPerDay,
                biometricVerified = biometricVerified
            )
            val hrs = rec.workedMinutes / 60
            val mins = rec.workedMinutes % 60
            showStatusBanner("Clocked OUT (${hrs}h ${mins}m worked, Overtime: ${rec.overtimeMinutes}m)")
        }
    }

    fun markAttendanceStatus(
        dateString: String,
        status: String,
        workedMinutes: Int,
        overtimeMinutes: Int,
        notes: String,
        biometricVerified: Boolean
    ) {
        viewModelScope.launch {
            repository.markAttendanceStatus(
                dateString = dateString,
                status = status,
                workedMinutes = workedMinutes,
                overtimeMinutes = overtimeMinutes,
                notes = notes,
                biometricVerified = biometricVerified
            )
            showStatusBanner("Attendance for $dateString marked as $status.")
        }
    }

    fun addSalaryAdjustment(type: String, amount: Double, description: String) {
        viewModelScope.launch {
            repository.addSalaryAdjustment(type, amount, description)
            showStatusBanner("Added $type: ₹$amount")
        }
    }

    fun deleteSalaryAdjustment(adjustment: SalaryAdjustmentEntity) {
        viewModelScope.launch {
            repository.deleteSalaryAdjustment(adjustment)
        }
    }

    fun updateSalaryConfig(
        employeeName: String,
        employeeRole: String,
        monthlyBaseSalary: Double,
        standardWorkHoursPerDay: Double,
        hourlyOvertimeRate: Double,
        weeklyHolidayDayOfWeek: Int
    ) {
        viewModelScope.launch {
            settingsRepo.updateSalaryConfig(
                employeeName = employeeName,
                employeeRole = employeeRole,
                monthlyBaseSalary = monthlyBaseSalary,
                standardWorkHoursPerDay = standardWorkHoursPerDay,
                hourlyOvertimeRate = hourlyOvertimeRate,
                weeklyHolidayDayOfWeek = weeklyHolidayDayOfWeek
            )
            showStatusBanner("Salary & holiday settings saved.")
        }
    }

    // --- Backup & Export Helpers ---
    fun generateBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportFullBackupJson(settings.value)
            onResult(json)
        }
    }

    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val count = repository.importBackupJson(jsonString)
                showStatusBanner("Restored $count records from JSON backup!")
            } catch (e: Exception) {
                showStatusBanner("Invalid backup JSON: ${e.localizedMessage}")
            }
        }
    }

    fun generateAttendanceCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.exportAttendanceCsv()
            onResult(csv)
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.wipeAllUserData()
            val sid = repository.ensureSeededAndInitialSession()
            _currentSessionId.value = sid
            showStatusBanner("All local records wiped cleanly.")
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
