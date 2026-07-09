package com.financialassistant.sms.parsers

import com.financialassistant.sms.models.Transaction
import com.financialassistant.sms.models.TransactionType
import java.util.regex.Pattern

class AbsaParser : SmsParser {
    override fun parse(smsBody: String, timestamp: Long): Transaction? {
        val body = smsBody.trim()
        if (!body.contains("ABSA", ignoreCase = true)) return null

        // Extract Ref
        val refRegex = Pattern.compile("Ref:\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE)
        val refMatcher = refRegex.matcher(body)
        if (!refMatcher.find()) return null
        val receiptNumber = refMatcher.group(1) ?: return null

        // Extract Balance
        val balanceRegex = Pattern.compile("Bal:\\s*Ksh\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
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

        // Determine transaction type
        return when {
            body.contains("DR:", ignoreCase = true) || body.contains("Debit", ignoreCase = true) -> {
                // Debit: ABSA: Txn DR: Ksh 4,000.00 from A/C ...9080. Merchant: UBER.
                val regex = Pattern.compile("DR:\\s*Ksh\\s*([\\d,]+\\.?\\d*)\\s+from\\s+[^\\n]+?\\.\\s+Merchant:\\s*([^\\n\\.]+)", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val merchant = matcher.group(2)?.trim() ?: "ABSA Merchant"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "ABSA",
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
            body.contains("CR:", ignoreCase = true) || body.contains("Credit", ignoreCase = true) -> {
                // Credit: ABSA: Txn CR: Ksh 80,000.00 to A/C ...9080. Source: SALARY.
                val regex = Pattern.compile("CR:\\s*Ksh\\s*([\\d,]+\\.?\\d*)\\s+to\\s+[^\\n]+?\\.\\s+Source:\\s*([^\\n\\.]+)", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val merchant = matcher.group(2)?.trim() ?: "ABSA Source"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "ABSA",
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
