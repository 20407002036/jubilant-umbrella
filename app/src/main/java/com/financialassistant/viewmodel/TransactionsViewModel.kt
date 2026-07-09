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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC
}

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedRecipient: String? = null,
    val sortOption: SortOption = SortOption.DATE_DESC
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedRecipient = MutableStateFlow<String?>(null)
    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.getAllTransactions(),
        repository.getAllCategories(),
        _searchQuery,
        _selectedCategory,
        _selectedRecipient,
        _sortOption
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = flows[1] as List<CategoryEntity>
        val search = flows[2] as String
        val category = flows[3] as String?
        val recipient = flows[4] as String?
        val sort = flows[5] as SortOption
        
        var filteredList = transactions

        // Search Filter
        if (search.isNotBlank()) {
            filteredList = filteredList.filter {
                it.recipient.contains(search, ignoreCase = true) ||
                it.receiptNumber.contains(search, ignoreCase = true) ||
                it.category.contains(search, ignoreCase = true) ||
                it.provider.contains(search, ignoreCase = true)
            }
        }

        // Category Filter
        if (category != null) {
            filteredList = filteredList.filter { it.category == category }
        }

        // Recipient Filter
        if (recipient != null) {
            filteredList = filteredList.filter { it.recipient == recipient }
        }

        // Sorting
        filteredList = when (sort) {
            SortOption.DATE_DESC -> filteredList.sortedByDescending { it.timestamp }
            SortOption.DATE_ASC -> filteredList.sortedBy { it.timestamp }
            SortOption.AMOUNT_DESC -> filteredList.sortedByDescending { it.amount }
            SortOption.AMOUNT_ASC -> filteredList.sortedBy { it.amount }
        }

        TransactionsUiState(
            transactions = filteredList,
            categories = categories,
            searchQuery = search,
            selectedCategory = category,
            selectedRecipient = recipient,
            sortOption = sort
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun onRecipientSelected(recipient: String?) {
        _selectedRecipient.value = recipient
    }

    fun onSortOptionChanged(option: SortOption) {
        _sortOption.value = option
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
