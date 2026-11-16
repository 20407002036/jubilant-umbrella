package com.financialassistant.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queues one sync job per incoming payment. Unique work names prevent duplicate
 * jobs when the same SMS is processed more than once.
 */
@Singleton
class IncomeSyncScheduler @Inject constructor(
    @ApplicationContext context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueue(receiptNumber: String) {
        if (receiptNumber.isBlank()) return

        val request = OneTimeWorkRequestBuilder<IncomeSyncWorker>()
            .setInputData(workDataOf(IncomeSyncWorker.KEY_RECEIPT_NUMBER to receiptNumber))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                MIN_BACKOFF_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG)
            .build()

        workManager.enqueueUniqueWork(
            "$UNIQUE_PREFIX$receiptNumber",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private companion object {
        const val TAG = "google_sheet_income_sync"
        const val UNIQUE_PREFIX = "income-sync-"
        const val MIN_BACKOFF_SECONDS = 30L
    }
}
