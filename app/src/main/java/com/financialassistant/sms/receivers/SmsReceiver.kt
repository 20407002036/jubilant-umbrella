package com.financialassistant.sms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.financialassistant.data.repository.TransactionRepository
import com.financialassistant.sms.filters.SenderFilter
import com.financialassistant.sms.parsers.ParserFactory
import com.financialassistant.tracking.SmsSimSlotResolver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TransactionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            for (message in messages) {
                val sender = message.originatingAddress ?: continue
                val body = message.messageBody ?: continue
                val timestamp = message.timestampMillis

                Timber.d("Received SMS from: $sender")

                if (SenderFilter.isFinancialSender(sender)) {
                    val parser = ParserFactory.getParser(sender)
                    if (parser != null) {
                        val parsed = parser.parse(body, timestamp)
                        if (parsed != null) {
                            // Tag the record with the SIM line the notification arrived on.
                            val transaction = parsed.copy(
                                simSlot = SmsSimSlotResolver.resolveSlot(context, intent)
                            )
                            // Asynchronously insert the transaction
                            CoroutineScope(Dispatchers.IO).launch {
                                val inserted = repository.insertTransaction(transaction)
                                if (inserted) {
                                    Timber.d("Successfully saved transaction from SMS: ${transaction.receiptNumber}")
                                } else {
                                    Timber.d("Transaction already exists or is invalid: ${transaction.receiptNumber}")
                                }
                            }
                        } else {
                            Timber.w("Failed to parse financial SMS from: $sender. Content: $body")
                        }
                    } else {
                        Timber.w("No parser found for sender: $sender")
                    }
                } else {
                    Timber.d("Ignored non-financial SMS from: $sender")
                }
            }
        }
    }
}
