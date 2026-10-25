package com.financialassistant.tracking

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import com.financialassistant.security.EncryptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the M-PESA line the user wants to monitor. The number is stored in the
 * app's [EncryptedSharedPreferences] file and exposed as observable state so screens
 * react when the user changes it.
 */
@Singleton
class TrackedLinePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptionManager.getEncryptedSharedPreferences(context)

    private val _trackedNumber = MutableStateFlow(prefs.getString(KEY_NUMBER, null))
    /** Canonical (`2547XXXXXXXX`) tracked number, or `null` when none is configured. */
    val trackedNumber: StateFlow<String?> = _trackedNumber.asStateFlow()

    private val _trackedSlot = MutableStateFlow(prefs.getInt(KEY_SLOT, SLOT_UNSPECIFIED))
    /** The SIM slot (0/1) the tracked number lives on, or [SLOT_UNSPECIFIED]. */
    val trackedSlot: StateFlow<Int> = _trackedSlot.asStateFlow()

    private val _selectedSlot = MutableStateFlow(
        prefs.getInt(KEY_SELECTED_SLOT, prefs.getInt(KEY_SLOT, SLOT_UNSPECIFIED))
    )
    /** The SIM slot currently selected on the lines screen, or [SLOT_UNSPECIFIED]. */
    val selectedSlot: StateFlow<Int> = _selectedSlot.asStateFlow()

    fun save(number: String, slot: Int) {
        prefs.edit()
            .putString(KEY_NUMBER, number)
            .putInt(KEY_SLOT, slot)
            .putInt(KEY_SELECTED_SLOT, slot)
            .apply()
        _trackedNumber.value = number
        _trackedSlot.value = slot
        _selectedSlot.value = slot
    }

    fun selectSlot(slot: Int) {
        prefs.edit().putInt(KEY_SELECTED_SLOT, slot).apply()
        _selectedSlot.value = slot
    }

    fun updateSlot(slot: Int) {
        val number = _trackedNumber.value ?: return
        save(number, slot)
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_NUMBER)
            .remove(KEY_SLOT)
            .remove(KEY_SELECTED_SLOT)
            .apply()
        _trackedNumber.value = null
        _trackedSlot.value = SLOT_UNSPECIFIED
        _selectedSlot.value = SLOT_UNSPECIFIED
    }

    companion object {
        const val SLOT_UNSPECIFIED = -1
        private const val KEY_NUMBER = "tracked_msisdn"
        private const val KEY_SLOT = "tracked_sim_slot"
        private const val KEY_SELECTED_SLOT = "selected_sim_slot"
    }
}
