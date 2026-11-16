package com.financialassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financialassistant.data.entities.CategoryEntity
import com.financialassistant.data.entities.TransactionEntity
import com.financialassistant.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class ReportType(val label: String) {
    EXPENSES("Expenses"),
    INCOME("Income"),
    TRANSFERS("Transfers"),
    BILLS("Bills"),
    SAVINGS("Savings")
}

enum class TimeRange(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month")
}

data class ChartBarData(
    val label: String,
    val amount: Double,
    val isCurrentPeriod: Boolean = false
)

data class CategorySpend(
    val name: String,
    val amount: Double,
    val colorHex: String
)

data class HomeUiState(
    val reportType: ReportType = ReportType.EXPENSES,
    val timeRange: TimeRange = TimeRange.WEEK,
    val selectedBarIndex: Int = 0,
    val chartData: List<ChartBarData> = emptyList(),
    val periodLabel: String = "",
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val spendingByCategory: List<CategorySpend> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _reportType = MutableStateFlow(ReportType.EXPENSES)
    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    private val _selectedBarIndex = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllTransactions().map { it as Any },
        repository.getAllCategories().map { it as Any },
        repository.getIncomingSumSince(startOfMonth).map { it as Any },
        repository.getOutgoingSumSince(startOfMonth).map { it as Any },
        _reportType.map { it as Any },
        _timeRange.map { it as Any },
        _selectedBarIndex.map { it as Any }
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val transactions = values[0] as List<TransactionEntity>
        val categories = values[1] as List<CategoryEntity>
        val incoming = values[2] as Double
        val outgoing = values[3] as Double
        val reportType = values[4] as ReportType
        val timeRange = values[5] as TimeRange
        val selectedBarIndex = values[6] as Int

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

        val filteredByType = filterByReportType(transactions, reportType)
        val chartData = computeChartData(filteredByType, timeRange)
        val safeSelectedIndex = selectedBarIndex.coerceIn(0, (chartData.size - 1).coerceAtLeast(0))
        val periodTransactions = getTransactionsForBar(filteredByType, chartData, safeSelectedIndex, timeRange)
        val periodLabel = computePeriodLabel(timeRange)

        HomeUiState(
            reportType = reportType,
            timeRange = timeRange,
            selectedBarIndex = safeSelectedIndex,
            chartData = chartData,
            periodLabel = periodLabel,
            filteredTransactions = periodTransactions,
            monthlyIncome = incoming,
            monthlyExpenses = outgoing,
            spendingByCategory = categorySpendList,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun setReportType(type: ReportType) {
        _reportType.value = type
        _selectedBarIndex.value = 0
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
        _selectedBarIndex.value = 0
    }

    fun setSelectedBar(index: Int) {
        _selectedBarIndex.value = index
    }

    private fun filterByReportType(
        transactions: List<TransactionEntity>,
        type: ReportType
    ): List<TransactionEntity> {
        return when (type) {
            ReportType.EXPENSES -> transactions.filter { it.type != "RECEIVED" }
            ReportType.INCOME -> transactions.filter { it.type == "RECEIVED" }
            ReportType.TRANSFERS -> transactions.filter {
                it.category.equals("Transfer", ignoreCase = true) ||
                it.recipient.uppercase().contains("TRANSFER")
            }
            ReportType.BILLS -> transactions.filter {
                it.category.equals("Utilities", ignoreCase = true)
            }
            ReportType.SAVINGS -> transactions.filter {
                it.category.equals("Savings", ignoreCase = true)
            }
        }
    }

    private fun computeChartData(
        transactions: List<TransactionEntity>,
        timeRange: TimeRange
    ): List<ChartBarData> {
        val calendar = Calendar.getInstance()
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        return when (timeRange) {
            TimeRange.DAY -> {
                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)

                val startOfWeek = Calendar.getInstance()
                startOfWeek.time = today.time
                startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)
                if (startOfWeek.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    startOfWeek.add(Calendar.DAY_OF_WEEK, 1)
                }

                val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
                val todayIndex = when (todayDayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }

                dayNames.mapIndexed { index, name ->
                    val dayStart = Calendar.getInstance()
                    dayStart.time = startOfWeek.time
                    dayStart.add(Calendar.DAY_OF_YEAR, index)
                    dayStart.set(Calendar.HOUR_OF_DAY, 0)
                    dayStart.set(Calendar.MINUTE, 0)
                    dayStart.set(Calendar.SECOND, 0)
                    dayStart.set(Calendar.MILLISECOND, 0)

                    val dayEnd = Calendar.getInstance()
                    dayEnd.time = dayStart.time
                    dayEnd.add(Calendar.DAY_OF_YEAR, 1)

                    val dayTotal = transactions
                        .filter { it.timestamp >= dayStart.timeInMillis && it.timestamp < dayEnd.timeInMillis }
                        .sumOf { it.amount }

                    ChartBarData(
                        label = name,
                        amount = dayTotal,
                        isCurrentPeriod = index == todayIndex
                    )
                }
            }

            TimeRange.WEEK -> {
                val now = Calendar.getInstance()
                val weeks = (3 downTo 0).map { weeksAgo ->
                    val weekStart = Calendar.getInstance()
                    weekStart.time = now.time
                    weekStart.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
                    weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
                    weekStart.set(Calendar.HOUR_OF_DAY, 0)
                    weekStart.set(Calendar.MINUTE, 0)
                    weekStart.set(Calendar.SECOND, 0)
                    weekStart.set(Calendar.MILLISECOND, 0)

                    val weekEnd = Calendar.getInstance()
                    weekEnd.time = weekStart.time
                    weekEnd.add(Calendar.WEEK_OF_YEAR, 1)

                    val weekTotal = transactions
                        .filter { it.timestamp >= weekStart.timeInMillis && it.timestamp < weekEnd.timeInMillis }
                        .sumOf { it.amount }

                    val weekNum = 4 - weeksAgo
                    ChartBarData(
                        label = "W$weekNum",
                        amount = weekTotal,
                        isCurrentPeriod = weeksAgo == 0
                    )
                }
                weeks
            }

            TimeRange.MONTH -> {
                val now = Calendar.getInstance()
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

                (5 downTo 0).map { monthsAgo ->
                    val monthStart = Calendar.getInstance()
                    monthStart.time = now.time
                    monthStart.add(Calendar.MONTH, -monthsAgo)
                    monthStart.set(Calendar.DAY_OF_MONTH, 1)
                    monthStart.set(Calendar.HOUR_OF_DAY, 0)
                    monthStart.set(Calendar.MINUTE, 0)
                    monthStart.set(Calendar.SECOND, 0)
                    monthStart.set(Calendar.MILLISECOND, 0)

                    val monthEnd = Calendar.getInstance()
                    monthEnd.time = monthStart.time
                    monthEnd.add(Calendar.MONTH, 1)

                    val monthTotal = transactions
                        .filter { it.timestamp >= monthStart.timeInMillis && it.timestamp < monthEnd.timeInMillis }
                        .sumOf { it.amount }

                    val monthIndex = monthStart.get(Calendar.MONTH)
                    ChartBarData(
                        label = monthNames[monthIndex],
                        amount = monthTotal,
                        isCurrentPeriod = monthsAgo == 0
                    )
                }
            }
        }
    }

    private fun getTransactionsForBar(
        transactions: List<TransactionEntity>,
        chartData: List<ChartBarData>,
        barIndex: Int,
        timeRange: TimeRange
    ): List<TransactionEntity> {
        if (barIndex >= chartData.size) return emptyList()

        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()

        return when (timeRange) {
            TimeRange.DAY -> {
                val startOfWeek = Calendar.getInstance()
                startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
                startOfWeek.set(Calendar.MINUTE, 0)
                startOfWeek.set(Calendar.SECOND, 0)
                startOfWeek.set(Calendar.MILLISECOND, 0)
                startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)
                if (startOfWeek.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    startOfWeek.add(Calendar.DAY_OF_WEEK, 1)
                }

                val dayStart = Calendar.getInstance()
                dayStart.time = startOfWeek.time
                dayStart.add(Calendar.DAY_OF_YEAR, barIndex)

                val dayEnd = Calendar.getInstance()
                dayEnd.time = dayStart.time
                dayEnd.add(Calendar.DAY_OF_YEAR, 1)

                transactions.filter {
                    it.timestamp >= dayStart.timeInMillis && it.timestamp < dayEnd.timeInMillis
                }.sortedByDescending { it.timestamp }
            }

            TimeRange.WEEK -> {
                val weeksAgo = 3 - barIndex
                val weekStart = Calendar.getInstance()
                weekStart.time = now.time
                weekStart.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
                weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
                weekStart.set(Calendar.HOUR_OF_DAY, 0)
                weekStart.set(Calendar.MINUTE, 0)
                weekStart.set(Calendar.SECOND, 0)
                weekStart.set(Calendar.MILLISECOND, 0)

                val weekEnd = Calendar.getInstance()
                weekEnd.time = weekStart.time
                weekEnd.add(Calendar.WEEK_OF_YEAR, 1)

                transactions.filter {
                    it.timestamp >= weekStart.timeInMillis && it.timestamp < weekEnd.timeInMillis
                }.sortedByDescending { it.timestamp }
            }

            TimeRange.MONTH -> {
                val monthsAgo = 5 - barIndex
                val monthStart = Calendar.getInstance()
                monthStart.time = now.time
                monthStart.add(Calendar.MONTH, -monthsAgo)
                monthStart.set(Calendar.DAY_OF_MONTH, 1)
                monthStart.set(Calendar.HOUR_OF_DAY, 0)
                monthStart.set(Calendar.MINUTE, 0)
                monthStart.set(Calendar.SECOND, 0)
                monthStart.set(Calendar.MILLISECOND, 0)

                val monthEnd = Calendar.getInstance()
                monthEnd.time = monthStart.time
                monthEnd.add(Calendar.MONTH, 1)

                transactions.filter {
                    it.timestamp >= monthStart.timeInMillis && it.timestamp < monthEnd.timeInMillis
                }.sortedByDescending { it.timestamp }
            }
        }
    }

    private fun computePeriodLabel(timeRange: TimeRange): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    companion object {
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
    }
}
