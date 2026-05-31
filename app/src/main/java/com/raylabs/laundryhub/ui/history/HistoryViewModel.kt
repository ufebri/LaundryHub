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
                pagingData.map {
                    val entry: DateListItemUI = DateListItemUI.Entry(it.toUiItem())
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

    private val _uiState = mutableStateOf(HistoryUiState())
    val uiState: HistoryUiState get() = _uiState.value

    fun deleteOrderOptimistic(
        orderId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 1. Instantly hide the item visually for 0ms perceived latency
        _uiState.value = _uiState.value.copy(
            hiddenOrderIds = _uiState.value.hiddenOrderIds + orderId
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                deleteOrder = _uiState.value.deleteOrder.loading()
            )

            when (val result = deleteOrderUseCase(orderId = orderId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        deleteOrder = _uiState.value.deleteOrder.success(result.data)
                    )
                    onSuccess()
                }

                is Resource.Error -> {
                    // 2. Perform visual rollback: make the item reappear if background deletion fails
                    _uiState.value = _uiState.value.copy(
                        deleteOrder = _uiState.value.deleteOrder.error(result.message),
                        hiddenOrderIds = _uiState.value.hiddenOrderIds - orderId
                    )
                    onError(result.message)
                }

                else -> {
                    // Rollback for safety in empty or other unexpected states
                    _uiState.value = _uiState.value.copy(
                        hiddenOrderIds = _uiState.value.hiddenOrderIds - orderId
                    )
                }
            }
        }
    }
}
