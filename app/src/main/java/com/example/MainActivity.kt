package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.configureAppCheck
import com.example.service.AssistantForegroundService
import com.example.ui.AssistantViewModel
import com.example.ui.screens.AttendanceAndSalaryScreen
import com.example.ui.screens.ChatAndVoiceScreen
import com.example.ui.screens.CommandsAndWakeScreen
import com.example.ui.screens.FirstLaunchOnboardingScreen
import com.example.ui.screens.GitHubAndProjectSaveSection
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.SettingsAndProjectScreen
import com.example.ui.theme.PersonalAssistantTheme

enum class AssistantTab(val route: String, val label: String) {
    CHAT("chat", "Assistant"),
    COMMANDS("commands", "Commands"),
    MEMORY("memory", "Memory"),
    ATTENDANCE("attendance", "Attendance"),
    SETTINGS("settings", "GitHub/Settings")
}

class MainActivity : FragmentActivity() {

    private var launchVoiceRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE any UI or ViewModels
        configureAppCheck(this, intent)

        handleIncomingAssistantIntent(intent)
        enableEdgeToEdge()

        setContent {
            val viewModel: AssistantViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            PersonalAssistantTheme(
                themePreset = settings.themePreset,
                useDynamicColors = settings.useDynamicColors
            ) {
                PersonalAssistantApp(
                    viewModel = viewModel,
                    launchVoiceRequested = launchVoiceRequested,
                    onConsumeLaunchVoiceRequest = { launchVoiceRequested = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingAssistantIntent(intent)
    }

    private fun handleIncomingAssistantIntent(intent: Intent?) {
        if (intent == null) return
        val fromNotification = intent.getBooleanExtra(
            AssistantForegroundService.EXTRA_TRIGGER_VOICE_ON_LAUNCH,
            false
        )
        val isAssistAction = intent.action == Intent.ACTION_ASSIST ||
            intent.action == Intent.ACTION_VOICE_COMMAND
        if (fromNotification || isAssistAction) {
            launchVoiceRequested = true
        }
    }
}

@Composable
fun PersonalAssistantApp(
    viewModel: AssistantViewModel,
    launchVoiceRequested: Boolean,
    onConsumeLaunchVoiceRequest: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessions by viewModel.chatSessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val streamingPartial by viewModel.streamingPartialResponse.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val aiStatus by viewModel.aiConnectionStatus.collectAsStateWithLifecycle()
    val pendingSensitive by viewModel.pendingSensitiveCommand.collectAsStateWithLifecycle()
    val customCommands by viewModel.customCommands.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val memories by viewModel.memoryItems.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val salaryAdjustments by viewModel.salaryAdjustments.collectAsStateWithLifecycle()
    val monthlySalarySummary by viewModel.monthlySalarySummary.collectAsStateWithLifecycle()
    val verifiedZipInfo by viewModel.verifiedZipInfo.collectAsStateWithLifecycle()
    val githubState by viewModel.githubConnectionState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val memoryUnlocked by viewModel.memoryVaultUnlockedInSession.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(AssistantTab.CHAT) }
    var showQuickRenameAssistantDialog by remember { mutableStateOf(false) }
    var showGitHubAndZipModal by remember { mutableStateOf(false) }

    // Handle system back button when on secondary tabs
    BackHandler(enabled = currentTab != AssistantTab.CHAT) {
        currentTab = AssistantTab.CHAT
    }

    LaunchedEffect(launchVoiceRequested) {
        if (launchVoiceRequested) {
            currentTab = AssistantTab.CHAT
            viewModel.voiceManager.startListening(isWakeCheckMode = false)
            onConsumeLaunchVoiceRequest()
        }
    }

    // First-Launch Step 1 (Save Project Options: GitHub / ZIP / Continue) + Step 2 (Assistant Name Setup)
    if (!settings.hasCompletedOnboarding) {
        FirstLaunchOnboardingScreen(
            settings = settings,
            verifiedZipInfo = verifiedZipInfo,
            githubState = githubState,
            onFetchGitHubRepos = viewModel::fetchGitHubReposForUser,
            onVerifyGitHubRepo = viewModel::verifyAndConnectGitHubRepo,
            onVerifyAndGenerateZip = { viewModel.prepareVerifiedProjectZip() },
            onCompleteSetup = { name, saveChoice, lang, theme, repoUrl ->
                viewModel.completeOnboarding(name, saveChoice, lang, theme)
                if (repoUrl.isNotBlank()) {
                    viewModel.updateProjectSaveOption(saveChoice, repoUrl)
                }
            }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            // Persistent Top Bar with one-tap "GitHub & Source ZIP" button visible on every screen
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${settings.assistantName} • Personal AI",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (settings.githubRepoUrl.isNotBlank()) {
                                "GitHub: ${settings.githubRepoUrl}"
                            } else {
                                "Save Mode: ${settings.projectSaveChoice} • ZIP Verified"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    FilledTonalButton(
                        onClick = { showGitHubAndZipModal = true },
                        modifier = Modifier.testTag("top_github_zip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Connect GitHub or Download Source ZIP",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("GitHub & ZIP")
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == AssistantTab.CHAT,
                    onClick = { currentTab = AssistantTab.CHAT },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "Assistant Chat & Voice") },
                    label = { Text(settings.assistantName.take(10)) },
                    modifier = Modifier.testTag("nav_tab_chat")
                )
                NavigationBarItem(
                    selected = currentTab == AssistantTab.COMMANDS,
                    onClick = { currentTab = AssistantTab.COMMANDS },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Commands & Wake") },
                    label = { Text("Commands") },
                    modifier = Modifier.testTag("nav_tab_commands")
                )
                NavigationBarItem(
                    selected = currentTab == AssistantTab.MEMORY,
                    onClick = { currentTab = AssistantTab.MEMORY },
                    icon = { Icon(Icons.Default.Memory, contentDescription = "Personal Memory Vault") },
                    label = { Text("Memory") },
                    modifier = Modifier.testTag("nav_tab_memory")
                )
                NavigationBarItem(
                    selected = currentTab == AssistantTab.ATTENDANCE,
                    onClick = { currentTab = AssistantTab.ATTENDANCE },
                    icon = { Icon(Icons.Default.Badge, contentDescription = "PagarBook Attendance & Salary") },
                    label = { Text("Attendance") },
                    modifier = Modifier.testTag("nav_tab_attendance")
                )
                NavigationBarItem(
                    selected = currentTab == AssistantTab.SETTINGS,
                    onClick = { currentTab = AssistantTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "GitHub, ZIP & Settings") },
                    label = { Text("GitHub/Setup") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = statusBanner != null) {
                statusBanner?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.showStatusBanner(null) }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss notification")
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    AssistantTab.CHAT -> {
                        ChatAndVoiceScreen(
                            settings = settings,
                            sessions = sessions,
                            currentSessionId = currentSessionId,
                            messages = messages,
                            streamingPartialResponse = streamingPartial,
                            voiceState = voiceState,
                            aiConnectionStatus = aiStatus,
                            pendingSensitiveCommand = pendingSensitive,
                            onSelectSession = viewModel::selectChatSession,
                            onCreateSession = viewModel::createNewChatSession,
                            onRenameSession = viewModel::renameChatSession,
                            onDeleteSession = viewModel::deleteChatSession,
                            onClearAllChatsOnly = viewModel::clearAllChatHistorySeparately,
                            onSendMessage = { viewModel.sendMessageOrCommand(it, fromVoiceInput = false) },
                            onStartVoiceInput = { viewModel.voiceManager.startListening(isWakeCheckMode = false) },
                            onStopVoiceInput = { viewModel.voiceManager.stopListening() },
                            onSpeakMessage = { viewModel.voiceManager.speak(it) },
                            onStopSpeaking = { viewModel.voiceManager.stopSpeaking() },
                            onApproveMemory = viewModel::approveMemoryFromChat,
                            onConfirmSensitiveCommand = viewModel::confirmPendingSensitiveCommand,
                            onDismissSensitiveCommand = viewModel::dismissPendingSensitiveCommand,
                            onOpenPersonalizationDialog = { showQuickRenameAssistantDialog = true }
                        )
                    }

                    AssistantTab.COMMANDS -> {
                        CommandsAndWakeScreen(
                            settings = settings,
                            voiceState = voiceState,
                            wakeEngine = viewModel.voiceManager.wakePhraseEngine,
                            customCommands = customCommands,
                            installedApps = installedApps,
                            commandProcessor = viewModel.localCommandProcessor,
                            onUpdateWakePhrase = { viewModel.updateWakePhrase(it, isCustom = true) },
                            onToggleWakeListening = viewModel::toggleWakeListening,
                            onStopVoiceListening = { viewModel.voiceManager.stopListening() },
                            onToggleLockScreenService = viewModel::startOrStopLockScreenService,
                            onToggleLockScreenVisibility = viewModel::setLockScreenNotificationVisibility,
                            onSaveCommand = viewModel::saveCustomCommand,
                            onToggleCommandEnabled = viewModel::toggleCustomCommand,
                            onDeleteCommand = viewModel::deleteCustomCommand,
                            onTestRunCommand = { phrase ->
                                currentTab = AssistantTab.CHAT
                                viewModel.sendMessageOrCommand(phrase, fromVoiceInput = false)
                            }
                        )
                    }

                    AssistantTab.MEMORY -> {
                        MemoryVaultScreen(
                            settings = settings,
                            memories = memories,
                            isUnlockedInSession = memoryUnlocked,
                            onUnlockSuccess = viewModel::markMemoryVaultUnlocked,
                            onToggleAskBeforeSaving = {
                                viewModel.updatePrivacyAndBiometrics(askBeforeSavingMemory = it)
                            },
                            onToggleRequireBiometricForMemory = {
                                viewModel.updatePrivacyAndBiometrics(requireBiometricMemory = it)
                            },
                            onSaveMemory = viewModel::saveMemoryItem,
                            onDeleteMemory = viewModel::deleteMemoryItem,
                            onClearChatHistorySeparately = viewModel::clearAllChatHistorySeparately,
                            onStatusMessage = viewModel::showStatusBanner
                        )
                    }

                    AssistantTab.ATTENDANCE -> {
                        AttendanceAndSalaryScreen(
                            settings = settings,
                            attendanceRecords = attendanceRecords,
                            salaryAdjustments = salaryAdjustments,
                            monthlySalarySummary = monthlySalarySummary,
                            onClockInToday = { verified -> viewModel.clockInToday(verified) },
                            onClockOutToday = { verified -> viewModel.clockOutToday(verified) },
                            onMarkCustomAttendance = viewModel::markAttendanceStatus,
                            onAddSalaryAdjustment = viewModel::addSalaryAdjustment,
                            onDeleteSalaryAdjustment = viewModel::deleteSalaryAdjustment,
                            onUpdateSalaryConfig = viewModel::updateSalaryConfig,
                            onToggleRequireBiometricForAttendance = {
                                viewModel.updatePrivacyAndBiometrics(requireBiometricAttendance = it)
                            },
                            onExportCsv = viewModel::generateAttendanceCsv,
                            onExportJsonBackup = viewModel::generateBackupJson,
                            onImportJsonBackup = viewModel::importBackupJson,
                            onStatusMessage = viewModel::showStatusBanner
                        )
                    }

                    AssistantTab.SETTINGS -> {
                        SettingsAndProjectScreen(
                            settings = settings,
                            aiConnectionStatus = aiStatus,
                            verifiedZipInfo = verifiedZipInfo,
                            githubState = githubState,
                            onFetchGitHubRepos = viewModel::fetchGitHubReposForUser,
                            onVerifyGitHubRepo = viewModel::verifyAndConnectGitHubRepo,
                            onUpdateAssistantName = viewModel::updateAssistantName,
                            onUpdatePersonalization = viewModel::updatePersonalization,
                            onTestVoice = { viewModel.voiceManager.speak(it) },
                            onUpdateAiConfig = viewModel::updateAiConfig,
                            onUpdateProjectSaveOption = viewModel::updateProjectSaveOption,
                            onVerifyAndGenerateZip = { viewModel.prepareVerifiedProjectZip() },
                            onClearChatHistoryOnly = viewModel::clearAllChatHistorySeparately,
                            onWipeAllData = viewModel::wipeAllData,
                            onStatusMessage = viewModel::showStatusBanner
                        )
                    }
                }
            }
        }
    }

    // One-Tap GitHub & Source ZIP Modal Accessible From Top Bar on Any Tab
    if (showGitHubAndZipModal) {
        var modalSaveChoice by remember(settings.projectSaveChoice) {
            mutableStateOf(
                if (settings.projectSaveChoice == "UNSELECTED") "GITHUB" else settings.projectSaveChoice
            )
        }
        var modalRepoInput by remember(settings.githubRepoUrl) {
            mutableStateOf(settings.githubRepoUrl)
        }

        AlertDialog(
            onDismissRequest = { showGitHubAndZipModal = false },
            title = { Text("GitHub Connection & Source Code ZIP") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    GitHubAndProjectSaveSection(
                        selectedSaveChoice = modalSaveChoice,
                        onSelectSaveChoice = { choice ->
                            modalSaveChoice = choice
                            viewModel.updateProjectSaveOption(choice, modalRepoInput)
                        },
                        githubRepoInput = modalRepoInput,
                        onGitHubRepoInputChange = { modalRepoInput = it },
                        githubState = githubState,
                        verifiedZipInfo = verifiedZipInfo,
                        onFetchGitHubRepos = viewModel::fetchGitHubReposForUser,
                        onVerifyGitHubRepo = viewModel::verifyAndConnectGitHubRepo,
                        onVerifyAndGenerateZip = { viewModel.prepareVerifiedProjectZip() }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProjectSaveOption(modalSaveChoice, modalRepoInput)
                        showGitHubAndZipModal = false
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (showQuickRenameAssistantDialog) {
        var newName by remember(settings.assistantName) { mutableStateOf(settings.assistantName) }
        AlertDialog(
            onDismissRequest = { showQuickRenameAssistantDialog = false },
            title = { Text("Change Assistant Name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter any Hindi, Hinglish, or English name. Chat history, memories, and attendance data will be preserved.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Assistant Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateAssistantName(newName)
                    showQuickRenameAssistantDialog = false
                }) {
                    Text("Save Name")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickRenameAssistantDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
