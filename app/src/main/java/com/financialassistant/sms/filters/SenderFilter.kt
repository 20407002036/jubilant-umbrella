package com.financialassistant.sms.filters

object SenderFilter {
    private val SUPPORTED_SENDERS = setOf(
        "MPESA", "M-PESA", "KCB", "KCBBANK", "EQUITY", "EQUITYBANK",
        "NCBA", "COOP", "CO-OP", "COOPBANK", "ABSA", "FAMILYBANK"
    )

    fun isFinancialSender(sender: String): Boolean {
        if (sender.isBlank()) return false
        val normalized = sender.uppercase().trim()
        return SUPPORTED_SENDERS.any { supported ->
            normalized.contains(supported) || supported.contains(normalized)
        }
    }

    fun getProviderName(sender: String): String? {
        if (sender.isBlank()) return null
        val normalized = sender.uppercase().trim()
        return when {
            normalized.contains("MPESA") || normalized.contains("M-PESA") -> "M-PESA"
            normalized.contains("KCB") -> "KCB"
            normalized.contains("EQUITY") -> "Equity Bank"
            normalized.contains("NCBA") -> "NCBA"
            normalized.contains("COOP") || normalized.contains("CO-OP") -> "Co-operative Bank"
            normalized.contains("ABSA") -> "ABSA"
            normalized.contains("FAMILYBANK") -> "Family Bank"
            else -> null
        }
    }
}
