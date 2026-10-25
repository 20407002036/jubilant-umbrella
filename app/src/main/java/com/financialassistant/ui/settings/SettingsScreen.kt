package com.financialassistant.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.financialassistant.tracking.MsisdnNormalizer
import com.financialassistant.tracking.TrackedLinePreferences
import com.financialassistant.ui.theme.*
import com.financialassistant.viewmodel.ImportStatus
import com.financialassistant.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showResetDialog by remember { mutableStateOf(false) }

    // Check permissions dynamically
    val hasReadSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    val hasReceiveSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasReadSms, hasReceiveSms) {
        viewModel.setSmsPermissionGranted(hasReadSms && hasReceiveSms)
    }

    LaunchedEffect(hasPhoneState) {
        viewModel.setPhonePermissionGranted(hasPhoneState)
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_SMS] ?: false
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        viewModel.setSmsPermissionGranted(readGranted && receiveGranted)
    }

    // Phone permission launcher (used to auto-detect which SIM holds the tracked number)
    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_PHONE_STATE] ?: false
        viewModel.setPhonePermissionGranted(granted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SMS Permission Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SMS Monitoring",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        // Status Indicator dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (uiState.hasSmsPermission) EmeraldPrimary else CoralExpense)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.hasSmsPermission) {
                            "Active and listening for financial messages. Parsed transactions will automatically populate the dashboard."
                        } else {
                            "Permission is required to automatically capture incoming financial SMS notifications and display transaction summaries."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!uiState.hasSmsPermission) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant SMS Permissions", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.12f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓ Monitoring Active",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Tracked M-PESA Line Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Tracked M-PESA Line",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Track money coming into one of your Safaricom numbers. On a dual-SIM phone, the app matches each M-PESA SMS to the SIM line it arrived on.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.trackedNumberInput,
                        onValueChange = { viewModel.onTrackedNumberInputChange(it) },
                        label = { Text("M-PESA number", color = TextSecondary) },
                        placeholder = { Text("0712 345 678", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedLabelColor = TextSecondary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = EmeraldPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (uiState.isTrackingConfigured) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val slotLabel = if (uiState.trackedSlot == TrackedLinePreferences.SLOT_UNSPECIFIED) {
                                "SIM not set"
                            } else {
                                "SIM ${uiState.trackedSlot + 1}"
                            }
                            Text(
                                text = "Tracking ${MsisdnNormalizer.format(uiState.trackedNumber)} • $slotLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    uiState.trackingMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.saveTrackedLine() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (uiState.isTrackingConfigured) "Update Tracked Line" else "Save & Detect SIM",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!uiState.hasPhonePermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                phonePermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_PHONE_STATE,
                                        Manifest.permission.READ_PHONE_NUMBERS
                                    )
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow SIM detection", fontWeight = FontWeight.Medium)
                        }
                    }

                    if (uiState.isTrackingConfigured &&
                        uiState.trackedSlot == TrackedLinePreferences.SLOT_UNSPECIFIED
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Which SIM holds this number?",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0..1).forEach { slot ->
                                OutlinedButton(
                                    onClick = { viewModel.selectTrackedSlot(slot) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                                    border = BorderStroke(1.dp, EmeraldPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("SIM ${slot + 1}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (uiState.isTrackingConfigured) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { viewModel.clearTrackedLine() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Remove tracked line", color = CoralExpense)
                        }
                    }
                }
            }

            // Google Sheet Sync Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Sheet Sync",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When money is received on your tracked line, add a row with the sender and amount to a Google Sheet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable sync",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Incoming payments only",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.sheetSyncEnabled,
                            onCheckedChange = viewModel::onSheetSyncEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant,
                                uncheckedBorderColor = BorderColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.sheetSyncUrl,
                        onValueChange = viewModel::onSheetSyncUrlChange,
                        label = { Text("Apps Script web app URL", color = TextSecondary) },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedLabelColor = TextSecondary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = EmeraldPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.sheetSyncSecret,
                        onValueChange = viewModel::onSheetSyncSecretChange,
                        label = { Text("Shared secret", color = TextSecondary) },
                        placeholder = { Text("Same value as SHARED_SECRET in the script", color = TextMuted) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedLabelColor = TextSecondary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = EmeraldPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    uiState.sheetSyncMessage?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.saveSheetSync() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.sendSheetSyncTestRow() },
                            enabled = !uiState.sheetSyncTesting,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                            border = BorderStroke(1.dp, EmeraldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.sheetSyncTesting) {
                                CircularProgressIndicator(
                                    color = EmeraldPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("Send test row", fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Setup: open your Sheet → Extensions → Apps Script → paste google-sheet-sync.gs → Deploy → Web app (execute as you, access: anyone) → paste the URL and secret above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Historical Data Import Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Import SMS History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan existing SMS messages in your inbox to construct your historical financial profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (uiState.importStatus) {
                        ImportStatus.IDLE -> {
                            Button(
                                onClick = {
                                    if (uiState.hasSmsPermission) {
                                        viewModel.triggerHistoricalSmsImport()
                                    }
                                },
                                enabled = uiState.hasSmsPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Scan Inbox Now", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                        ImportStatus.RUNNING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Parsing messages...", color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        ImportStatus.SUCCEEDED -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.12f))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Successfully imported history!",
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        ImportStatus.FAILED -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Failed to complete import scan.", color = CoralExpense, modifier = Modifier.padding(vertical = 4.dp))
                                Button(
                                    onClick = { viewModel.triggerHistoricalSmsImport() },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry Scan", color = DarkBackground)
                                }
                            }
                        }
                    }
                }
            }

            // Supported Institutions
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Supported Providers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The application automatically detects and parses records from the following institutions:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val banks = listOf(
                        "M-PESA (Safaricom Money)",
                        "KCB (Kenya Commercial Bank)",
                        "Equity Bank",
                        "Co-operative Bank",
                        "ABSA Bank",
                        "NCBA Bank & Family Bank (MVP filters)"
                    )
                    
                    banks.forEach { bank ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Checked", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = bank, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }

            // Database Actions (Reset)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CoralExpense
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Deleting database will clear all parsed transactions, category mappings, and statistics. Since this is an offline-first app, this data cannot be recovered.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralExpense),
                        border = BorderStroke(1.dp, CoralExpense),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset All Local Data", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Database?", color = TextPrimary) },
            text = { Text("This will permanently delete all parsed transaction records from your device.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = CoralExpense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}
