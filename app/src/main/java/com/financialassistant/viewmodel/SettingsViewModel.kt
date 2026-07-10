package com.financialassistant.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.financialassistant.data.repository.TransactionRepository
import com.financialassistant.security.EncryptionManager
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
    val totalTransactionsCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TransactionRepository
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
    }

    fun setSmsPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasSmsPermission = granted)
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
