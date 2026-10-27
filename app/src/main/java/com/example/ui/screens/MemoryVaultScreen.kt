package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.fragment.app.FragmentActivity
import com.example.data.local.MemoryItemEntity
import com.example.data.preferences.AssistantSettings
import com.example.security.BiometricAuthManager
import com.example.security.BiometricCapability
import com.example.security.BiometricResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryVaultScreen(
    settings: AssistantSettings,
    memories: List<MemoryItemEntity>,
    isUnlockedInSession: Boolean,
    onUnlockSuccess: () -> Unit,
    onToggleAskBeforeSaving: (Boolean) -> Unit,
    onToggleRequireBiometricForMemory: (Boolean) -> Unit,
    onSaveMemory: (title: String, content: String, category: String, id: Long) -> Unit,
    onDeleteMemory: (MemoryItemEntity) -> Unit,
    onClearChatHistorySeparately: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricCapability = remember(context) { BiometricAuthManager.checkCapability(context) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var editingMemory by remember { mutableStateOf<MemoryItemEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val requiresLockGate = settings.requireBiometricForMemory && !isUnlockedInSession

    if (requiresLockGate) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Protected Memory Vault",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Memory Vault Locked",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Authenticate with AndroidX BiometricPrompt (Fingerprint, Face, or Device PIN) to view or edit your saved private notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Hardware Status: ${BiometricAuthManager.capabilityLabel(biometricCapability)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Button(
                        onClick = {
                            if (activity != null && biometricCapability is BiometricCapability.Ready) {
                                BiometricAuthManager.authenticate(
                                    activity = activity,
                                    title = "Unlock Personal Memory Vault",
                                    subtitle = "Verify your identity to access private notes"
                                ) { res ->
                                    when (res) {
                                        is BiometricResult.Success -> onUnlockSuccess()
                                        is BiometricResult.Cancelled -> onStatusMessage("Authentication cancelled.")
                                        is BiometricResult.Lockout -> onStatusMessage("Biometric lockout: ${res.message}")
                                        is BiometricResult.HardwareUnavailable -> onStatusMessage(res.message)
                                        is BiometricResult.Failed -> onStatusMessage("Authentication failed: ${res.message}")
                                    }
                                }
                            } else {
                                onStatusMessage("No biometric/PIN enrolled on this emulator—disable biometric requirement below or enroll a PIN in Android Settings.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unlock_memory_biometric_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Authenticate with BiometricPrompt")
                    }

                    if (biometricCapability !is BiometricCapability.Ready) {
                        OutlinedButton(
                            onClick = {
                                onToggleRequireBiometricForMemory(false)
                                onUnlockSuccess()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disable Biometric Gate (Hardware Not Enrolled)")
                        }
                    }
                }
            }
        }
        return
    }

    val filteredMemories = memories.filter { item ->
        val matchesCat = selectedCategory == "ALL" || item.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
            item.title.contains(searchQuery, ignoreCase = true) ||
            item.content.contains(searchQuery, ignoreCase = true)
        matchesCat && matchesQuery
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingMemory = null
                    showEditDialog = true
                },
                modifier = Modifier.testTag("add_memory_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Personal Memory")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${settings.assistantName}'s Personal Memory Vault",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "All personal facts, notes, and preferences are stored locally in Room SQLite on your phone. ${settings.assistantName} uses these memories to personalize answers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ask Permission Before Saving Facts", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "Prompts you in chat before saving personal info",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.askBeforeSavingMemory,
                                onCheckedChange = onToggleAskBeforeSaving,
                                modifier = Modifier.testTag("ask_before_saving_memory_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Protect Memory Vault with BiometricPrompt", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    BiometricAuthManager.capabilityLabel(biometricCapability),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Switch(
                                checked = settings.requireBiometricForMemory,
                                onCheckedChange = onToggleRequireBiometricForMemory,
                                modifier = Modifier.testTag("memory_biometric_switch")
                            )
                        }

                        OutlinedButton(
                            onClick = onClearChatHistorySeparately,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_clear_chat_only_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clear Chat History Separately (Keep Saved Memory)")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search saved notes, facts, or preferences...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_search_input"),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "FACT", "PREFERENCE", "NOTE").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            if (filteredMemories.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No Saved Memories Yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap the + button to save a personal note, preference, or fact (e.g., \"My favorite language is Kotlin\" or \"Remind me about Wednesday holidays\").",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredMemories, key = { it.id }) { item ->
                    val dateStr = remember(item.updatedAt) {
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(item.updatedAt))
                    }
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
                                        text = item.category,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingMemory = item
                                            showEditDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Memory")
                                    }
                                    IconButton(
                                        onClick = { onDeleteMemory(item) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Memory",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Saved locally • $dateStr",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        MemoryEditorDialog(
            initial = editingMemory,
            onDismiss = { showEditDialog = false },
            onSave = { title, content, category, id ->
                onSaveMemory(title, content, category, id)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun MemoryEditorDialog(
    initial: MemoryItemEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, id: Long) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "FACT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Save Personal Memory" else "Edit Memory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Work Schedule, Favorite Food)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_title_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details / Note") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_content_input"),
                    minLines = 3
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("FACT", "PREFERENCE", "NOTE").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onSave(
                            title.ifBlank { "Personal Note" },
                            content,
                            category,
                            initial?.id ?: 0L
                        )
                    }
                },
                enabled = content.isNotBlank(),
                modifier = Modifier.testTag("save_memory_dialog_button")
            ) {
                Text("Save Locally")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
