package com.financialassistant.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.financialassistant.data.repository.TransactionRepository
import com.financialassistant.security.EncryptionManager
import com.financialassistant.tracking.MsisdnNormalizer
import com.financialassistant.tracking.SmsSimSlotResolver
import com.financialassistant.tracking.TrackedLinePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ImportStatus {
    IDLE, RUNNING, SUCCEEDED, FAILED
}

data class SettingsUiState(
    val hasSmsPermission: Boolean = false,
    val importStatus: ImportStatus = ImportStatus.IDLE,
    val totalTransactionsCount: Int = 0,
    val hasPhonePermission: Boolean = false,
    val trackedNumberInput: String = "",
    val trackedNumber: String = "",
    val trackedSlot: Int = TrackedLinePreferences.SLOT_UNSPECIFIED,
    val trackingMessage: String? = null
) {
    val isTrackingConfigured: Boolean get() = trackedNumber.isNotBlank()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TransactionRepository,
    private val trackedLinePreferences: TrackedLinePreferences
) : ViewModel() {

    private val sharedPrefs = EncryptionManager.getEncryptedSharedPreferences(context)
    private val workManager = WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Track transaction counts dynamically
        viewModelScope.launch {
            repository.getAllTransactions().collect { list ->
                _uiState.value = _uiState.value.copy(
                    totalTransactionsCount = list.size
                )
            }
        }
        checkImportStatus()
        restoreTrackedLine()
    }

    fun setSmsPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasSmsPermission = granted)
    }

    fun setPhonePermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPhonePermission = granted)
    }

    private fun restoreTrackedLine() {
        val canonical = trackedLinePreferences.trackedNumber.value
        _uiState.value = _uiState.value.copy(
            trackedNumber = canonical.orEmpty(),
            trackedNumberInput = if (canonical.isNullOrBlank()) "" else MsisdnNormalizer.format(canonical),
            trackedSlot = trackedLinePreferences.trackedSlot.value
        )
    }

    fun onTrackedNumberInputChange(value: String) {
        _uiState.value = _uiState.value.copy(
            trackedNumberInput = value,
            trackingMessage = null
        )
    }

    /**
     * Validates and stores the tracked number, then tries to auto-detect the SIM slot
     * it belongs to (requires the phone permissions).
     */
    fun saveTrackedLine() {
        val canonical = MsisdnNormalizer.normalize(_uiState.value.trackedNumberInput)
        if (canonical == null) {
            _uiState.value = _uiState.value.copy(
                trackingMessage = "Enter a valid Safaricom number, e.g. 0712 345 678."
            )
            return
        }

        val detectedSlot = if (_uiState.value.hasPhonePermission) {
            SmsSimSlotResolver.findSlotForNumber(context, canonical)
        } else {
            null
        }

        val slot = detectedSlot ?: TrackedLinePreferences.SLOT_UNSPECIFIED
        trackedLinePreferences.save(canonical, slot)

        _uiState.value = _uiState.value.copy(
            trackedNumber = canonical,
            trackedNumberInput = MsisdnNormalizer.format(canonical),
            trackedSlot = slot,
            trackingMessage = when {
                slot != TrackedLinePreferences.SLOT_UNSPECIFIED -> "Tracking incoming payments on SIM ${slot + 1}."
                !_uiState.value.hasPhonePermission -> "Saved. Allow phone access to auto-detect the SIM, or pick the slot below."
                else -> "Saved, but the SIM slot could not be detected. Pick it below."
            }
        )
    }

    fun selectTrackedSlot(slot: Int) {
        if (_uiState.value.trackedNumber.isBlank()) return
        trackedLinePreferences.updateSlot(slot)
        _uiState.value = _uiState.value.copy(
            trackedSlot = slot,
            trackingMessage = "Tracking incoming payments on SIM ${slot + 1}."
        )
    }

    fun clearTrackedLine() {
        trackedLinePreferences.clear()
        _uiState.value = _uiState.value.copy(
            trackedNumber = "",
            trackedNumberInput = "",
            trackedSlot = TrackedLinePreferences.SLOT_UNSPECIFIED,
            trackingMessage = null
        )
    }

    private fun checkImportStatus() {
        val savedStatus = sharedPrefs.getString("import_status", ImportStatus.IDLE.name) ?: ImportStatus.IDLE.name
        _uiState.value = _uiState.value.copy(
            importStatus = ImportStatus.valueOf(savedStatus)
        )
    }

    fun triggerHistoricalSmsImport() {
        _uiState.value = _uiState.value.copy(importStatus = ImportStatus.RUNNING)
        sharedPrefs.edit().putString("import_status", ImportStatus.RUNNING.name).apply()

        // Build worker request
        val importRequest = OneTimeWorkRequestBuilder<com.financialassistant.workers.SmsImportWorker>()
            .addTag("sms_import_work")
            .build()

        workManager.enqueue(importRequest)

        // Monitor worker execution status
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(importRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            _uiState.value = _uiState.value.copy(importStatus = ImportStatus.SUCCEEDED)
                            sharedPrefs.edit().putString("import_status", ImportStatus.SUCCEEDED.name).apply()
                        }
                        WorkInfo.State.FAILED -> {
                            _uiState.value = _uiState.value.copy(importStatus = ImportStatus.FAILED)
                            sharedPrefs.edit().putString("import_status", ImportStatus.FAILED.name).apply()
                        }
                        else -> { /* Do nothing while in progress */ }
                    }
                }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _uiState.value = _uiState.value.copy(
                importStatus = ImportStatus.IDLE
            )
            sharedPrefs.edit().remove("import_status").apply()
        }
    }
}
