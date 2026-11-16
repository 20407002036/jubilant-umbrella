package com.financialassistant

import com.financialassistant.sync.TrackedIncomeFilter
import com.financialassistant.tracking.SmsSimSlotResolver
import com.financialassistant.tracking.TrackedLinePreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackedIncomeFilterTest {

    private val trackedNumber = "254712345678"

    @Test
    fun receivedOnTrackedSlotIsSynced() {
        assertTrue(
            TrackedIncomeFilter.shouldSync(
                transactionType = "RECEIVED",
                simSlot = 1,
                trackedNumber = trackedNumber,
                trackedSlot = 1
            )
        )
    }

    @Test
    fun sentTransactionsAreNeverSynced() {
        assertFalse(
            TrackedIncomeFilter.shouldSync(
                transactionType = "SENT",
                simSlot = 1,
                trackedNumber = trackedNumber,
                trackedSlot = 1
            )
        )
    }

    @Test
    fun noTrackedNumberDisablesSync() {
        assertFalse(
            TrackedIncomeFilter.shouldSync(
                transactionType = "RECEIVED",
                simSlot = 1,
                trackedNumber = "",
                trackedSlot = 1
            )
        )
    }

    @Test
    fun receivedOnAnotherSlotIsIgnored() {
        assertFalse(
            TrackedIncomeFilter.shouldSync(
                transactionType = "RECEIVED",
                simSlot = 0,
                trackedNumber = trackedNumber,
                trackedSlot = 1
            )
        )
    }

    @Test
    fun unknownSlotIsStillSynced() {
        assertTrue(
            TrackedIncomeFilter.shouldSync(
                transactionType = "RECEIVED",
                simSlot = SmsSimSlotResolver.SLOT_UNKNOWN,
                trackedNumber = trackedNumber,
                trackedSlot = 1
            )
        )
    }

    @Test
    fun unspecifiedTrackedSlotAcceptsAnyResolvedSlot() {
        assertTrue(
            TrackedIncomeFilter.shouldSync(
                transactionType = "RECEIVED",
                simSlot = 0,
                trackedNumber = trackedNumber,
                trackedSlot = TrackedLinePreferences.SLOT_UNSPECIFIED
            )
        )
    }
}
