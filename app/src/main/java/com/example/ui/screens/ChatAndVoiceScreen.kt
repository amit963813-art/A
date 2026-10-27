package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.preferences.AssistantSettings
import com.example.ui.AiConnectionStatus
import com.example.ui.PendingSensitiveCommand
import com.example.voice.VoiceUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatAndVoiceScreen(
    settings: AssistantSettings,
    sessions: List<ChatSessionEntity>,
    currentSessionId: Long,
    messages: List<ChatMessageEntity>,
    streamingPartialResponse: String?,
    voiceState: VoiceUiState,
    aiConnectionStatus: AiConnectionStatus,
    pendingSensitiveCommand: PendingSensitiveCommand?,
    onSelectSession: (Long) -> Unit,
    onCreateSession: (String) -> Unit,
    onRenameSession: (Long, String) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onClearAllChatsOnly: () -> Unit,
    onSendMessage: (String) -> Unit,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onApproveMemory: (ChatMessageEntity) -> Unit,
    onConfirmSensitiveCommand: () -> Unit,
    onDismissSensitiveCommand: () -> Unit,
    onOpenPersonalizationDialog: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showMicPermissionRationale by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoiceInput()
        } else {
            showMicPermissionRationale = true
        }
    }

    LaunchedEffect(messages.size, streamingPartialResponse) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Assistant Identity & Status Header
        AssistantHeaderCard(
            settings = settings,
            voiceState = voiceState,
            aiConnectionStatus = aiConnectionStatus,
            onStopListening = onStopVoiceInput,
            onStopSpeaking = onStopSpeaking,
            onEditAssistantName = onOpenPersonalizationDialog
        )

        // 2. Chat Sessions Bar (New, Rename, Delete, Clear History)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onCreateSession("Chat ${sessions.size + 1}") },
                label = { Text("New Chat") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Create New Chat", modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("new_chat_button")
            )

            sessions.forEach { session ->
                val isSelected = session.id == currentSessionId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectSession(session.id) },
                    label = { Text(session.title) }
                )
            }

            IconButton(
                onClick = {
                    val current = sessions.find { it.id == currentSessionId }
                    renameText = current?.title ?: "Chat"
                    showRenameDialog = true
                },
                modifier = Modifier.testTag("rename_chat_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Rename current chat")
            }

            IconButton(
                onClick = { showClearConfirmDialog = true },
                modifier = Modifier.testTag("clear_chats_button")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear chat history")
            }
        }

        // 3. Quick Hindi/Hinglish Voice & Local Command Chips
        val quickCommands = listOf(
            "Time batao",
            "Aaj ki attendance batao",
            "YouTube kholo",
            "Camera kholo",
            "Calculator kholo",
            "Settings kholo",
            "Mujhe jawab bolkar do"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCommands.forEach { cmd ->
                AssistChip(
                    onClick = { onSendMessage(cmd) },
                    label = { Text(cmd, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        // 4. Messages Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list"),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageBubble(
                    message = msg,
                    assistantName = settings.assistantName,
                    onSpeak = { onSpeakMessage(msg.content) },
                    onApproveMemory = { onApproveMemory(msg) }
                )
            }

            if (streamingPartialResponse != null) {
                item {
                    StreamingMessageBubble(
                        assistantName = settings.assistantName,
                        partialText = streamingPartialResponse
                    )
                }
            }
        }

        // 5. Live Partial Speech Transcript Banner
        AnimatedVisibility(visible = voiceState.isListening || voiceState.partialTranscript.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = voiceState.statusMessage,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (voiceState.partialTranscript.isNotBlank()) {
                            Text(
                                text = "\"${voiceState.partialTranscript}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Button(
                        onClick = onStopVoiceInput,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("banner_stop_listening_button")
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = "Stop Microphone")
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }
        }

        voiceState.errorText?.let { err ->
            Text(
                text = err,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // 6. Voice & Text Input Composer Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = {
                    if (voiceState.isListening) {
                        onStopVoiceInput()
                    } else {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            onStartVoiceInput()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .testTag("voice_mic_button")
            ) {
                Icon(
                    imageVector = if (voiceState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (voiceState.isListening) "Stop Listening" else "Speak in Hindi or English"
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text("Ask ${settings.assistantName} in Hindi, Hinglish, or English...")
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(20.dp),
                maxLines = 3
            )

            FilledIconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank(),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("send_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message"
                )
            }
        }
    }

    // Rename Chat Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Conversation Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameSession(currentSessionId, renameText)
                    showRenameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    if (sessions.size > 1) {
                        TextButton(onClick = {
                            onDeleteSession(currentSessionId)
                            showRenameDialog = false
                        }) {
                            Text("Delete Chat", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Clear Chat History Only Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Chat History Only?") },
            text = {
                Text("This will clear your chat conversation threads. Your Personal Memory Vault, Custom Commands, and PagarBook Attendance/Salary records will NOT be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllChatsOnly()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Chats Only")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sensitive Action Confirmation Dialog (Calls, SMS, or Confirm-Gated Commands)
    pendingSensitiveCommand?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissSensitiveCommand,
            title = { Text("Confirm Action: ${pending.title}") },
            text = { Text(pending.warningMessage) },
            confirmButton = {
                Button(onClick = onConfirmSensitiveCommand) {
                    Text("Confirm & Proceed")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissSensitiveCommand) {
                    Text("Cancel")
                }
            }
        )
    }

    // Microphone Permission Explanation Dialog
    if (showMicPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showMicPermissionRationale = false },
            title = { Text("Microphone Permission Needed for Voice Input") },
            text = {
                Text("${settings.assistantName} uses the microphone ONLY when you tap the mic button or enable active wake-phrase listening. No audio is ever recorded secretly. You can continue using text chat without granting microphone access.")
            },
            confirmButton = {
                Button(onClick = {
                    showMicPermissionRationale = false
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicPermissionRationale = false }) {
                    Text("Use Text Only")
                }
            }
        )
    }
}

@Composable
private fun AssistantHeaderCard(
    settings: AssistantSettings,
    voiceState: VoiceUiState,
    aiConnectionStatus: AiConnectionStatus,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onEditAssistantName: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated Voice Orb
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val active = voiceState.isListening || voiceState.isSpeaking
                    val radius = (size.minDimension / 2f) * (if (active) pulseScale else 0.9f)
                    drawCircle(
                        color = if (voiceState.isListening) secondaryColor.copy(alpha = 0.28f)
                        else primaryColor.copy(alpha = 0.22f),
                        radius = radius
                    )
                    drawCircle(
                        color = if (voiceState.isListening) secondaryColor else primaryColor,
                        radius = size.minDimension / 3.1f
                    )
                }
                Icon(
                    imageVector = if (voiceState.isListening) Icons.Default.Mic else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Assistant Orb",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = settings.assistantName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clickable { onEditAssistantName() }
                            .testTag("header_assistant_name")
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change Assistant Name",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onEditAssistantName() }
                    )
                }

                val statusLabel = when (aiConnectionStatus) {
                    is AiConnectionStatus.Ready -> "Firebase AI (${settings.aiModelName}) + Offline Engine"
                    is AiConnectionStatus.Generating -> "${settings.assistantName} is thinking..."
                    is AiConnectionStatus.OfflineCommandModeOnly -> "Offline Local Command Mode Active"
                    is AiConnectionStatus.Error -> "AI Offline — Local Commands Ready"
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Wake Phrase: \"${settings.wakePhrase}\" • ${voiceState.statusMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (voiceState.isListening) {
                IconButton(
                    onClick = onStopListening,
                    modifier = Modifier.testTag("stop_listening_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = "Stop Listening",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else if (voiceState.isSpeaking) {
                IconButton(
                    onClick = onStopSpeaking,
                    modifier = Modifier.testTag("stop_speaking_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Stop Speaking",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    assistantName: String,
    onSpeak: () -> Unit,
    onApproveMemory: () -> Unit
) {
    val isUser = message.sender == "USER"
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isUser) "You" else assistantName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary
                    )
                    if (message.isOfflineCommand && !isUser) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "Offline Command",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Offline Command",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (!isUser) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak response aloud",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Step 9: Ask permission before saving personal information to Memory Vault
                if (!isUser && message.proposedMemoryContent != null) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (message.memorySaved) "Saved to Memory Vault" else "Save fact to local Memory Vault?",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = message.proposedMemoryContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!message.memorySaved) {
                                Button(
                                    onClick = onApproveMemory,
                                    modifier = Modifier.testTag("approve_memory_button")
                                ) {
                                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Allow")
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Memory Saved",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingMessageBubble(
    assistantName: String,
    partialText: String
) {
    Surface(
        modifier = Modifier.widthIn(max = 330.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "$assistantName (Streaming...)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = partialText.ifBlank { "..." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
