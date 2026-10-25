package com.financialassistant.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialassistant.data.entities.TransactionEntity
import com.financialassistant.ui.theme.*
import com.financialassistant.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToIncoming: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reports",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateToIncoming,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallReceived,
                            contentDescription = "My lines",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Report Type Dropdown
                item {
                    ReportTypeDropdown(
                        selectedType = uiState.reportType,
                        onTypeSelected = { viewModel.setReportType(it) }
                    )
                }

                // Time Range Selector
                item {
                    TimeRangeSelector(
                        selectedRange = uiState.timeRange,
                        onRangeSelected = { viewModel.setTimeRange(it) }
                    )
                }

                // Bar Chart
                item {
                    BarChartCard(
                        chartData = uiState.chartData,
                        selectedIndex = uiState.selectedBarIndex,
                        onBarSelected = { viewModel.setSelectedBar(it) }
                    )
                }

                // Period Label
                item {
                    Text(
                        text = uiState.periodLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Transaction Breakdown Header
                item {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                // Transaction List or Empty State
                if (uiState.filteredTransactions.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    itemsIndexed(uiState.filteredTransactions) { _, transaction ->
                        TransactionRowItem(
                            transaction = transaction,
                            onClick = { onNavigateToDetails(transaction.receiptNumber) }
                        )
                    }
                }

                // Bottom spacer
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportTypeDropdown(
    selectedType: ReportType,
    onTypeSelected: (ReportType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedType.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Report Type", color = TextSecondary) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedLabelColor = TextSecondary,
                unfocusedLabelColor = TextSecondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(16.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
            ) {
                ReportType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = type.label,
                                color = if (type == selectedType) EmeraldPrimary else TextPrimary
                            )
                        },
                        onClick = {
                            onTypeSelected(type)
                            expanded = false
                        },
                        leadingIcon = if (type == selectedType) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimeRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            val bgColor by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(200),
                label = "bg"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        lerp(DarkSurfaceVariant, FilterActiveBg, bgColor)
                    )
                    .clickable { onRangeSelected(range) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) TextPrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
fun BarChartCard(
    chartData: List<ChartBarData>,
    selectedIndex: Int,
    onBarSelected: (Int) -> Unit
) {
    val maxAmount = chartData.maxOfOrNull { it.amount } ?: 1.0
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(chartData) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(600))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Selected value badge
            if (selectedIndex < chartData.size && chartData[selectedIndex].amount > 0) {
                val badgeText = String.format("KES %,.0f", chartData[selectedIndex].amount)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChartBadgeBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Chart Canvas with clickable overlay
            val barColor = ChartBarBlue
            val barSelectedColor = ChartBarBlueSelected
            val barInactiveColor = ChartBarDefault
            val labelColor = TextSecondary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (chartData.isEmpty()) return@Canvas

                    val barCount = chartData.size
                    val spacing = 12.dp.toPx()
                    val totalSpacing = spacing * (barCount + 1)
                    val barWidth = (size.width - totalSpacing) / barCount
                    val chartHeight = size.height - 30.dp.toPx()
                    val cornerRadius = 8.dp.toPx()

                    chartData.forEachIndexed { index, barData ->
                        val normalizedHeight = if (maxAmount > 0) {
                            (barData.amount / maxAmount * chartHeight * animatedProgress.value).toFloat()
                        } else 0f

                        val x = spacing + index * (barWidth + spacing)
                        val y = chartHeight - normalizedHeight

                        val color = when {
                            index == selectedIndex -> barSelectedColor
                            barData.amount > 0 -> barColor
                            else -> barInactiveColor
                        }

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, normalizedHeight.coerceAtLeast(4.dp.toPx())),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )

                        if (index == selectedIndex && normalizedHeight > 0) {
                            drawRoundRect(
                                color = barSelectedColor.copy(alpha = 0.3f),
                                topLeft = Offset(x - 2.dp.toPx(), y - 2.dp.toPx()),
                                size = Size(barWidth + 4.dp.toPx(), normalizedHeight + 4.dp.toPx()),
                                cornerRadius = CornerRadius(cornerRadius + 2.dp.toPx(), cornerRadius + 2.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }

                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            paint.color = labelColor.toArgb()
                            drawText(
                                barData.label,
                                x + barWidth / 2,
                                size.height,
                                paint
                            )
                        }
                    }
                }

                // Clickable overlay for each bar
                if (chartData.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        chartData.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onBarSelected(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
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
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No financial activity recorded\nfor this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TransactionRowItem(transaction: TransactionEntity, onClick: () -> Unit) {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateString = formatter.format(Date(transaction.timestamp))
    val isInflow = transaction.type == "RECEIVED"

    val icon: ImageVector
    val iconColor: Color
    val iconBg: Color

    if (isInflow) {
        icon = Icons.Default.ArrowDownward
        iconColor = TealInflow
        iconBg = TealInflow.copy(alpha = 0.15f)
    } else {
        icon = when (transaction.category) {
            "Groceries" -> Icons.Default.ShoppingCart
            "Food" -> Icons.Default.Restaurant
            "Transport" -> Icons.Default.DirectionsCar
            "Utilities" -> Icons.Default.FlashOn
            "Airtime" -> Icons.Default.PhoneAndroid
            "Savings" -> Icons.Default.AccountBalanceWallet
            "Entertainment" -> Icons.Default.Movie
            else -> Icons.Default.ReceiptLong
        }
        iconColor = CoralExpense
        iconBg = CoralExpense.copy(alpha = 0.15f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = transaction.category,
                tint = iconColor,
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
                text = "$dateString • ${transaction.provider}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = String.format("${if (isInflow) "+" else "-"}KES %,.2f", transaction.amount),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isInflow) TealInflow else TextPrimary
            )
            Text(
                text = transaction.category,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary
            )
        }
    }
}
