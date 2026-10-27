package com.example.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.CustomCommandEntity
import com.example.data.local.MemoryItemEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

sealed interface CommandExecutionOutcome {
    data object NotMatched : CommandExecutionOutcome
    data class ExecutedOffline(
        val spokenAndChatReply: String,
        val actionPerformed: String,
        val enableTtsRequested: Boolean = false
    ) : CommandExecutionOutcome
    data class ConfirmationRequired(
        val commandTitle: String,
        val warningMessage: String,
        val pendingIntent: Intent,
        val replyAfterConfirm: String
    ) : CommandExecutionOutcome
}

/**
 * Deterministic local command & phone intent engine.
 * Works 100% offline for app launching, time/date queries, attendance summaries,
 * math calculations, memory lookup, and user-created custom commands.
 */
class LocalCommandProcessor(private val context: Context) {

    fun queryLaunchableApps(): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            resolveInfos
                .mapNotNull { info ->
                    val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                    val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                    if (label.isBlank()) null else InstalledAppInfo(label, pkg)
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase(Locale.ROOT) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun buildPreviewDescription(
        actionType: String,
        targetPackageOrAction: String,
        customReplyText: String,
        requiresConfirmation: Boolean
    ): String {
        val base = when (actionType) {
            "OPEN_APP" -> "Launches installed Android app package: '$targetPackageOrAction' via PackageManager.getLaunchIntentForPackage()."
            "SYSTEM_INTENT" -> "Fires official Android system Intent: '$targetPackageOrAction'."
            "SPEAK_TIME" -> "Reads the current device time and date in Hindi/Hinglish and speaks it via TextToSpeech."
            "CHECK_ATTENDANCE" -> "Queries local Room database for today's Clock IN/OUT status and speaks summary."
            "TOGGLE_TTS" -> "Enables automatic spoken TextToSpeech responses in DataStore settings."
            "OPEN_ASSISTANT" -> "Focuses the assistant voice/chat screen and greets you by name."
            "MAKE_CALL" -> "Opens Android Dialer (ACTION_DIAL) with number '$targetPackageOrAction'."
            "SEND_SMS" -> "Opens Android SMS composer (ACTION_SENDTO) for '$targetPackageOrAction'."
            else -> "Responds offline with custom reply: \"${customReplyText.ifBlank { "Done" }}\"."
        }
        return if (requiresConfirmation) {
            "$base [Safety Gate: Will ask for your explicit confirmation before executing]"
        } else {
            base
        }
    }

    fun processInput(
        userInput: String,
        assistantName: String,
        customCommands: List<CustomCommandEntity>,
        todayAttendance: AttendanceRecordEntity?,
        memories: List<MemoryItemEntity>
    ): CommandExecutionOutcome {
        val normalized = userInput.lowercase(Locale.ROOT).trim().trimEnd('.', '!', '?')
        if (normalized.isBlank()) return CommandExecutionOutcome.NotMatched

        // 1. Check user & built-in Custom Commands from Room first
        val enabledCommands = customCommands.filter { it.isEnabled }
        for (cmd in enabledCommands) {
            val triggers = cmd.triggerPhrases
                .split(",")
                .map { it.trim().lowercase(Locale.ROOT).trimEnd('.', '!', '?') }
                .filter { it.isNotBlank() }

            val matched = triggers.any { trigger ->
                normalized == trigger || normalized.contains(trigger)
            }
            if (matched) {
                return executeMatchedCommand(cmd, assistantName, todayAttendance)
            }
        }

        // 2. Check dynamic "App Kholo" / "Open [App Name]" against installed apps
        val openAppPrefixMatch = extractAppNameFromPhrase(normalized)
        if (openAppPrefixMatch != null) {
            val installed = queryLaunchableApps()
            val foundApp = installed.firstOrNull { app ->
                app.appName.lowercase(Locale.ROOT).contains(openAppPrefixMatch) ||
                    openAppPrefixMatch.contains(app.appName.lowercase(Locale.ROOT))
            }
            if (foundApp != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(foundApp.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    return try {
                        context.startActivity(launchIntent)
                        CommandExecutionOutcome.ExecutedOffline(
                            spokenAndChatReply = "$assistantName: ${foundApp.appName} open kar diya hai.",
                            actionPerformed = "Launched ${foundApp.appName} (${foundApp.packageName})"
                        )
                    } catch (e: Exception) {
                        CommandExecutionOutcome.ExecutedOffline(
                            spokenAndChatReply = "${foundApp.appName} launch karne mein error aaya: ${e.localizedMessage}",
                            actionPerformed = "App launch error"
                        )
                    }
                }
            }
        }

        // 3. Offline Math / Calculator evaluation (e.g., "calculate 125 * 14" or "hisab karo 500 + 250")
        val mathResult = tryEvaluateMathExpression(normalized)
        if (mathResult != null) {
            return CommandExecutionOutcome.ExecutedOffline(
                spokenAndChatReply = "$assistantName (Offline Calculator): Jawab hai **$mathResult**.",
                actionPerformed = "Evaluated math expression locally"
            )
        }

        // 4. Offline Memory Lookup ("meri memory dikhao" / "what do you remember about me")
        if (normalized.contains("meri memory") ||
            normalized.contains("kya yaad hai") ||
            normalized.contains("what do you remember")
        ) {
            val reply = if (memories.isEmpty()) {
                "Abhi aapke Personal Memory Vault mein koi note save nahi hai. Aap Memory tab mein jaakar ya chat mein bata kar save kar sakte hain."
            } else {
                val top = memories.take(5).joinToString("\n") { "• **${it.title}**: ${it.content}" }
                "Mujhe aapke baare mein yeh baatein yaad hain (Local Memory Vault):\n$top"
            }
            return CommandExecutionOutcome.ExecutedOffline(
                spokenAndChatReply = reply,
                actionPerformed = "Read from local Room Memory Vault"
            )
        }

        return CommandExecutionOutcome.NotMatched
    }

    private fun executeMatchedCommand(
        cmd: CustomCommandEntity,
        assistantName: String,
        todayAttendance: AttendanceRecordEntity?
    ): CommandExecutionOutcome {
        return when (cmd.actionType) {
            "SPEAK_TIME" -> {
                val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US).format(Date())
                CommandExecutionOutcome.ExecutedOffline(
                    spokenAndChatReply = "Abhi samay hua hai **$timeStr**, aur aaj ki tareekh hai **$dateStr**.",
                    actionPerformed = "Local Time & Date Check"
                )
            }

            "CHECK_ATTENDANCE" -> {
                val reply = if (todayAttendance == null) {
                    "Aaj ki attendance abhi mark nahi hui hai. Aap Attendance tab mein jaakar Biometric IN mark kar sakte hain."
                } else {
                    val inTime = todayAttendance.checkInTimeMillis?.let {
                        SimpleDateFormat("hh:mm a", Locale.US).format(Date(it))
                    } ?: "Not clocked in"
                    val outTime = todayAttendance.checkOutTimeMillis?.let {
                        SimpleDateFormat("hh:mm a", Locale.US).format(Date(it))
                    } ?: "Abhi OUT mark nahi hua"
                    val hrs = todayAttendance.workedMinutes / 60
                    val mins = todayAttendance.workedMinutes % 60
                    "Aaj ki Attendance Status: **${todayAttendance.status}**\n• IN Time: $inTime\n• OUT Time: $outTime\n• Worked: ${hrs}h ${mins}m (Overtime: ${todayAttendance.overtimeMinutes}m)"
                }
                CommandExecutionOutcome.ExecutedOffline(
                    spokenAndChatReply = reply,
                    actionPerformed = "Checked local PagarBook Attendance"
                )
            }

            "TOGGLE_TTS" -> {
                CommandExecutionOutcome.ExecutedOffline(
                    spokenAndChatReply = cmd.customReplyText.ifBlank {
                        "Bilkul! Ab se main aapko har jawab bolkar sunaunga."
                    },
                    actionPerformed = "Enabled spoken TTS responses",
                    enableTtsRequested = true
                )
            }

            "OPEN_ASSISTANT" -> {
                CommandExecutionOutcome.ExecutedOffline(
                    spokenAndChatReply = cmd.customReplyText.ifBlank {
                        "Namaste! Main $assistantName hoon, bataiye main aapki kya seva karoon?"
                    },
                    actionPerformed = "Opened Assistant Interface"
                )
            }

            "OPEN_APP" -> {
                val pkg = cmd.targetPackageOrAction.trim()
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    ?: if (pkg.contains("youtube")) {
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com"))
                    } else null

                if (intent == null) {
                    CommandExecutionOutcome.ExecutedOffline(
                        spokenAndChatReply = "App ($pkg) is phone par installed nahi mila.",
                        actionPerformed = "Package not found: $pkg"
                    )
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (cmd.requiresConfirmation) {
                        CommandExecutionOutcome.ConfirmationRequired(
                            commandTitle = cmd.title,
                            warningMessage = "Do you want $assistantName to open ${cmd.title}?",
                            pendingIntent = intent,
                            replyAfterConfirm = cmd.customReplyText.ifBlank { "${cmd.title} open kar diya." }
                        )
                    } else {
                        try {
                            context.startActivity(intent)
                            CommandExecutionOutcome.ExecutedOffline(
                                spokenAndChatReply = cmd.customReplyText.ifBlank { "${cmd.title} open kar raha hoon." },
                                actionPerformed = "Launched $pkg"
                            )
                        } catch (e: Exception) {
                            CommandExecutionOutcome.ExecutedOffline(
                                spokenAndChatReply = "App open nahi ho saka: ${e.localizedMessage}",
                                actionPerformed = "Launch failed"
                            )
                        }
                    }
                }
            }

            "SYSTEM_INTENT" -> {
                val intent = when (cmd.targetPackageOrAction) {
                    MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA, "android.media.action.STILL_IMAGE_CAMERA" ->
                        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    Settings.ACTION_SETTINGS, "android.settings.SETTINGS" ->
                        Intent(Settings.ACTION_SETTINGS)
                    "ANDROID_CALCULATOR" -> {
                        Intent.makeMainSelectorActivity(
                            Intent.ACTION_MAIN,
                            Intent.CATEGORY_APP_CALCULATOR
                        )
                    }
                    else -> Intent(cmd.targetPackageOrAction)
                }.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (cmd.requiresConfirmation) {
                    CommandExecutionOutcome.ConfirmationRequired(
                        commandTitle = cmd.title,
                        warningMessage = "Confirm executing system action: ${cmd.title}?",
                        pendingIntent = intent,
                        replyAfterConfirm = cmd.customReplyText.ifBlank { "${cmd.title} chalu kar diya." }
                    )
                } else {
                    try {
                        context.startActivity(intent)
                        CommandExecutionOutcome.ExecutedOffline(
                            spokenAndChatReply = cmd.customReplyText.ifBlank { "${cmd.title} open kar diya hai." },
                            actionPerformed = "Executed system intent: ${cmd.targetPackageOrAction}"
                        )
                    } catch (e: Exception) {
                        CommandExecutionOutcome.ExecutedOffline(
                            spokenAndChatReply = "${cmd.title} is device/emulator par available nahi hai. Aap chat mein seedha hisab pooch sakte hain (jaise '240 * 15').",
                            actionPerformed = "System intent fallback"
                        )
                    }
                }
            }

            "MAKE_CALL" -> {
                val number = cmd.targetPackageOrAction.trim()
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                // Sensitive action ALWAYS asks for confirmation per Step 7
                CommandExecutionOutcome.ConfirmationRequired(
                    commandTitle = cmd.title,
                    warningMessage = "Sensitive Action: Open phone dialer for $number?",
                    pendingIntent = dialIntent,
                    replyAfterConfirm = "Dialer open kar diya hai ($number)."
                )
            }

            "SEND_SMS" -> {
                val target = cmd.targetPackageOrAction.trim()
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$target")).apply {
                    putExtra("sms_body", cmd.customReplyText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                // Sensitive action ALWAYS asks for confirmation per Step 7
                CommandExecutionOutcome.ConfirmationRequired(
                    commandTitle = cmd.title,
                    warningMessage = "Sensitive Action: Open SMS app to message $target?",
                    pendingIntent = smsIntent,
                    replyAfterConfirm = "SMS composer open kar diya hai."
                )
            }

            else -> {
                CommandExecutionOutcome.ExecutedOffline(
                    spokenAndChatReply = cmd.customReplyText.ifBlank { "Command '${cmd.title}' pura hua." },
                    actionPerformed = "Custom reply executed"
                )
            }
        }
    }

    private fun extractAppNameFromPhrase(normalized: String): String? {
        if (normalized.endsWith(" kholo")) {
            val candidate = normalized.removeSuffix(" kholo").trim()
            if (candidate.length >= 2) return candidate
        }
        if (normalized.startsWith("open ")) {
            val candidate = normalized.removePrefix("open ").trim()
            if (candidate.length >= 2) return candidate
        }
        return null
    }

    private fun tryEvaluateMathExpression(input: String): String? {
        val cleaned = input
            .replace("calculate", "")
            .replace("hisab karo", "")
            .replace("kitna hota hai", "")
            .replace("what is", "")
            .replace("=", "")
            .trim()

        val regex = Regex("""^(-?\d+(?:\.\d+)?)\s*([+\-*/%])\s*(-?\d+(?:\.\d+)?)$""")
        val match = regex.find(cleaned) ?: return null
        val left = match.groupValues[1].toDoubleOrNull() ?: return null
        val op = match.groupValues[2]
        val right = match.groupValues[3].toDoubleOrNull() ?: return null

        val result = when (op) {
            "+" -> left + right
            "-" -> left - right
            "*" -> left * right
            "/" -> if (right == 0.0) return "Zero se divide nahi kar sakte" else left / right
            "%" -> if (right == 0.0) return "Zero se modulo nahi kar sakte" else left % right
            else -> return null
        }
        return if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", result)
        }
    }
}
