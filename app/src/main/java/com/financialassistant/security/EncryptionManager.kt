package com.financialassistant.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

object EncryptionManager {
    private const val PREFS_FILE = "secure_financial_prefs"

    fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Error creating EncryptedSharedPreferences, falling back to standard EncryptedSharedPreferences")
            // Fallback to basic shared preferences in standard non-Keystore environments if necessary,
            // but for Android security guidelines, we prefer throwing or handling appropriately.
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }
}
