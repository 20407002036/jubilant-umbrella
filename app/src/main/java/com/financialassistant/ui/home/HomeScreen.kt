package com.financialassistant.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financialassistant.data.entities.TransactionEntity
import com.financialassistant.ui.theme.*
import com.financialassistant.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
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
                Column {
                    Text(
                        text = "Hello, welcome back",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Financial Assistant",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
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
                // Balance and Overview Card
                item {
                    BalanceOverviewCard(
                        income = uiState.monthlyIncome,
                        expense = uiState.monthlyExpenses
                    )
                }

                // Analytic Summaries (Top Merchant & Frequent Merchant)
                if (uiState.topMerchant != null || uiState.mostFrequentMerchant != null) {
                    item {
                        MerchantMetricsSection(
                            topMerchant = uiState.topMerchant,
                            frequentMerchant = uiState.mostFrequentMerchant
                        )
                    }
                }

                // Category Spending Chart
                if (uiState.spendingByCategory.isNotEmpty()) {
                    item {
                        CategorySpendingCard(spending = uiState.spendingByCategory)
                    }
                }

                // Recent Transactions Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .clickable { onNavigateToTransactions() }
                                .padding(4.dp)
                        )
                    }
                }

                // Recent Transactions List
                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent transactions found.\nGo to Settings to import your SMS history.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(uiState.recentTransactions) { transaction ->
                        TransactionRowItem(
                            transaction = transaction,
                            onClick = { onNavigateToDetails(transaction.receiptNumber) }
                        )
                    }
                }

                // Spacer at bottom
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun BalanceOverviewCard(income: Double, expense: Double) {
    val netBalance = income - expense
    val formattedBalance = String.format("KES %,.2f", netBalance)
    val formattedIncome = String.format("KES %,.2f", income)
    val formattedExpense = String.format("KES %,.2f", expense)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .padding(24.dp)
    ) {
        Text(
            text = "TOTAL NET BALANCE",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Text(
            text = formattedBalance,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = if (netBalance >= 0) TextPrimary else CoralExpense,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Divider(
            color = BorderColor,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Income Side
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealInflow.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Income",
                        tint = TealInflow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Income", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = formattedIncome,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = TealInflow
                    )
                }
            }

            // Expense Side
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CoralExpense.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Expense",
                        tint = CoralExpense,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Expenses", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = formattedExpense,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = CoralExpense
                    )
                }
            }
        }
    }
}

@Composable
fun MerchantMetricsSection(
    topMerchant: Pair<String, Double>?,
    frequentMerchant: Pair<String, Int>?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (topMerchant != null) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.LocalMall,
                        contentDescription = "Largest Spend",
                        tint = AccentOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Largest Expense", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = topMerchant.first,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                    Text(
                        text = String.format("KES %,.2f", topMerchant.second),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        if (frequentMerchant != null) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Frequent Merchant",
                        tint = EmeraldSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Top Payee", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = frequentMerchant.first,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                    Text(
                        text = "${frequentMerchant.second} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySpendingCard(spending: List<com.financialassistant.viewmodel.CategorySpend>) {
    val totalExpense = spending.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .padding(20.dp)
    ) {
        Text(
            text = "SPENDING BY CATEGORY",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        spending.take(4).forEach { catSpend ->
            val percentage = if (totalExpense > 0) catSpend.amount / totalExpense else 0.0
            val color = try {
                Color(android.graphics.Color.parseColor(catSpend.colorHex))
            } catch (e: Exception) {
                TextMuted
            }

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = catSpend.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = String.format("KES %,.2f", catSpend.amount),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = percentage.toFloat())
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }
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
        // Icon Box
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

        // Merchant and Date
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

        // Amount & Category Badge
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
