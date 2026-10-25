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
    val originalSms: String,
    /** Phone number of the counterparty (sender for RECEIVED), when present in the SMS. */
    val senderPhone: String = "",
    /** SIM slot the notification arrived on (0 = SIM 1, 1 = SIM 2, -1 = unknown). */
    val simSlot: Int = -1
)
