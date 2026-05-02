package com.raylabs.laundryhub.ui.outcome

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.getPaymentValueFromDescription
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.DeleteOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetLastOutcomeIdUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.GetOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.ReadOutcomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.SubmitOutcomeUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.outcome.UpdateOutcomeUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.TextUtil.removeRupiahFormatWithComma
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState
import com.raylabs.laundryhub.ui.outcome.state.toDateListUiItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.toEntryItemUI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@HiltViewModel
class OutcomeViewModel @Inject constructor(
    private val readOutcomeUseCase: ReadOutcomeTransactionUseCase,
    private val submitOutcomeUseCase: SubmitOutcomeUseCase,
    private val getLastOutcomeIdUseCase: GetLastOutcomeIdUseCase,
    private val updateOutcomeUseCase: UpdateOutcomeUseCase,
    private val getOutcomeUseCase: GetOutcomeUseCase,
    private val deleteOutcomeUseCase: DeleteOutcomeUseCase
) : ViewModel() {

    val outcomePagingData: Flow<PagingData<DateListItemUI>> = 
        readOutcomeUseCase.getPagingData()
            .map { pagingData ->
                pagingData.map { DateListItemUI.Entry(it.toEntryItemUI()) }
                    .insertSeparators { before: DateListItemUI.Entry?, after: DateListItemUI.Entry? ->
                        if (after != null && (before == null || before.item.date != after.item.date)) {
                            DateListItemUI.Header(after.item.date)
                        } else {
                            null
                        }
                    }
            }
            .cachedIn(viewModelScope)

    private val _uiState = mutableStateOf(OutcomeUiState())
    val uiState: OutcomeUiState get() = _uiState.value

    init {
        refreshOutcomeList(isManual = false)
    }

    fun refreshOutcomeList(isManual: Boolean = true) {
        if (isManual) {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
        }
        viewModelScope.launch {
            try {
                loadOutcomeList()
                loadLastOutcomeId()
            } finally {
                if (isManual) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
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
                loadOutcomeList()
                loadLastOutcomeId()
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
                loadOutcomeList()
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

    suspend fun deleteOutcome(
        outcomeId: String,
        onComplete: suspend () -> Unit,
        onError: suspend (String) -> Unit = {}
    ) {
        _uiState.value = _uiState.value.copy(
            deleteOutcome = _uiState.value.deleteOutcome.loading()
        )

        when (val result = deleteOutcomeUseCase(outcomeId = outcomeId)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(
                    deleteOutcome = _uiState.value.deleteOutcome.success(result.data)
                )
                loadOutcomeList()
                loadLastOutcomeId()
                onComplete()
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    deleteOutcome = _uiState.value.deleteOutcome.error(result.message)
                )
                onError(result.message)
            }

            else -> Unit
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
        viewModelScope.launch {
            loadLastOutcomeId()
        }
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

    private suspend fun loadLastOutcomeId() {
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

    private suspend fun loadOutcomeList() {
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
