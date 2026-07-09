package com.financialassistant.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["receiptNumber"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String, // Unique receipt number from provider (e.g. MPESA reference code)
    val provider: String,      // M-PESA, KCB, Equity, ABSA, Coop, etc.
    val type: String,          // INCOME, EXPENSE, TRANSFER, etc.
    val amount: Double,
    val recipient: String,     // Merchant or person
    val balance: Double,       // Account balance after transaction
    val fee: Double,           // Cost of transaction
    val timestamp: Long,       // Time of transaction in milliseconds
    val originalSms: String,   // Raw SMS body
    val category: String = "Uncategorized"
)
