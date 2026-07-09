package com.financialassistant.data.repository

import com.financialassistant.data.database.AppDatabase
import com.financialassistant.data.entities.CategoryEntity
import com.financialassistant.data.entities.RecipientEntity
import com.financialassistant.data.entities.TransactionEntity
import com.financialassistant.sms.models.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : TransactionRepository {

    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val recipientDao = database.recipientDao()

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    override fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    override fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCategory(category)

    override fun getTransactionsByRecipient(recipient: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByRecipient(recipient)

    override fun getIncomingSumSince(startTime: Long): Flow<Double> =
        transactionDao.getIncomingSumSince(startTime).map { it ?: 0.0 }

    override fun getOutgoingSumSince(startTime: Long): Flow<Double> =
        transactionDao.getOutgoingSumSince(startTime).map { it ?: 0.0 }

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override fun getAllRecipients(): Flow<List<RecipientEntity>> =
        recipientDao.getAllRecipients()

    override suspend fun insertTransaction(transaction: Transaction): Boolean {
        // 1. Validate Transaction
        if (transaction.receiptNumber.isBlank()) return false
        if (transaction.amount <= 0.0) return false
        if (transaction.timestamp <= 0) return false
        
        // Deduplication Check
        if (transactionDao.getTransactionByReceipt(transaction.receiptNumber) != null) {
            return false
        }

        // 2. Auto-categorize based on Merchant name
        val category = autoCategoryForMerchant(transaction.merchant)

        // 3. Save transaction to Room Database
        val transactionEntity = TransactionEntity(
            receiptNumber = transaction.receiptNumber,
            provider = transaction.provider,
            type = transaction.transactionType.name,
            amount = transaction.amount,
            recipient = transaction.merchant,
            balance = transaction.balance,
            fee = transaction.fee,
            timestamp = transaction.timestamp,
            originalSms = transaction.originalSms,
            category = category
        )
        val rowId = transactionDao.insertTransaction(transactionEntity)
        if (rowId == -1L) return false

        // 4. Update or insert recipient metrics (Recipient Recognition)
        val recipientName = transaction.merchant
        val existingRecipient = recipientDao.getRecipientByName(recipientName)
        if (existingRecipient == null) {
            recipientDao.insertRecipient(
                RecipientEntity(
                    name = recipientName,
                    totalTransactionsCount = 1,
                    totalAmountSpent = transaction.amount,
                    defaultCategory = category
                )
            )
        } else {
            recipientDao.updateRecipient(
                existingRecipient.copy(
                    totalTransactionsCount = existingRecipient.totalTransactionsCount + 1,
                    totalAmountSpent = existingRecipient.totalAmountSpent + transaction.amount,
                    defaultCategory = existingRecipient.defaultCategory ?: category
                )
            )
        }
        return true
    }

    override suspend fun insertTransactions(transactions: List<Transaction>): Int {
        var insertedCount = 0
        for (transaction in transactions) {
            if (insertTransaction(transaction)) {
                insertedCount++
            }
        }
        return insertedCount
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    override suspend fun clearAllData() {
        database.clearAllTables()
    }

    private fun autoCategoryForMerchant(merchant: String): String {
        val m = merchant.uppercase().trim()
        return when {
            m.contains("NAIVAS") || m.contains("CARREFOUR") || m.contains("SUPERMARKET") -> "Groceries"
            m.contains("JAVA HOUSE") || m.contains("RESTAURANT") || m.contains("CAFE") || m.contains("FOOD") || m.contains("KFC") -> "Food"
            m.contains("UBER") || m.contains("BOLT") || m.contains("TAXI") || m.contains("TRANSPORT") || m.contains("MATATU") -> "Transport"
            m.contains("KPLC") || m.contains("TOKEN") || m.contains("WATER") || m.contains("POWER") || m.contains("UTILITIES") -> "Utilities"
            m.contains("SAFARICOM") || m.contains("AIRTIME") || m.contains("AIRTEL") || m.contains("TELKOM") || m.contains("RECHARGE") -> "Airtime"
            m.contains("SALARY") || m.contains("INCOME") || m.contains("PAYSLIP") || m.contains("CREDITED") || m.contains("DEPOSIT") -> "Income"
            m.contains("SAVING") || m.contains("INVEST") || m.contains("SACCO") || m.contains("FUND") -> "Savings"
            m.contains("NETFLIX") || m.contains("SHOWMAX") || m.contains("CINEMA") || m.contains("SPORTS") || m.contains("GAME") || m.contains("PLAY") -> "Entertainment"
            else -> "Uncategorized"
        }
    }
}
