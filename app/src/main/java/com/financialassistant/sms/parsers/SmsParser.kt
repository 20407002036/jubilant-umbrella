package com.financialassistant.sms.parsers

import com.financialassistant.sms.models.Transaction

interface SmsParser {
    /**
     * Parses a raw SMS message body and returns a structured Transaction,
     * or null if the message doesn't match the expected format or isn't a transaction.
     */
    fun parse(smsBody: String, timestamp: Long): Transaction?
}
