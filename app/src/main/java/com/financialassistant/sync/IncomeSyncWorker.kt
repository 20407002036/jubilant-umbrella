package com.financialassistant.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.financialassistant.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Sends a single incoming payment to the configured Google Sheet. Runs as a
 * background job so the SMS broadcast receiver never performs network I/O.
 */
@HiltWorker
class IncomeSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TransactionRepository,
    private val preferences: GoogleSheetSyncPreferences,
    private val client: IncomeSyncClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val receiptNumber = inputData.getString(KEY_RECEIPT_NUMBER) ?: return Result.failure()

        if (!preferences.isConfigured()) return Result.success()

        val transaction = repository.getTransactionByReceipt(receiptNumber)
            ?: return Result.success()

        // Only incoming payments are mirrored, and never twice.
        if (transaction.type != "RECEIVED") return Result.success()
        if (transaction.syncedAt > 0L) return Result.success()

        return when (val outcome = client.send(transaction)) {
            SyncOutcome.Success -> {
                repository.markTransactionSynced(receiptNumber, System.currentTimeMillis())
                Timber.d("Synced incoming payment $receiptNumber to Google Sheet")
                Result.success()
            }

            SyncOutcome.NotConfigured -> Result.success()

            SyncOutcome.Unauthorized -> {
                Timber.w("Google Sheet sync rejected the shared secret for $receiptNumber")
                Result.failure()
            }

            is SyncOutcome.Rejected -> {
                Timber.w("Google Sheet sync rejected $receiptNumber: HTTP ${outcome.code} ${outcome.message}")
                Result.failure()
            }

            is SyncOutcome.Retry -> {
                if (runAttemptCount + 1 >= MAX_ATTEMPTS) {
                    Timber.w("Google Sheet sync gave up on $receiptNumber after ${runAttemptCount + 1} attempts")
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        }
    }

    companion object {
        const val KEY_RECEIPT_NUMBER = "receipt_number"
        private const val MAX_ATTEMPTS = 5
    }
}
