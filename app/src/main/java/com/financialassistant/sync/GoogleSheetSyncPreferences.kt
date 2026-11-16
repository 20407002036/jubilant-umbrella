package com.financialassistant.sync

import android.content.Context
import com.financialassistant.security.EncryptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the Google Apps Script web-app endpoint and shared secret used to append
 * incoming payments to the user's Google Sheet. Values live in the app's
 * [EncryptedSharedPreferences] file and are exposed as observable state.
 */
@Singleton
class GoogleSheetSyncPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptionManager.getEncryptedSharedPreferences(context)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    /** Whether new incoming payments should be written to the sheet. */
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _webAppUrl = MutableStateFlow(prefs.getString(KEY_URL, null).orEmpty())
    /** The deployed Apps Script `/exec` URL. */
    val webAppUrl: StateFlow<String> = _webAppUrl.asStateFlow()

    private val _secret = MutableStateFlow(prefs.getString(KEY_SECRET, null).orEmpty())
    /** Shared secret checked by the Apps Script before appending a row. */
    val secret: StateFlow<String> = _secret.asStateFlow()

    fun save(enabled: Boolean, webAppUrl: String, secret: String) {
        val url = webAppUrl.trim()
        val token = secret.trim()
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_URL, url)
            .putString(KEY_SECRET, token)
            .apply()
        _enabled.value = enabled
        _webAppUrl.value = url
        _secret.value = token
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    /** True when sync is switched on and a web-app URL has been configured. */
    fun isConfigured(): Boolean = _enabled.value && _webAppUrl.value.isNotBlank()

    fun clear() {
        prefs.edit()
            .remove(KEY_ENABLED)
            .remove(KEY_URL)
            .remove(KEY_SECRET)
            .apply()
        _enabled.value = false
        _webAppUrl.value = ""
        _secret.value = ""
    }

    companion object {
        private const val KEY_ENABLED = "google_sheet_sync_enabled"
        private const val KEY_URL = "google_sheet_sync_url"
        private const val KEY_SECRET = "google_sheet_sync_secret"
    }
}
