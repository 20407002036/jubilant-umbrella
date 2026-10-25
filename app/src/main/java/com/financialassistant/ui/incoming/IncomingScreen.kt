package com.financialassistant.ui.incoming

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialassistant.data.entities.TransactionEntity
import com.financialassistant.ui.theme.*
import com.financialassistant.viewmodel.IncomingUiState
import com.financialassistant.viewmodel.IncomingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingScreen(
    viewModel: IncomingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshLines()
    }
    val requestPhonePermission: () -> Unit = {
        phonePermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLines()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Lines", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                uiState.isLoading -> item { LoadingState() }

                !uiState.hasLines -> item {
                    if (uiState.hasPhonePermission) {
                        NoLinesState()
                    } else {
                        PermissionState(onRequestPermission = requestPhonePermission)
                    }
                }

                else -> {
                    item {
                        LineSelectorCard(
                            uiState = uiState,
                            onSelectLine = viewModel::selectLine,
                            onRequestPermission = requestPhonePermission
                        )
                    }

                    if (uiState.transactions.isEmpty()) {
                        item {
                            EmptyState(
                                message = "No messages recorded for this line yet.\nNew messages will appear here automatically."
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "Messages",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        items(uiState.transactions, key = { it.id }) { transaction ->
                            LineMessageRow(transaction)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineSelectorCard(
    uiState: IncomingUiState,
    onSelectLine: (Int) -> Unit,
    onRequestPermission: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Mobile lines",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Messages received and sent on each SIM.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                uiState.lines.forEachIndexed { index, line ->
                    SegmentedButton(
                        selected = line.slot == uiState.selectedSlot,
                        onClick = { onSelectLine(line.slot) },
                        shape = SegmentedButtonDefaults.itemShape(index, uiState.lines.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = EmeraldPrimary.copy(alpha = 0.18f),
                            activeContentColor = TextPrimary,
                            activeBorderColor = EmeraldPrimary,
                            inactiveContainerColor = DarkSurfaceVariant,
                            inactiveContentColor = TextSecondary,
                            inactiveBorderColor = BorderColor
                        ),
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = line.slotLabel,
                                    style = MaterialTheme.typography.labelLarge
                                        .copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    text = line.subtitle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )
                }
            }

            if (!uiState.hasPhonePermission) {
                Spacer(modifier = Modifier.height(12.dp))
                PermissionBanner(onRequestPermission = onRequestPermission)
            }

            uiState.selectedLine?.let { line ->
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderColor)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryStat(label = "Received", amount = uiState.totalReceived, color = TealInflow)
                    SummaryStat(
                        label = "Sent",
                        amount = uiState.totalSent,
                        color = CoralExpense,
                        modifier = Modifier,
                        alignEnd = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = buildString {
                        append(uiState.transactions.size)
                        append(" message")
                        if (uiState.transactions.size != 1) append("s")
                        append(" • ")
                        append(line.numberLabel)
                        if (uiState.selectedIsTracked) append(" • Tracked line")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = String.format("KES %,.2f", amount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun PermissionBanner(onRequestPermission: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SimCard,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Allow phone access to see every SIM line.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRequestPermission) {
            Text("Allow", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LineMessageRow(transaction: TransactionEntity) {
    val isIncoming = transaction.type == "RECEIVED"
    val accent = if (isIncoming) TealInflow else CoralExpense
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateString = formatter.format(Date(transaction.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.recipient,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    if (transaction.senderPhone.isNotBlank()) {
                        append(transaction.senderPhone)
                        append(" • ")
                    }
                    append(messageTypeLabel(transaction.type))
                    append(" • ")
                    append(transaction.provider)
                    append(" • ")
                    append(dateString)
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Code ${transaction.receiptNumber}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = (if (isIncoming) "+" else "-") + String.format("KES %,.2f", transaction.amount),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
    }
}

private fun messageTypeLabel(type: String): String = when (type) {
    "RECEIVED" -> "Received"
    "SENT" -> "Sent"
    "PAYBILL" -> "Paybill"
    "WITHDRAWAL" -> "Withdrawal"
    "AIRTIME" -> "Airtime"
    "UTILITY" -> "Utility"
    else -> "Message"
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = EmeraldPrimary)
    }
}

@Composable
private fun PermissionState(onRequestPermission: () -> Unit) {
    InfoCard(
        icon = Icons.Default.SimCard,
        title = "See all of your lines",
        message = "Allow phone access so the app can list the SIM lines present on this device and show their messages.",
        action = {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Allow access", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun NoLinesState() {
    InfoCard(
        icon = Icons.Default.SimCard,
        title = "No lines found",
        message = "No active SIM lines were found on this device."
    )
}

@Composable
private fun EmptyState(message: String) {
    InfoCard(
        icon = Icons.Default.Payments,
        title = "Nothing here yet",
        message = message
    )
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    message: String,
    action: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(20.dp))
                action()
            }
        }
    }
}
