package com.financialassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financialassistant.data.entities.CategoryEntity
import com.financialassistant.data.entities.TransactionEntity
import com.financialassistant.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class CategorySpend(
    val name: String,
    val amount: Double,
    val colorHex: String
)

data class HomeUiState(
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val spendingByCategory: List<CategorySpend> = emptyList(),
    val topMerchant: Pair<String, Double>? = null,
    val mostFrequentMerchant: Pair<String, Int>? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val startOfMonth: Long
        get() {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllTransactions(),
        repository.getAllCategories(),
        repository.getIncomingSumSince(startOfMonth),
        repository.getOutgoingSumSince(startOfMonth)
    ) { transactions, categories, incoming, outgoing ->
        
        // 1. Recent Transactions (First 5)
        val recent = transactions.take(5)

        // 2. Spending by Category (excluding Income)
        val categoryColorMap = categories.associate { it.name to it.colorHex }
        val categorySpendList = transactions
            .filter { it.type != "RECEIVED" && it.category != "Income" }
            .groupBy { it.category }
            .map { (catName, txs) ->
                CategorySpend(
                    name = catName,
                    amount = txs.sumOf { it.amount },
                    colorHex = categoryColorMap[catName] ?: "#757575"
                )
            }
            .sortedByDescending { it.amount }

        // 3. Top Merchant (Highest total spend)
        val merchantSpends = transactions
            .filter { it.type != "RECEIVED" }
            .groupBy { it.recipient }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val topMerchant = merchantSpends.maxByOrNull { it.value }?.toPair()

        // 4. Most Frequent Merchant (Highest transaction count)
        val merchantCounts = transactions
            .filter { it.type != "RECEIVED" }
            .groupBy { it.recipient }
            .mapValues { entry -> entry.value.size }
        val mostFrequent = merchantCounts.maxByOrNull { it.value }?.toPair()

        HomeUiState(
            monthlyIncome = incoming,
            monthlyExpenses = outgoing,
            recentTransactions = recent,
            spendingByCategory = categorySpendList,
            topMerchant = topMerchant,
            mostFrequentMerchant = mostFrequent,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
