package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AssistantSettings
import com.example.data.repository.VerifiedZipInfo
import com.example.domain.GitHubConnectionState
import com.example.domain.GitHubSyncManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstLaunchOnboardingScreen(
    settings: AssistantSettings,
    verifiedZipInfo: VerifiedZipInfo?,
    githubState: GitHubConnectionState,
    onFetchGitHubRepos: (String) -> Unit,
    onVerifyGitHubRepo: (String, Boolean) -> Unit,
    onVerifyAndGenerateZip: () -> Unit,
    onCompleteSetup: (
        assistantName: String,
        projectSaveChoice: String,
        languageMode: String,
        themePreset: String,
        githubRepoUrl: String
    ) -> Unit
) {
    var selectedSaveChoice by remember {
        mutableStateOf(
            if (settings.projectSaveChoice == "UNSELECTED") "GITHUB" else settings.projectSaveChoice
        )
    }
    var assistantNameInput by remember { mutableStateOf(settings.assistantName) }
    var selectedLanguage by remember { mutableStateOf(settings.languageMode) }
    var selectedTheme by remember { mutableStateOf(settings.themePreset) }
    var githubRepoInput by remember(settings.githubRepoUrl) { mutableStateOf(settings.githubRepoUrl) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "AI Assistant Setup",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column {
                            Text(
                                text = "MY PERSONAL AI ASSISTANT",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "GitHub, Source ZIP & Identity Setup",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = "Step 1: Choose how to save your project (Connect GitHub, Download Source Code ZIP, or Continue Without Connecting). Step 2: Name your personal AI assistant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                    )
                }
            }

            // STEP 1: HOW TO SAVE PROJECT (Reusable interactive component)
            GitHubAndProjectSaveSection(
                selectedSaveChoice = selectedSaveChoice,
                onSelectSaveChoice = { selectedSaveChoice = it },
                githubRepoInput = githubRepoInput,
                onGitHubRepoInputChange = { githubRepoInput = it },
                githubState = githubState,
                verifiedZipInfo = verifiedZipInfo,
                onFetchGitHubRepos = onFetchGitHubRepos,
                onVerifyGitHubRepo = onVerifyGitHubRepo,
                onVerifyAndGenerateZip = onVerifyAndGenerateZip
            )

            // STEP 2: ASSISTANT IDENTITY & PERSONALIZATION
            Text(
                text = "STEP 2: Choose Your Assistant's Name & Style",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter any Hindi, Hinglish, or English name. You can change this name whenever you want in Settings without losing chat history or attendance data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = assistantNameInput,
                        onValueChange = { assistantNameInput = it },
                        label = { Text("Assistant Name (Hindi or English)") },
                        placeholder = { Text("e.g., Aarya, Raja, Veer, Nova, आरव") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_assistant_name_input")
                    )

                    Text(
                        text = "Quick Name Suggestions:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    val presetNames = listOf("Aarya", "Raja", "Veer", "Nova", "Jarvis", "Kavi")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetNames.forEach { preset ->
                            AssistChip(
                                onClick = { assistantNameInput = preset },
                                label = { Text(preset) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (assistantNameInput.equals(preset, ignoreCase = true)) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            )
                        }
                    }

                    val previewName = assistantNameInput.trim().ifBlank { "Aarya" }
                    Text(
                        text = "Default Wake Phrase will be: \"Hey $previewName\"",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Language Selection
                    Text(
                        text = "Preferred Conversation Language:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "HINGLISH" to "Hinglish (Hindi+Eng)",
                            "HINDI" to "Hindi (हिंदी)",
                            "ENGLISH" to "English"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = selectedLanguage == code,
                                onClick = { selectedLanguage = code },
                                label = { Text(label) }
                            )
                        }
                    }

                    // Theme Selection
                    Text(
                        text = "Visual Theme:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "CYBER_INDIGO" to "Cyber Indigo",
                            "SAFFRON_OBSIDIAN" to "Saffron Dark",
                            "DAYLIGHT_STUDIO" to "Daylight"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = selectedTheme == code,
                                onClick = { selectedTheme = code },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Security & Privacy Pledge Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Privacy Guarantee",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Privacy First: Personal Memory Vault and PagarBook Attendance/Salary records are stored locally on your device using Room SQLite. No secret audio recording.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    onCompleteSetup(
                        assistantNameInput.trim().ifBlank { "Aarya" },
                        selectedSaveChoice,
                        selectedLanguage,
                        selectedTheme,
                        githubRepoInput
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("launch_assistant_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Save & Open ${assistantNameInput.trim().ifBlank { "Aarya" }} Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GitHubAndProjectSaveSection(
    selectedSaveChoice: String,
    onSelectSaveChoice: (String) -> Unit,
    githubRepoInput: String,
    onGitHubRepoInputChange: (String) -> Unit,
    githubState: GitHubConnectionState,
    verifiedZipInfo: VerifiedZipInfo?,
    onFetchGitHubRepos: (String) -> Unit,
    onVerifyGitHubRepo: (String, Boolean) -> Unit,
    onVerifyAndGenerateZip: () -> Unit
) {
    val context = LocalContext.current
    var githubUsernameInput by remember {
        mutableStateOf(
            githubRepoInput.substringBefore("/", "").ifBlank { "amit963813" }
        )
    }
    var newRepoNameInput by remember { mutableStateOf("my-personal-ai-assistant") }
    var preferPrivateRepo by remember { mutableStateOf(true) }
    var zipSaveStatus by remember { mutableStateOf<String?>(null) }

    val saveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null && verifiedZipInfo != null && verifiedZipInfo.file.exists()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    verifiedZipInfo.file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                zipSaveStatus = "Complete Source ZIP saved (${verifiedZipInfo.sizeBytes / 1024} KB) to your phone!"
            } catch (e: Exception) {
                zipSaveStatus = "Save error: ${e.localizedMessage}"
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "STEP 1: How Do You Want to Save Your Project?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // OPTION 1: CONNECT GITHUB
        SaveOptionCard(
            title = "1. Connect GitHub (Create or Select Repository)",
            subtitle = "Connect via official GitHub browser flow or AI Studio's top-bar GitHub button. Select an existing repo or create a new Private repository.",
            icon = Icons.Default.CloudSync,
            isSelected = selectedSaveChoice == "GITHUB",
            testTag = "save_option_github",
            onClick = { onSelectSaveChoice("GITHUB") }
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Repository Visibility Selector (Private Preferred per Step 1)
                Text(
                    text = "Repository Visibility (Explained Before Creation):",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferPrivateRepo,
                        onClick = { preferPrivateRepo = true },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Private Repo (Recommended)") }
                    )
                    FilterChip(
                        selected = !preferPrivateRepo,
                        onClick = { preferPrivateRepo = false },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Public Repo") }
                    )
                }
                Text(
                    text = if (preferPrivateRepo) {
                        "Private Repository: Only you can view or clone your personal AI assistant code on GitHub."
                    } else {
                        "Public Repository: Anyone on GitHub can view your project source code (never include secrets)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                // A: Create a New Repository via Official GitHub Web Session
                OutlinedTextField(
                    value = newRepoNameInput,
                    onValueChange = { newRepoNameInput = it },
                    label = { Text("New Repository Name to Create") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val targetFull = if (githubUsernameInput.isNotBlank()) {
                            "${githubUsernameInput.trim()}/${newRepoNameInput.trim()}"
                        } else {
                            newRepoNameInput.trim()
                        }
                        onGitHubRepoInputChange(targetFull)
                        GitHubSyncManager.openOfficialGitHubCreateRepo(
                            context = context,
                            suggestedRepoName = newRepoNameInput.trim().ifBlank { "my-personal-ai-assistant" },
                            isPrivate = preferPrivateRepo
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_github_repo_browser_button")
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Authorize & Create ${if (preferPrivateRepo) "Private" else "Public"} Repo on GitHub")
                }

                // B: Load Existing Repositories from GitHub
                Text(
                    text = "Or Select an Existing Repository from Your GitHub Account:",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = githubUsernameInput,
                        onValueChange = { githubUsernameInput = it },
                        label = { Text("Your GitHub Username") },
                        placeholder = { Text("e.g. amit963813") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("github_username_input")
                    )
                    OutlinedButton(
                        onClick = { onFetchGitHubRepos(githubUsernameInput) },
                        modifier = Modifier.testTag("fetch_github_repos_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Load Repos")
                    }
                }

                // Selected Repository Input + Verify Connection Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = githubRepoInput,
                        onValueChange = onGitHubRepoInputChange,
                        label = { Text("Selected Repository (owner/repo)") },
                        placeholder = { Text("amit963813/my-personal-ai-assistant") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("github_repo_input")
                    )
                    Button(
                        onClick = {
                            onVerifyGitHubRepo(githubRepoInput, preferPrivateRepo)
                        },
                        enabled = githubRepoInput.isNotBlank(),
                        modifier = Modifier.testTag("verify_github_repo_button")
                    ) {
                        Text("Connect")
                    }
                }

                // Live GitHub Connection Status Display
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (githubState) {
                            is GitHubConnectionState.NotConnected -> {
                                Text(
                                    text = if (githubRepoInput.isBlank()) {
                                        "Connection Status: Not Connected. Enter your GitHub username above to load existing repositories or create a new Private repository."
                                    } else {
                                        "Selected Repository: $githubRepoInput — Tap 'Connect' to verify status."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            is GitHubConnectionState.Checking -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("Contacting official GitHub API (api.github.com)...")
                                }
                            }
                            is GitHubConnectionState.ReposLoaded -> {
                                Text(
                                    text = "Found ${githubState.repos.size} repositories for @${githubState.username} (Tap one to select):",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (githubState.repos.isEmpty()) {
                                    Text(
                                        text = "No public repositories found for @${githubState.username}. You can create a new Private repository using the button above.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        githubState.repos.forEach { repo ->
                                            FilterChip(
                                                selected = githubRepoInput.equals(repo.fullName, ignoreCase = true),
                                                onClick = {
                                                    onGitHubRepoInputChange(repo.fullName)
                                                    onVerifyGitHubRepo(repo.fullName, repo.isPrivate)
                                                },
                                                label = { Text(repo.name) }
                                            )
                                        }
                                    }
                                }
                            }
                            is GitHubConnectionState.RepoVerified -> {
                                Text(
                                    text = "CONNECTED & VERIFIED: ${githubState.repo.fullName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = githubState.statusNote,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = {
                                        GitHubSyncManager.openGitHubRepoInBrowser(context, githubState.repo.fullName)
                                    }
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open ${githubState.repo.fullName} on GitHub")
                                }
                            }
                            is GitHubConnectionState.PrivateRepoSelected -> {
                                Text(
                                    text = "PRIVATE REPO CONFIGURED: ${githubState.fullName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = githubState.explanation,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = {
                                        GitHubSyncManager.openGitHubRepoInBrowser(context, githubState.fullName)
                                    }
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open ${githubState.fullName} on GitHub")
                                }
                            }
                            is GitHubConnectionState.Error -> {
                                Text(
                                    text = "GitHub Status: ${githubState.message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Text(
                            text = "Security Guarantee: We never ask you to paste your GitHub password, personal access token, or secret credentials into chat or source code. To push directly from AI Studio's cloud workspace, use the GitHub icon in the AI Studio top bar, or download the Complete Source ZIP below.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // OPTION 2: DOWNLOAD SOURCE CODE (ZIP)
        SaveOptionCard(
            title = "2. Download Complete Source Code (Verified ZIP)",
            subtitle = "Download the complete Android project, manifest, Gradle config, Room database code, README, and data as a verified ZIP file.",
            icon = Icons.Default.FolderZip,
            isSelected = selectedSaveChoice == "ZIP_DOWNLOAD",
            testTag = "save_option_zip",
            onClick = {
                onSelectSaveChoice("ZIP_DOWNLOAD")
                if (verifiedZipInfo == null) {
                    onVerifyAndGenerateZip()
                }
            }
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "• Includes: AndroidManifest.xml, build.gradle.kts, settings.gradle.kts, Room Database schema, README.md with mobile build instructions, CHANGELOG.md, and JSON/CSV backups.\n• Excludes: Passwords, API secrets, private keys, and local credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onVerifyAndGenerateZip,
                        modifier = Modifier.testTag("verify_zip_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Re-Verify ZIP")
                    }

                    if (verifiedZipInfo != null && verifiedZipInfo.isValid) {
                        Button(
                            onClick = {
                                saveZipLauncher.launch("personal_ai_assistant_complete_source.zip")
                            },
                            modifier = Modifier.testTag("download_verified_zip_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download Source ZIP (${verifiedZipInfo.sizeBytes / 1024} KB)")
                        }
                    }
                }
                if (verifiedZipInfo != null) {
                    Text(
                        text = if (verifiedZipInfo.isValid) {
                            "Verified Valid ZIP: ${verifiedZipInfo.file.name} (${verifiedZipInfo.entryCount} files, ${verifiedZipInfo.sizeBytes} bytes)."
                        } else {
                            "ZIP verification failed."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                zipSaveStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // OPTION 3: CONTINUE WITHOUT CONNECTING
        SaveOptionCard(
            title = "3. Continue Without Connecting",
            subtitle = "Build and preview the live Android assistant immediately without GitHub. You can open this GitHub & ZIP dialog anytime from the top bar.",
            icon = Icons.Default.PlayArrow,
            isSelected = selectedSaveChoice == "CONTINUE_LOCAL",
            testTag = "save_option_continue",
            onClick = { onSelectSaveChoice("CONTINUE_LOCAL") }
        ) {}
    }
}

@Composable
private fun SaveOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                expandedContent()
            }
        }
    }
}
