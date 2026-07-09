package com.financialassistant.sms.parsers

import com.financialassistant.sms.models.Transaction
import com.financialassistant.sms.models.TransactionType
import java.util.regex.Pattern

class KcbParser : SmsParser {
    override fun parse(smsBody: String, timestamp: Long): Transaction? {
        val body = smsBody.trim()
        if (!body.contains("KCB", ignoreCase = true)) return null

        // Extract Ref
        val refRegex = Pattern.compile("Ref:\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE)
        val refMatcher = refRegex.matcher(body)
        if (!refMatcher.find()) return null
        val receiptNumber = refMatcher.group(1) ?: return null

        // Extract Balance
        val balanceRegex = Pattern.compile("Balance:\\s*Ksh\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
        val balanceMatcher = balanceRegex.matcher(body)
        val balance = if (balanceMatcher.find()) {
            balanceMatcher.group(1)?.cleanAmount() ?: 0.0
        } else {
            0.0
        }

        // Extract Fee
        val feeRegex = Pattern.compile("Fee:\\s*Ksh\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
        val feeMatcher = feeRegex.matcher(body)
        val fee = if (feeMatcher.find()) {
            feeMatcher.group(1)?.cleanAmount() ?: 0.0
        } else {
            0.0
        }

        // Determine transaction type (DR vs CR)
        return when {
            body.contains("DR.", ignoreCase = true) || body.contains("Debit", ignoreCase = true) -> {
                // Debit: DR. Ksh 2,500.00 from A/C ... to NAIVAS
                val regex = Pattern.compile("DR\\.\\s*Ksh\\s*([\\d,]+\\.?\\d*)\\s+from\\s+[^\\n]+?\\s+to\\s+([^\\n\\.]+)", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val merchant = matcher.group(2)?.trim() ?: "Merchant"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "KCB",
                        transactionType = TransactionType.SENT,
                        amount = amount,
                        merchant = merchant,
                        balance = balance,
                        fee = fee,
                        timestamp = timestamp,
                        originalSms = smsBody
                    )
                } else null
            }
            body.contains("CR.", ignoreCase = true) || body.contains("Credit", ignoreCase = true) -> {
                // Credit: CR. Ksh 50,000.00 to A/C ... from SALARY
                val regex = Pattern.compile("CR\\.\\s*Ksh\\s*([\\d,]+\\.?\\d*)\\s+to\\s+[^\\n]+?\\s+from\\s+([^\\n\\.]+)", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val merchant = matcher.group(2)?.trim() ?: "Sender"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "KCB",
                        transactionType = TransactionType.RECEIVED,
                        amount = amount,
                        merchant = merchant,
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

    private fun String.cleanAmount(): Double {
        return this.replace(",", "").toDoubleOrNull() ?: 0.0
    }
}
