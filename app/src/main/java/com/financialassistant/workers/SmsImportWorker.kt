package com.financialassistant.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.financialassistant.data.repository.TransactionRepository
import com.financialassistant.sms.filters.SenderFilter
import com.financialassistant.sms.parsers.ParserFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltWorker
class SmsImportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.d("Starting historical SMS import worker...")

        // 1. Check for runtime permission within the worker
        val hasPermission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Timber.e("Missing READ_SMS permission for historical import")
            return@withContext Result.failure()
        }

        try {
            val contentResolver = applicationContext.contentResolver
            
            // 2. Use official Telephony.Sms.Inbox.CONTENT_URI
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.Inbox.ADDRESS,
                    Telephony.Sms.Inbox.BODY,
                    Telephony.Sms.Inbox.DATE
                ),
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC"
            )

            var totalParsed = 0
            var totalSaved = 0

            cursor?.use { c ->
                // 3. Use Telephony.Sms constants for column indexing
                val addressColumn = c.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
                val bodyColumn = c.getColumnIndex(Telephony.Sms.Inbox.BODY)
                val dateColumn = c.getColumnIndex(Telephony.Sms.Inbox.DATE)

                if (addressColumn != -1 && bodyColumn != -1 && dateColumn != -1) {
                    while (c.moveToNext()) {
                        val sender = c.getString(addressColumn) ?: continue
                        val body = c.getString(bodyColumn) ?: continue
                        val timestamp = c.getLong(dateColumn)

                        if (SenderFilter.isFinancialSender(sender)) {
                            val parser = ParserFactory.getParser(sender)
                            if (parser != null) {
                                val transaction = parser.parse(body, timestamp)
                                if (transaction != null) {
                                    totalParsed++
                                    val inserted = repository.insertTransaction(transaction)
                                    if (inserted) {
                                        totalSaved++
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Timber.d("Import completed: found $totalParsed financial messages, saved $totalSaved new transactions.")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error occurred during historical SMS import")
            Result.failure()
        }
    }
}
