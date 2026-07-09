package com.financialassistant.sms.models

enum class TransactionType {
    SENT,        // Money transferred out to another person/number
    RECEIVED,    // Money incoming (salary, friend transfer)
    PAYBILL,     // Bill payment (shopping, paybill)
    WITHDRAWAL,  // Cash out at agent or ATM
    UTILITY,     // Standard utility payment (power, water)
    AIRTIME,     // Mobile recharge
    UNKNOWN
}
