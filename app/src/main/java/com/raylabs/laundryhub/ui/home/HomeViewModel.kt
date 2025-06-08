package com.raylabs.laundryhub.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.HistoryFilter
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadOrderStatusUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.toUI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val summaryUseCase: ReadSpreadsheetDataUseCase,
    private val readIncomeUseCase: ReadIncomeTransactionUseCase,
    private val userUseCase: UserUseCase,
    private val readOrderStatusUseCase: ReadOrderStatusUseCase
) : ViewModel() {

    private val _uiState = mutableStateOf(HomeUiState())
    val uiState: HomeUiState get() = _uiState.value

    init {
        fetchUser()
        fetchTodayIncome()
        fetchSummary()
        fetchOrder()
    }

    private fun fetchUser() {
        val user = userUseCase.getCurrentUser()
        _uiState.value = _uiState.value.copy(
            user = SectionState(data = user?.toUI())
        )
    }

    private fun fetchTodayIncome() {
        _uiState.value = _uiState.value.copy(todayIncome = _uiState.value.todayIncome.loading())

        viewModelScope.launch {
            when (val result = readIncomeUseCase(filter = FILTER.TODAY_TRANSACTION_ONLY)) {
                is Resource.Success -> {
                    val uiData = result.data.toUI()
                    _uiState.value = _uiState.value.copy(
                        todayIncome = _uiState.value.todayIncome.success(uiData)
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        todayIncome = _uiState.value.todayIncome.error(result.message)
                    )
                }

                Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        todayIncome = _uiState.value.todayIncome.error("Tidak ada data hari ini")
                    )
                }

                else -> Unit
            }
        }
    }

    private fun fetchSummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(summary = SectionState(isLoading = true))

            when (val result = summaryUseCase()) {
                is Resource.Success -> {
                    val uiData = result.data.toUI() // convert to UI model
                    _uiState.value = _uiState.value.copy(
                        summary = SectionState(data = uiData)
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        summary = SectionState(errorMessage = result.message)
                    )
                }

                Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        summary = SectionState(errorMessage = "Data kosong")
                    )
                }

                is Resource.Loading -> {}
            }
        }
    }

    private fun fetchOrder() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(orderStatus = SectionState(isLoading = true))

            when (val result =
                readOrderStatusUseCase(filterHistory = HistoryFilter.SHOW_UNDONE_ORDER)) {
                is Resource.Success -> {
                    val uiData = result.data.toUI()
                    _uiState.value = _uiState.value.copy(
                        orderStatus = SectionState(data = uiData)
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        orderStatus = SectionState(errorMessage = result.message)
                    )
                }

                is Resource.Empty -> {
                    _uiState.value = _uiState.value.copy(
                        orderStatus = SectionState(errorMessage = "Data Kosong")
                    )
                }

                is Resource.Loading -> {}
            }
        }
    }
}
