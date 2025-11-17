package com.raylabs.laundryhub.ui.outcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadOutcomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.SubmitOutcomeUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.outcome.state.OutcomeFormState
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState
import com.raylabs.laundryhub.ui.outcome.state.toUiItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OutcomeViewModel @Inject constructor(
    private val readOutcomeTransactionUseCase: ReadOutcomeTransactionUseCase,
    private val submitOutcomeUseCase: SubmitOutcomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutcomeUiState())
    val uiState: StateFlow<OutcomeUiState> = _uiState

    init {
        refreshHistory()
    }

    private fun refreshHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(history = it.history.loading()) }
            when (val result = readOutcomeTransactionUseCase()) {
                is Resource.Success -> {
                    val mapped = result.data.toUiItems()
                    val nextId =
                        ((result.data.maxOfOrNull { it.id.toIntOrNull() ?: 0 } ?: 0) + 1).toString()
                    _uiState.update {
                        it.copy(
                            history = SectionState(data = mapped),
                            form = it.form.copy(id = nextId)
                        )
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(history = it.history.error(result.message))
                    }
                }

                is Resource.Empty -> {
                    _uiState.update {
                        it.copy(
                            history = SectionState(data = emptyList()),
                            form = it.form.copy(id = "1")
                        )
                    }
                }

                else -> Unit
            }
        }
    }

    fun showAddSheet() {
        _uiState.update { it.copy(isAddSheetVisible = true) }
    }

    fun hideAddSheet() {
        _uiState.update { it.copy(isAddSheetVisible = false) }
    }

    fun onDateSelected(date: String) {
        _uiState.update { it.copy(form = it.form.copy(date = date)) }
    }

    fun onPurposeChanged(value: String) {
        _uiState.update { it.copy(form = it.form.copy(purpose = value)) }
    }

    fun onPriceChanged(digitsOnly: String) {
        val sanitized = digitsOnly.filter { it.isDigit() }.take(9)
        _uiState.update { it.copy(form = it.form.copy(priceRaw = sanitized)) }
    }

    fun onPaymentSelected(value: String) {
        _uiState.update { it.copy(form = it.form.copy(payment = value)) }
    }

    fun onRemarkChanged(value: String) {
        _uiState.update { it.copy(form = it.form.copy(remark = value)) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun submitOutcome() {
        val currentForm = _uiState.value.form
        if (!currentForm.isValid || currentForm.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(form = currentForm.copy(isSubmitting = true)) }
            val data = OutcomeData(
                id = currentForm.id,
                date = currentForm.date,
                purpose = currentForm.purpose,
                price = currentForm.priceForSheet,
                remark = currentForm.remark,
                payment = currentForm.payment.lowercase()
            )
            when (val result = submitOutcomeUseCase(data)) {
                is Resource.Success -> {
                    val nextId =
                        (currentForm.id.toIntOrNull()?.plus(1) ?: currentForm.id).toString()
                    _uiState.update {
                        it.copy(
                            form = OutcomeFormState(
                                id = nextId,
                                payment = currentForm.payment
                            ),
                            isAddSheetVisible = false,
                            snackbarMessage = "Outcome #${currentForm.id} added"
                        )
                    }
                    refreshHistory()
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            form = currentForm.copy(isSubmitting = false),
                            snackbarMessage = result.message
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(form = currentForm.copy(isSubmitting = false))
                    }
                }
            }
        }
    }
}
