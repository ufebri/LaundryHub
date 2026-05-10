package com.raylabs.laundryhub.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.usecase.reminder.DismissReminderUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.MarkReminderAssumedPickedUpUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.MarkReminderCheckedUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SnoozeReminderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.reminder.state.ReminderInboxUiState
import com.raylabs.laundryhub.ui.reminder.state.toReminderSections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val readIncomeTransactionUseCase: ReadIncomeTransactionUseCase,
    private val observeReminderSettingsUseCase: ObserveReminderSettingsUseCase,
    private val observeReminderLocalStatesUseCase: ObserveReminderLocalStatesUseCase,
    private val evaluateReminderCandidatesUseCase: EvaluateReminderCandidatesUseCase,
    private val markReminderCheckedUseCase: MarkReminderCheckedUseCase,
    private val markReminderAssumedPickedUpUseCase: MarkReminderAssumedPickedUpUseCase,
    private val dismissReminderUseCase: DismissReminderUseCase,
    private val snoozeReminderUseCase: SnoozeReminderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderInboxUiState())
    val uiState: StateFlow<ReminderInboxUiState> = _uiState

    private var cachedTransactions = emptyList<com.raylabs.laundryhub.core.domain.model.sheets.TransactionData>()
    private var cachedLocalStates = emptyMap<String, com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState>()
    private var hasLoadedTransactions = false

    init {
        observeSettings()
        observeLocalStates()
        refresh()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            observeReminderSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(reminderSettings = settings) }
                recompute()
            }
        }
    }

    private fun observeLocalStates() {
        viewModelScope.launch {
            observeReminderLocalStatesUseCase().collect { states ->
                cachedLocalStates = states
                recompute()
            }
        }
    }

    fun refresh() {
        _uiState.update { state ->
            state.copy(
                reminders = if (state.reminders.data.isNullOrEmpty()) {
                    state.reminders.loading()
                } else {
                    state.reminders.copy(isLoading = true, errorMessage = null)
                }
            )
        }
        viewModelScope.launch {
            when (val result = readIncomeTransactionUseCase(filter = FILTER.SHOW_ALL_DATA)) {
                is Resource.Success -> {
                    hasLoadedTransactions = true
                    cachedTransactions = result.data
                    recompute()
                }

                is Resource.Empty -> {
                    hasLoadedTransactions = true
                    cachedTransactions = emptyList()
                    _uiState.update { state ->
                        state.copy(reminders = state.reminders.success(emptyList()))
                    }
                }

                is Resource.Error -> {
                    hasLoadedTransactions = true
                    _uiState.update { state ->
                        state.copy(reminders = state.reminders.error(result.message))
                    }
                }

                is Resource.Loading -> {
                    _uiState.update { state ->
                        state.copy(reminders = state.reminders.loading())
                    }
                }
            }
        }
    }

    fun markChecked(orderId: String) {
        viewModelScope.launch { markReminderCheckedUseCase(orderId) }
    }

    fun markAssumedPickedUp(orderId: String) {
        viewModelScope.launch { markReminderAssumedPickedUpUseCase(orderId) }
    }

    fun dismiss(orderId: String) {
        viewModelScope.launch { dismissReminderUseCase(orderId) }
    }

    fun snooze(orderId: String) {
        viewModelScope.launch {
            val tomorrow = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
            snoozeReminderUseCase(orderId, tomorrow)
        }
    }

    private fun recompute() {
        if (!hasLoadedTransactions) return

        val candidates = evaluateReminderCandidatesUseCase(
            transactions = cachedTransactions,
            localStates = cachedLocalStates
        )
        _uiState.update { state ->
            state.copy(
                reminders = SectionState(
                    data = candidates.toReminderSections(),
                    isLoading = false
                )
            )
        }
    }
}
