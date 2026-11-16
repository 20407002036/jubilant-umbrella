package com.financialassistant.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financialassistant.data.entities.TransactionEntity
import com.financialassistant.data.repository.TransactionRepository
import com.financialassistant.tracking.SimLine
import com.financialassistant.tracking.SmsSimSlotResolver
import com.financialassistant.tracking.TrackedLinePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class IncomingUiState(
    val trackedNumber: String = "",
    val trackedSlot: Int = TrackedLinePreferences.SLOT_UNSPECIFIED,
    val lines: List<SimLine> = emptyList(),
    val selectedSlot: Int = TrackedLinePreferences.SLOT_UNSPECIFIED,
    val transactions: List<TransactionEntity> = emptyList(),
    val hasPhonePermission: Boolean = false,
    val isLoading: Boolean = true
) {
    val selectedLine: SimLine? get() = lines.firstOrNull { it.slot == selectedSlot }
    val hasLines: Boolean get() = lines.isNotEmpty()
    val selectedIsTracked: Boolean
        get() = trackedSlot != TrackedLinePreferences.SLOT_UNSPECIFIED && selectedSlot == trackedSlot
    val totalReceived: Double
        get() = transactions.filter { it.type == "RECEIVED" }.sumOf { it.amount }
    val totalSent: Double
        get() = transactions.filter { it.type != "RECEIVED" }.sumOf { it.amount }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IncomingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TransactionRepository,
    private val trackedLinePreferences: TrackedLinePreferences
) : ViewModel() {

    private val _lines = MutableStateFlow<List<SimLine>>(emptyList())
    private val _hasPhonePermission = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)

    private val lineTransactions: Flow<List<TransactionEntity>> =
        trackedLinePreferences.selectedSlot.flatMapLatest { slot ->
            if (slot == TrackedLinePreferences.SLOT_UNSPECIFIED) {
                flowOf(emptyList())
            } else {
                repository.getBySim(slot)
            }
        }

    private val lineState = combine(
        trackedLinePreferences.trackedNumber,
        trackedLinePreferences.trackedSlot,
        trackedLinePreferences.selectedSlot,
        _lines,
        _hasPhonePermission
    ) { number, trackedSlot, selectedSlot, lines, hasPermission ->
        LineSelectionState(
            trackedNumber = number.orEmpty(),
            trackedSlot = trackedSlot,
            selectedSlot = selectedSlot,
            lines = lines,
            hasPhonePermission = hasPermission
        )
    }

    val uiState: StateFlow<IncomingUiState> = combine(
        lineState,
        lineTransactions,
        _isLoading
    ) { state, transactions, isLoading ->
        IncomingUiState(
            trackedNumber = state.trackedNumber,
            trackedSlot = state.trackedSlot,
            lines = state.lines,
            selectedSlot = state.selectedSlot,
            transactions = transactions,
            hasPhonePermission = state.hasPhonePermission,
            isLoading = isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = IncomingUiState()
    )

    init {
        refreshLines()
        // Re-read the SIM list when the tracked line (and therefore its slot) changes.
        viewModelScope.launch {
            trackedLinePreferences.trackedSlot.collect { refreshLines() }
        }
    }

    /** Re-reads the active SIM subscriptions and keeps the selected line valid. */
    fun refreshLines() {
        viewModelScope.launch {
            val hasPermission = SmsSimSlotResolver.hasPhoneStatePermission(context)
            val deviceLines = withContext(Dispatchers.IO) {
                SmsSimSlotResolver.activeLines(context)
            }
            val lines = deviceLines.ifEmpty { fallbackLine() }

            _hasPhonePermission.value = hasPermission

            val current = trackedLinePreferences.selectedSlot.value
            val selected = lines.firstOrNull { it.slot == current }
                ?: lines.firstOrNull { it.slot == trackedLinePreferences.trackedSlot.value }
                ?: lines.firstOrNull()

            when {
                selected == null && current != TrackedLinePreferences.SLOT_UNSPECIFIED ->
                    trackedLinePreferences.selectSlot(TrackedLinePreferences.SLOT_UNSPECIFIED)
                selected != null && selected.slot != current ->
                    trackedLinePreferences.selectSlot(selected.slot)
            }

            _lines.value = lines
            _isLoading.value = false
        }
    }

    fun selectLine(slot: Int) {
        if (_lines.value.none { it.slot == slot }) return
        trackedLinePreferences.selectSlot(slot)
    }

    /**
     * When the SIM list is not readable yet, still surface the line configured in
     * Settings so the screen has something to show.
     */
    private fun fallbackLine(): List<SimLine> {
        val slot = trackedLinePreferences.trackedSlot.value
        if (slot == TrackedLinePreferences.SLOT_UNSPECIFIED) return emptyList()
        return listOf(
            SimLine(
                subscriptionId = -1,
                slot = slot,
                number = trackedLinePreferences.trackedNumber.value,
                displayName = "Tracked line",
                carrierName = ""
            )
        )
    }

    private data class LineSelectionState(
        val trackedNumber: String,
        val trackedSlot: Int,
        val selectedSlot: Int,
        val lines: List<SimLine>,
        val hasPhonePermission: Boolean
    )
}
