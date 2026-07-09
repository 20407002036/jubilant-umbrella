package com.financialassistant.sms.parsers

import com.financialassistant.sms.models.Transaction
import com.financialassistant.sms.models.TransactionType
import java.util.regex.Pattern

class EquityParser : SmsParser {
    override fun parse(smsBody: String, timestamp: Long): Transaction? {
        val body = smsBody.trim()
        if (!body.contains("EQUITY", ignoreCase = true)) return null

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
            body.contains("debited", ignoreCase = true) -> {
                // Debit: EQUITY Alert: Ksh 10,000.00 debited from A/C XXXXXX7890. Paid to JAVA HOUSE. ...
                val regex = Pattern.compile("Ksh\\s*([\\d,]+\\.?\\d*)\\s+debited\\s+from\\s+[^\\n]+?\\.\\s+(?:Paid\\s+to|Sent\\s+to)\\s+([^\\n\\.]+)", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val merchant = matcher.group(2)?.trim() ?: "Merchant"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "Equity Bank",
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
            body.contains("credited", ignoreCase = true) -> {
                // Credit: EQUITY Alert: Ksh 35,000.00 credited to A/C XXXXXX7890. Received from SALARY. ...
                val regex = Pattern.compile("Ksh\\s*([\\d,]+\\.?\\d*)\\s+credited\\s+to\\s+[^\\n]+?\\.\\s+(?:Received\\s+from|From)\\s+([^\\n\\.]+)", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val merchant = matcher.group(2)?.trim() ?: "Sender"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "Equity Bank",
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
