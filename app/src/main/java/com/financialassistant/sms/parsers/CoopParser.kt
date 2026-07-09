package com.financialassistant.sms.parsers

import com.financialassistant.sms.models.Transaction
import com.financialassistant.sms.models.TransactionType
import java.util.regex.Pattern

class CoopParser : SmsParser {
    override fun parse(smsBody: String, timestamp: Long): Transaction? {
        val body = smsBody.trim()
        if (!body.contains("Co-op", ignoreCase = true) && !body.contains("Coop", ignoreCase = true)) return null

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
            body.contains("withdrawn", ignoreCase = true) || body.contains("debited", ignoreCase = true) || body.contains("paid", ignoreCase = true) -> {
                // Debit: Co-op Bank Alert: Ksh 1,500.00 withdrawn from A/C XXXXXX9988. Paid to KPLC.
                val regex = Pattern.compile("Ksh\\s*([\\d,]+\\.?\\d*)\\s+(?:withdrawn|debited|paid)\\s+from\\s+[^\\n]+?(?:\\.\\s+Paid\\s+to\\s+([^\\n\\.]+))?", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val merchant = matcher.group(2)?.trim() ?: "Co-op Bank Merchant"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "Co-operative Bank",
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
            body.contains("deposited", ignoreCase = true) || body.contains("credited", ignoreCase = true) || body.contains("received", ignoreCase = true) -> {
                // Credit: Co-op Bank Alert: Ksh 25,000.00 deposited to A/C XXXXXX9988. Received from COOP.
                val regex = Pattern.compile("Ksh\\s*([\\d,]+\\.?\\d*)\\s+(?:deposited|credited|received)\\s+to\\s+[^\\n]+?(?:\\.\\s+Received\\s+from\\s+([^\\n\\.]+))?", Pattern.CASE_INSENSITIVE)
                val matcher = regex.matcher(body)
                if (matcher.find()) {
                    val amount = matcher.group(1)?.cleanAmount() ?: 0.0
                    val merchant = matcher.group(2)?.trim() ?: "Co-op Bank Sender"
                    Transaction(
                        receiptNumber = receiptNumber,
                        provider = "Co-operative Bank",
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
