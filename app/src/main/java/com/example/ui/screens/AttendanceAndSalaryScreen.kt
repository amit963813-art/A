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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.SalaryAdjustmentEntity
import com.example.data.preferences.AssistantSettings
import com.example.data.repository.MonthlySalarySummary
import com.example.security.BiometricAuthManager
import com.example.security.BiometricCapability
import com.example.security.BiometricResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceAndSalaryScreen(
    settings: AssistantSettings,
    attendanceRecords: List<AttendanceRecordEntity>,
    salaryAdjustments: List<SalaryAdjustmentEntity>,
    monthlySalarySummary: MonthlySalarySummary,
    onClockInToday: (biometricVerified: Boolean) -> Unit,
    onClockOutToday: (biometricVerified: Boolean) -> Unit,
    onMarkCustomAttendance: (
        dateString: String,
        status: String,
        workedMinutes: Int,
        overtimeMinutes: Int,
        notes: String,
        biometricVerified: Boolean
    ) -> Unit,
    onAddSalaryAdjustment: (type: String, amount: Double, description: String) -> Unit,
    onDeleteSalaryAdjustment: (SalaryAdjustmentEntity) -> Unit,
    onUpdateSalaryConfig: (
        employeeName: String,
        employeeRole: String,
        monthlyBaseSalary: Double,
        standardWorkHoursPerDay: Double,
        hourlyOvertimeRate: Double,
        weeklyHolidayDayOfWeek: Int
    ) -> Unit,
    onToggleRequireBiometricForAttendance: (Boolean) -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    onExportJsonBackup: ((String) -> Unit) -> Unit,
    onImportJsonBackup: (String) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricCapability = remember(context) { BiometricAuthManager.checkCapability(context) }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val todayRecord = attendanceRecords.find { it.dateString == todayStr }

    var showSalaryConfigDialog by remember { mutableStateOf(false) }
    var showAdjustmentDialog by remember { mutableStateOf(false) }
    var showManualStatusDialog by remember { mutableStateOf(false) }

    // CSV Export Launcher
    var pendingCsvContent by remember { mutableStateOf("") }
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingCsvContent.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pendingCsvContent.toByteArray(Charsets.UTF_8))
                }
                onStatusMessage("Attendance CSV exported successfully!")
            } catch (e: Exception) {
                onStatusMessage("CSV Export error: ${e.localizedMessage}")
            }
        }
    }

    // JSON Backup Export Launcher
    var pendingJsonContent by remember { mutableStateOf("") }
    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingJsonContent.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pendingJsonContent.toByteArray(Charsets.UTF_8))
                }
                onStatusMessage("Full JSON backup saved to device!")
            } catch (e: Exception) {
                onStatusMessage("JSON Export error: ${e.localizedMessage}")
            }
        }
    }

    // JSON Restore Launcher
    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    onImportJsonBackup(text)
                }
            } catch (e: Exception) {
                onStatusMessage("Restore error: ${e.localizedMessage}")
            }
        }
    }

    fun triggerBiometricOrAction(actionLabel: String, onCompleted: (Boolean) -> Unit) {
        if (!settings.requireBiometricForAttendance) {
            onCompleted(false)
            return
        }
        if (activity != null && biometricCapability is BiometricCapability.Ready) {
            BiometricAuthManager.authenticate(
                activity = activity,
                title = "Biometric Attendance: $actionLabel",
                subtitle = "Verify identity using AndroidX BiometricPrompt"
            ) { result ->
                when (result) {
                    is BiometricResult.Success -> onCompleted(true)
                    is BiometricResult.Cancelled -> onStatusMessage("Biometric attendance cancelled.")
                    is BiometricResult.Lockout -> onStatusMessage("Biometric lockout: ${result.message}")
                    is BiometricResult.HardwareUnavailable -> onStatusMessage(result.message)
                    is BiometricResult.Failed -> onStatusMessage("Biometric failed: ${result.message}")
                }
            }
        } else {
            onStatusMessage("Biometric sensor not enrolled on this device (${BiometricAuthManager.capabilityLabel(biometricCapability)}). Marked without biometric hardware.")
            onCompleted(false)
        }
    }

    val holidayName = dayOfWeekLabel(settings.weeklyHolidayDayOfWeek)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Employee & Biometric Clock IN / OUT Card
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = settings.employeeName,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "${settings.employeeRole} • Weekly Holiday: $holidayName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showSalaryConfigDialog = true },
                            modifier = Modifier.testTag("salary_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Configure Salary & Holiday")
                        }
                    }

                    // Biometric Gate Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Require AndroidX BiometricPrompt for IN/OUT", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = BiometricAuthManager.capabilityLabel(biometricCapability),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.requireBiometricForAttendance,
                            onCheckedChange = onToggleRequireBiometricForAttendance,
                            modifier = Modifier.testTag("attendance_biometric_switch")
                        )
                    }

                    // Today's Status Summary
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Today ($todayStr) — Status: ${todayRecord?.status ?: "NOT MARKED"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val inTimeStr = todayRecord?.checkInTimeMillis?.let {
                                SimpleDateFormat("hh:mm a", Locale.US).format(Date(it))
                            } ?: "--:--"
                            val outTimeStr = todayRecord?.checkOutTimeMillis?.let {
                                SimpleDateFormat("hh:mm a", Locale.US).format(Date(it))
                            } ?: "--:--"
                            val hrs = (todayRecord?.workedMinutes ?: 0) / 60
                            val mins = (todayRecord?.workedMinutes ?: 0) % 60
                            Text(
                                text = "IN: $inTimeStr  |  OUT: $outTimeStr  |  Worked: ${hrs}h ${mins}m (OT: ${todayRecord?.overtimeMinutes ?: 0}m)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (todayRecord?.biometricVerified == true) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Verified by AndroidX BiometricPrompt",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }

                    // Biometric Clock IN and Clock OUT Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                triggerBiometricOrAction("Clock IN") { verified ->
                                    onClockInToday(verified)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("biometric_clock_in_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(
                                if (settings.requireBiometricForAttendance) Icons.Default.Fingerprint else Icons.Default.Login,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Biometric IN")
                        }

                        Button(
                            onClick = {
                                triggerBiometricOrAction("Clock OUT") { verified ->
                                    onClockOutToday(verified)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("biometric_clock_out_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(
                                if (settings.requireBiometricForAttendance) Icons.Default.Fingerprint else Icons.Default.Logout,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Biometric OUT")
                        }
                    }

                    OutlinedButton(
                        onClick = { showManualStatusDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mark_custom_status_button")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark Present / Half-Day / Holiday / Overtime")
                    }
                }
            }
        }

        // 2. PagarBook Monthly Salary Report Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MONTHLY SALARY LEDGER (${monthlySalarySummary.yearMonth})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", monthlySalarySummary.netPayableSalary)}",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Net Payable Salary (Local PagarBook Calculation)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SalaryStatItem("Present", "${monthlySalarySummary.presentDays}d")
                        SalaryStatItem("Half Day", "${monthlySalarySummary.halfDays}d")
                        SalaryStatItem("Holidays ($holidayName)", "${monthlySalarySummary.weeklyHolidayCount}d")
                        SalaryStatItem("Absent", "${monthlySalarySummary.absentDays}d")
                    }

                    val workedHours = monthlySalarySummary.totalWorkedMinutes / 60
                    val workedMins = monthlySalarySummary.totalWorkedMinutes % 60
                    val otHours = monthlySalarySummary.totalOvertimeMinutes / 60
                    val otMins = monthlySalarySummary.totalOvertimeMinutes % 60

                    Text(
                        text = "• Base Salary: ₹${String.format(Locale.US, "%.0f", monthlySalarySummary.baseMonthlySalary)} (Daily: ₹${String.format(Locale.US, "%.0f", monthlySalarySummary.dailyWage)})\n" +
                            "• Earned Base: ₹${String.format(Locale.US, "%.2f", monthlySalarySummary.earnedBaseSalary)}\n" +
                            "• Total Hours: ${workedHours}h ${workedMins}m | Overtime (${otHours}h ${otMins}m): +₹${String.format(Locale.US, "%.2f", monthlySalarySummary.overtimePay)}\n" +
                            "• Bonuses: +₹${String.format(Locale.US, "%.2f", monthlySalarySummary.totalBonuses)} | Advances: -₹${String.format(Locale.US, "%.2f", monthlySalarySummary.totalAdvances)} | Deductions: -₹${String.format(Locale.US, "%.2f", monthlySalarySummary.totalDeductions)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAdjustmentDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_salary_adjustment_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Advance / Deduction")
                        }
                    }
                }
            }
        }

        // 3. CSV & JSON Backup and Restore Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Backup & Restore (Local-By-Default Privacy)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onExportCsv { csv ->
                                    pendingCsvContent = csv
                                    saveCsvLauncher.launch("attendance_${monthlySalarySummary.yearMonth}.csv")
                                }
                            },
                            modifier = Modifier.testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                onExportJsonBackup { json ->
                                    pendingJsonContent = json
                                    saveJsonLauncher.launch("assistant_backup_${todayStr}.json")
                                }
                            },
                            modifier = Modifier.testTag("export_json_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Backup JSON")
                        }

                        OutlinedButton(
                            onClick = {
                                openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier.testTag("restore_json_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore JSON")
                        }
                    }
                }
            }
        }

        // 4. Salary Adjustments History
        if (salaryAdjustments.isNotEmpty()) {
            item {
                Text(
                    text = "Advances, Overtime Bonuses & Deductions (${salaryAdjustments.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(salaryAdjustments, key = { it.id }) { adj ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${adj.type} • ₹${String.format(Locale.US, "%.2f", adj.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (adj.type == "BONUS" || adj.type == "OVERTIME_MANUAL") {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = "${adj.dateString} — ${adj.description}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteSalaryAdjustment(adj) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Adjustment",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // 5. Daily Attendance History
        item {
            Text(
                text = "Daily Attendance History (${attendanceRecords.size} days)",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (attendanceRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No attendance records yet. Tap \"Biometric IN\" above to log today's attendance.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(attendanceRecords, key = { it.id }) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rec.dateString,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = rec.status,
                                style = MaterialTheme.typography.labelLarge,
                                color = when (rec.status) {
                                    "PRESENT" -> MaterialTheme.colorScheme.tertiary
                                    "WEEKLY_HOLIDAY", "PAID_LEAVE" -> MaterialTheme.colorScheme.secondary
                                    "ABSENT" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        val hrs = rec.workedMinutes / 60
                        val mins = rec.workedMinutes % 60
                        Text(
                            text = "Worked: ${hrs}h ${mins}m | Overtime: ${rec.overtimeMinutes}m" +
                                if (rec.biometricVerified) " • Biometric Verified" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (rec.notes.isNotBlank()) {
                            Text(
                                text = "Note: ${rec.notes}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSalaryConfigDialog) {
        SalaryConfigDialog(
            settings = settings,
            onDismiss = { showSalaryConfigDialog = false },
            onSave = { empName, empRole, baseSalary, stdHours, otRate, holidayDow ->
                onUpdateSalaryConfig(empName, empRole, baseSalary, stdHours, otRate, holidayDow)
                showSalaryConfigDialog = false
            }
        )
    }

    if (showAdjustmentDialog) {
        SalaryAdjustmentDialog(
            onDismiss = { showAdjustmentDialog = false },
            onSave = { type, amount, desc ->
                onAddSalaryAdjustment(type, amount, desc)
                showAdjustmentDialog = false
            }
        )
    }

    if (showManualStatusDialog) {
        ManualAttendanceDialog(
            defaultDate = todayStr,
            standardHours = settings.standardWorkHoursPerDay,
            onDismiss = { showManualStatusDialog = false },
            onSave = { dateStr, status, workedMins, otMins, notes ->
                onMarkCustomAttendance(dateStr, status, workedMins, otMins, notes, false)
                showManualStatusDialog = false
            }
        )
    }
}

@Composable
private fun SalaryStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

private fun dayOfWeekLabel(isoDay: Int): String = when (isoDay) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    7 -> "Sunday"
    else -> "Wednesday"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SalaryConfigDialog(
    settings: AssistantSettings,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Int) -> Unit
) {
    var empName by remember { mutableStateOf(settings.employeeName) }
    var empRole by remember { mutableStateOf(settings.employeeRole) }
    var baseSalary by remember { mutableStateOf(settings.monthlyBaseSalary.toInt().toString()) }
    var stdHours by remember { mutableStateOf(settings.standardWorkHoursPerDay.toString()) }
    var otRate by remember { mutableStateOf(settings.hourlyOvertimeRate.toInt().toString()) }
    var holidayDow by remember { mutableStateOf(settings.weeklyHolidayDayOfWeek) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salary, Wage & Weekly Holiday Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = empName,
                    onValueChange = { empName = it },
                    label = { Text("Employee Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = empRole,
                    onValueChange = { empRole = it },
                    label = { Text("Role / Designation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseSalary,
                    onValueChange = { baseSalary = it },
                    label = { Text("Monthly Base Salary (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stdHours,
                    onValueChange = { stdHours = it },
                    label = { Text("Standard Working Hours / Day") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = otRate,
                    onValueChange = { otRate = it },
                    label = { Text("Hourly Overtime Rate (₹ / hour)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Weekly Holiday (Default: Wednesday per Step 10):",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = holidayDow == day,
                            onClick = { holidayDow = day },
                            label = { Text(dayOfWeekLabel(day)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        empName,
                        empRole,
                        baseSalary.toDoubleOrNull() ?: 24000.0,
                        stdHours.toDoubleOrNull() ?: 8.0,
                        otRate.toDoubleOrNull() ?: 150.0,
                        holidayDow
                    )
                }
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SalaryAdjustmentDialog(
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, description: String) -> Unit
) {
    var type by remember { mutableStateOf("ADVANCE") }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Advance, Deduction, or Bonus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ADVANCE", "DEDUCTION", "BONUS").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjustment_amount_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Reason / Note") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjustment_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onSave(type, amt, description)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("Add Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualAttendanceDialog(
    defaultDate: String,
    standardHours: Double,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int, String) -> Unit
) {
    var dateStr by remember { mutableStateOf(defaultDate) }
    var status by remember { mutableStateOf("PRESENT") }
    var workedHoursText by remember { mutableStateOf(standardHours.toInt().toString()) }
    var overtimeMinsText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Daily Attendance & Overtime") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PRESENT", "HALF_DAY", "WEEKLY_HOLIDAY", "PAID_LEAVE", "ABSENT").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st) }
                        )
                    }
                }
                OutlinedTextField(
                    value = workedHoursText,
                    onValueChange = { workedHoursText = it },
                    label = { Text("Worked Hours") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = overtimeMinsText,
                    onValueChange = { overtimeMinsText = it },
                    label = { Text("Overtime Minutes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val workedMins = ((workedHoursText.toDoubleOrNull() ?: standardHours) * 60).toInt()
                    val otMins = overtimeMinsText.toIntOrNull() ?: 0
                    onSave(dateStr.trim(), status, workedMins, otMins, notes.trim())
                }
            ) {
                Text("Save Attendance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
