package com.financialassistant.sms.models

data class Transaction(
    val receiptNumber: String,
    val provider: String,
    val transactionType: TransactionType,
    val amount: Double,
    val merchant: String,
    val balance: Double,
    val fee: Double,
    val timestamp: Long,
    val originalSms: String
)
