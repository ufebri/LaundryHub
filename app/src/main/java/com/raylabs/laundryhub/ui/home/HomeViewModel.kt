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

    private var originalUnpaidOrders: List<UnpaidOrderItem> = emptyList()
    private var orderUpdateCounter: Long = 0

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

            is Resource.Empty -> {
                _uiState.update {
                    // Consider success with empty list or specific message for empty state
                    it.copy(todayIncome = it.todayIncome.success(emptyList())) // Or error if preferred
                }
            }

            is Resource.Loading -> {
                _uiState.update {
                    it.copy(todayIncome = it.todayIncome.loading())
                }
            }

            // else -> Unit // Removed to ensure all branches are handled if Resource is sealed with more types
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

            is Resource.Empty -> {
                _uiState.update {
                    // Consider success with empty list or specific message for empty state
                    it.copy(summary = it.summary.success(emptyList())) // Or error if preferred
                }
            }

            is Resource.Loading -> {
                _uiState.update {
                    it.copy(summary = it.summary.loading())
                }
            }
        }
    }

    private fun fetchOrderFromInit() {
        viewModelScope.launch { fetchOrder() }
    }

    suspend fun fetchOrder() {
        _uiState.update {
            it.copy(unpaidOrder = it.unpaidOrder.loading())
        }
        when (val result = readIncomeUseCase(filter = FILTER.SHOW_UNPAID_DATA)) {
            is Resource.Success -> {
                originalUnpaidOrders = result.data.toUi() // Store original list
                updateDisplayedUnpaidOrders()
            }

            is Resource.Error -> {
                originalUnpaidOrders = emptyList()
                _uiState.update {
                    it.copy(unpaidOrder = it.unpaidOrder.error(result.message))
                }
            }

            is Resource.Empty -> {
                originalUnpaidOrders = emptyList()
                _uiState.update {
                    it.copy(unpaidOrder = it.unpaidOrder.success(emptyList()))
                }
            }

            is Resource.Loading -> {
                _uiState.update {
                    it.copy(unpaidOrder = it.unpaidOrder.loading())
                }
            }
        }
    }


    private fun parseDateForSorting(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        val patterns = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd-MM-yyyy HH:mm",
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm"
        )

        for (pattern in patterns) {
            DateUtil.parseDate(dateString, pattern)?.let { return it }
        }
        return null
    }

    private fun applySort(
        orders: List<UnpaidOrderItem>, sortOption: SortOption
    ): List<UnpaidOrderItem> {
        // No need to check for nullOrEmpty here as it's handled by callers or originalUnpaidOrders
        return when (sortOption) {
            SortOption.ORDER_DATE_DESC -> orders.sortedWith(compareByDescending(nullsLast()) {
                parseDateForSorting(
                    it.orderDate
                )
            })

            SortOption.ORDER_DATE_ASC -> orders.sortedWith(compareBy(nullsFirst()) {
                parseDateForSorting(
                    it.orderDate
                )
            })

            SortOption.DUE_DATE_ASC -> orders.sortedWith(compareBy(nullsFirst()) {
                parseDateForSorting(
                    it.dueDate
                )
            })

            SortOption.DUE_DATE_DESC -> orders.sortedWith(compareByDescending(nullsLast()) {
                parseDateForSorting(
                    it.dueDate
                )
            })
        }
    }

    private fun updateDisplayedUnpaidOrders() {
        val currentState = _uiState.value
        val filteredOrders = if (currentState.searchQuery.isBlank()) {
            originalUnpaidOrders
        } else {
            originalUnpaidOrders.filter {
                it.customerName.contains(currentState.searchQuery, ignoreCase = true)
            }
        }
        val sortedAndFilteredOrders = applySort(filteredOrders, currentState.currentSortOption)
        _uiState.update {
            it.copy(
                unpaidOrder = SectionState(
                    data = sortedAndFilteredOrders, isLoading = false, errorMessage = null
                ), orderUpdateKey = ++orderUpdateCounter
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, unpaidOrder = it.unpaidOrder.loading()) }
        updateDisplayedUnpaidOrders()
    }

    fun toggleSearch() {
        val newIsSearchActive = !_uiState.value.isSearchActive
        _uiState.update { it.copy(isSearchActive = newIsSearchActive) }
        if (!newIsSearchActive) {
            // If search is deactivated, clear query and update list
            onSearchQueryChanged("")
        }
    }

    fun changeSortOrder(newSortOption: SortOption) {
        _uiState.update {
            it.copy(
                currentSortOption = newSortOption,
                unpaidOrder = it.unpaidOrder.loading() // Set loading true before re-processing
            )
        }
        updateDisplayedUnpaidOrders() // This will apply new sort and existing search query
    }

    fun refreshAllData() {
        _uiState.update {
            it.copy(
                isRefreshing = true, searchQuery = "", isSearchActive = false
            )
        } // Reset search on refresh
        viewModelScope.launch {
            try {
                fetchUser()
                val jobs = listOf(
                    async { fetchTodayIncome() },
                    async { fetchSummary() },
                    async { fetchOrder() } // fetchOrder will call updateDisplayedUnpaidOrders
                )
                jobs.awaitAll()
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }
}
