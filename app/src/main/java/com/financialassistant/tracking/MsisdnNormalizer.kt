package com.financialassistant.tracking

/**
 * Helpers for normalizing and formatting Kenyan mobile numbers so that user input,
 * SIM subscription numbers and SMS sender numbers can be reliably compared.
 */
object MsisdnNormalizer {

    private const val COUNTRY_CODE = "254"

    /**
     * Normalizes a raw phone number into the canonical `2547XXXXXXXX` / `2541XXXXXXXX` form.
     *
     * Accepts: `0722123456`, `+254722123456`, `254722123456`, `722123456`, and numbers
     * containing spaces, dashes or parentheses. Returns `null` when the input cannot be
     * interpreted as a Kenyan mobile number.
     */
    fun normalize(raw: String?): String? {
        val digits = raw?.filter { it.isDigit() } ?: return null
        return when {
            digits.length == 12 && digits.startsWith(COUNTRY_CODE) -> digits
            digits.length == 10 && digits.startsWith("0") -> COUNTRY_CODE + digits.substring(1)
            digits.length == 9 && (digits.startsWith("7") || digits.startsWith("1")) -> COUNTRY_CODE + digits
            else -> null
        }
    }

    /** Formats a canonical (`2547XXXXXXXX`) or raw number as `07XX XXX XXX` for display. */
    fun format(raw: String?): String {
        val canonical = normalize(raw) ?: return raw?.trim().orEmpty()
        val local = "0" + canonical.substring(COUNTRY_CODE.length)
        return "${local.substring(0, 4)} ${local.substring(4, 7)} ${local.substring(7)}"
    }

    /** Returns true when two raw numbers refer to the same Kenyan mobile number. */
    fun sameNumber(a: String?, b: String?): Boolean {
        val na = normalize(a) ?: return false
        val nb = normalize(b) ?: return false
        return na == nb
    }
}
