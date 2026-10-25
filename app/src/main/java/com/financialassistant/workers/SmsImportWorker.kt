package com.financialassistant.workers

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.financialassistant.data.repository.TransactionRepository
import com.financialassistant.sms.filters.SenderFilter
import com.financialassistant.sms.parsers.ParserFactory
import com.financialassistant.tracking.SmsSimSlotResolver
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
            val inboxUri = Uri.parse("content://sms/inbox")
            // "sub_id" lets us tag historical messages with their SIM line, but the column is
            // not guaranteed to exist on every provider, so fall back to a plain projection.
            val cursor = try {
                contentResolver.query(
                    inboxUri,
                    arrayOf("address", "body", "date", "sub_id"),
                    null,
                    null,
                    "date DESC"
                )
            } catch (e: Exception) {
                Timber.w("SMS inbox 'sub_id' projection unavailable, falling back: ${e.message}")
                contentResolver.query(
                    inboxUri,
                    arrayOf("address", "body", "date"),
                    null,
                    null,
                    "date DESC"
                )
            } ?: run {
                Timber.w("SMS inbox query returned null cursor (permission denied or provider unavailable).")
                return@withContext Result.failure()
            }

            var totalParsed = 0
            var totalSaved = 0

            cursor.use { c ->
                val addressColumn = c.getColumnIndex("address")
                val bodyColumn = c.getColumnIndex("body")
                val dateColumn = c.getColumnIndex("date")
                val subIdColumn = c.getColumnIndex("sub_id")
                val slotCache = HashMap<Int, Int>()

                if (addressColumn != -1 && bodyColumn != -1 && dateColumn != -1) {
                    while (c.moveToNext()) {
                        val sender = c.getString(addressColumn) ?: continue
                        val body = c.getString(bodyColumn) ?: continue
                        val timestamp = c.getLong(dateColumn)

                        if (SenderFilter.isFinancialSender(sender)) {
                            val parser = ParserFactory.getParser(sender)
                            if (parser != null) {
                                val parsed = parser.parse(body, timestamp)
                                if (parsed != null) {
                                    val slot = resolveSlot(c, subIdColumn, slotCache)
                                    val transaction = parsed.copy(simSlot = slot)
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

    /** Maps a row's `sub_id` to a SIM slot, caching lookups for the duration of the scan. */
    private fun resolveSlot(
        cursor: android.database.Cursor,
        subIdColumn: Int,
        cache: HashMap<Int, Int>
    ): Int {
        if (subIdColumn == -1) return SmsSimSlotResolver.SLOT_UNKNOWN
        if (cursor.isNull(subIdColumn)) return SmsSimSlotResolver.SLOT_UNKNOWN
        val subId = try {
            cursor.getInt(subIdColumn)
        } catch (e: Exception) {
            return SmsSimSlotResolver.SLOT_UNKNOWN
        }
        if (subId == SmsSimSlotResolver.SLOT_UNKNOWN) return SmsSimSlotResolver.SLOT_UNKNOWN
        return cache.getOrPut(subId) {
            SmsSimSlotResolver.slotFromSubscriptionId(applicationContext, subId)
                ?: SmsSimSlotResolver.SLOT_UNKNOWN
        }
    }
}
