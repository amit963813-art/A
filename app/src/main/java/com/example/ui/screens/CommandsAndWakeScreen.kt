package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.CustomCommandEntity
import com.example.data.preferences.AssistantSettings
import com.example.domain.InstalledAppInfo
import com.example.domain.LocalCommandProcessor
import com.example.voice.VoiceUiState
import com.example.voice.WakePhraseEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandsAndWakeScreen(
    settings: AssistantSettings,
    voiceState: VoiceUiState,
    wakeEngine: WakePhraseEngine,
    customCommands: List<CustomCommandEntity>,
    installedApps: List<InstalledAppInfo>,
    commandProcessor: LocalCommandProcessor,
    onUpdateWakePhrase: (String) -> Unit,
    onToggleWakeListening: (Boolean) -> Unit,
    onStopVoiceListening: () -> Unit,
    onToggleLockScreenService: (Boolean) -> Unit,
    onToggleLockScreenVisibility: (Boolean) -> Unit,
    onSaveCommand: (CustomCommandEntity) -> Unit,
    onToggleCommandEnabled: (CustomCommandEntity, Boolean) -> Unit,
    onDeleteCommand: (CustomCommandEntity) -> Unit,
    onTestRunCommand: (String) -> Unit
) {
    val context = LocalContext.current
    var wakePhraseInput by remember(settings.wakePhrase) { mutableStateOf(settings.wakePhrase) }
    var editingCommand by remember { mutableStateOf<CustomCommandEntity?>(null) }
    var showCommandDialog by remember { mutableStateOf(false) }

    val servicePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val micGranted = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (micGranted) {
            onToggleLockScreenService(true)
        }
    }

    val wakeMicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleWakeListening(true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCommand = null
                    showCommandDialog = true
                },
                modifier = Modifier.testTag("add_custom_command_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Custom Command")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // STEP 5: CUSTOM WAKE PHRASE CARD
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
                            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Wake Phrase & On-Device Listening",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = "Configured for ${settings.assistantName}. Example: \"Hey ${settings.assistantName}\" or \"Namaste ${settings.assistantName}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = wakePhraseInput,
                                onValueChange = { wakePhraseInput = it },
                                label = { Text("Wake Phrase") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wake_phrase_input")
                            )
                            Button(
                                onClick = { onUpdateWakePhrase(wakePhraseInput) },
                                modifier = Modifier.testTag("save_wake_phrase_button")
                            ) {
                                Text("Save")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Wake-Phrase Listening Session",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "Status: ${voiceState.statusMessage}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Switch(
                                checked = voiceState.isListening,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        val hasMic = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasMic) {
                                            onToggleWakeListening(true)
                                        } else {
                                            wakeMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    } else {
                                        onToggleWakeListening(false)
                                    }
                                },
                                modifier = Modifier.testTag("wake_listening_switch")
                            )
                        }

                        if (voiceState.isListening) {
                            Button(
                                onClick = onStopVoiceListening,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("commands_stop_listening_button")
                            ) {
                                Icon(Icons.Default.StopCircle, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Stop Listening Immediately")
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Engine: ${wakeEngine.engineName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = wakeEngine.statusExplanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // STEP 6: LOCK-SCREEN ASSISTANT BUTTON & FOREGROUND SERVICE
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
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "Lock-Screen Assistant Button (Foreground Service)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = "Displays a persistent notification with a \"Talk to ${settings.assistantName}\" button so you can trigger your assistant from the lock screen without bypassing your phone's PIN or biometric lock.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Run Persistent Assistant Notification", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = if (settings.isLockScreenServiceRunning) "Active in notification bar" else "Stopped",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (settings.isLockScreenServiceRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.isLockScreenServiceRunning,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        servicePermissionsLauncher.launch(perms.toTypedArray())
                                    } else {
                                        onToggleLockScreenService(false)
                                    }
                                },
                                modifier = Modifier.testTag("lockscreen_service_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Notification Button on Lock Screen", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "Uses VISIBILITY_PUBLIC when enabled; respects PIN/biometrics for protected actions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.showNotificationOnLockScreen,
                                onCheckedChange = onToggleLockScreenVisibility,
                                modifier = Modifier.testTag("lockscreen_visibility_switch")
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Optional: Open Android Digital Assistant Role Settings")
                        }
                    }
                }
            }

            // STEP 7: EDITABLE CUSTOM VOICE COMMANDS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Custom Phone Commands (${customCommands.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Hindi, Hinglish & English commands to open apps or run actions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(customCommands, key = { it.id }) { cmd ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cmd.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Phrases: ${cmd.triggerPhrases}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Switch(
                                checked = cmd.isEnabled,
                                onCheckedChange = { onToggleCommandEnabled(cmd, it) }
                            )
                        }

                        Text(
                            text = commandProcessor.buildPreviewDescription(
                                cmd.actionType,
                                cmd.targetPackageOrAction,
                                cmd.customReplyText,
                                cmd.requiresConfirmation
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val firstPhrase = cmd.triggerPhrases.split(",").firstOrNull()?.trim().orEmpty()
                                    if (firstPhrase.isNotBlank()) {
                                        onTestRunCommand(firstPhrase)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Test Run")
                            }
                            IconButton(
                                onClick = {
                                    editingCommand = cmd
                                    showCommandDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Command")
                            }
                            IconButton(
                                onClick = { onDeleteCommand(cmd) }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Command",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCommandDialog) {
        CustomCommandEditorDialog(
            initial = editingCommand,
            installedApps = installedApps,
            commandProcessor = commandProcessor,
            onDismiss = { showCommandDialog = false },
            onSave = { saved ->
                onSaveCommand(saved)
                showCommandDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomCommandEditorDialog(
    initial: CustomCommandEntity?,
    installedApps: List<InstalledAppInfo>,
    commandProcessor: LocalCommandProcessor,
    onDismiss: () -> Unit,
    onSave: (CustomCommandEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var triggers by remember { mutableStateOf(initial?.triggerPhrases ?: "") }
    var actionType by remember { mutableStateOf(initial?.actionType ?: "OPEN_APP") }
    var targetPkgOrAction by remember { mutableStateOf(initial?.targetPackageOrAction ?: "com.google.android.youtube") }
    var customReply by remember { mutableStateOf(initial?.customReplyText ?: "") }
    var requiresConfirmation by remember { mutableStateOf(initial?.requiresConfirmation ?: false) }

    val actionTypes = listOf(
        "OPEN_APP" to "Open App",
        "SYSTEM_INTENT" to "System Intent",
        "CUSTOM_REPLY" to "Custom Spoken Reply",
        "SPEAK_TIME" to "Speak Time",
        "CHECK_ATTENDANCE" to "Check Attendance",
        "MAKE_CALL" to "Dial Phone (Confirm)",
        "SEND_SMS" to "Compose SMS (Confirm)"
    )

    val livePreview = commandProcessor.buildPreviewDescription(
        actionType = actionType,
        targetPackageOrAction = targetPkgOrAction,
        customReplyText = customReply,
        requiresConfirmation = requiresConfirmation || actionType == "MAKE_CALL" || actionType == "SEND_SMS"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Custom Voice Command" else "Edit Custom Command") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Command Name (e.g. Open WhatsApp)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cmd_title_input")
                )

                OutlinedTextField(
                    value = triggers,
                    onValueChange = { triggers = it },
                    label = { Text("Hindi / Hinglish Triggers (comma-separated)") },
                    placeholder = { Text("whatsapp kholo,open whatsapp,व्हाट्सएप खोलो") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cmd_triggers_input")
                )

                Text("Action Type:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    actionTypes.forEach { (code, label) ->
                        FilterChip(
                            selected = actionType == code,
                            onClick = { actionType = code },
                            label = { Text(label) }
                        )
                    }
                }

                if (actionType == "OPEN_APP" || actionType == "SYSTEM_INTENT" || actionType == "MAKE_CALL" || actionType == "SEND_SMS") {
                    OutlinedTextField(
                        value = targetPkgOrAction,
                        onValueChange = { targetPkgOrAction = it },
                        label = {
                            Text(
                                when (actionType) {
                                    "OPEN_APP" -> "Target Package Name"
                                    "MAKE_CALL", "SEND_SMS" -> "Phone Number"
                                    else -> "Android Intent Action"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (actionType == "OPEN_APP" && installedApps.isNotEmpty()) {
                    Text(
                        text = "Detected Installed Apps (Tap to select):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        installedApps.take(12).forEach { app ->
                            FilterChip(
                                selected = targetPkgOrAction == app.packageName,
                                onClick = {
                                    targetPkgOrAction = app.packageName
                                    if (title.isBlank()) title = "Open ${app.appName}"
                                    if (triggers.isBlank()) triggers = "${app.appName.lowercase()} kholo,open ${app.appName.lowercase()}"
                                },
                                label = { Text(app.appName) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customReply,
                    onValueChange = { customReply = it },
                    label = { Text("Spoken Confirmation / Custom Reply") },
                    placeholder = { Text("App khol raha hoon...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ask confirmation before executing", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = requiresConfirmation || actionType == "MAKE_CALL" || actionType == "SEND_SMS",
                        onCheckedChange = { requiresConfirmation = it }
                    )
                }

                // Step 7 Requirement: Show a preview of the action before saving a new command
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Action Preview Before Saving:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = livePreview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && triggers.isNotBlank()) {
                        onSave(
                            CustomCommandEntity(
                                id = initial?.id ?: 0L,
                                title = title.trim(),
                                triggerPhrases = triggers.trim(),
                                actionType = actionType,
                                targetPackageOrAction = targetPkgOrAction.trim(),
                                customReplyText = customReply.trim(),
                                requiresConfirmation = requiresConfirmation || actionType == "MAKE_CALL" || actionType == "SEND_SMS",
                                isEnabled = initial?.isEnabled ?: true,
                                isBuiltIn = initial?.isBuiltIn ?: false
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && triggers.isNotBlank(),
                modifier = Modifier.testTag("save_custom_command_button")
            ) {
                Text("Save Command")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
