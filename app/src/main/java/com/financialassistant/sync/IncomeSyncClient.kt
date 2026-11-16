package com.financialassistant.sync

import com.financialassistant.data.entities.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Result of attempting to append one payment to the user's Google Sheet. */
sealed interface SyncOutcome {
    data object Success : SyncOutcome
    data object NotConfigured : SyncOutcome
    data object Unauthorized : SyncOutcome
    data class Rejected(val code: Int, val message: String? = null) : SyncOutcome
    data class Retry(val code: Int, val message: String? = null) : SyncOutcome
}

/**
 * Posts incoming payments to a Google Apps Script web app, which appends a row to a
 * Google Sheet. The request is a plain HTTPS POST so the app needs no Google OAuth
 * or Play Services client.
 */
@Singleton
class IncomeSyncClient @Inject constructor(
    private val preferences: GoogleSheetSyncPreferences
) {

    suspend fun send(transaction: TransactionEntity): SyncOutcome = withContext(Dispatchers.IO) {
        if (!preferences.enabled.value) return@withContext SyncOutcome.NotConfigured
        val url = preferences.webAppUrl.value
        if (url.isBlank()) return@withContext SyncOutcome.NotConfigured

        val payload = JSONObject().apply {
            put("token", preferences.secret.value)
            put("receiptNumber", transaction.receiptNumber)
            put("sender", transaction.recipient)
            put("senderPhone", transaction.senderPhone)
            put("amount", transaction.amount)
            put("provider", transaction.provider)
            put("timestamp", transaction.timestamp)
        }
        post(url, payload)
    }

    /** Sends a harmless test row so the user can verify the sheet setup. */
    suspend fun sendTest(): SyncOutcome = withContext(Dispatchers.IO) {
        val url = preferences.webAppUrl.value
        if (url.isBlank()) return@withContext SyncOutcome.NotConfigured

        val now = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("token", preferences.secret.value)
            put("receiptNumber", "TEST-$now")
            put("sender", "Test entry")
            put("senderPhone", "")
            put("amount", 0.0)
            put("provider", "Test")
            put("timestamp", now)
            put("test", true)
        }
        post(url, payload)
    }

    private fun post(url: String, payload: JSONObject): SyncOutcome {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                instanceFollowRedirects = true
            }

            connection.outputStream.use { output ->
                output.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            when {
                code == HttpURLConnection.HTTP_UNAUTHORIZED ||
                    code == HttpURLConnection.HTTP_FORBIDDEN -> SyncOutcome.Unauthorized

                code !in 200..299 -> {
                    val message = body.take(200).ifBlank { null }
                    if (code in 400..499) {
                        SyncOutcome.Rejected(code, message)
                    } else {
                        SyncOutcome.Retry(code, message)
                    }
                }

                else -> {
                    // Apps Script returns { "ok": false, "error": ... } for a bad token.
                    val json = try {
                        if (body.isBlank()) null else JSONObject(body)
                    } catch (e: Exception) {
                        Timber.w(e, "Google Sheet sync returned a non-JSON response")
                        null
                    }
                    when {
                        json == null || json.optBoolean("ok", true) -> SyncOutcome.Success
                        json.optString("error") == "unauthorized" -> SyncOutcome.Unauthorized
                        else -> SyncOutcome.Rejected(code, json.optString("error").ifBlank { null })
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Google Sheet sync request failed")
            SyncOutcome.Retry(-1, e.message)
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 15_000
    }
}
