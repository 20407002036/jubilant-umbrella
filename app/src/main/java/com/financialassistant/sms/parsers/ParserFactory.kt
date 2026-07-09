package com.financialassistant.sms.parsers

import com.financialassistant.sms.filters.SenderFilter

object ParserFactory {
    fun getParser(sender: String): SmsParser? {
        val provider = SenderFilter.getProviderName(sender) ?: return null
        return when (provider) {
            "M-PESA" -> MpesaParser()
            "KCB" -> KcbParser()
            "Equity Bank" -> EquityParser()
            "Co-operative Bank" -> CoopParser()
            "ABSA" -> AbsaParser()
            else -> null // For NCBA and Family Bank, we return null if direct custom parsers aren't added yet, keeping it extensible.
        }
    }
}
