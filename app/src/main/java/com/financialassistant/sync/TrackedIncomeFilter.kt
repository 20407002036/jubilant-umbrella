package com.financialassistant.sync

import com.financialassistant.tracking.SmsSimSlotResolver
import com.financialassistant.tracking.TrackedLinePreferences

/**
 * Decides whether a freshly saved transaction belongs to the incoming payments the
 * user wants mirrored to Google Sheets: money received on the tracked line.
 */
object TrackedIncomeFilter {

    /**
     * @param transactionType `RECEIVED`, `SENT`, ... as stored in the database.
     * @param simSlot SIM slot the SMS arrived on, or -1 when it could not be resolved.
     * @param trackedNumber Canonical number saved in Settings, or blank when unset.
     * @param trackedSlot SIM slot saved in Settings, or -1 when not configured.
     */
    fun shouldSync(
        transactionType: String,
        simSlot: Int,
        trackedNumber: String,
        trackedSlot: Int
    ): Boolean {
        if (transactionType != "RECEIVED") return false
        if (trackedNumber.isBlank()) return false

        // The receiving SIM could not be resolved. Slot tagging is best-effort on
        // Android, so accept the payment rather than silently dropping real income.
        if (simSlot == SmsSimSlotResolver.SLOT_UNKNOWN) return true

        // No slot configured: the tracked number is the only line being monitored.
        if (trackedSlot == TrackedLinePreferences.SLOT_UNSPECIFIED) return true

        return simSlot == trackedSlot
    }
}
