package com.raylabs.laundryhub.ui.home

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val summaryUseCase: ReadSpreadsheetDataUseCase,
    private val readIncomeUseCase: ReadIncomeTransactionUseCase,
    private val userUseCase: UserUseCase,
    private val readOrderStatusUseCase: ReadOrderStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        fetchUser()
        fetchTodayIncomeFromInit()
        fetchSummaryFromInit()
        fetchOrderFromInit()
    }

    private fun fetchUser() {
        val user = userUseCase.getCurrentUser()
        _uiState.update {
            it.copy(user = SectionState(data = user?.toUI()))
        }
    }

    private fun fetchTodayIncomeFromInit() {
        viewModelScope.launch { fetchTodayIncome() }
    }

    suspend fun fetchTodayIncome() {
        _uiState.update {
            it.copy(todayIncome = it.todayIncome.loading())
        }

        when (val result = readIncomeUseCase(filter = FILTER.TODAY_TRANSACTION_ONLY)) {
            is Resource.Success -> {
                val uiData = result.data.toUI()
                _uiState.update {
                    it.copy(todayIncome = it.todayIncome.success(uiData))
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(todayIncome = it.todayIncome.error(result.message))
                }
            }

            Resource.Empty -> {
                _uiState.update {
                    it.copy(todayIncome = it.todayIncome.error("Tidak ada data hari ini"))
                }
            }

            else -> Unit
        }
    }

    private fun fetchSummaryFromInit() {
        viewModelScope.launch { fetchSummary() }
    }

    suspend fun fetchSummary() {
        _uiState.update {
            it.copy(summary = SectionState(isLoading = true))
        }
        when (val result = summaryUseCase()) {
            is Resource.Success -> {
                val uiData = result.data.toUI()
                _uiState.update {
                    it.copy(summary = it.summary.success(uiData))
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(summary = it.summary.error(result.message))
                }
            }

            Resource.Empty -> {
                _uiState.update {
                    it.copy(summary = it.summary.error("Data Kosong"))
                }
            }

            else -> Unit
        }
    }

    private fun fetchOrderFromInit() {
        viewModelScope.launch { fetchOrder() }
    }

    suspend fun fetchOrder() {
        _uiState.update {
            it.copy(orderStatus = SectionState(isLoading = true))
        }
        when (val result =
            readOrderStatusUseCase(filterHistory = HistoryFilter.SHOW_UNDONE_ORDER)) {
            is Resource.Success -> {
                val uiData = result.data.toUI()
                _uiState.update {
                    it.copy(
                        orderStatus = SectionState(
                            data = uiData,
                            isLoading = false,
                            errorMessage = null
                        ),
                        orderUpdateKey = System.currentTimeMillis()
                    )
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(orderStatus = it.orderStatus.error(result.message))
                }
            }

            is Resource.Empty -> {
                _uiState.update {
                    it.copy(orderStatus = it.orderStatus.error("Data Kosong"))
                }
            }

            else -> Unit
        }
    }
}