package com.raylabs.laundryhub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.util.DateUtil
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.common.util.error
import com.raylabs.laundryhub.ui.common.util.loading
import com.raylabs.laundryhub.ui.common.util.success
import com.raylabs.laundryhub.ui.home.state.HomeUiState
import com.raylabs.laundryhub.ui.home.state.SortOption
import com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem
import com.raylabs.laundryhub.ui.home.state.toUI
import com.raylabs.laundryhub.ui.home.state.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val summaryUseCase: ReadSpreadsheetDataUseCase,
    private val readIncomeUseCase: ReadIncomeTransactionUseCase,
    private val userUseCase: UserUseCase,
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

    private fun parseDateForSorting(dateString: String?): Date? {
        return dateString?.let { DateUtil.parseDate(it, "dd/MM/yyyy") }
    }

    private fun applySort(orders: List<UnpaidOrderItem>?, sortOption: SortOption): List<UnpaidOrderItem> {
        if (orders.isNullOrEmpty()) return emptyList()

        return when (sortOption) {
            SortOption.ORDER_DATE_DESC -> orders.sortedWith(compareByDescending(nullsLast()) { parseDateForSorting(it.orderDate) })
            SortOption.ORDER_DATE_ASC -> orders.sortedWith(compareBy(nullsFirst()) { parseDateForSorting(it.orderDate) })
            SortOption.DUE_DATE_ASC -> orders.sortedWith(compareBy(nullsFirst()) { parseDateForSorting(it.dueDate) })
            SortOption.DUE_DATE_DESC -> orders.sortedWith(compareByDescending(nullsLast()) { parseDateForSorting(it.dueDate) })
        }
    }

    fun changeSortOrder(newSortOption: SortOption) {
        val currentUnpaidOrderSection = _uiState.value.unpaidOrder
        _uiState.update {
            it.copy(
                currentSortOption = newSortOption,
                unpaidOrder = currentUnpaidOrderSection.copy(isLoading = true, errorMessage = null),
                orderUpdateKey = System.currentTimeMillis()
            )
        }

        val sortedOrders = applySort(currentUnpaidOrderSection.data, newSortOption)

        _uiState.update {
            it.copy(
                unpaidOrder = currentUnpaidOrderSection.success(sortedOrders),
                orderUpdateKey = System.currentTimeMillis()
            )
        }
    }

    suspend fun fetchOrder() {
        _uiState.update {
            it.copy(unpaidOrder = it.unpaidOrder.loading())
        }
        when (val result =
            readIncomeUseCase(filter = FILTER.SHOW_UNPAID_DATA)) {
            is Resource.Success -> {
                val rawUiData = result.data.toUi()
                val sortedUiData = applySort(rawUiData, _uiState.value.currentSortOption)
                _uiState.update {
                    it.copy(
                        unpaidOrder = SectionState(
                            data = sortedUiData, isLoading = false, errorMessage = null
                        ),
                        orderUpdateKey = System.currentTimeMillis()
                    )
                }
            }

            is Resource.Error -> {
                _uiState.update {
                    it.copy(unpaidOrder = it.unpaidOrder.error(result.message))
                }
            }

            is Resource.Empty -> {
                _uiState.update {
                    it.copy(unpaidOrder = it.unpaidOrder.success(emptyList()))
                }
            }

            else -> Unit
        }
    }

    fun refreshAllData() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                // fetchUser() is not suspend, call directly
                fetchUser()
                // Launch suspend functions in parallel
                val jobs = listOf(
                    async { fetchTodayIncome() },
                    async { fetchSummary() },
                    async { fetchOrder() }
                )
                jobs.awaitAll() // Wait for all of them to complete
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }
}