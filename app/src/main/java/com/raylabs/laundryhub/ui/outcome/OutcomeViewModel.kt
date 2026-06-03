package com.raylabs.laundryhub.ui.outcome

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
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
import com.raylabs.laundryhub.ui.common.util.TextUtil.toRupiahFormat
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.home.state.SyncStatus
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import com.raylabs.laundryhub.ui.outcome.state.OutcomeUiState
import com.raylabs.laundryhub.ui.outcome.state.toDateListUiItems
import com.raylabs.laundryhub.ui.outcome.state.toEntryItemUI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                pagingData.map {
                    val entry: DateListItemUI = DateListItemUI.Entry(it.toEntryItemUI())
                    entry
                }
                    .insertSeparators { before: DateListItemUI?, after: DateListItemUI? ->
                        val beforeEntry = before as? DateListItemUI.Entry
                        val afterEntry = after as? DateListItemUI.Entry
                        if (afterEntry != null && (beforeEntry == null || beforeEntry.item.date != afterEntry.item.date)) {
                            DateListItemUI.Header(afterEntry.item.date)
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

    fun submitOutcome(
        outcome: OutcomeData,
        onError: (String) -> Unit = {},
        onComplete: (String) -> Unit = {}
    ) {
        val fakeId = "TEMP_${System.currentTimeMillis()}"
        val tempItem = EntryItem(
            id = fakeId,
            name = outcome.purpose,
            date = DateUtil.formatToLongDate(outcome.date, inputFormat = DateUtil.STANDARD_DATE_FORMATED),
            price = outcome.price.toRupiahFormat(),
            remark = outcome.remark,
            paymentStatus = outcome.payment,
            typeCard = com.raylabs.laundryhub.ui.outcome.state.TypeCard.OUTCOME,
            syncStatus = SyncStatus.PENDING,
            rawPayload = outcome
        )

        _uiState.value = _uiState.value.copy(
            optimisticOutcomes = _uiState.value.optimisticOutcomes + tempItem,
            submitNewOutcome = _uiState.value.submitNewOutcome.loading(),
            isSubmitting = true
        )

        viewModelScope.launch {
            when (val result = submitOutcomeUseCase(order = outcome)) {
                is Resource.Success -> {
                    updateOptimisticOutcomeStatus(fakeId, SyncStatus.SYNCED, result.data)
                    _uiState.value = _uiState.value.copy(
                        submitNewOutcome = _uiState.value.submitNewOutcome.success(result.data),
                        isSubmitting = false
                    )
                    onComplete(result.data)
                    loadLastOutcomeId()
                }
                is Resource.Error -> {
                    updateOptimisticOutcomeStatus(fakeId, SyncStatus.FAILED)
                    _uiState.value = _uiState.value.copy(
                        submitNewOutcome = _uiState.value.submitNewOutcome.error(result.message),
                        isSubmitting = false
                    )
                    onError(result.message)
                }
                else -> {
                    updateOptimisticOutcomeStatus(fakeId, SyncStatus.FAILED)
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    onError("Unknown error")
                }
            }
        }
    }

    private fun updateOptimisticOutcomeStatus(fakeId: String, status: SyncStatus, realId: String? = null) {
        val updatedList = _uiState.value.optimisticOutcomes.map {
            if (it.id == fakeId) {
                it.copy(
                    syncStatus = status,
                    id = realId ?: it.id
                )
            } else it
        }
        _uiState.value = _uiState.value.copy(
            optimisticOutcomes = updatedList
        )
    }

    fun removeOptimisticOutcome(fakeId: String) {
        _uiState.value = _uiState.value.copy(
            optimisticOutcomes = _uiState.value.optimisticOutcomes.filter { it.id != fakeId }
        )
    }

    fun retryOptimisticOutcome(fakeId: String, onError: (String) -> Unit = {}, onComplete: (String) -> Unit = {}) {
        val outcomeToRetry = _uiState.value.optimisticOutcomes.find { it.id == fakeId } ?: return
        val payload = outcomeToRetry.rawPayload ?: return
        updateOptimisticOutcomeStatus(fakeId, SyncStatus.PENDING)
        viewModelScope.launch {
            when (val result = submitOutcomeUseCase(order = payload)) {
                is Resource.Success -> {
                    updateOptimisticOutcomeStatus(fakeId, SyncStatus.SYNCED, result.data)
                    onComplete(result.data)
                    loadLastOutcomeId()
                }
                is Resource.Error -> {
                    updateOptimisticOutcomeStatus(fakeId, SyncStatus.FAILED)
                    onError(result.message)
                }
                else -> {
                    updateOptimisticOutcomeStatus(fakeId, SyncStatus.FAILED)
                    onError("Unknown error")
                }
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

    fun updateOutcome(
        outcome: OutcomeData,
        onError: (String) -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        val id = outcome.id
        val tempItem = EntryItem(
            id = id,
            name = outcome.purpose,
            date = DateUtil.formatToLongDate(outcome.date, inputFormat = DateUtil.STANDARD_DATE_FORMATED),
            price = outcome.price.toRupiahFormat(),
            remark = outcome.remark,
            paymentStatus = outcome.payment,
            typeCard = com.raylabs.laundryhub.ui.outcome.state.TypeCard.OUTCOME,
            syncStatus = SyncStatus.PENDING,
            rawPayload = outcome
        )

        _uiState.value = _uiState.value.copy(
            optimisticUpdates = _uiState.value.optimisticUpdates + (id to tempItem),
            updateOutcome = _uiState.value.updateOutcome.loading(),
            isSubmitting = true
        )

        viewModelScope.launch {
            when (val result = updateOutcomeUseCase(order = outcome)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        optimisticUpdates = _uiState.value.optimisticUpdates - id,
                        updateOutcome = _uiState.value.updateOutcome.success(result.data),
                        isSubmitting = false
                    )
                    onComplete()
                }
                is Resource.Error -> {
                    val failedItem = tempItem.copy(syncStatus = SyncStatus.FAILED)
                    _uiState.value = _uiState.value.copy(
                        optimisticUpdates = _uiState.value.optimisticUpdates + (id to failedItem),
                        updateOutcome = _uiState.value.updateOutcome.error(result.message),
                        isSubmitting = false
                    )
                    onError(result.message)
                }
                else -> {
                    val failedItem = tempItem.copy(syncStatus = SyncStatus.FAILED)
                    _uiState.value = _uiState.value.copy(
                        optimisticUpdates = _uiState.value.optimisticUpdates + (id to failedItem),
                        isSubmitting = false
                    )
                    onError("Unknown error")
                }
            }
        }
    }

    fun removeOptimisticUpdate(id: String) {
        _uiState.value = _uiState.value.copy(
            optimisticUpdates = _uiState.value.optimisticUpdates - id
        )
    }

    fun retryOptimisticUpdate(id: String, onError: (String) -> Unit = {}, onComplete: () -> Unit = {}) {
        val updateToRetry = _uiState.value.optimisticUpdates[id] ?: return
        val payload = updateToRetry.rawPayload ?: return
        
        val tempItem = updateToRetry.copy(syncStatus = SyncStatus.PENDING)
        _uiState.value = _uiState.value.copy(
            optimisticUpdates = _uiState.value.optimisticUpdates + (id to tempItem)
        )

        viewModelScope.launch {
            when (val result = updateOutcomeUseCase(order = payload)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        optimisticUpdates = _uiState.value.optimisticUpdates - id
                    )
                    onComplete()
                }
                is Resource.Error -> {
                    val failedItem = tempItem.copy(syncStatus = SyncStatus.FAILED)
                    _uiState.value = _uiState.value.copy(
                        optimisticUpdates = _uiState.value.optimisticUpdates + (id to failedItem)
                    )
                    onError(result.message)
                }
                else -> {
                    val failedItem = tempItem.copy(syncStatus = SyncStatus.FAILED)
                    _uiState.value = _uiState.value.copy(
                        optimisticUpdates = _uiState.value.optimisticUpdates + (id to failedItem)
                    )
                    onError("Unknown error")
                }
            }
        }
    }

    fun deleteOutcome(
        outcomeId: String,
        onError: (String) -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        _uiState.value = _uiState.value.copy(
            deleteOutcome = _uiState.value.deleteOutcome.loading(),
            hiddenOutcomeIds = _uiState.value.hiddenOutcomeIds + outcomeId
        )

        viewModelScope.launch {
            when (val result = deleteOutcomeUseCase(outcomeId = outcomeId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        deleteOutcome = _uiState.value.deleteOutcome.success(result.data)
                    )
                    onComplete()
                    loadLastOutcomeId()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        deleteOutcome = _uiState.value.deleteOutcome.error(result.message),
                        hiddenOutcomeIds = _uiState.value.hiddenOutcomeIds - outcomeId
                    )
                    onError(result.message)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        hiddenOutcomeIds = _uiState.value.hiddenOutcomeIds - outcomeId
                    )
                    onError("Unknown error")
                }
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

    fun buildOutcomeDataForSubmit(): OutcomeData {
        val date =
            _uiState.value.date.ifBlank { DateUtil.getTodayDate(DateUtil.STANDARD_DATE_FORMATED) }

        return OutcomeData(
            id = "",
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

    private suspend fun loadOutcomeList(isSilent: Boolean = false) {
        if (!isSilent) {
            _uiState.value = _uiState.value.copy(
                outcome = _uiState.value.outcome.loading()
            )
        }

        when (val result = readOutcomeUseCase()) {
            is Resource.Success -> {
                val loadedIds = result.data.map { it.id }.toSet()
                _uiState.value = _uiState.value.copy(
                    outcome = _uiState.value.outcome.success(result.data.toDateListUiItems()),
                    hiddenOutcomeIds = _uiState.value.hiddenOutcomeIds - loadedIds
                )
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    outcome = _uiState.value.outcome.error(result.message)
                )
            }

            is Resource.Empty -> {
                _uiState.value = _uiState.value.copy(
                    outcome = _uiState.value.outcome.error("Empty Data")
                )
            }

            else -> Unit
        }
    }
}
