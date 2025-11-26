package com.raylabs.laundryhub.ui.outcome

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.getPaymentValueFromDescription
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetLastOutcomeIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.ReadOutcomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.SubmitOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.UpdateOutcomeUseCase
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.TextUtil.removeRupiahFormatWithComma
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState
import com.raylabs.laundryhub.ui.outcome.state.toDateListUiItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OutcomeViewModel @Inject constructor(
    private val readOutcomeUseCase: ReadOutcomeTransactionUseCase,
    private val submitOutcomeUseCase: SubmitOutcomeUseCase,
    private val getLastOutcomeIdUseCase: GetLastOutcomeIdUseCase,
    private val updateOutcomeUseCase: UpdateOutcomeUseCase,
    private val getOutcomeUseCase: GetOutcomeUseCase
) : ViewModel() {

    private val _uiState = mutableStateOf(OutcomeUiState())
    val uiState: OutcomeUiState get() = _uiState.value

    init {
        fetchOutcomeList()
        fetchLastOutcomeId()
    }

    private fun fetchLastOutcomeId() {
        viewModelScope.launch {
            when (val result = getLastOutcomeIdUseCase()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(lastOutcomeId = result.data)
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(lastOutcomeId = "Error, try again")
                }

                else -> Unit
            }
        }
    }

    private fun fetchOutcomeList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                outcome = _uiState.value.outcome.loading()
            )

            when (val result = readOutcomeUseCase()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        outcome = _uiState.value.outcome.success(result.data.toDateListUiItems())
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        outcome = _uiState.value.outcome.error(result.message)
                    )
                }

                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        outcome = _uiState.value.outcome.error("Data Kosong")
                    )
                }

                else -> Unit
            }
        }
    }

    suspend fun submitOutcome(outcome: OutcomeData, onComplete: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(
            submitNewOutcome = _uiState.value.submitNewOutcome.loading(),
            isSubmitting = true
        )

        when (val result = submitOutcomeUseCase(order = outcome)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    submitNewOutcome = _uiState.value.submitNewOutcome.success(result.data),
                    isSubmitting = false
                )
                fetchOutcomeList()
                fetchLastOutcomeId()
                onComplete()
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    submitNewOutcome = _uiState.value.submitNewOutcome.error(result.message),
                    isSubmitting = false
                )
            }

            else -> {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }

    suspend fun onOutcomeEditClick(outcomeID: String): Boolean {
        _uiState.value = _uiState.value.copy(
            editOutcome = _uiState.value.editOutcome.loading(),
            isEditMode = false
        )

        when (val result = getOutcomeUseCase(outcomeID = outcomeID)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    editOutcome = _uiState.value.editOutcome.success(result.data),
                    isEditMode = true,
                    outcomeID = result.data.id,
                    name = result.data.purpose,
                    date = result.data.date,
                    price = sanitizePrice(result.data.price),
                    remark = result.data.remark,
                    paymentStatus = result.data.paidDescription()
                )
                return true
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    editOutcome = _uiState.value.editOutcome.error(result.message),
                    isEditMode = false
                )
                return false
            }

            is Resource.Empty -> {
                _uiState.value = _uiState.value.copy(
                    editOutcome = _uiState.value.editOutcome.error("No data found for outcome ID: $outcomeID"),
                    isEditMode = false
                )
                return false
            }

            else -> return false
        }
    }

    suspend fun updateOutcome(outcome: OutcomeData, onComplete: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(
            updateOutcome = _uiState.value.updateOutcome.loading(),
            isSubmitting = true
        )

        when (val result = updateOutcomeUseCase(order = outcome)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    updateOutcome = _uiState.value.updateOutcome.success(result.data),
                    isSubmitting = false
                )
                fetchOutcomeList()
                onComplete()
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    updateOutcome = _uiState.value.updateOutcome.error(result.message),
                    isSubmitting = false
                )
            }

            else -> {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
            }
        }
    }

    fun onPurposeChanged(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onPriceChanged(value: String) {
        _uiState.value = _uiState.value.copy(price = sanitizePrice(value))
    }

    fun onDateChanged(value: String) {
        _uiState.value = _uiState.value.copy(date = value)
    }

    fun onPaymentMethodSelected(value: String) {
        _uiState.value = _uiState.value.copy(paymentStatus = value)
    }

    fun onRemarkChanged(value: String) {
        _uiState.value = _uiState.value.copy(remark = value)
    }

    fun prepareNewOutcome() {
        _uiState.value = _uiState.value.copy(
            isEditMode = false,
            outcomeID = "",
            name = "",
            price = "",
            remark = "",
            paymentStatus = "",
            date = DateUtil.getTodayDate(DateUtil.STANDARD_DATE_FORMATED)
        )
    }

    fun buildOutcomeDataForSubmit(): OutcomeData? {
        val id =
            _uiState.value.lastOutcomeId?.takeIf { it.all { ch -> ch.isDigit() } } ?: return null
        val date =
            _uiState.value.date.ifBlank { DateUtil.getTodayDate(DateUtil.STANDARD_DATE_FORMATED) }

        return OutcomeData(
            id = id,
            date = date,
            purpose = _uiState.value.name,
            price = sanitizePrice(_uiState.value.price),
            remark = _uiState.value.remark,
            payment = getPaymentValueFromDescription(_uiState.value.paymentStatus)
        )
    }

    fun buildOutcomeDataForUpdate(): OutcomeData? {
        val id = _uiState.value.outcomeID.takeIf { it.isNotBlank() } ?: return null
        val date =
            _uiState.value.date.ifBlank { DateUtil.getTodayDate(DateUtil.STANDARD_DATE_FORMATED) }
        return OutcomeData(
            id = id,
            date = date,
            purpose = _uiState.value.name,
            price = sanitizePrice(_uiState.value.price),
            remark = _uiState.value.remark,
            payment = getPaymentValueFromDescription(_uiState.value.paymentStatus)
        )
    }

    fun resetForm() {
        fetchLastOutcomeId()
        _uiState.value = _uiState.value.copy(
            isEditMode = false,
            outcomeID = "",
            name = "",
            date = "",
            price = "",
            remark = "",
            paymentStatus = ""
        )
    }

    private fun sanitizePrice(value: String): String {
        return value.removeRupiahFormatWithComma()
    }
}
