package com.financialassistant.sms.parsers

import com.financialassistant.sms.models.Transaction
import com.financialassistant.sms.models.TransactionType
import java.util.regex.Pattern

class MpesaParser : SmsParser {

    override fun parse(smsBody: String, timestamp: Long): Transaction? {
        val body = smsBody.trim()
        if (!body.contains("Confirmed") && !body.contains("M-PESA")) return null

        // Extract receipt number (usually 10 alphanumeric characters at the beginning)
        val receiptRegex = Pattern.compile("^([A-Z0-9]{10})\\s+Confirmed\\.")
        val receiptMatcher = receiptRegex.matcher(body)
        if (!receiptMatcher.find()) return null
        val receiptNumber = receiptMatcher.group(1) ?: return null

        // Extract Balance
        val balanceRegex = Pattern.compile("balance\\s+is\\s+Ksh\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
        val balanceMatcher = balanceRegex.matcher(body)
        val balance = if (balanceMatcher.find()) {
            balanceMatcher.group(1)?.cleanAmount() ?: 0.0
        } else {
            0.0
        }

        // Extract Fee
        val feeRegex = Pattern.compile("Transaction\\s+cost,\\s+Ksh\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
        val feeMatcher = feeRegex.matcher(body)
        val fee = if (feeMatcher.find()) {
            feeMatcher.group(1)?.cleanAmount() ?: 0.0
        } else {
            0.0
        }

        // Determine transaction type and details
        return when {
            body.contains("received", ignoreCase = true) -> {
                // Received pattern: QX87654321 Confirmed. You have received Ksh1,200.00 from SARAH WANJIKU 0787654321 on ...
                // The sender segment runs up to " on <date>" (or ". New M-PESA balance") and may contain
                // the sender's phone number, which may be masked (e.g. 0702***507).
                val regex = Pattern.compile(
                    "received\\s+Ksh\\s*([\\d,]+\\.?\\d*)\\s+from\\s+(.+?)(?:\\s+on\\s+|\\.\\s*New\\s+M-PESA|\\s*$)",
                    Pattern.CASE_INSENSITIVE
                )
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val senderSegment = matcher.group(2)?.trim() ?: ""
                    val (senderName, senderPhone) = splitSenderSegment(senderSegment)
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "M-PESA",
                        transactionType = TransactionType.RECEIVED,
                        amount = amount,
                        merchant = senderName.ifBlank { "Unknown Sender" },
                        balance = balance,
                        fee = fee,
                        timestamp = timestamp,
                        originalSms = smsBody,
                        senderPhone = senderPhone
                    )
                } else null
            }
            body.contains("sent to", ignoreCase = true) && body.contains("airtime", ignoreCase = true) -> {
                // Airtime pattern: QA11223344 Confirmed. Ksh100.00 sent to SAFARICOM airtime for number 0712345678 on ...
                val regex = Pattern.compile("Ksh\\s*([\\d,]+\\.?\\d*)\\s+sent\\s+to\\s+([^\\n]+)\\s+airtime", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val operator = matcher.group(2)?.trim() ?: "Safaricom"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "M-PESA",
                        transactionType = TransactionType.AIRTIME,
                        amount = amount,
                        merchant = "$operator Airtime",
                        balance = balance,
                        fee = fee,
                        timestamp = timestamp,
                        originalSms = smsBody
                    )
                } else null
            }
            body.contains("sent to", ignoreCase = true) -> {
                // Sent pattern: QW12345678 Confirmed. Ksh500.00 sent to JOHN MWANGI 0712345678 on ...
                val regex = Pattern.compile("Ksh\\s*([\\d,]+\\.?\\d*)\\s+sent\\s+to\\s+([^\\n\\d]+)", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val recipientName = matcher.group(2)?.trim() ?: "Unknown Recipient"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "M-PESA",
                        transactionType = TransactionType.SENT,
                        amount = amount,
                        merchant = recipientName,
                        balance = balance,
                        fee = fee,
                        timestamp = timestamp,
                        originalSms = smsBody
                    )
                } else null
            }
            body.contains("paid to", ignoreCase = true) -> {
                // Paid pattern: QY56781234 Confirmed. Ksh1,850.00 paid to NAIVAS SUPERMARKET on ...
                val regex = Pattern.compile("Ksh\\s*([\\d,]+\\.?\\d*)\\s+paid\\s+to\\s+([^\\n]+?)\\s+on", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val merchantName = matcher.group(2)?.trim() ?: "Merchant"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "M-PESA",
                        transactionType = TransactionType.PAYBILL,
                        amount = amount,
                        merchant = merchantName,
                        balance = balance,
                        fee = fee,
                        timestamp = timestamp,
                        originalSms = smsBody
                    )
                } else null
            }
            body.contains("withdrawn from", ignoreCase = true) -> {
                // Withdrawal pattern: QZ98765432 Confirmed. Ksh1,000.00 withdrawn from 123456 - Agent Name on ...
                val regex = Pattern.compile("Ksh\\s*([\\d,]+\\.?\\d*)\\s+withdrawn\\s+from\\s+([^\\n]+?)\\s+on", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val agentName = matcher.group(2)?.trim() ?: "Agent"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "M-PESA",
                        transactionType = TransactionType.WITHDRAWAL,
                        amount = amount,
                        merchant = agentName,
                        balance = balance,
                        fee = fee,
                        timestamp = timestamp,
                        originalSms = smsBody
                    )
                } else null
            }
            else -> null
        }
    }

    /**
     * Splits the text between "from" and " on <date>" into the sender's display name and,
     * when present, their phone number. Handles masked numbers such as "0702***507".
     */
    private fun splitSenderSegment(segment: String): Pair<String, String> {
        val trimmed = segment.trim()
        if (trimmed.isEmpty()) return "Unknown Sender" to ""

        val tokens = trimmed.split(Regex("\\s+"))
        val lastToken = tokens.last()
        return if (PHONE_TOKEN.matches(lastToken)) {
            tokens.dropLast(1).joinToString(" ").trim() to lastToken
        } else {
            trimmed to ""
        }
    }

    private fun String.cleanAmount(): Double {
        return this.replace(",", "").toDoubleOrNull() ?: 0.0
    }

    private companion object {
        // Kenyan MSISDN, optionally masked: 07XX..., +2547XX..., 0702***507
        val PHONE_TOKEN = Regex("^(?:\\+?254|0)[\\d*]{6,13}$")
    }
}
