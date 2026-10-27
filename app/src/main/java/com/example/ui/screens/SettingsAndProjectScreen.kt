package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AssistantSettings
import com.example.data.repository.VerifiedZipInfo
import com.example.ui.AiConnectionStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndProjectScreen(
    settings: AssistantSettings,
    aiConnectionStatus: AiConnectionStatus,
    verifiedZipInfo: VerifiedZipInfo?,
    githubState: com.example.domain.GitHubConnectionState,
    onFetchGitHubRepos: (String) -> Unit,
    onVerifyGitHubRepo: (String, Boolean) -> Unit,
    onUpdateAssistantName: (String) -> Unit,
    onUpdatePersonalization: (
        languageMode: String?,
        responseStyle: String?,
        themePreset: String?,
        useDynamicColors: Boolean?,
        voiceSpeed: Float?,
        voicePitch: Float?,
        speakResponsesByDefault: Boolean?
    ) -> Unit,
    onTestVoice: (String) -> Unit,
    onUpdateAiConfig: (provider: String, modelName: String) -> Unit,
    onUpdateProjectSaveOption: (choice: String, repoUrl: String) -> Unit,
    onVerifyAndGenerateZip: () -> Unit,
    onClearChatHistoryOnly: () -> Unit,
    onWipeAllData: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember(settings.assistantName) { mutableStateOf(settings.assistantName) }
    var speedSlider by remember(settings.voiceSpeed) { mutableFloatStateOf(settings.voiceSpeed) }
    var pitchSlider by remember(settings.voicePitch) { mutableFloatStateOf(settings.voicePitch) }
    var githubRepoInput by remember(settings.githubRepoUrl) { mutableStateOf(settings.githubRepoUrl) }
    var selectedSaveChoice by remember(settings.projectSaveChoice) {
        mutableStateOf(
            if (settings.projectSaveChoice == "UNSELECTED") "GITHUB" else settings.projectSaveChoice
        )
    }
    var showWipeConfirm by remember { mutableStateOf(false) }

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
                onStatusMessage("Project Bundle ZIP saved (${verifiedZipInfo.sizeBytes / 1024} KB)!")
            } catch (e: Exception) {
                onStatusMessage("ZIP save failed: ${e.localizedMessage}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. GitHub Connection & Complete Source ZIP Export (Step 1 & Step 12)
        item {
            GitHubAndProjectSaveSection(
                selectedSaveChoice = selectedSaveChoice,
                onSelectSaveChoice = { choice ->
                    selectedSaveChoice = choice
                    onUpdateProjectSaveOption(choice, githubRepoInput)
                },
                githubRepoInput = githubRepoInput,
                onGitHubRepoInputChange = { githubRepoInput = it },
                githubState = githubState,
                verifiedZipInfo = verifiedZipInfo,
                onFetchGitHubRepos = onFetchGitHubRepos,
                onVerifyGitHubRepo = onVerifyGitHubRepo,
                onVerifyAndGenerateZip = onVerifyAndGenerateZip
            )
        }

        // 1. Settings > Personalization > Assistant Name (Step 2)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Settings > Personalization > Assistant Name",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "Change your assistant's name anytime (supports Hindi, Hinglish & English). Renaming updates greetings, voice responses, and wake phrases without deleting chat history or attendance data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Assistant Name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_assistant_name_input")
                        )
                        Button(
                            onClick = { onUpdateAssistantName(nameInput) },
                            modifier = Modifier.testTag("settings_save_assistant_name_button")
                        ) {
                            Text("Save Name")
                        }
                    }
                }
            }
        }

        // 2. Theme, Language, Response Style & Voice Speed
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Theme, Language, Voice & Style", style = MaterialTheme.typography.titleMedium)
                    }

                    Text("Theme Preset:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "CYBER_INDIGO" to "Cyber Indigo",
                            "SAFFRON_OBSIDIAN" to "Saffron Dark",
                            "DAYLIGHT_STUDIO" to "Daylight Studio"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = settings.themePreset == code,
                                onClick = { onUpdatePersonalization(null, null, code, null, null, null, null) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Material You Dynamic Wallpaper Colors (Android 12+)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.useDynamicColors,
                            onCheckedChange = { onUpdatePersonalization(null, null, null, it, null, null, null) }
                        )
                    }

                    Text("Language Mode:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "HINGLISH" to "Hinglish (Hindi + Eng)",
                            "HINDI" to "Hindi (हिंदी)",
                            "ENGLISH" to "English"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = settings.languageMode == code,
                                onClick = { onUpdatePersonalization(code, null, null, null, null, null, null) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Response Style:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "FRIENDLY" to "Friendly",
                            "CONCISE" to "Concise",
                            "DETAILED" to "Detailed",
                            "CODING_EXPERT" to "Coding Expert"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = settings.responseStyle == code,
                                onClick = { onUpdatePersonalization(null, code, null, null, null, null, null) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speak Responses Aloud by Default (TextToSpeech)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.speakResponsesByDefault,
                            onCheckedChange = { onUpdatePersonalization(null, null, null, null, null, null, it) },
                            modifier = Modifier.testTag("speak_responses_switch")
                        )
                    }

                    Text(
                        text = "Voice Speed: ${String.format("%.2fx", speedSlider)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = speedSlider,
                        onValueChange = { speedSlider = it },
                        onValueChangeFinished = {
                            onUpdatePersonalization(null, null, null, null, speedSlider, null, null)
                        },
                        valueRange = 0.5f..2.0f
                    )

                    Text(
                        text = "Voice Pitch: ${String.format("%.2fx", pitchSlider)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = pitchSlider,
                        onValueChange = { pitchSlider = it },
                        onValueChangeFinished = {
                            onUpdatePersonalization(null, null, null, null, null, pitchSlider, null)
                        },
                        valueRange = 0.5f..2.0f
                    )

                    OutlinedButton(
                        onClick = {
                            onTestVoice("Namaste! Mera naam ${settings.assistantName} hai. Main aapki kya madad kar sakta hoon?")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_voice_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Test ${settings.assistantName}'s Voice")
                    }
                }
            }
        }

        // 3. AI Provider, Model & Connection Status (Step 4)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("AI Provider, Model & Connection Status", style = MaterialTheme.typography.titleMedium)
                    }

                    val connStatusText = when (aiConnectionStatus) {
                        is AiConnectionStatus.Ready -> "Connected: Firebase AI Logic (App Check Attested) + Local Command Engine"
                        is AiConnectionStatus.Generating -> "Generating response..."
                        is AiConnectionStatus.OfflineCommandModeOnly -> "Offline Local Command Mode (Cloud AI Disabled)"
                        is AiConnectionStatus.Error -> "Cloud AI Error: ${aiConnectionStatus.message} (Offline Commands Active)"
                    }
                    Text(
                        text = connStatusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Text("Select AI Mode:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.aiProvider == "FIREBASE_GEMINI",
                            onClick = { onUpdateAiConfig("FIREBASE_GEMINI", settings.aiModelName) },
                            label = { Text("Firebase Gemini AI + Offline Engine") }
                        )
                        FilterChip(
                            selected = settings.aiProvider == "OFFLINE_ONLY",
                            onClick = { onUpdateAiConfig("OFFLINE_ONLY", settings.aiModelName) },
                            label = { Text("Offline Local Commands Only") }
                        )
                    }

                    if (settings.aiProvider == "FIREBASE_GEMINI") {
                        Text("Select Gemini Model:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "gemini-3.8-flash" to "gemini-3.8-flash (Default, No Billing Req)",
                                "gemini-3.5-flash-lite" to "gemini-3.5-flash-lite (Ultra Fast)",
                                "gemini-4-argon" to "gemini-4-argon (Pro Reasoning, Blaze)"
                            ).forEach { (modelId, label) ->
                                FilterChip(
                                    selected = settings.aiModelName == modelId,
                                    onClick = { onUpdateAiConfig("FIREBASE_GEMINI", modelId) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Transparency & Security: This app uses cloud-based Firebase AI Logic for general questions and coding help, authenticated via Firebase App Check (no secret API keys are hardcoded into the APK). Supported phone commands, time checks, calculator math, and attendance queries run 100% locally offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Project Saving, GitHub & Source Code ZIP Export (Steps 1, 12, 13)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Project Ownership: GitHub & Source ZIP Download", style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        text = "Current Project Mode: ${settings.projectSaveChoice}\n• To export the entire Gradle project ZIP or generate an installable APK: open the AI Studio Settings/Export menu in the top bar.\n• To verify and save the in-app Project & Data Bundle ZIP right now, use the verified buttons below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = githubRepoInput,
                        onValueChange = { githubRepoInput = it },
                        label = { Text("GitHub Repository Reference (Private recommended)") },
                        placeholder = { Text("username/personal-ai-assistant") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onUpdateProjectSaveOption("GITHUB", githubRepoInput)
                            }
                        ) {
                            Text("Save Repo Reference")
                        }
                        OutlinedButton(
                            onClick = onVerifyAndGenerateZip,
                            modifier = Modifier.testTag("settings_verify_zip_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Verify Project ZIP")
                        }
                    }

                    if (verifiedZipInfo != null && verifiedZipInfo.isValid) {
                        Button(
                            onClick = {
                                saveZipLauncher.launch("personal_ai_assistant_bundle.zip")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_download_zip_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download Verified Project Bundle ZIP (${verifiedZipInfo.sizeBytes / 1024} KB)")
                        }
                    }
                }
            }
        }

        // 5. Privacy, Permissions & Data Deletion Controls (Step 11)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("Privacy, Permissions & Data Control", style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        text = "• RECORD_AUDIO: Used only when you tap the mic button or enable active wake-phrase listening.\n• POST_NOTIFICATIONS & FOREGROUND_SERVICE_MICROPHONE: Used only when you enable the Lock-Screen Assistant button.\n• USE_BIOMETRIC: Used only for official AndroidX BiometricPrompt verification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onClearChatHistoryOnly,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Chats Only")
                        }
                        Button(
                            onClick = { showWipeConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Wipe All Data")
                        }
                    }
                }
            }
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Wipe All Local User Data?") },
            text = {
                Text("This will permanently delete all local chat sessions, Personal Memory Vault items, and PagarBook Attendance & Salary records from this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onWipeAllData()
                        showWipeConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
