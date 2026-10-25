package com.financialassistant.data.repository

import com.financialassistant.data.entities.CategoryEntity
import com.financialassistant.data.entities.RecipientEntity
import com.financialassistant.data.entities.TransactionEntity
import com.financialassistant.sms.models.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>
    fun getTransactionsByRecipient(recipient: String): Flow<List<TransactionEntity>>
    fun getBySim(simSlot: Int): Flow<List<TransactionEntity>>

    fun getIncomingSumSince(startTime: Long): Flow<Double>
    fun getOutgoingSumSince(startTime: Long): Flow<Double>

    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getAllRecipients(): Flow<List<RecipientEntity>>

    suspend fun insertTransaction(transaction: Transaction): Boolean
    suspend fun insertTransactions(transactions: List<Transaction>): Int
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun clearAllData()
}
