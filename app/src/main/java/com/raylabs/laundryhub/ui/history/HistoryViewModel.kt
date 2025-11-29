package com.raylabs.laundryhub.ui.history

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.history.state.HistoryUiState
import com.raylabs.laundryhub.ui.history.state.toUiItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val readIncomeUseCase: ReadIncomeTransactionUseCase
) : ViewModel() {

    private val _uiState = mutableStateOf(HistoryUiState())
    val uiState: HistoryUiState get() = _uiState.value

    init {
        fetchHistory()
    }

    private fun fetchHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(history = SectionState(isLoading = true))

            when (val result = readIncomeUseCase(filter = FILTER.SHOW_ALL_DATA)) {
                is Resource.Success -> {
                    val mapped = result.data.toUiItems() // Ubah jadi List<HistoryUiItem>
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
}