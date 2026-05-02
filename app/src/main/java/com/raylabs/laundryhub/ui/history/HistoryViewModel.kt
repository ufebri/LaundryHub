package com.raylabs.laundryhub.ui.history

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.DeleteOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.history.state.HistoryUiState
import com.raylabs.laundryhub.ui.history.state.toUiItem
import com.raylabs.laundryhub.ui.history.state.toUiItems
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import com.raylabs.laundryhub.ui.outcome.state.EntryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val readIncomeUseCase: ReadIncomeTransactionUseCase,
    private val deleteOrderUseCase: DeleteOrderUseCase
) : ViewModel() {

    val historyPagingData: Flow<PagingData<DateListItemUI>> = 
        readIncomeUseCase.getPagingData(filter = FILTER.SHOW_ALL_DATA)
            .map { pagingData ->
                pagingData.map { DateListItemUI.Entry(it.toUiItem()) }
                    .insertSeparators { before: DateListItemUI.Entry?, after: DateListItemUI.Entry? ->
                        if (after != null && (before == null || before.item.date != after.item.date)) {
                            DateListItemUI.Header(after.item.date)
                        } else {
                            null
                        }
                    }
            }
            .cachedIn(viewModelScope)

    private val _uiState = mutableStateOf(HistoryUiState())
    val uiState: HistoryUiState get() = _uiState.value

    init {
        refreshHistory(isManual = false)
    }

    fun refreshHistory(isManual: Boolean = true) {
        if (isManual) {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
        }
        viewModelScope.launch {
            try {
                loadHistory()
            } finally {
                if (isManual) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            }
        }
    }

    suspend fun deleteOrder(
        orderId: String,
        onComplete: suspend () -> Unit,
        onError: suspend (String) -> Unit = {}
    ) {
        _uiState.value = _uiState.value.copy(
            deleteOrder = _uiState.value.deleteOrder.loading()
        )

        when (val result = deleteOrderUseCase(orderId = orderId)) {
            is Resource.Success -> {
                // For Paging 3, we usually refresh the list or use a list of deleted IDs to filter
                _uiState.value = _uiState.value.copy(
                    deleteOrder = _uiState.value.deleteOrder.success(result.data)
                )
                onComplete()
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    deleteOrder = _uiState.value.deleteOrder.error(result.message)
                )
                onError(result.message)
            }

            else -> Unit
        }
    }

    private suspend fun loadHistory() {
        _uiState.value = _uiState.value.copy(history = SectionState(isLoading = true))

        when (val result = readIncomeUseCase(filter = FILTER.SHOW_ALL_DATA)) {
            is Resource.Success -> {
                val mapped = result.data.toUiItems()
                _uiState.value = _uiState.value.copy(
                    history = SectionState(data = mapped)
                )
            }

            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    history = SectionState(errorMessage = result.message)
                )
            }

            is Resource.Empty -> {
                _uiState.value = _uiState.value.copy(
                    history = SectionState(errorMessage = "Data Kosong")
                )
            }

            else -> Unit
        }
    }
}
