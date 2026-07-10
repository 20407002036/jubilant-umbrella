package com.financialassistant.workers

import android.content.Context
import android.net.Uri
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
        try {
            val contentResolver = applicationContext.contentResolver
            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null,
                null,
                "date DESC"
            ) ?: run {
                Timber.w("SMS inbox query returned null cursor (permission denied or provider unavailable).")
                return@withContext Result.failure()
            }

            var totalParsed = 0
            var totalSaved = 0

            cursor.use { c ->
                val addressColumn = c.getColumnIndex("address")
                val bodyColumn = c.getColumnIndex("body")
                val dateColumn = c.getColumnIndex("date")

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
